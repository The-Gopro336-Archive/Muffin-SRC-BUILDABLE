package me.han.muffin.client.gui.altmanager;

import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import com.mojang.util.UUIDTypeAdapter;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.events.network.YggAuthEvent;
import me.han.muffin.client.imixin.IMinecraft;
import net.minecraft.util.Session;

import java.net.Proxy;
import java.util.UUID;

public class YggdrasilPayload {

    private static final YggdrasilAuthenticationService yas = new YggdrasilAuthenticationService(Proxy.NO_PROXY, "");
    public static final YggdrasilUserAuthentication sessionAuth = (YggdrasilUserAuthentication) yas.createUserAuthentication(Agent.MINECRAFT);
    private static final YggdrasilMinecraftSessionService ymss = (YggdrasilMinecraftSessionService) yas.createMinecraftSessionService();

    public static int login(String user, String password) {
        if (user == null || user.isEmpty() || password == null || password.isEmpty()) return 1;

        sessionAuth.logOut();
        sessionAuth.setUsername(user);
        sessionAuth.setPassword(password);

        try {
            sessionAuth.logIn();
        } catch (AuthenticationException e) {
            if (e instanceof AuthenticationUnavailableException) return 2; // unavailable
            return 1; // login fail
        }

        String username = sessionAuth.getSelectedProfile().getName();
        String uuid = UUIDTypeAdapter.fromUUID(sessionAuth.getSelectedProfile().getId());
        String access = sessionAuth.getAuthenticatedToken();
        String type = sessionAuth.getUserType().getName();

        ((IMinecraft) Globals.mc).setSession(new Session(username, uuid, access, type));
        return 0;
    }

    public static boolean loginOffline(String username) {
        ((IMinecraft) Globals.mc).setSession(new Session(username, username, "0", "mojang"));
        //YggAuthEvent.INSTANCE.setStatus(YggAuthEvent.AccountStatus.Cracked);
        return true;
    }

    public static boolean isOffline() {
        return Globals.mc.getSession().getUsername().equalsIgnoreCase(Globals.mc.getSession().getPlayerID());
        //return Globals.mc.getSession().getProfile().getId().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + Globals.mc.getSession().getProfile().getName()).getBytes(Charsets.UTF_8)));
    }

    public static boolean sessionValid() {
        try {
            GameProfile gp = Globals.mc.getSession().getProfile();
            String token = Globals.mc.getSession().getToken();
            String id = UUID.randomUUID().toString();
            ymss.joinServer(gp, token, id);
            if (ymss.hasJoinedServer(gp, id, null).isComplete()) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static Session getNewSession(String username, String password) throws AuthenticationException {
        if (username.isEmpty() || password.isEmpty()) {
            return null;
        }
        YggdrasilUserAuthentication yggAuth = (YggdrasilUserAuthentication) new YggdrasilAuthenticationService(Proxy.NO_PROXY, "").createUserAuthentication(Agent.MINECRAFT);
        yggAuth.logOut();
        yggAuth.setUsername(username);
        yggAuth.setPassword(password);
        yggAuth.logIn();

        if (yggAuth.getSelectedProfile() == null) return null;
        return new Session(yggAuth.getSelectedProfile().getName(), yggAuth.getSelectedProfile().getId().toString(), yggAuth.getAuthenticatedToken(), "mojang");
    }

    public void loginToNewSession(final String username, final String password) {
        if (password.isEmpty()) {
            ((IMinecraft) Globals.mc).setSession(new Session(username, username, "0", "mojang"));
            YggAuthEvent.INSTANCE.setStatus(YggAuthEvent.AccountStatus.Cracked);
            return;
        }
        Session session = null;
        try {
            session = getNewSession(username, password);
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
        if (session != null) {
            YggAuthEvent.INSTANCE.setStatus(YggAuthEvent.AccountStatus.Premium);
        }
    }


}