package me.han.muffin.client.mixin.mixins.authlib;

import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import me.han.muffin.client.event.events.network.YggAuthEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.MalformedURLException;
import java.net.URL;

@Mixin(value = YggdrasilUserAuthentication.class, remap = false)
public abstract class MixinYggdrasilUserAuthentication {
    @Shadow
    @Final
    private static URL ROUTE_AUTHENTICATE;
    @Shadow
    @Final
    private static URL ROUTE_REFRESH;
    @Shadow
    @Final
    private static URL ROUTE_VALIDATE;

    private URL getNewURL(URL url) throws MalformedURLException {
        return YggAuthEvent.INSTANCE.getStatus().equals(YggAuthEvent.AccountStatus.Cracked) ? new URL("http://authserver.thealtening.com" + url.getFile()) : url;
    }

    @Redirect(method = "logInWithPassword", at = @At(value="FIELD", target="com/mojang/authlib/yggdrasil/YggdrasilUserAuthentication.ROUTE_AUTHENTICATE:Ljava/net/URL;"))
    private URL onLoginWithPassword() throws MalformedURLException {
        return getNewURL(ROUTE_AUTHENTICATE);
    }

    @Redirect(method = "logInWithToken", at = @At(value = "FIELD", target = "com/mojang/authlib/yggdrasil/YggdrasilUserAuthentication.ROUTE_REFRESH:Ljava/net/URL;"))
    private URL onLoginWithToken() throws MalformedURLException {
        return getNewURL(ROUTE_REFRESH);
    }

    @Redirect(method = "selectGameProfile", at = @At(value = "FIELD", target = "com/mojang/authlib/yggdrasil/YggdrasilUserAuthentication.ROUTE_REFRESH:Ljava/net/URL;"))
    private URL onSelectGameProfile() throws MalformedURLException {
        return getNewURL(ROUTE_REFRESH);
    }

    @Redirect(method = "checkTokenValidity", at = @At(value = "FIELD", target = "com/mojang/authlib/yggdrasil/YggdrasilUserAuthentication.ROUTE_VALIDATE:Ljava/net/URL;"))
    private URL onCheckTokenValidity() throws MalformedURLException {
        return getNewURL(ROUTE_VALIDATE);
    }

}