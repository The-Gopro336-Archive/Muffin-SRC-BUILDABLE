package me.han.muffin.client.mixin.mixins;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.command.commands.FirstAccountCommand;
import me.han.muffin.client.config.ConfigSaver;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.UpdateFramebufferSizeEvent;
import me.han.muffin.client.event.events.client.*;
import me.han.muffin.client.event.events.gui.GuiScreenEvent;
import me.han.muffin.client.event.events.world.AllowInteractEvent;
import me.han.muffin.client.event.events.world.WorldEvent;
import me.han.muffin.client.imixin.IMinecraft;
import me.han.muffin.client.manager.managers.CommandManager;
import me.han.muffin.client.manager.managers.ModuleManager;
import me.han.muffin.client.module.modules.combat.TriggerModule;
import me.han.muffin.client.module.modules.other.FPSLimitModule;
import me.han.muffin.client.module.modules.other.HudModule;
import me.han.muffin.client.module.modules.other.MainMenuModule;
import me.han.muffin.client.utils.client.BindUtils;
import me.han.muffin.client.utils.render.IconUtils;
import me.han.muffin.client.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.Session;
import net.minecraft.util.Timer;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ScreenshotEvent;
import org.apache.logging.log4j.Logger;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.FutureTask;

@Mixin(value = Minecraft.class, priority = 1)
public abstract class MixinMinecraft implements IMinecraft {

    @Nonnull
    @Override
    @Accessor(value = "timer")
    public abstract Timer getTimer();

    @Override
    @Accessor(value = "rightClickDelayTimer")
    public abstract int getRightClickDelayTimer();

    @Override
    @Accessor(value = "rightClickDelayTimer")
    public abstract void setRightClickDelayTimer(int delay);

    @Override
    @Accessor(value = "renderPartialTicksPaused")
    public abstract float getRenderPartialTicksPaused();

    @Override
    @Accessor(value = "session")
    public abstract void setSession(@Nonnull Session session);

    @Shadow
    public WorldClient world;

    @Shadow
    public EntityPlayerSP player;

    @Shadow public GuiScreen currentScreen;
    @Shadow public GameSettings gameSettings;
    @Shadow private int leftClickCounter;
    @Shadow @Final private Session session;
    @Shadow @Final public Profiler profiler;

    @Shadow @Final private static Logger LOGGER;
    @Shadow public GuiIngame ingameGUI;

    @Shadow @Final private Queue<FutureTask<?>> scheduledTasks;

    @Shadow private static int debugFPS;

    @Shadow private int fpsCounter;

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;checkGLError(Ljava/lang/String;)V", ordinal = 2, shift = At.Shift.AFTER))
    private void onLoadClient(CallbackInfo ci) {
        Muffin.getInstance().init();
        FirstAccountCommand.INSTANCE.setFirstSession(session);
        ClientEvent.FinalLoading event = new ClientEvent.FinalLoading();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }

    @Inject(method = "createDisplay", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V", remap = false, shift = At.Shift.AFTER))
    private void onCreateDisplayTitle(CallbackInfo ci) {
        Display.setTitle("muffin");
    }

    /*
    @Inject(method = "displayGuiScreen", at = @At("HEAD"))
    public void displayGuiScreenClosed(GuiScreen guiScreenIn, CallbackInfo ci) {
        GuiScreenEvent.Closed screenEvent = new GuiScreenEvent.Closed(Globals.mc.currentScreen);
        Muffin.getInstance().getEventManager().dispatchEvent(screenEvent);
    }

    @Inject(method = "displayGuiScreen", at = @At(value = "INVOKE",
    target = "Lnet/minecraftforge/client/event/GuiOpenEvent;<init>(Lnet/minecraft/client/gui/GuiScreen;)V"),
    remap = false)
    public void displayGuiScreenYeets(GuiScreen guiScreenIn, CallbackInfo ci) {
        GuiScreenEvent.Displayed screenEvent = new GuiScreenEvent.Displayed(guiScreenIn);
        Muffin.getInstance().getEventManager().dispatchEvent(screenEvent);
    }
     */

    @ModifyVariable(method = "displayGuiScreen", at = @At("HEAD"))
    private GuiScreen onOpenScreenPre(GuiScreen screen) {
        GuiScreenEvent.Closed closedEvent = new GuiScreenEvent.Closed(currentScreen);
        Muffin.getInstance().getEventManager().dispatchEvent(closedEvent);
        if (closedEvent.isCanceled()) return currentScreen;
        GuiScreenEvent.Displayed displayedEvent = new GuiScreenEvent.Displayed(screen);
        Muffin.getInstance().getEventManager().dispatchEvent(displayedEvent);
        if (displayedEvent.isCanceled()) return currentScreen;
        return displayedEvent.getScreen();
    }

    @Inject(method = "updateFramebufferSize", at = @At("HEAD"))
    private void onUpdateFramebufferSizePre(CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new UpdateFramebufferSizeEvent());
    }

    @Redirect(method = "sendClickBlockToController", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isHandActive()Z"))
    private boolean onIsHandActiveWrapper(EntityPlayerSP playerSP) {
        AllowInteractEvent event = new AllowInteractEvent(playerSP.isHandActive());
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        return event.isUsingItem();
    }

    @Inject(method = "runTickMouse", at = @At(value = "INVOKE_ASSIGN", id = "Lorg/lwjgl/input/Mouse;getEventButton()I", remap = false))
    private void onRunTickMouseClicked(CallbackInfo ci) {
        int button = Mouse.getEventButton();
        if (button >= 0 && BindUtils.INSTANCE.wasButtonPressed(button, Mouse.getEventButtonState())) {
            ModuleManager.onBind(-button);
            MouseEvent event = new MouseEvent(button);
            Muffin.getInstance().getEventManager().dispatchEvent(event);
        }
    }

    @Inject(method = "runTickKeyboard", at = @At(value = "INVOKE_ASSIGN", target = "Lorg/lwjgl/input/Keyboard;getEventKeyState()Z", remap = false))
    private void onRunTickKeyboardPressed(CallbackInfo ci) {
        final int i = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
        if (Keyboard.getEventKeyState()) {
            ModuleManager.onBind(i);
            Muffin.getInstance().getMacroManager().onKeyPress(Keyboard.getKeyName(Keyboard.getEventKey()));
            Muffin.getInstance().getEventManager().dispatchEvent(new KeyPressedEvent(i));
        }
    }

    /*
    @Inject(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;joinPlayerCounter:I", shift = At.Shift.BEFORE))
    private void onTick(final CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new TickEvent());
    }
     */

    @Inject(method = "setWindowIcon", at = @At("HEAD"), cancellable = true)
    private void onSetWindowIconPre(CallbackInfo ci) {
        if (Util.getOSType() != Util.EnumOS.OSX) {
            final ByteBuffer[] icon = IconUtils.getFavicon();
            if (icon != null) {
                Display.setIcon(icon);
                ci.cancel();
            }
        }
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V", ordinal = 0, shift = At.Shift.BEFORE))
    private void onRunTickPre(final CallbackInfo ci) {
        Globals.mc.profiler.startSection("muffinPreTick");
        Muffin.getInstance().getEventManager().dispatchEvent(new TickEvent(EventStageable.EventStage.PRE));
        Globals.mc.profiler.endSection();
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endSection()V", ordinal = 1, shift = At.Shift.AFTER))
    private void onRunTickPost(final CallbackInfo ci) {
        Globals.mc.profiler.startSection("muffinPostTick");
        Muffin.getInstance().getEventManager().dispatchEvent(new TickEvent(EventStageable.EventStage.POST));
        Globals.mc.profiler.endSection();
    }

    @Inject(method = "runGameLoop", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;debugFPS:I", opcode = 179))
    private void onRunGameLoopDebugFpsModified(final CallbackInfo ci) {
        HudModule.updateFpsCounter(fpsCounter);
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void onLoadWorldPre(@Nullable WorldClient worldClientIn, String loadingMessage, CallbackInfo ci) {
        // If the world is null, then it must be unloading
        if (worldClientIn != null) Muffin.getInstance().getEventManager().dispatchEvent(new WorldEvent.Load(worldClientIn));
        else Muffin.getInstance().getEventManager().dispatchEvent(new WorldEvent.Unload());
    }

    private long lastFrame = getTime();

    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void onRunGameLoopPre(final CallbackInfo ci) {
        Globals.mc.profiler.startSection("muffinGameLoop");
        Muffin.getInstance().getEventManager().dispatchEvent(new ClientTickEvent());
        Globals.mc.profiler.endSection();

        final long currentTime = getTime();
        final int deltaTime = (int) (currentTime - lastFrame);
        lastFrame = currentTime;
        RenderUtils.deltaTime = deltaTime;
    }

    @Inject(method = "middleClickMouse", at = @At(value = "HEAD"))
    private void onMiddleClickMousePre(CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new MiddleClickEvent());
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", ordinal = 2, shift = At.Shift.BEFORE))
    private void onCallingRenderTickEventPre(CallbackInfo ci) {
        if (Globals.mc.world != null && Globals.mc.player != null) { // is ingame
            Globals.mc.profiler.startSection("muffinPreRenderLoop");
            Muffin.getInstance().getEventManager().dispatchEvent(new RenderTickEvent(EventStageable.EventStage.PRE));
            Globals.mc.profiler.endSection();
        }
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endSection()V", ordinal = 3, shift = At.Shift.AFTER))
    private void onCallingRenderTickEventPost(CallbackInfo ci) {
        if (Globals.mc.world != null && Globals.mc.player != null) { // is ingame
            Globals.mc.profiler.startSection("muffinPostRenderLoop");
            Muffin.getInstance().getEventManager().dispatchEvent(new RenderTickEvent(EventStageable.EventStage.POST));
            Globals.mc.profiler.endSection();
        }
    }

//    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endSection()V", ordinal = 0, shift = At.Shift.AFTER))
//    public void onRunGameLoop(CallbackInfo ci) {
//        if (Globals.mc.world != null && Globals.mc.player != null) { // is ingame

  //      }
  //  }

    public long getTime() {
        return (Sys.getTime() * 1000) / Sys.getTimerResolution();
    }

    @Inject(method = "processKeyBinds", at = @At("HEAD"))
    public void onPreProcessKeyBinds(CallbackInfo ci) {
        if (Globals.mc.currentScreen == null && Keyboard.getEventCharacter() == CommandManager.prefix.charAt(0)) {
            Globals.mc.displayGuiScreen(new GuiChat(CommandManager.prefix));
        }
    }

    @Redirect(method = "clickMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;attackEntity(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;)V"))
    public void onPreventAttackingRiddenEntity(PlayerControllerMP controller, EntityPlayer attacker, Entity attacked) {
        if (!attacked.isPassenger(attacker)) {
            controller.attackEntity(attacker, attacked);
        }
    }

    /*
    @Inject(method = "sendClickBlockToController", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/RayTraceResult;getBlockPos()Lnet/minecraft/util/math/BlockPos;"))
    private void onClick(boolean leftClick, CallbackInfo ci) {
        ClickBlockEvent event = new ClickBlockEvent(objectMouseOver.getBlockPos(), objectMouseOver.sideHit);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }
     */

    @Inject(method = "displayCrashReport", at = @At(value = "HEAD"))
    public void onDisplayCrashReportPre(CallbackInfo ci) {
        save();
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    public void onShutdownPre(CallbackInfo info) {
        save();
    }

    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    public void onShutdownMcAppPre(CallbackInfo info) {
        save();
    }

    @Inject(method = "getLimitFramerate", at = @At(value = "RETURN"), cancellable = true)
    private void onSetFrameRatePre(CallbackInfoReturnable<Integer> cir) {
        if (world == null) {
            cir.setReturnValue(MainMenuModule.INSTANCE.getFPS().getValue());
        } else if (FPSLimitModule.INSTANCE.isEnabled() && !Display.isActive()) {
            cir.setReturnValue(FPSLimitModule.INSTANCE.getFps());
        }
    }

    @ModifyArg(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;sync(I)V"), index = 0, remap = false)
    public int onRunGameLoopDisplaySync(int maxFps) {
        if (world == null) {
            return MainMenuModule.INSTANCE.getFPS().getValue();
        } else if (FPSLimitModule.INSTANCE.isEnabled()) {
            return FPSLimitModule.INSTANCE.getFps();
        } else {
            return maxFps;
        }
    }

    @Inject(method = "clickMouse", at = @At("HEAD"))
    private void onClickMousePre(CallbackInfo ci) {
        if (TriggerModule.INSTANCE.isEnabled()) leftClickCounter = 0;
    }

    /** @reason Fix GUI logic being included as part of "root.tick.textures" (https://bugs.mojang.com/browse/MC-129556) */
    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", ordinal = 0))
    private void endStartGUISection(Profiler profiler, String name) {
        profiler.endStartSection("gui");
    }

    /** @reason Part 2 of GUI logic fix. */
    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;tick()V", ordinal = 0))
    private void tickTextureManagerWithCorrectProfiler(TextureManager textureManager) {
        profiler.endStartSection("textures");
        textureManager.tick();
        profiler.endStartSection("gui");
    }

    /** @reason Make saving screenshots async (https://bugs.mojang.com/browse/MC-33383) */
    @Redirect(method = "dispatchKeypresses", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ScreenShotHelper;saveScreenshot(Ljava/io/File;IILnet/minecraft/client/shader/Framebuffer;)Lnet/minecraft/util/text/ITextComponent;", ordinal = 0))
    private ITextComponent saveScreenshotAsync(File gameDirectory, int width, int height, Framebuffer buffer) {
        try {
            final BufferedImage screenshot = ScreenShotHelper.createScreenshot(width, height, buffer);

            new Thread(() -> {
                try {
                    File screenshotDir = new File(gameDirectory, "screenshots");
                    screenshotDir.mkdir();
                    File screenshotFile = getTimestampedPNGFileForDirectory(screenshotDir).getCanonicalFile();

                    // Forge event
                    ScreenshotEvent event = ForgeHooksClient.onScreenshot(screenshot, screenshotFile);
                    if (event.isCanceled()) {
                        ingameGUI.getChatGUI().printChatMessage(event.getCancelMessage());
                        return;
                    } else {
                        screenshotFile = event.getScreenshotFile();
                    }

                    ImageIO.write(screenshot, "png", screenshotFile);

                    // Forge event
                    if (event.getResultMessage() != null) {
                        ingameGUI.getChatGUI().printChatMessage(event.getResultMessage());
                        return;
                    }

                    ITextComponent screenshotLink = new TextComponentString(screenshotFile.getName());
                    screenshotLink.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, screenshotFile.getAbsolutePath()));
                    screenshotLink.getStyle().setUnderlined(true);
                    ingameGUI.getChatGUI().printChatMessage(new TextComponentTranslation("screenshot.success", screenshotLink));
                } catch (Exception e) {
                    LOGGER.warn("Couldn't save screenshot", e);
                    ingameGUI.getChatGUI().printChatMessage(new TextComponentTranslation("screenshot.failure", e.getMessage()));
                }
            }, "Screenshot Saving Thread").start();
        } catch (Exception e) {
            LOGGER.warn("Couldn't save screenshot", e);
            ingameGUI.getChatGUI().printChatMessage(new TextComponentTranslation("screenshot.failure", e.getMessage()));
        }

        return null;
    }

    /** @reason Message is sent from screenshot method now. */
    @Redirect(method = "dispatchKeypresses", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;printChatMessage(Lnet/minecraft/util/text/ITextComponent;)V", ordinal = 0))
    private void sendScreenshotMessage(GuiNewChat guiNewChat, ITextComponent chatComponent) {}

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    private static File getTimestampedPNGFileForDirectory(File gameDirectory) {
        String s = DATE_FORMAT.format(new Date()).toString();
        int i = 1;

        while (true) {
            File file1 = new File(gameDirectory, s + (i == 1 ? "" : "_" + i) + ".png");
            if (!file1.exists()) {
                return file1;
            }
            ++i;
        }
    }

    private void save() {
        ConfigSaver.INSTANCE.saveConfig();
    }

}