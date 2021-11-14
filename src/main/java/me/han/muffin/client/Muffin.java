package me.han.muffin.client;

import me.han.muffin.client.config.*;
import me.han.muffin.client.macro.MacroManager;
import me.han.muffin.client.manager.managers.*;
import me.han.muffin.client.module.modules.misc.AnnouncerModule;
import me.han.muffin.client.utils.client.TrayUtils;
import me.han.muffin.client.utils.render.BlockRenderer;
import me.han.muffin.client.utils.render.IconUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;
import team.stiff.pomelo.EventManager;
import team.stiff.pomelo.impl.annotated.AnnotatedEventManager;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Muffin {
    public static final String MODID = "muffin";
    public static final String MODNAME = "Muffin";
    public static final String MODVER = "0.10.4";

    public static final Logger LOGGER = LogManager.getLogger("Muffin");
    public static String DIRECTORY_PATH = "Muffin";

    public static final ResourceLocation ICON = new ResourceLocation("textures/muffin/icon.png");

    public long startTime;

    private EventManager eventManager;
    private static final Muffin INSTANCE = new Muffin();

    public static Thread MAIN_THREAD;
    private ExecutorService asyncExecutorService;
    private ExecutorService pooledExecutorService;

    public GuiManager guiManager;
    public CommandManager commandManager;
    public File directory;
    private AltManagerConfig altManagerConfig;
    private FontManager fontManager;
    private TTFFontManager ttfFontManager;
    private BlockRenderer blockRenderer;
    private HudManager hudManager;
    private MacroManager macroManager;

    private TrayUtils trayUtils;

    public volatile boolean hadRespondedFromServer = false;

    private class Test extends Thread {
        @Override
        public void run() {
            super.run();
            final String s3 = "java.lang.Shutdown";
            try {
                final Method declaredMethod3 = Class.forName(s3).getDeclaredMethod("exit", Integer.TYPE);
                declaredMethod3.setAccessible(true);
                declaredMethod3.invoke(null, 0);
            } catch (Exception ex5) {
                throw new NullPointerException("Failed to load! Please post on the forums with the error code \"0x17E49\" to get help.");
            }
        }
    }

    public void init() {
        MAIN_THREAD = Thread.currentThread();
        asyncExecutorService = Executors.newSingleThreadExecutor();
        pooledExecutorService = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));

/*
        try {
            Field serverValid = Class.forName("me.han.muffin.loader.connection.ServerThread").getDeclaredField("isValidUser");
            serverValid.setAccessible(true);
            if (serverValid.get(null) != "betul") {
                final Method shutdownMethod = Class.forName("java.lang.Shutdown").getDeclaredMethod("exit", Integer.TYPE);
                shutdownMethod.setAccessible(true);
                shutdownMethod.invoke(null, 0);
            }
        } catch (NoSuchFieldException | ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new Crasher("Stop trying to crack da client.");
        }
 */

        startTime = System.nanoTime() / 1000000L;

//        final boolean hasLoaderRan;
//        try {
//            final Class<?> loaderMixinClass = Class.forName("me.han.muffin.loader.mixin.MixinLoader");
//            final Field devCheckField = loaderMixinClass.getDeclaredField("loggedToken");
//            devCheckField.setAccessible(true);
//            hasLoaderRan = devCheckField.getBoolean(loaderMixinClass);
//        } catch (NoSuchFieldException | ClassNotFoundException | IllegalAccessException e) {
//            throw new Crasher("Stop trying to crack da client.");
//        }

//        if (hasLoaderRan) {
//            try {
//                final Field mixinUUIDCache = Class.forName("me.han.muffin.loader.mixin.MixinLoader").getDeclaredField("tokens");
//                mixinUUIDCache.setAccessible(true);
//                final Method isEmptyMethod = List.class.getDeclaredMethod("isEmpty");
//                final Object checkIsEmpty = isEmptyMethod.invoke(mixinUUIDCache);
//                if (checkIsEmpty instanceof Boolean) {
//                    final Method shutdownMethod = Class.forName("java.lang.Shutdown").getDeclaredMethod("exit", Integer.TYPE);
//                    shutdownMethod.setAccessible(true);
//                    shutdownMethod.invoke(null, 0);
//                    throw new Crasher("Stop trying to crack da client.");
//                }
//            } catch (NoSuchFieldException | ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
//                throw new Crasher("Stop trying to crack da client.");
//            }
//        }

        eventManager = new AnnotatedEventManager();

        directory = new File(System.getProperty("user.home"), DIRECTORY_PATH);
        if (!this.directory.exists()) LOGGER.info(String.format("%s client directory.", this.directory.mkdir() ? "Created" : "Failed to create"));

        Runtime.getRuntime().addShutdownHook(new ConfigSaver.RuntimeSaver());

        ChatManager.INSTANCE.initListener();

        this.ttfFontManager = new TTFFontManager();
        this.guiManager = new GuiManager();

        this.blockRenderer = new BlockRenderer();
        this.fontManager = new FontManager();
        this.commandManager = new CommandManager();

        this.hudManager = new HudManager();
        this.hudManager.init();
        this.macroManager = new MacroManager();
        this.macroManager.initialize();

        JoinLeaveManager.INSTANCE.addListener();
        CommandPrefixConfig.INSTANCE.loadCommandPrefix();

        FriendsConfig.INSTANCE.loadFriend();
        ProfileManager.INSTANCE.loadAll();

        this.altManagerConfig = new AltManagerConfig();
        CustomFontConfig.INSTANCE.loadCustomFont();

        TotemPopManager.INSTANCE.initListener();
        setMuffinIcon();

        HoleManager.INSTANCE.initListener();
        SpeedManager.INSTANCE.initListener();

        if (AnnouncerModule.INSTANCE.isEnabled() && AnnouncerModule.INSTANCE.getQueueLogging().getValue()) BBTTQueueManager.INSTANCE.init();
    }

    private void setMuffinIcon() {
        Display.setTitle(Muffin.MODID);
        if (Util.getOSType() != Util.EnumOS.OSX) {
            final ByteBuffer[] icon = IconUtils.getFavicon();
            if (icon != null) {
                Display.setIcon(icon);
            }
        }
    }

    public static Muffin getInstance() {
        return INSTANCE;
    }

    public EventManager getEventManager() {
        if (this.eventManager == null) this.eventManager = new AnnotatedEventManager();
        return this.eventManager;
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public BlockRenderer getBlockRenderer() {
        return blockRenderer;
    }

    public TTFFontManager getTtfFontManager() {
        return ttfFontManager;
    }

    public FontManager getFontManager() {
        return fontManager;
    }

    public File getDirectory() {
        return directory;
    }

    public AltManagerConfig getAltManagerConfig() {
        return altManagerConfig;
    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    public MacroManager getMacroManager() {
        return macroManager;
    }

    public ExecutorService getAsyncExecutorService() {
        return asyncExecutorService;
    }

    public ExecutorService getPooledExecutorService() {
        return pooledExecutorService;
    }

    public TrayUtils getTrayUtils() {
        return trayUtils;
    }

    public void setTrayUtils(TrayUtils trayUtils) {
        this.trayUtils = trayUtils;
    }

}