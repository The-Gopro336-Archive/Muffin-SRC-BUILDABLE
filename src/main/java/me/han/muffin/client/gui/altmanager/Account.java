package me.han.muffin.client.gui.altmanager;

import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.utils.network.MojangAccountUtils;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class Account {
    private final String username;
    private final String name, password, uuid;
    private final boolean premium;
    private DynamicTexture texture;
    private ResourceLocation defaultTexture;
    private final String avatarLink = "https://crafatar.com/avatars/";

    public String errorMessage;

    private static Map<String, String> authenticatorStatus;
    private static final String minecraftSession = "session.minecraft.net";
    private static final String minecraftAuthServer = "authserver.mojang.com";

    private static final YggdrasilAuthenticationService yas = new YggdrasilAuthenticationService(Globals.mc.getProxy(), UUID.randomUUID().toString());
    public static final YggdrasilUserAuthentication yua = (YggdrasilUserAuthentication) yas.createUserAuthentication(Agent.MINECRAFT);
    private static final YggdrasilMinecraftSessionService ymss = (YggdrasilMinecraftSessionService) yas.createMinecraftSessionService();

    public boolean isConnected() {
        return Globals.mc.getSession().getProfile().getName().equals(username);
    }

    public Account(String uuid, String name, String password) {
        this.uuid = uuid;
        this.name = name;
        this.password = password;
        this.premium = true;
        errorMessage = "";
        this.username = MojangAccountUtils.getName(uuid);
    }

    public Account(String name, String password) {
        this(getUuid(name, password), name, password);
    }

    public Account(String name) {
        this.premium = false;
        this.name = name;
        this.password = "N/A";
        this.uuid = MojangAccountUtils.getUUID(name);
        errorMessage = "";
        this.username = name;
    }

    private void getImage() {
        DynamicTexture dynamicTexture;
        try {
            dynamicTexture = new DynamicTexture(ImageIO.read(new URL(avatarLink + this.uuid)));
        } catch (IOException e) {
            dynamicTexture = null;
        }

        defaultTexture = AbstractClientPlayer.getLocationSkin(name);
        this.texture = dynamicTexture;
    }

    public String getFileLine() {
        return premium ? name.concat(":").concat(password) : name;
    }

    public String getLabel() {
        return name;
    }

    public String getPassword() throws AccountException {
        if (premium) {
            return password;
        } else {
            throw new AccountException("Non-Premium accounts do not have passwords!");
        }
    }

    public String getUuid() {
        return uuid;
    }

    public boolean isPremium() {
        return premium;
    }

    public String getUsername() {
        return username;
    }

    public void drawHead(double x, double y, double width, double height) {
        if (texture == null) {
            getImage();
            return;
        }

        GL11.glColor4d(1, 1, 1, 1);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();

        // bind head texture
        GlStateManager.bindTexture(texture.getGlTextureId());

        // render head
        GL11.glBegin(GL11.GL_TRIANGLES);{
            GL11.glTexCoord2d(1, 0);
            GL11.glVertex2d(x + width, y);

            GL11.glTexCoord2d(0, 0);
            GL11.glVertex2d(x, y);

            GL11.glTexCoord2d(0, 1);
            GL11.glVertex2d(x, y + height);

            GL11.glTexCoord2d(0, 1);
            GL11.glVertex2d(x, y + height);

            GL11.glTexCoord2d(1, 1);
            GL11.glVertex2d(x + width, y + height);

            GL11.glTexCoord2d(1, 0);
            GL11.glVertex2d(x + width, y);
        }

        GL11.glEnd();
    }

    private static String getUuid(String username, String password) {
        yua.logOut();
        yua.setUsername(username);
        yua.setPassword(password);
        try {
            yua.logIn();
        } catch (AuthenticationException ignored) {
            return null;
        }
        String uuid = yua.getSelectedProfile().getId().toString().replaceAll("-", "");
        yua.logOut();
        return uuid;
    }

    public static UUID getOfflineUUID(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }

}