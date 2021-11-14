package me.han.muffin.client.mixin.mixins.gui;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.network.ServerEvent;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiIngameMenu.class)
public class MixinGuiIngameMenu extends GuiScreen {

    @Inject(method = "actionPerformed", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/Minecraft;loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;)V"))
    private void onPostLoadWorldAssign(CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new ServerEvent.Disconnect(EventStageable.EventStage.POST, false, this.mc.getCurrentServerData()));
    }

}