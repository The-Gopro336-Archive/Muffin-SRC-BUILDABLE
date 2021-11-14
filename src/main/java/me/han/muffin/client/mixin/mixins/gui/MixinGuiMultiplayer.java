package me.han.muffin.client.mixin.mixins.gui;

import me.han.muffin.client.utils.network.ServerUtils;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiMultiplayer.class)
public abstract class MixinGuiMultiplayer extends GuiScreen {

    @Shadow private ServerSelectionList serverListSelector;

    @Inject(method = "drawScreen", at = @At(value = "HEAD"))
    private void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        fontRenderer.drawStringWithShadow("Username: " + mc.getSession().getUsername(), 7, 15, 0xFFFFFFFF);
    }

    @Inject(method = "connectToSelected", at = @At(value = "HEAD"))
    private void onConnectToSelected(CallbackInfo ci) {
        ServerUtils.INSTANCE.setServerType(serverListSelector.getSelected() < 0 ? null : serverListSelector.getListEntry(serverListSelector.getSelected()));
    }

}