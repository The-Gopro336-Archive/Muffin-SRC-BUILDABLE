package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.manager.managers.ProfileManager

object ProfileCommand: Command(arrayOf("profile", "p"), Argument("select/new/delete/save"), Argument("name")) {

    override fun dispatch(): String {
        val actionArgument = getArgument("select/new/delete/save")?.value?.toLowerCase() ?: return "Unsupported action."
        val profileName = getArgument("name")?.value ?: "Invalid arguments."

        when (actionArgument) {
            "select" -> {
                val profile = ProfileManager.getProfileByName(profileName) ?: return "Profile is not valid."
                ProfileManager.setActiveProfile(profile)
                return "Successfully select profile ${profile.name}"
            }
            "new" -> {
                ProfileManager.addProfile(profileName)
                return "Successfully create new profile $profileName"
            }
            "delete" -> {
                ProfileManager.deleteProfile(profileName)
                return "Successfully delete profile $profileName"
            }
            "save" -> {
                ProfileManager.saveProfile(profileName)
                return "Successfully save profile $profileName"
            }
        }
        return "Unsupported action."
    }

}