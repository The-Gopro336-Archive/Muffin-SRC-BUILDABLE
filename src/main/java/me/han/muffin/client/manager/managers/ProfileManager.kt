package me.han.muffin.client.manager.managers

import me.han.muffin.client.Muffin
import me.han.muffin.client.profiles.Profile
import java.io.File

object ProfileManager {
    val PROFILE_FOLDER = File(Muffin.getInstance().directory, "profiles")
    val profiles = ArrayList<Profile>()

    init {
        profiles.add(Profile("default"))
    }

    fun loadAll() {
        if (!PROFILE_FOLDER.exists()) PROFILE_FOLDER.mkdir()

        val directories = File(PROFILE_FOLDER.absolutePath).listFiles(File::isDirectory) ?: return

        for (directory in directories) {
            if (directory.name.toString().equals("default", ignoreCase = true)) continue
            val profile = Profile(directory.name.toString())
            profiles.add(profile)
        }

        for (profile in profiles) {
            if (profile.name.equals("default", ignoreCase = true)) setActiveProfile(profile)
        }
    }

    fun saveAll() {
        if (!PROFILE_FOLDER.exists()) PROFILE_FOLDER.mkdir()
        profiles.forEach { if (it.isActive) it.saveAll() }
    }

    fun addProfile(profileName: String) {
        val newProfile = Profile(profileName)

        val findingProfile = getProfileByName(profileName)
        if (findingProfile != null) return

        if (!PROFILE_FOLDER.exists()) PROFILE_FOLDER.mkdir()
        val currentProfile = File(PROFILE_FOLDER, profileName)
        if (!currentProfile.exists()) currentProfile.mkdir()

        val modDirectory = File(currentProfile, "modules")
        if (!modDirectory.exists()) modDirectory.mkdir()

        profiles.add(newProfile)
        newProfile.saveAll()
        setActiveProfile(newProfile)
    }

    fun deleteProfile(profileName: String) {
        val profile = getProfileByName(profileName) ?: return
        if (profile.name.equals("default", ignoreCase = true)) return

        val pendingDeleteFile = File(PROFILE_FOLDER, profileName)
        pendingDeleteFile.delete()
        profiles.remove(profile)
    }

    fun saveProfile(profileName: String) {
        val profile = getProfileByName(profileName) ?: return
        profile.saveAll()
    }

    fun getProfileByName(name: String): Profile? {
        return profiles.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    fun setActiveProfile(profile: Profile) {
        for (p in profiles) p.isActive = false
        profile.isActive = true
        profile.loadAll()
    }

    fun getActiveProfile(): Profile {
        for (p in profiles) if (p.isActive) return p
        return profiles[0]
    }

}