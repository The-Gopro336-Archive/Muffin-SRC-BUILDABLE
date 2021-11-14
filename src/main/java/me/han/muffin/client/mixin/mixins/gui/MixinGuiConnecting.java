package me.han.muffin.client.mixin.mixins.gui;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.network.ServerEvent;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiConnecting.class)
public class MixinGuiConnecting extends GuiScreen {

    @Shadow
    private NetworkManager networkManager;
/*
    @Inject(method = "connect", at = @At("HEAD"))
    private void headConnect(final String ip, final int port, CallbackInfo callbackInfo) {
        ServerUtils.INSTANCE.setServerData(new ServerData("", ip + ":" + port, false));
    }
 */

    /**
     * @author han
     * @reason idk xD
     */
    @Overwrite
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        this.drawDefaultBackground();
        String ip = "Unknown";

        final ServerData serverData = this.mc.getCurrentServerData();
        if (serverData != null && serverData.serverIP != null) {
            ip = serverData.serverIP;
        }
        if (this.networkManager == null) {
            this.drawCenteredString(this.fontRenderer, "Connecting to " + ip + "...", this.width / 2, this.height / 2 - 50, 16777215);
        } else {
            this.drawCenteredString(this.fontRenderer, "Logging into " + ip + "...", this.width / 2, this.height / 2 - 50, 16777215);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * @See ServerEvent.Connect
     */
    @Inject(method = "connect", at = @At("HEAD"))
    private void onPreConnect(CallbackInfo info) {
        Muffin.getInstance().getEventManager().dispatchEvent(new ServerEvent.Connect(ServerEvent.Connect.State.PRE, this.mc.getCurrentServerData()));
    }

    /**
     * @See ServerEvent.Connect
     */
    @Inject(method = "connect", at = @At(value = "INVOKE", target = "net/minecraft/client/Minecraft.displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V"))
    private void onError(CallbackInfo info) {
        Muffin.getInstance().getEventManager().dispatchEvent(new ServerEvent.Connect(ServerEvent.Connect.State.FAILED, this.mc.getCurrentServerData()));
    }

    /**
     * @See ServerEvent.Connect
     */
    @Inject(method = "connect", at = @At(value = "INVOKE_ASSIGN", target = "net/minecraft/network/NetworkManager.sendPacket(Lnet/minecraft/network/Packet;)V", ordinal = 1))
    private void onSendPacket(CallbackInfo info) {
        Muffin.getInstance().getEventManager().dispatchEvent(new ServerEvent.Connect(ServerEvent.Connect.State.CONNECT, this.mc.getCurrentServerData()));
    }

}
