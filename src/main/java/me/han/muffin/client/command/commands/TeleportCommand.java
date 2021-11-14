package me.han.muffin.client.command.commands;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.command.Argument;
import me.han.muffin.client.command.Command;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.manager.managers.ChatManager;
import net.minecraft.util.math.Vec3d;

import java.text.DecimalFormat;

public class TeleportCommand extends Command {

    DecimalFormat df = new DecimalFormat("#.###");
    private long lastTp;
    private Vec3d lastPos;
    public static Vec3d finalPos;
    public static double blocksPerTeleport;
    boolean disable = false;

    public TeleportCommand() {
        super(new String[]{"tp", "teleport"}, new Argument("x"), new Argument("y"), new Argument("z"), new Argument("blocks"));
    }

    public void teleport() {
        Vec3d tpDirectionVec = finalPos.subtract(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ).normalize();

        if (Globals.mc.world.isBlockLoaded(Globals.mc.player.getPosition())) {
            lastPos = new Vec3d(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ);
            if (finalPos.distanceTo(new Vec3d(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ)) < 0.3 || blocksPerTeleport == 0) {
                ChatManager.sendMessage("You Had Arrived!");
                disable = true;
            } else {
                Globals.mc.player.setVelocity(0, 0, 0);
            }

            if (disable) return;

            if (finalPos.distanceTo(new Vec3d(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ)) >= blocksPerTeleport) {
                final Vec3d vec = tpDirectionVec.scale(blocksPerTeleport);
                Globals.mc.player.setPosition(Globals.mc.player.posX + vec.x, Globals.mc.player.posY + vec.y, Globals.mc.player.posZ + vec.z);
            } else {
                final Vec3d vec = tpDirectionVec.scale(finalPos.distanceTo(new Vec3d(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ)));
                Globals.mc.player.setPosition(Globals.mc.player.posX + vec.x, Globals.mc.player.posY + vec.y, Globals.mc.player.posZ + vec.z);
                disable = true;
            }

            if (disable) return;
            lastTp = System.currentTimeMillis();
        } else if (lastTp + 2000L > System.currentTimeMillis()) {
            Globals.mc.player.setPosition(lastPos.x, lastPos.y, lastPos.z);
        }
    }

    @Override
    public String dispatch() {
        if (getArgument("x") == null || getArgument("y") == null || getArgument("z") == null) {
            return "Enter full coordinate that you want to teleport";
        }

        Argument xArgument = getArgument("x");
        if (xArgument == null) return "Invalid argument.";

        Argument yArgument = getArgument("y");
        if (yArgument == null) return "Invalid argument.";

        Argument zArgument = getArgument("z");
        if (zArgument == null) return "Invalid argument.";

        Argument blocksArgument = getArgument("blocks");
        if (blocksArgument == null) return "Invalid argument.";

        String posX = xArgument.getValue();
        String posY = yArgument.getValue();
        String posZ = zArgument.getValue();
        String blocks = blocksArgument.getValue();

        if (blocks.equalsIgnoreCase("ez")) {
            blocksPerTeleport = Double.MAX_VALUE;
        } else {
            blocksPerTeleport = Double.parseDouble(blocks);
        }

        try {
            final double x = posX.equals("~") ? Globals.mc.player.posX : posX.charAt(0) == '~' ? Double.parseDouble(posX.substring(1)) + Globals.mc.player.posX : Double.parseDouble(posX);
            final double y = posY.equals("~") ? Globals.mc.player.posY : posY.charAt(0) == '~' ? Double.parseDouble(posY.substring(1)) + Globals.mc.player.posY : Double.parseDouble(posY);
            final double z = posZ.equals("~") ? Globals.mc.player.posZ : posZ.charAt(0) == '~' ? Double.parseDouble(posZ.substring(1)) + Globals.mc.player.posZ : Double.parseDouble(posZ);
            finalPos = new Vec3d(x, y, z);
            disable = false;
            teleport();

        } catch (NullPointerException ignored) {}

        return (Muffin.getInstance().guiManager.getTextColor() + "Teleport to X: " + df.format(posX) + " Y: " + df.format(posY) + "Z: " + df.format(posZ) + df.format(blocksPerTeleport));
    }

}