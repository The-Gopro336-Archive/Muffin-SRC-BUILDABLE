package me.han.muffin.client.manager.managers

import me.han.muffin.client.friend.Friend

object FriendManager {
    @JvmField val friends = arrayListOf<Friend>()

    @JvmStatic
    fun add(element: Friend) {
        friends.add(element)
    }

    @JvmStatic
    fun remove(element: Friend) {
        friends.remove(element)
    }

    @JvmStatic
    fun getFriendByAliasOrLabel(aliasOrLabel: String): Friend? {
        if (friends.isEmpty()) return null
        return friends.firstOrNull { (name, alias) -> aliasOrLabel.equals(name, ignoreCase = true) || aliasOrLabel.equals(alias, ignoreCase = true) }
    }

    @JvmStatic
    fun isFriend(aliasOrLabel: String): Boolean {
        return friends.any { (name, alias) -> aliasOrLabel.equals(name, ignoreCase = true) || aliasOrLabel.equals(alias, ignoreCase = true) }
    }

}