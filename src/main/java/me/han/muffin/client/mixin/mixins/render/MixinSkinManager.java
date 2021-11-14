package me.han.muffin.client.mixin.mixins.render;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.module.modules.other.StreamerModeModule;
import net.minecraft.client.resources.SkinManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Mixin(value = SkinManager.class)
public abstract class MixinSkinManager {

    @Inject(method = "loadSkinFromCache", at = @At("HEAD"), cancellable = true)
    private void onInjectSkinProtectPre(GameProfile gameProfile, CallbackInfoReturnable<Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>> cir) {
        if (gameProfile != null && StreamerModeModule.INSTANCE.isEnabled() && StreamerModeModule.INSTANCE.getSkinProtectValue().getValue()) {
            if (StreamerModeModule.INSTANCE.getAllPlayersValue().getValue() || Objects.equals(gameProfile.getId(), Globals.mc.getSession().getProfile().getId())) {
                cir.setReturnValue(new HashMap<>());
                cir.cancel();
            }
        }
    }

}