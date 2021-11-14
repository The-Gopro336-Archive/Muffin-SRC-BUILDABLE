package me.han.muffin.client.mixin.mixins.gui;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.network.ServerEvent;
import me.han.muffin.client.module.modules.misc.AutoReconnectModule;
import me.han.muffin.client.utils.network.ServerUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.multiplayer.GuiConnecting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(value = GuiDisconnected.class)
public abstract class MixinGuiDisconnected extends MixinGuiScreen {
    @Shadow private int textHeight;

    @Shadow protected abstract void actionPerformed(GuiButton button) throws IOException;

    /**
     * ClientBase 1.12.2 API
     * Thanks to Brady!
     * @See ServerEvent.Disconnect
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new ServerEvent.Disconnect(EventStageable.EventStage.PRE, true, Globals.mc.getCurrentServerData()));
    }

    @Inject(method = "initGui", at = @At("RETURN"))
    private void onInitGui(CallbackInfo ci) {
        buttonList.add(new GuiButton(269, width / 2 - 100, height / 2 + textHeight / 2 + fontRenderer.FONT_HEIGHT + 72, "Reconnect"));
        buttonList.add(new GuiButton(281, width / 2 - 100, height / 2 + textHeight / 2 + fontRenderer.FONT_HEIGHT + 96, "AutoReconnect"));
        if (AutoReconnectModule.INSTANCE.isEnabled()) {
            ServerUtils.seconds = Math.round(AutoReconnectModule.INSTANCE.getDelay().getValue() * 20.0f);
        }
    }

    @Inject(method = "actionPerformed", at = @At("RETURN"))
    private void onActionPerformed(GuiButton button, CallbackInfo info) {
        if (button.id == 269) {
            if (ServerUtils.INSTANCE.getServerData() != null)
                Globals.mc.displayGuiScreen(new GuiConnecting((GuiDisconnected) (Object) this, Globals.mc, ServerUtils.INSTANCE.getServerData()));
        } else if (button.id == 281) {
            AutoReconnectModule.INSTANCE.toggle();
            if (AutoReconnectModule.INSTANCE.isEnabled()) ServerUtils.seconds = Math.round(AutoReconnectModule.INSTANCE.getDelay().getValue() * 20.0f);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (buttonList.size() > 1) {
            if (AutoReconnectModule.INSTANCE.isEnabled()) {
                buttonList.get(2).displayString = "AutoReconnect (" + (ServerUtils.seconds / 20 + 1) + ")";
                if (ServerUtils.seconds > 0) {
                    --ServerUtils.seconds;
                } else {
                    try {
                        actionPerformed(buttonList.get(1));
                    } catch (IOException ignored) {}
                }
            } else {
                buttonList.get(2).displayString = "AutoReconnect";
            }
        }
    }

}