package me.han.muffin.client.mixin.mixins.gui;

import me.han.muffin.client.core.mixin.MixinDispatcher;
import me.han.muffin.client.gui.altmanager.GuiAltList;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(value = GuiMainMenu.class)
public class MixinGuiMainMenu extends GuiScreen {
    @Shadow
    private String splashText;

    private int lowestButtonY = 0;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void postConstructor(final CallbackInfo ci) {
        this.splashText = "han is so fucking handsome";
    }

    @Inject(method = "initGui", at = @At(value = "RETURN"))
    public void onInitGui(CallbackInfo ci) {
        this.lowestButtonY = 0;
        for (GuiButton guiButton : buttonList) {
            if (guiButton.y <= lowestButtonY) continue;
            this.lowestButtonY = guiButton.y;
        }

        buttonList.add(new GuiButton(569, this.width / 2 + 104, this.lowestButtonY, 98, 20, "Alt Manager"));
        if (MixinDispatcher.INSTANCE.shouldDisplayCustomMenu()) mc.displayGuiScreen(MixinDispatcher.INSTANCE.getCustomMenu());
    }

    @Inject(method = "drawScreen", at = @At(value = "RETURN"))
    private void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        int y = this.lowestButtonY - 4;
        int x = this.width / 2 + 104 + 47;
    //    mc.getRenderManager().cacheActiveRenderInfo(mc.world, mc.fontRenderer, mc.getRenderViewEntity(), mc.pointedEntity, mc.gameSettings, partialTicks);
    //    GuiInventory.drawEntityOnScreen(width / 16 + 5, height / 16, 10, mouseX, mouseY, (EntityLivingBase) mc.getRenderViewEntity());
        // this.renderStats(width / 16 + 5, height / 16 + 6);
    }

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiMainMenu;drawRect(IIIII)V", ordinal = 0))
    private void skipDrawCopyrightUnderline(int left, int top, int right, int bottom, int color) {
        drawRect(left, top, right, bottom, color);
        fontRenderer.drawStringWithShadow("muffin on top my man", left + right / 2, top + bottom / 2, Color.WHITE.getRGB());
    }

    @Inject(method = "actionPerformed", at= @At(value = "RETURN"))
    protected void onActionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == 569) {
            mc.displayGuiScreen(new GuiAltList((GuiMainMenu) (Object) this));
        }
    }

}