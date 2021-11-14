package me.han.muffin.client.gui.hud.item.component.world;

import com.mojang.realmsclient.gui.ChatFormatting;
import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.hud.item.HudItem;
import me.han.muffin.client.manager.managers.FriendManager;
import me.han.muffin.client.manager.managers.PotionManager;
import me.han.muffin.client.manager.managers.TotemPopManager;
import me.han.muffin.client.value.Value;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class RadarItem extends HudItem {

    private final Value<Boolean> popped = new Value<>(true, "TotemCount");
    private final Value<Boolean> heightDifference = new Value<>(false, "HeightDifference");
    //private final EnumValue<Priority> priority = new EnumValue<>(Priority.Distance, "Priority");

    public RadarItem() {
        super("Radar", HudCategory.World, 90, 300);
        addSettings(popped, heightDifference/*, priority*/);
        Muffin.getInstance().getEventManager().addEventListener(this);
    }

    private enum Priority {
        Health, Distance
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (Globals.mc.player == null || Globals.mc.world == null) {
            return;
        }

        List<EntityPlayer> entityPlayers = Globals.mc.world.playerEntities
                .stream()
                .sorted(Comparator.comparing(entity -> Globals.mc.player.getDistance(entity)))
                .collect(Collectors.toList());

        DecimalFormat dfHealth = new DecimalFormat("#.#");
        dfHealth.setRoundingMode(RoundingMode.CEILING);

        DecimalFormat dfDistance = new DecimalFormat("#.#");
        dfDistance.setRoundingMode(RoundingMode.CEILING);

        StringBuilder healthSB = new StringBuilder();
        StringBuilder distanceSB = new StringBuilder();

        List<Entity> entityList = Globals.mc.world.loadedEntityList
                .stream()
                .filter(entity -> entity instanceof EntityPlayer)
                .sorted(Comparator.comparing(entity -> Globals.mc.player.getDistance(entity)))
                .collect(Collectors.toList());

        Map<String, Integer> players = new HashMap<>();

        for (Entity entity : entityList) {

            if (entity == Globals.mc.player) continue;

            if (!(entity instanceof EntityPlayer)) continue;

            EntityPlayer player = (EntityPlayer) entity;

            String posString;
            if (heightDifference.getValue())
                posString = (player.posY > Globals.mc.player.posY ? ChatFormatting.GREEN + "+" : (player.posY == Globals.mc.player.posY ? " " : ChatFormatting.RED + "-"));
            else posString = "";


            float hpRaw = player.getHealth() + player.getAbsorptionAmount();
            String hp = dfHealth.format(hpRaw);

            if (hpRaw >= 20) {
                healthSB.append(ChatFormatting.GREEN);
            } else if (hpRaw >= 15) {
                healthSB.append(ChatFormatting.YELLOW);
            } else if (hpRaw >= 12) {
                healthSB.append(ChatFormatting.GOLD);
            } else {
                healthSB.append(ChatFormatting.RED);
            }

            healthSB.append(hp);

            int distanceInt = (int) Math.floor(Globals.mc.player.getDistance(player));
            String distance = dfDistance.format(distanceInt);
            if (Globals.mc.player.getDistance(player) <= 11) {
                distanceSB.append(ChatFormatting.RED);
            } else if (Globals.mc.player.getDistance(player) >= 12 && Globals.mc.player.getDistance(player) <= 26) {
                distanceSB.append(ChatFormatting.GOLD);
            } else {
                distanceSB.append(ChatFormatting.GREEN);
            }

            distanceSB.append(distance);

            String playerName;

            if (FriendManager.isFriend(player.getName())) {
                playerName = ChatFormatting.AQUA + player.getName();
            } else {
                playerName = ChatFormatting.RESET + player.getName();
            }

            players.put(
                    posString +
                    healthSB.toString() + " " +
                            playerName + " "
                            + PotionManager.getTextRadarPotion(player)
                            + distanceSB.toString() + " " + ChatFormatting.WHITE +
                            (popped.getValue() ? TotemPopManager.INSTANCE.getTotemPopString(player) : "")

                    , (int) Math.floor(Globals.mc.player.getDistance(player)));

            healthSB.setLength(0);
            distanceSB.setLength(0);

        }

        if (players.isEmpty()) {
            return;
        }

        players = sortByValue(players);

        int y = getY();
        float height = Muffin.getInstance().getFontManager().getStringHeight() * 3;


        for (Map.Entry<String, Integer> player : players.entrySet()) {

            Muffin.getInstance().getFontManager().drawStringWithShadow(
                    ChatFormatting.RESET + player.getKey() + ChatFormatting.RESET, getX(), y);

            y += 12;
            height += Muffin.getInstance().getFontManager().getStringHeight();

            setWidth(Muffin.getInstance().getFontManager().getStringWidth(player.getKey() + " " + player.getValue()) + 5);
            setHeight(height);
        }

        players.clear();

    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

}