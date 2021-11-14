package me.han.muffin.client.config

import com.google.gson.*
import me.han.muffin.client.Muffin
import me.han.muffin.client.friend.Friend
import me.han.muffin.client.manager.managers.FriendManager
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

object FriendsConfig {
    val FRIEND_FILE = File(Muffin.getInstance().getDirectory(), "friends.json")

    fun saveFriend() {
        if (FRIEND_FILE.exists()) FRIEND_FILE.delete()

        val friendsArray = JsonArray()

        FriendManager.friends.forEach { (name, alias) ->
            try {
                val properties = JsonObject().apply {
                    addProperty("friend-label", name)
                    addProperty("friend-alias", alias)
                }
                friendsArray.add(properties)
            } catch (ignored: Exception) {
            }
        }

        try {
            FileWriter(FRIEND_FILE).use { it.write(GsonBuilder().setPrettyPrinting().create().toJson(friendsArray)) }
        } catch (ignored: Exception) {
        }
    }

    fun loadFriend() {
        if (!FRIEND_FILE.exists()) FRIEND_FILE.createNewFile()
        if (!FRIEND_FILE.exists()) return

        var root: JsonElement?
        try {
            FileReader(FRIEND_FILE).use { reader -> root = JsonParser().parse(reader) }
        } catch (e: IOException) {
            e.printStackTrace()
            root = null
            return
        }

        if (root == null || root !is JsonArray) return

        val friendsArray = root as JsonArray
        friendsArray.forEach { node: JsonElement? ->
            if (node !is JsonObject) return@forEach

            val friendLabelNode = node["friend-label"] ?: node["name"] ?: node["label"] ?: node["a"] ?: node["auT"]
            val friendLabel = friendLabelNode.asString ?: return@forEach

            val friendAliasNode = node["friend-alias"] ?: node["alias"] ?: node["b"] ?: node["auU"]
            val friendAlias = friendAliasNode.asString ?: return@forEach

            try {
                FriendManager.add(Friend(friendLabel, friendAlias))
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}