package me.han.muffin.client.manager.managers;

import com.mojang.realmsclient.gui.ChatFormatting;
import me.han.muffin.client.Muffin;
import me.han.muffin.client.command.Command;
import me.han.muffin.client.command.commands.*;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.network.PacketEvent;
import me.han.muffin.client.module.Module;
import me.han.muffin.client.value.EnumValue;
import me.han.muffin.client.value.NumberValue;
import me.han.muffin.client.value.Value;
import net.minecraft.network.play.client.CPacketChatMessage;
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener;

import java.util.*;

public class CommandManager {
    public static String prefix = ".";
    private final List<Command> commands;

    public CommandManager() {
        commands = new ArrayList<>();

        addCommand(BindCommand.INSTANCE);
        addCommand(DrawnCommand.INSTANCE);
        addCommand(ConfigCommand.INSTANCE);
        addCommand(CommandsCommand.INSTANCE);
        addCommand(new FriendCommand());
        addCommand(new ModuleCommand());
        addCommand(new PeekCommand());
        addCommand(PrefixCommand.INSTANCE);
        addCommand(ReloadSoundCommand.INSTANCE);
        addCommand(new TeleportCommand());
        addCommand(FakePlayerCommand.INSTANCE);
        addCommand(ToggleCommand.INSTANCE);
        addCommand(GrabCommand.INSTANCE);
        addCommand(new PluginsCommand());
        addCommand(FakeNameCommand.INSTANCE);

        addCommand(ConnectCommand.INSTANCE);
        addCommand(DisconnectCommand.INSTANCE);
        addCommand(DamageCommand.INSTANCE);
        addCommand(DupeCommand.INSTANCE);
        addCommand(PresetsCommand.INSTANCE);
        addCommand(LegitCommand.INSTANCE);
        addCommand(OpenFolderCommand.INSTANCE);

        addCommand(RuntimeCommand.INSTANCE);
        addCommand(HClipCommand.INSTANCE);
        addCommand(VClipCommand.INSTANCE);
        addCommand(LiveCommand.INSTANCE);

        addCommand(QueueLookupCommand.INSTANCE);

        addCommand(new InstantFriendCommand.Add());
        addCommand(new InstantFriendCommand.Remove());

        addCommand(new MacroCommand.AddMacro());
        addCommand(new MacroCommand.RemoveMacro());

        addCommand(NonPremiumLoginCommand.INSTANCE);
        addCommand(PremiumLoginCommand.INSTANCE);
        addCommand(FirstAccountCommand.INSTANCE);

        addCommand(ProfileCommand.INSTANCE);
        addCommand(FontsCommand.INSTANCE);
        addCommand(TextureReloadCommand.INSTANCE);

        initSortedList();
        Muffin.getInstance().getEventManager().addEventListener(this);
    }

    private void initSortedList() {
        new Thread(() -> commands.sort(Comparator.comparing(command -> command.getAliases()[0]))).start();
    }

    public void addCommand(Command c) {
        commands.add(c);
    }

    public void callCommand(String command) {
        String[] parts = command.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); // Split by every space if it isn't surrounded by quotes

        String label = parts[0].substring(1);
        String[] args = removeElement(parts, 0);

        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) continue;
            args[i] = strip(args[i], "\"");
        }

        for (Command c : commands) {
            if (c.getAliases()[0].equalsIgnoreCase(label) || c.getAliases()[1].equalsIgnoreCase(label)) {
                c.dispatch(parts);
                return;
            }
        }

        Command.sendChatMessageWithDeletion("Unknown command. try 'commands' for a list of commands.");
    }

    public static String[] removeElement(String[] input, int indexToDelete) {
        List<String> result = new LinkedList<>();

        for (int i = 0; i < input.length; i++) {
            if (i != indexToDelete)
                result.add(input[i]);
        }

        return result.toArray(input);
    }


    private static String strip(String str, String key) {
        if (str.startsWith(key) && str.endsWith(key)) return str.substring(key.length(), str.length() - key.length());
        return str;
    }

    public Command getCommandByLabel(String commandLabel) {
        for (Command c : commands) {
            if (c.getSyntax().equals(commandLabel)) return c;
        }
        return null;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public void setPrefix(String prefix) {
        CommandManager.prefix = prefix;
    }

    private String getFixedValue(Enum enumd) {
        return enumd.name().charAt(0) + enumd.name().toLowerCase().replace(Character.toString(enumd.name().charAt(0)).toLowerCase(), "");
    }

    /**
     * Get command instance by given [name]
     */
    private Command getCommand(String name) {
        for (Command command : commands) {
            for (String alias : command.getAliases()) {
                if (alias.equalsIgnoreCase(name)) return command;
            }
        }
        return null;
    }

    @Listener
    public void onPacketSend(PacketEvent.Send event) {
        if (event.getStage() != EventStageable.EventStage.PRE) return;

        if (event.getPacket() instanceof CPacketChatMessage) {
            CPacketChatMessage packet = (CPacketChatMessage) event.getPacket();
            String message = packet.getMessage().trim();
            if (message.startsWith(prefix)) {
                event.cancel();

                boolean exists = false;

                String[] arguments = message.split(" ");

                if (message.length() < 1) {
                    Command.sendChatMessageWithDeletion("No command was entered.");
                    return;
                }

                String execute = message.contains(" ") ? arguments[0] : message;
                for (Command command : getCommands()) {
                    for (String alias : command.getAliases()) {
                        if (execute.replace(prefix, "").equalsIgnoreCase(alias.replaceAll(" ", ""))) {
                            exists = true;
                            try {
                                Command.sendChatMessageWithDeletion(command.dispatch(arguments));
                            } catch (Exception ignored) {
                                Command.sendChatMessageWithDeletion(
                                        ChatManager.INSTANCE.getDarkTextColour() + prefix +
                                                ChatManager.INSTANCE.getTextColour() + alias +
                                                " " + command.getSyntax()
                                );
                            }
                        }

                    }
                }

                String[] argz = message.split(" ");
                for (Module mod : ModuleManager.modules) {
                    try {
                        if (argz[0].equalsIgnoreCase(prefix + mod.getName().replace(" ", ""))) {
                            exists = true;

                            if (argz.length > 1) {
                                String valueName = argz[1];
                                if (argz[1].equalsIgnoreCase("list")) {
                                    if (!mod.getSettings().isEmpty()) {
                                        StringJoiner stringJoiner = new StringJoiner(", ");
                                        for (Value property : mod.getSettings()) {
                                            stringJoiner.add(
                                                    property.getAliases()[0] +
                                                            " [" + (property.getValue() instanceof Enum ? ((EnumValue) property).getFixedValue() : property.getValue()) +
                                                            "]" + ChatFormatting.GRAY);
                                        }
                                        Command.sendChatMessageWithDeletion(
                                                "Values " + "(" + mod.getSettings().size() + ") " + stringJoiner.toString()
                                        );
                                    } else {
                                        Command.sendChatMessageWithDeletion(
                                                mod.getName() + ChatFormatting.GRAY + " has no values."
                                        );
                                    }
                                } else {
                                    Value property = mod.getSettingByName(valueName);

                                    if (property != null) {
                                        if (property.getValue() instanceof Number) {
                                            if (!argz[2].equalsIgnoreCase("get")) {
                                                ((NumberValue) property).setClamp(false);

                                                if (property.getValue() instanceof Integer) {
                                                    property.setValue(Integer.parseInt(argz[2]));
                                                }

                                                if (property.getValue() instanceof Double) {
                                                    property.setValue(Double.parseDouble(argz[2]));
                                                }

                                                if (property.getValue() instanceof Float) {
                                                    property.setValue(Float.parseFloat(argz[2]));
                                                }

                                                if (property.getValue() instanceof Long) {
                                                    property.setValue(Long.parseLong(argz[2]));
                                                }

                                                Command.sendChatMessageWithDeletion(
                                                        property.getAliases()[0] + ChatFormatting.GRAY + " has been set to " +
                                                                Muffin.getInstance().guiManager.getTextColor() + property.getValue() +
                                                                ChatFormatting.GRAY + " for " + Muffin.getInstance().guiManager.getTextColor() +
                                                                mod.getName()
                                                );
                                            } else {
                                                Command.sendChatMessageWithDeletion(
                                                        property.getAliases()[0] +
                                                                ChatFormatting.GRAY +
                                                                " current value is " +
                                                                Muffin.getInstance().guiManager.getTextColor() +
                                                                property.getValue() + ChatFormatting.GRAY +
                                                                " for " + Muffin.getInstance().guiManager.getTextColor() + mod.getName());
                                            }
                                        } else if (property.getValue() instanceof Enum) {
                                            if (!argz[2].equalsIgnoreCase("list")) {
                                                ((EnumValue) property).setEnumValue(argz[2]);
                                                Command.sendChatMessageWithDeletion(
                                                        property.getAliases()[0] + ChatFormatting.GRAY + " has been set to " +
                                                                Muffin.getInstance().guiManager.getTextColor() + ((EnumValue) property).getFixedValue() +
                                                                ChatFormatting.GRAY + " for " + Muffin.getInstance().guiManager.getTextColor() +
                                                                mod.getName()
                                                );
                                            } else {
                                                StringJoiner stringJoiner = new StringJoiner(", ");
                                                Enum[] array;
                                                for (int length = (array = (Enum[]) ((property).getValue()).getClass().getEnumConstants()).length, i = 0; i < length; i++) {
                                                    stringJoiner.add(
                                                            array[i].name().equalsIgnoreCase(
                                                                    property.getValue().toString()) ? "&a"
                                                                    : "&c" + getFixedValue(array[i])
                                                                    + ChatFormatting.GRAY
                                                    );
                                                }
                                                Command.sendChatMessageWithDeletion(
                                                        "Modes (" + array.length + ") " + stringJoiner.toString()
                                                );
                                            }
                                        } else if (property.getValue() instanceof String) {
                                            property.setValue(argz[2]);
                                            Command.sendChatMessageWithDeletion(
                                                    property.getAliases()[0] + ChatFormatting.GRAY + " has been set to " +
                                                            Muffin.getInstance().guiManager.getTextColor() + property.getValue() +
                                                            ChatFormatting.GRAY + " for " + Muffin.getInstance().guiManager.getTextColor() +
                                                            mod.getName()
                                            );
                                        } else if (property.getValue() instanceof Boolean) {
                                            property.setValue(!(Boolean) property.getValue());
                                            Command.sendChatMessageWithDeletion(
                                                    property.getAliases()[0] + ChatFormatting.GRAY + " has been" +
                                                            ((Boolean) property.getValue() ? ChatFormatting.GREEN + " enabled" : ChatFormatting.RED + " disabled") +
                                                            ChatFormatting.GRAY + " for " + Muffin.getInstance().guiManager.getTextColor() +
                                                            mod.getName()
                                            );
                                        }
                                    }
                                }
                            } else {
                                Command.sendChatMessageWithDeletion(argz[0] + " [list|valuename] [list|get]");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (!exists) {
                    Command.sendChatMessageWithDeletion(ChatFormatting.GRAY + "Invalid command entered.");
                }

            }
        }

    }

}
