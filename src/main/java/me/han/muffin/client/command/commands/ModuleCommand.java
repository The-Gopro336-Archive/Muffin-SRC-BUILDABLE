package me.han.muffin.client.command.commands;

import com.mojang.realmsclient.gui.ChatFormatting;
import me.han.muffin.client.command.Command;
import me.han.muffin.client.manager.managers.ModuleManager;
import me.han.muffin.client.module.Module;

import java.util.List;
import java.util.StringJoiner;

public class ModuleCommand extends Command {

    public ModuleCommand() {
        super(new String[]{"modules", "mods", "ms", "ml", "lm"});
    }

    @Override
    public String dispatch() {
        StringJoiner stringJoiner = new StringJoiner(", ");

        List<Module> modules = ModuleManager.modules;

        modules.forEach(module -> stringJoiner.add((module.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED) + module.getName() + ChatFormatting.GRAY));
        return "Modules (" + ModuleManager.modules.size() + ") " + stringJoiner.toString();
    }

}