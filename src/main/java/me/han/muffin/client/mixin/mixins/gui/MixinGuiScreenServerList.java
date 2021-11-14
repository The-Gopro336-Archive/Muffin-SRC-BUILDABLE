package me.han.muffin.client.mixin.mixins.gui;

import me.han.muffin.client.utils.network.ServerUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreenServerList;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiScreenServerList.class)
public abstract class MixinGuiScreenServerList {

    @Shadow @Final private ServerData serverData;

    @Inject(method = "actionPerformed", at = @At(value = "INVOKE", ordinal = 1, target = "net/minecraft/client/gui/GuiScreen.confirmClicked(ZI)V"))
    private void onActionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == 0) {
            ServerUtils.INSTANCE.setServerData(serverData);
        }
    }

}