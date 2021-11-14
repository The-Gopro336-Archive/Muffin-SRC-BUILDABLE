package me.han.muffin.client.command.commands;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.command.Argument;
import me.han.muffin.client.command.Command;

import java.util.Locale;

public class MacroCommand {

    public static class AddMacro extends Command {
        public AddMacro() {
            super(new String[]{"macroa", "madd"}, new Argument("macro"), new Argument("input"));
        }

        @Override
        public String dispatch() {
            Argument macrosKeyArgument = getArgument("macro");
            Argument inputsArgument = getArgument("input");

            if (macrosKeyArgument == null) return "Invalid macro argument.";
            if (inputsArgument == null) return "Invalid input argument.";

            String macroKey = macrosKeyArgument.getValue();
            String input = inputsArgument.getValue();

            if (macroKey.length() > 0 && input.length() > 0) {
                Muffin.getInstance().getMacroManager().addMacro(macroKey.toUpperCase(), input);
                return ("Added a macro with the keybind " + macroKey.toUpperCase());
            }

            return null;
        }
    }

    public static class RemoveMacro extends Command {
        public RemoveMacro() {
            super(new String[]{"macrod", "mdel"}, new Argument("macro"));
        }

        @Override
        public String dispatch() {
            Argument macroArgument = getArgument("macro");
            if (macroArgument == null) return "Invalid macro,";

            String macrosKey = macroArgument.getValue().toUpperCase(Locale.ROOT);
            Muffin.getInstance().getMacroManager().removeMacro(macrosKey);
            return "Removed a macro with the keybind " + macrosKey;
        }

    }


}