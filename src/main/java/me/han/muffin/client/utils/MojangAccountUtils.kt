package me.han.muffin.client.utils

import me.han.muffin.client.utils.extensions.kotlin.synchronized
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL

object MojangAccountUtils {
    private val playerCache = LinkedHashMap<String, String>().synchronized()

    fun getUUID(name: String): String? {
        if (playerCache.containsValue(name)) return playerCache.getValue(name)

        try {
            val url = URL("https://api.mojang.com/users/profiles/minecraft/$name")
            val jsonObject = JSONObject(JSONTokener(InputStreamReader(url.openStream())))
            val uuid = jsonObject["id"].toString()
            playerCache[uuid] = name
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

}