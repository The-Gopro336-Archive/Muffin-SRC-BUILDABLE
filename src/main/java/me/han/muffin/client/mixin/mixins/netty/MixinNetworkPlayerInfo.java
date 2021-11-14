package me.han.muffin.client.mixin.mixins.netty;

import com.mojang.authlib.GameProfile;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.module.modules.other.StreamerModeModule;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(value = NetworkPlayerInfo.class)
public abstract class MixinNetworkPlayerInfo {
    @Shadow @Final private GameProfile gameProfile;

    @Inject(method = "getLocationSkin", at = @At("HEAD"), cancellable = true)
    private void onInjectSkinProtectPre(CallbackInfoReturnable<ResourceLocation> cir) {
        if (StreamerModeModule.INSTANCE.isEnabled() && StreamerModeModule.INSTANCE.getSkinProtectValue().getValue()) {
            if (StreamerModeModule.INSTANCE.getAllPlayersValue().getValue() || Objects.equals(gameProfile.getId(), Globals.mc.getSession().getProfile().getId())) {
                cir.setReturnValue(DefaultPlayerSkin.getDefaultSkin(this.gameProfile.getId()));
                cir.cancel();
            }
        }
    }

}