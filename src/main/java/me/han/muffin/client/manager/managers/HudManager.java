package me.han.muffin.client.manager.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.hud.GuiHud;
import me.han.muffin.client.gui.hud.item.AnchorPoint;
import me.han.muffin.client.gui.hud.item.HudItem;
import me.han.muffin.client.gui.hud.item.component.MenuItem;
import me.han.muffin.client.gui.hud.item.component.client.*;
import me.han.muffin.client.gui.hud.item.component.combat.*;
import me.han.muffin.client.gui.hud.item.component.info.CombatInfoItem;
import me.han.muffin.client.gui.hud.item.component.world.LagNotifierItem;
import me.han.muffin.client.gui.hud.item.component.world.RadarItem;
import me.han.muffin.client.gui.hud.item.component.world.RearViewItem;
import me.han.muffin.client.gui.hud.item.component.world.TooltipItem;
import me.han.muffin.client.value.Value;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class HudManager {

    public List<HudItem> items = new CopyOnWriteArrayList<>();
    private final List<AnchorPoint> anchorPoints = new ArrayList<>();
    private boolean canSave = false;

    public void init() {
        final ScaledResolution res = new ScaledResolution(Globals.mc);

        final AnchorPoint TOP_LEFT = new AnchorPoint(2, 2, AnchorPoint.Point.TOP_LEFT);
        final AnchorPoint TOP_RIGHT = new AnchorPoint(res.getScaledWidth() - 2, 2, AnchorPoint.Point.TOP_RIGHT);
        final AnchorPoint BOTTOM_LEFT = new AnchorPoint(2, res.getScaledHeight() - 2, AnchorPoint.Point.BOTTOM_LEFT);
        final AnchorPoint BOTTOM_RIGHT = new AnchorPoint(res.getScaledWidth() - 2, res.getScaledHeight() - 2, AnchorPoint.Point.BOTTOM_RIGHT);
        final AnchorPoint TOP_CENTER = new AnchorPoint(res.getScaledWidth() / 2, 2, AnchorPoint.Point.TOP_CENTER);
        addAnchor(TOP_LEFT);
        addAnchor(TOP_RIGHT);
        addAnchor(BOTTOM_LEFT);
        addAnchor(BOTTOM_RIGHT);
        addAnchor(TOP_CENTER);

      //  addItem(new HudInfoItem());
        addItem(new CombatInfoItem());
        addItem(new InventoryItem());
        addItem(new RearViewItem());
        addItem(WelcomerItem.INSTANCE);
        addItem(WatermarkItem.INSTANCE);

        addItem(new ArmourItem());
        addItem(PlayerModelItem.INSTANCE);
        addItem(LagNotifierItem.INSTANCE);
        addItem(new NotificationItem());
        addItem(TotemItem.INSTANCE);
        addItem(CrystalItem.INSTANCE);
        addItem(GodAppleItem.INSTANCE);
        addItem(FillEspItem.INSTANCE);
        addItem(IconItem.INSTANCE);
        addItem(ExpItem.INSTANCE);
        addItem(new RadarItem());
        addItem(new TooltipItem());
        addItem(HoleOverlayItem.INSTANCE);
        addItem(ObsidianWarningItem.INSTANCE);
        addItem(SuperWeaponDetectorItem.INSTANCE);

        addItem(new MenuItem());

        items.sort(Comparator.comparing(HudItem::getDisplayName));

        canSave = false;

        items.forEach(HudItem::loadSettings);

        canSave = true;
    }

    public void addItem(HudItem item) {
        items.add(item);
    }

    public void addAnchor(AnchorPoint point) {
        anchorPoints.add(point);
    }

/*
    public void addItem(HudItem item) {
        try {
            for (Field field : item.getClass().getDeclaredFields()) {
                if (Value.class.isAssignableFrom(field.getType())) {
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    final Value val = (Value) field.get(item);
                    item.valueList.add(val);
                }
            }
            items.add(item);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
*/

    public void onTicking() {
        GuiScreen guiScreen = Globals.mc.currentScreen;
        if (guiScreen instanceof GuiHud) return;

        items.forEach(item -> {
            if (!item.isHidden() && !item.hasFlag(HudItem.onlyVisibleInHudEditor)) {
                item.updateTicking();
            }
        });

    }

    public void onRender(float partialTicks) {
        GuiScreen guiScreen = Globals.mc.currentScreen;
        if (guiScreen instanceof GuiHud) return;

        GlStateManager.pushMatrix();

        items.forEach(item -> {
            if (!item.isHidden() && !item.hasFlag(HudItem.onlyVisibleInHudEditor)) {
                item.drawScreen(0, 0, partialTicks);
            }
        });

        final int chatHeight = (Globals.mc.currentScreen instanceof GuiChat) ? 14 : 0;
        final ScaledResolution res = new ScaledResolution(Globals.mc);
        for (AnchorPoint point : anchorPoints) {
            if (point.getPoint() == AnchorPoint.Point.TOP_LEFT) {
                point.setX(2);
                point.setY(2);
            }
            if (point.getPoint() == AnchorPoint.Point.TOP_RIGHT) {
                point.setX(res.getScaledWidth() - 2);
                point.setY(2);
            }
            if (point.getPoint() == AnchorPoint.Point.BOTTOM_LEFT) {
                point.setX(2);
                point.setY(res.getScaledHeight() - chatHeight - 2);
            }
            if (point.getPoint() == AnchorPoint.Point.BOTTOM_RIGHT) {
                point.setX(res.getScaledWidth() - 2);
                point.setY(res.getScaledHeight() - chatHeight - 2);
            }
            if (point.getPoint() == AnchorPoint.Point.TOP_CENTER) {
                point.setX(res.getScaledWidth() / 2);
                point.setY(2);
            }
        }

        GlStateManager.popMatrix();
    }

    public static HudManager getHudManager() {
        return Muffin.getInstance().getHudManager();
    }

    public void saveConfigShutDown() {
        if (!canSave) return;

        File hudDirectory = new File(Muffin.getInstance().getDirectory(), "hud");

        if (!hudDirectory.exists()) hudDirectory.mkdir();

        if (items.isEmpty()) return;

        items.forEach(item -> {

            File hudsFile = new File(hudDirectory, "/" + item.getDisplayName().toLowerCase() + ".json");

            if (hudsFile.exists()) {
                hudsFile.delete();
            } else {
                try {
                    hudsFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                GsonBuilder builder = new GsonBuilder();
                Gson gson = builder.setPrettyPrinting().create();

                Writer writer = Files.newBufferedWriter(Paths.get(hudDirectory + "/" + item.getDisplayName().toLowerCase() + ".json"));
                Map<String, String> map = new HashMap<>();
                map.put("displayname", item.getDisplayName());
                map.put("visible", !item.isHidden() ? "true" : "false");
                map.put("x", String.valueOf(item.getX()));
                map.put("y", String.valueOf(item.getY()));
                map.put("clamplvl", String.valueOf(item.getClampLevel()));
                map.put("clampX", String.valueOf(item.getX()));
                map.put("clampY", String.valueOf(item.getY()));
                map.put("side", String.valueOf(item.getSide()));

                for (Value l_Val : item.valueList) {
                    map.put(l_Val.getAliases()[0], l_Val.getValue().toString());
                }
                gson.toJson(map, writer);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    public void saveConfig(HudItem item) {
        if (!canSave)
            return;

        try {
            File hudDirectory = new File(Muffin.getInstance().getDirectory(), "hud");

            if (!hudDirectory.exists()) {
                hudDirectory.mkdir();
            }

            File hudsFile = new File(hudDirectory.getAbsolutePath());

            if (hudsFile.exists()) {
                hudsFile.delete();
            }

            GsonBuilder builder = new GsonBuilder();

            Gson gson = builder.setPrettyPrinting().create();

            Writer writer = Files.newBufferedWriter(Paths.get(hudDirectory + "/" + item.getDisplayName().toLowerCase() + ".json"));
            Map<String, String> map = new HashMap<>();

            map.put("displayname", item.getDisplayName());
            map.put("visible", !item.isHidden() ? "true" : "false");
            map.put("x", String.valueOf(item.getX()));
            map.put("y", String.valueOf(item.getY()));
            map.put("clamplvl", String.valueOf(item.getClampLevel()));
            map.put("clampX", String.valueOf(item.getX()));
            map.put("clampY", String.valueOf(item.getY()));
            map.put("side", String.valueOf(item.getSide()));

            for (Value value : item.valueList) map.put(value.getAliases()[0], value.getValue().toString());

            gson.toJson(map, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}