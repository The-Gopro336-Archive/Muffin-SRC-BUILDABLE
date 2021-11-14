package me.han.muffin.client.profiles

import com.google.gson.*
import me.han.muffin.client.manager.managers.ModuleManager
import me.han.muffin.client.manager.managers.ProfileManager
import me.han.muffin.client.utils.client.BindUtils
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

class Profile(val name: String) {
    var isActive = false
    private val currentProfile = File(ProfileManager.PROFILE_FOLDER, name)

    fun saveAll() {
        if (!ProfileManager.PROFILE_FOLDER.exists()) ProfileManager.PROFILE_FOLDER.mkdir()
        if (!currentProfile.exists()) currentProfile.mkdir()

        val moduleConfigFile = File(currentProfile, "module_configurations.json")
        if (!moduleConfigFile.exists()) moduleConfigFile.createNewFile()

        ModuleManager.modules.forEach { it.saveSettingConfig(currentProfile) }
        saveModuleConfiguration()
    }

    fun loadAll() {
        if (!currentProfile.exists()) return

        try {
            val moduleConfigFile = File(currentProfile, "module_configurations.json")
            if (!moduleConfigFile.exists()) return

            val modDirectory = File(currentProfile, "modules")
            if (!modDirectory.exists()) modDirectory.mkdir()

            ModuleManager.modules.forEach {
                val file = File(modDirectory, it.name.toLowerCase().replace(" ".toRegex(), "") + ".json")
                if (!file.exists()) return@forEach

                val reader = FileReader(file)
                var throwable: Throwable? = null

                try {
                    val node = JsonParser().parse(reader)
                    if (!node.isJsonObject) return@forEach
                    it.loadSettingConfig(currentProfile, node.asJsonObject)
                } catch (node: Throwable) {
                    throwable = node
                    throw node
                } finally {
                    if (throwable != null) {
                        try {
                            reader.close()
                        } catch (e: Throwable) {
                            throwable.addSuppressed(e)
                        }
                    } else {
                        reader.close()
                    }
                }
            }

            loadModuleConfiguration()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveModuleConfiguration() {
        if (!currentProfile.exists()) currentProfile.mkdir()

        val moduleConfigFile = File(currentProfile, "module_configurations.json")
        if (!moduleConfigFile.exists()) moduleConfigFile.createNewFile()

        if (moduleConfigFile.exists()) moduleConfigFile.delete()

        val modules = ModuleManager.modules
        if (modules.isEmpty()) return

        val jsonArray = JsonArray().apply {
            modules.forEach {
                val modObject = JsonObject().apply {
                    addProperty("module-label", it.name)
                    addProperty("module-state", it.isEnabled)
                    addProperty("module-drawn", it.isDrawn)
                    addProperty("module-keybind", BindUtils.getFormattedKeyBind(it.bind))
                }
                add(modObject)
            }
        }

        FileWriter(moduleConfigFile).use {
            it.write(GsonBuilder().setPrettyPrinting().create().toJson(jsonArray))
        }

    }

    private fun loadModuleConfiguration() {
        if (!currentProfile.exists()) return

        val moduleConfigFile = File(currentProfile, "module_configurations.json")
        if (!moduleConfigFile.exists()) return

        var root: JsonElement? = null
        try {
            FileReader(moduleConfigFile).use {
                root = JsonParser().parse(it)
            }
        } catch (e: IOException) {
            return
        }

        if (root == null || root !is JsonArray) return

        val mods = root as JsonArray
        mods.forEach { modNode ->
            if (modNode !is JsonObject) return@forEach

            ModuleManager.modules.forEach {
                if (it.name.equals(modNode["module-label"].asString, ignoreCase = true)) {

                    if (modNode["module-state"].asBoolean) {
                        if (it.isStartup && it.isEnabled) it.enable() else if (!it.isEnabled) it.toggle()
                    } else if (!modNode["module-state"].asBoolean) {
                        if (it.isEnabled) it.toggle()
                    }

                    it.isDrawn = modNode["module-drawn"].asBoolean
                    it.bind = BindUtils.getConvertedKeyBind(modNode["module-keybind"].asString)
                }
            }
        }
    }

}