package me.han.muffin.client.mixin.mixins.gui;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.events.render.Render2DEvent;
import me.han.muffin.client.gui.font.AWTFontRenderer;
import me.han.muffin.client.utils.render.RenderUtils;
import net.minecraft.client.gui.GuiSubtitleOverlay;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiSubtitleOverlay.class)
public abstract class MixinGuiSubtitleOverlay {

    @Inject(method = "renderSubtitles", at = @At(value = "HEAD"))
    private void onRender(ScaledResolution sr, CallbackInfo ci) {
        Globals.mc.profiler.startSection("muffinRender2D");
        Muffin.getInstance().getEventManager().dispatchEvent(new Render2DEvent(RenderUtils.getRenderPartialTicks(), sr));
        AWTFontRenderer.Companion.garbageCollectionTick();
        Globals.mc.profiler.endSection();
    }

}