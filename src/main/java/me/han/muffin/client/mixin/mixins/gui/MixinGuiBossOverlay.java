package me.han.muffin.client.mixin.mixins.gui;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.render.overlay.RenderGuiBossOverlayEvent;
import me.han.muffin.client.utils.BossInfoCounted;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.BossInfoClient;
import net.minecraft.client.gui.GuiBossOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.play.server.SPacketUpdateBossInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.BossInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(value = GuiBossOverlay.class)
public abstract class MixinGuiBossOverlay {
    private List<BossInfoCounted> counted_cache;
    private ResourceLocation GUI_BARS_TEXTURES_ALT;

    @Shadow @Final private Map<UUID, BossInfoClient> mapBossInfos;
    @Shadow @Final private Minecraft client;
    @Shadow protected abstract void render(int x, int y, BossInfo info);

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void onPostInit(Minecraft clientIn, CallbackInfo ci) {
        counted_cache = new ArrayList<>();
        GUI_BARS_TEXTURES_ALT = new ResourceLocation("textures/gui/bars.png");
    }

    @Inject(method = "renderBossHealth", at = @At("HEAD"), cancellable = true)
    public void onRenderBossHealthPre(CallbackInfo ci) {
        RenderGuiBossOverlayEvent event = new RenderGuiBossOverlayEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            ci.cancel();
            return;
        }

        if (!this.mapBossInfos.isEmpty()) {
            ScaledResolution scaledresolution = new ScaledResolution(this.client);
            int i = scaledresolution.getScaledWidth();
            int j = 12;
            for (BossInfoCounted counted : this.counted_cache) {
                int k = i / 2 - 91;
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.client.getTextureManager().bindTexture(GUI_BARS_TEXTURES_ALT);
                BossInfoClient info = counted.getInfo();
                this.render(k, j, info);
                String s = info.getName().getFormattedText() + (counted.count > 1 ? " (x" + counted.count + ')' : "");
                this.client.fontRenderer.drawStringWithShadow(s, (float) (i / 2 - this.client.fontRenderer.getStringWidth(s) / 2), (float) (j - 9), 16777215);
                j += 10 + this.client.fontRenderer.FONT_HEIGHT;
                if (j >= scaledresolution.getScaledHeight() / 3) break;
            }
            ci.cancel();
        }
    }

    @Inject(method = "read", at = @At("HEAD"))
    public void onReadingPre(SPacketUpdateBossInfo packetIn, CallbackInfo ci) {
        doReadingData();
    }

    private void doReadingData() {
        this.counted_cache.clear();
        ArrayList<String> known = new ArrayList<>();
        for (BossInfoClient infoLerping : this.mapBossInfos.values()) {
            if (known.contains(infoLerping.getName().getFormattedText())) continue;
            String formattedText = infoLerping.getName().getFormattedText();
            BossInfoCounted counted = new BossInfoCounted(infoLerping);
            for (BossInfoClient infoLerping2 : this.mapBossInfos.values()) {
                if (infoLerping2.getName().getFormattedText().equals(formattedText)) {
                    counted.count++;
                }
            }
            known.add(formattedText);
            this.counted_cache.add(counted);
        }
    }

}