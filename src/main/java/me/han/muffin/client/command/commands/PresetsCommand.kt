package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.manager.managers.ModuleManager
import java.util.*

object PresetsCommand: Command(arrayOf("preset", "presets"), Argument("module"), Argument("preset")) {

    override fun dispatch(): String {
        val moduleArgument = getArgument("module")?.value ?: return "Invalid argument."
        val module = ModuleManager.getModule(moduleArgument) ?: return "No such module exists."

        val presetArgument = getArgument("preset")?.value ?: return "Invalid preset argument."

        if (module.presets.isEmpty()) return "That module has no presets."

        val preset = module.getPresetByLabel(presetArgument)
            ?: return StringJoiner(", ").run {
                for (availablePreset in module.presets) add(availablePreset.name)
                "Try: %s.".format(toString())
            }

        preset.onSet()
        return "Loaded &e%s&7 preset for &e%s&7.".format(preset.name, module.name)
    }

}