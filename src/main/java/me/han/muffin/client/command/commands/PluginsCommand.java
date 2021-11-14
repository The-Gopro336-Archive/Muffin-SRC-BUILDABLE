package me.han.muffin.client.command.commands;

import com.mojang.realmsclient.gui.ChatFormatting;
import me.han.muffin.client.Muffin;
import me.han.muffin.client.command.Command;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.network.PacketEvent;
import me.han.muffin.client.manager.managers.ChatManager;
import me.han.muffin.client.utils.timer.Timer;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.network.play.server.SPacketTabComplete;
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener;

public class PluginsCommand extends Command {
    private final Timer timer = new Timer();

    public PluginsCommand() {
        super(new String[]{"plugin", "plugins"});
    }

    @Override
    public String dispatch() {
        timer.reset();
        Muffin.getInstance().getEventManager().addEventListener(this);
        Globals.mc.player.connection.sendPacket(new CPacketTabComplete("/", null, false));
        return "Discovering plugins.";
    }

    @Listener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.getStage() != EventStageable.EventStage.PRE)
            return;

        if (Globals.mc.player == null || Globals.mc.world == null)
            return;

        if (event.getPacket() instanceof SPacketTabComplete) {
            SPacketTabComplete packet = (SPacketTabComplete) event.getPacket();
            String[] commands = packet.getMatches();
            StringBuilder message = new StringBuilder();
            int size = 0;

            for (String command : commands) {
                String pluginName = command.split(":")[0].substring(1);

                if (!message.toString().contains(pluginName) && command.contains(":") && !pluginName.equalsIgnoreCase("minecraft") && !pluginName.equalsIgnoreCase("bukkit")) {
                    size++;
                    if (message.length() == 0) {
                        message.append(pluginName);
                    } else {
                        message.append("\2478, \247a").append(pluginName);
                    }
                }
            }

            if (message.length() > 0) {
                ChatManager.sendMessage("\2477Plugins (\247f" + size + "\2477): \247a " + message + "\2477.");
            } else {
                ChatManager.sendMessage("No plugins found.");
            }

            Muffin.getInstance().getEventManager().removeEventListener(this);
            event.cancel();
        }

        if (timer.passed(20000)) {
            Muffin.getInstance().getEventManager().removeEventListener(this);
            ChatManager.sendMessage(ChatFormatting.GRAY + "No plugins found.");
        }

    }

}