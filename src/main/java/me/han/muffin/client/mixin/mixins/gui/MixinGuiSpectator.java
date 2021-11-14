package me.han.muffin.client.mixin.mixins.gui;

import me.han.muffin.client.gui.font.AWTFontRenderer;
import net.minecraft.client.gui.GuiSpectator;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiSpectator.class)
public class MixinGuiSpectator {

    @Inject(method = "renderTooltip", at = @At(value = "RETURN"))
    private void onRenderTooltip(ScaledResolution p_175264_1_, float p_175264_2_, CallbackInfo ci) {
        AWTFontRenderer.Companion.garbageCollectionTick();
    }


}