package me.han.muffin.client.module.modules.other;


import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.client.UpdateEvent;
import me.han.muffin.client.module.Module;
import me.han.muffin.client.utils.math.RandomUtils;
import me.han.muffin.client.utils.timer.Timer;
import me.han.muffin.client.value.EnumValue;
import me.han.muffin.client.value.StringValue;
import me.han.muffin.client.value.Value;
import net.minecraft.client.multiplayer.ServerData;
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener;

public class DiscordPresence extends Module {
    private static final Value<Boolean> customSingleDetails = new Value<>(false, "CustomSingleDetails");
    private static final StringValue<String> singleDetails = new StringValue<>("enjoying muffin alone.", "SingleDetails");
    private static final Value<Boolean> customServerDetails = new Value<>(false, "CustomServerDetails");
    private static final StringValue<String> serverDetails = new StringValue<>("enjoying muffin", "ServerDetails");
    private static final EnumValue<Picture> picture = new EnumValue<>(Picture.Sunglasses, "Picture");

    private enum Picture {
        Old, Lite, Smiling, SmilingBlack, CuteSunglasses, Sunglasses, Smoking, MaBaoGuo, BuJiangWuDe, TaiChi, Loop, MaBaoGuoLoop
    }

    private static final String APP_ID = "767332371170721823";
    public static final DiscordRPC rpc = DiscordRPC.INSTANCE;
    public static DiscordRichPresence presence = new DiscordRichPresence();
    private static String details;
    private static String state;
    private static ServerData svr;
    private static String[] popInfo;
    private static int players2;
    private static int maxPlayers2;
    private final String[] muffinArray = new String[]{"oldmuffin", "litemuffin", "muffin", "muffinsmiling", "muffinsmilingblack", "muffincute", "muffinsmoking"};
    private final String[] maBaoGuoArray = new String[]{"mabaoguo", "bujiangwude"};
  //  private static String[] pictureArray = new String[]{""};4

    private final Timer loopTimer = new Timer();

    public DiscordPresence() {
        super("DiscordRPC", Category.OTHERS, true, "Allow people to see your ingame status on Discord.");
        addSettings(picture);
        picture.setListeners(value -> {
            if (isEnabled() && picture.getValue() != Picture.Loop && picture.getValue() != Picture.MaBaoGuoLoop) {
                presence.largeImageKey = getPictureName();
            }
        });
    }


    private static String getPictureName() {
        String pictureName = "muffin";
        switch (picture.getValue()) {
            case Old:
                pictureName = "oldmuffin";
                break;
            case Lite:
                pictureName = "litemuffin";
                break;
            case Sunglasses:
                pictureName = "muffin";
                break;
            case Smiling:
                pictureName = "muffinsmiling";
                break;
            case SmilingBlack:
                pictureName = "muffinsmilingblack";
                break;
            case CuteSunglasses:
                pictureName = "muffincute";
                break;
            case Smoking:
                pictureName = "muffinsmoking";
                break;
            case MaBaoGuo:
                pictureName = "mabaoguo";
                break;
            case BuJiangWuDe:
                pictureName = "bujiangwude";
                break;
            case TaiChi:
                pictureName = "taichi";
                break;
        }
        return pictureName;
    }

    @Override
    public void onEnable() {
        restartRPC();
    }

    @Override
    public void onDisable() {
        rpc.Discord_Shutdown();
    }

    private int nextIndex = 0;

    @Listener
    private void onPlayerUpdate(UpdateEvent event) {
        if (event.getStage() != EventStageable.EventStage.PRE) return;

        if (fullNullCheck()) return;

        if (picture.getValue() == Picture.Loop || picture.getValue() == Picture.MaBaoGuoLoop) {
            if (loopTimer.passedSeconds(2)) {
                if (picture.getValue() == Picture.Loop) {
                 //   presence.largeImageKey = muffinArray[nextIndex++ % muffinArray.length];
                    presence.largeImageKey = muffinArray[RandomUtils.INSTANCE.getRandom().nextInt(muffinArray.length)];
                } else if (picture.getValue() == Picture.MaBaoGuoLoop) {
                 //   presence.largeImageKey = maBaoGuoArray[nextIndex++ % maBaoGuoArray.length];
                    presence.largeImageKey = maBaoGuoArray[RandomUtils.INSTANCE.getRandom().nextInt(maBaoGuoArray.length)];
                }
                loopTimer.reset();
            }
        }

      //  if (nextIndex > muffinArray.length) nextIndex = 0;

    }

    public static void restartRPC() {
        Muffin.LOGGER.info("Starting Discord RPC");
        DiscordEventHandlers handlers = new DiscordEventHandlers();

        handlers.disconnected = ((var1, var2) -> Muffin.LOGGER.info("Discord RPC disconnected, var1: " + var1 + ", var2: " + var2));
        rpc.Discord_Initialize(APP_ID, handlers, true, "");
        presence.startTimestamp = System.currentTimeMillis() / 1000L;

        presence.details = "choosing muffin.";
        presence.state = "thinking about muffin.";
        presence.largeImageKey = getPictureName();
        presence.largeImageText = Muffin.MODVER;

        rpc.Discord_UpdatePresence(presence);

        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {

                try {
                    rpc.Discord_RunCallbacks();
                    details = "";
                    state = "";

                    if (Globals.mc.isIntegratedServerRunning()) {
                        details = "enjoying muffin alone.";
                    } else if (Globals.mc.getCurrentServerData() != null) {
                        svr = Globals.mc.getCurrentServerData();

                        if (!svr.serverIP.equals("")) {
                            details = "enjoying muffin.";
                            state = svr.serverIP.toLowerCase();

                            if (svr.populationInfo != null) {
                                popInfo = svr.populationInfo.split("/");
                                if (popInfo.length > 2) {
                                    players2 = Integer.parseInt(popInfo[0]);
                                    maxPlayers2 = Integer.parseInt(popInfo[1]);
                                }
                            }

                        }

                    } else {
                        details = "Choosing Muffin";
                        state = "Enjoying Muffin";
                    }

                    if (!details.equals(presence.details) || !state.equals(presence.state))
                        presence.startTimestamp = System.currentTimeMillis() / 1000L;

                    presence.details = details;
                    presence.state = state;
                    rpc.Discord_UpdatePresence(presence);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }, "Discord-RPC-Callback-Handler").start();

        Muffin.LOGGER.info("Discord RPC initialised successfully");
    }

}