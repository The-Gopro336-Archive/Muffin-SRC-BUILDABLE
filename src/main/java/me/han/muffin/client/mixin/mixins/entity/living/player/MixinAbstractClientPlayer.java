package me.han.muffin.client.mixin.mixins.entity.living.player;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.module.modules.other.StreamerModeModule;
import me.han.muffin.client.module.modules.player.FreecamModule;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(value = AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer extends MixinEntityPlayer {

    @Inject(method = "getLocationSkin()Lnet/minecraft/util/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void onGetSkinPre(CallbackInfoReturnable<ResourceLocation> ciR) {
        if (StreamerModeModule.INSTANCE.isEnabled() && StreamerModeModule.INSTANCE.getSkinProtectValue().getValue()) {
            if (!StreamerModeModule.INSTANCE.getAllPlayersValue().getValue() && !Objects.equals(getGameProfile().getName(), Globals.mc.player.getGameProfile().getName())) return;
            ciR.setReturnValue(DefaultPlayerSkin.getDefaultSkin(getUniqueID()));
        }
    }

    @Inject(method = "isSpectator", at = @At(value = "HEAD"), cancellable = true)
    private void onMarkFreecamSpectatorPre(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamModule.INSTANCE.isEnabled() && FreecamModule.INSTANCE.getCameraGuy() != null && ((AbstractClientPlayer) (Object) this).getEntityId() == -69) {
            cir.setReturnValue(true);
        }
    }

    /*
    @Inject(method = "getLocationCape", at = @At("HEAD"), cancellable = true)
    private void getCape(CallbackInfoReturnable<ResourceLocation> callbackInfoReturnable) {
        if(!CapeAPI.INSTANCE.hasCapeService())
            return;

        if (capeInfo == null)
            capeInfo = CapeAPI.INSTANCE.loadCape(getUniqueID());

        if(capeInfo != null && capeInfo.isCapeAvailable())
            callbackInfoReturnable.setReturnValue(capeInfo.getResourceLocation());
    }

    @Inject(method = "getFovModifier", at = @At("HEAD"), cancellable = true)
    private void getFovModifier(CallbackInfoReturnable<Float> cir) {
        final NoFOV fovModule = (NoFOV) LiquidBounce.moduleManager.getModule(NoFOV.class);

        if (fovModule.getState()) {
            float newFOV = fovModule.getFovValue().get();

            if (!this.isUsingItem()) {
                cir.setReturnValue(newFOV);
                return;
            }

            if (this.getItemInUse().getItem() != Items.bow) {
                cir.setReturnValue(newFOV);
                return;
            }

            int i = this.getItemInUseDuration();
            float f1 = (float) i / 20.0f;
            f1 = f1 > 1.0f ? 1.0f : f1 * f1;
            newFOV *= 1.0f - f1 * 0.15f;
            cir.setReturnValue(newFOV);
        }
    }
     */

}