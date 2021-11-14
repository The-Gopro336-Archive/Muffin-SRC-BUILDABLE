package me.han.muffin.client.mixin.mixins.authlib;

import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import me.han.muffin.client.event.events.network.YggAuthEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.MalformedURLException;
import java.net.URL;

@Mixin(value = YggdrasilMinecraftSessionService.class, remap = false)
public abstract class MixinYggdrasilMinecraftSessionService {
    @Shadow
    @Final
    private static URL JOIN_URL;

    @Redirect(method = "joinServer", at = @At(value = "FIELD", target = "Lcom/mojang/authlib/yggdrasil/YggdrasilMinecraftSessionService;JOIN_URL:Ljava/net/URL;"))
    private URL onJoinServer() throws MalformedURLException {
        return YggAuthEvent.INSTANCE.getStatus().equals(YggAuthEvent.AccountStatus.Cracked) ? new URL("http://sessionserver.thealtening.com" + JOIN_URL.getFile()) : JOIN_URL;
    }

}