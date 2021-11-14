package me.han.muffin.client.gui.hud.item;

import com.google.gson.Gson;
import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.click.ClickGui;
import me.han.muffin.client.manager.managers.HudManager;
import me.han.muffin.client.utils.color.ColourUtils;
import me.han.muffin.client.utils.render.RenderUtils;
import me.han.muffin.client.value.EnumValue;
import me.han.muffin.client.value.Value;
import net.minecraft.client.gui.ScaledResolution;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HudItem {
    public List<Value> valueList = new ArrayList<>();

    private String displayName;
    private int x;
    private int y;
    private final int defaultX;
    private final int defaultY;
    private float width;
    private float height;

    protected float deltaX;
    protected float deltaY;
    protected float clampX;
    protected float clampY;
    private int flags;

    private boolean hidden = true;
    private boolean dragging = false;
    protected int clampLevel = 0;
    protected int side = 0;
    private boolean selected = false;
    private boolean multiSelectedDragging = false;

    HudCategory category;

    public HudItem(String displayName, int x, int y) {
        this.displayName = displayName;
        this.category = HudCategory.Info;
        this.x = x;
        this.y = y;
        this.defaultX = x;
        this.defaultY = y;
    }

    public HudItem(String displayName, HudCategory category, int x, int y) {
        this.displayName = displayName;
        this.category = category;
        this.x = x;
        this.y = y;
        this.defaultX = x;
        this.defaultY = y;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getWidth() {
        return this.width;
    }

    public float getHeight() {
        return this.height;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
        HudManager.getHudManager().saveConfig(this);
    }

    public HudCategory getCategory() {
        return category;
    }

    public void setCategory(HudCategory category) {
        this.category = category;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public void setX(int x) {
        if (this.x == x)
            return;

        this.x = x;

        if (clampLevel == 0)
            HudManager.getHudManager().saveConfig(this);
    }

    public void setY(int y) {
        if (this.y == y)
            return;

        this.y = y;

        if (clampLevel == 0)
            HudManager.getHudManager().saveConfig(this);
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    protected void setClampPosition(float x, float y) {
        this.clampX = x;
        this.clampY = y;
    }

    protected void setClampLevel(int clampLevel) {
        this.clampLevel = clampLevel;
    }

    /// don't override unless you return this
    public boolean doRender(int mouseX, int mouseY, float partialTicks) {
        boolean isHovered = mouseX >= getX() && mouseX < getX() + getWidth() && mouseY >= getY() && mouseY < getY() + getHeight();

        if (isHovered) RenderUtils.drawRect(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x50384244);

        if (!displayName.equalsIgnoreCase("Menu")) {
            int colour = ColourUtils.toRGBA(20, 20, 20, 150);
            if (ClickGui.getClickGui().guiFont != null) {
                ClickGui.getClickGui().guiFont.drawStringWithShadow(displayName, getX(), getY() - 8, ColourUtils.Colors.WHITE);
            } else {
                Globals.mc.fontRenderer.drawStringWithShadow(displayName, getX(), getY() - 8, ColourUtils.Colors.WHITE);
            }
            RenderUtils.drawRect(getX(), getY(), getX() + getWidth(), getY() + getHeight(), colour);
        }

        if (isDragging()) {
            ScaledResolution resolution = new ScaledResolution(Globals.mc);

            float x = mouseX - this.deltaX;
            float y = mouseY - this.deltaY;

            this.setX((int) Math.min(Math.max(0, x), resolution.getScaledWidth() - getWidth()));
            this.setY((int) Math.min(Math.max(0, y), resolution.getScaledHeight() - getHeight()));
        }

        drawScreen(mouseX, mouseY, partialTicks);

        if (isSelected()) {
            RenderUtils.drawRect(
                    getX(), getY(),
                    getX() + getWidth(), getY() + getHeight(),
                    0x35DDDDDD);
        }

        return isHovered;
    }

    /// override for childs
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    }

    public void updateScreen() {
    }

    public void updateTicking() {
    }

    public void processKeyPressed(char character, int key) {
    }

    public boolean onMouseClick(int mouseX, int mouseY, int mouseButton) {
        if (mouseX >= getX() && mouseX < getX() + getWidth() && mouseY >= getY() && mouseY < getY() + getHeight()) {

            if (mouseButton == 0) {
                this.setDragging(true);
                this.deltaX = mouseX - this.getX();
                this.deltaY = mouseY - this.getY();

                HudManager.getHudManager().items.forEach(item -> {
                    if (item.isMultiSelectedDragging()) {
                        item.setDragging(true);
                        item.setDeltaX(mouseX - item.getX());
                        item.setDeltaY(mouseY - item.getY());
                    }
                });

            } else if (mouseButton == 1) {

                ++this.side;

                if (this.side > 3)
                    this.side = 0;

                HudManager.getHudManager().saveConfig(this);
            } else if (mouseButton == Globals.mc.gameSettings.keyBindPickBlock.getKeyCode()) {

                ++this.clampLevel;

                if (this.clampLevel > 2) this.clampLevel = 0;
                this.setClampPosition(this.getX(), this.getY());
                HudManager.getHudManager().saveConfig(this);
            }

            return true;
        }

        return false;
    }



    public void setDeltaX(float x) {
        this.deltaX = x;
    }

    public void setDeltaY(float y) {
        this.deltaY = y;
    }

    public void onMouseRelease(int mouseX, int mouseY, int mouseButton) {
        this.setDragging(false);
    }

    public void loadSettings() {
        File hudDirectory = new File(Muffin.getInstance().getDirectory(), "hud");

        if (!hudDirectory.exists()) {
            hudDirectory.mkdir();
        }

        File file = new File(hudDirectory, getDisplayName().toLowerCase() + ".json");

        if (!file.exists()) {
            return;
        }

        try {
                // create Gson instance
                Gson gson = new Gson();

                // create a reader
                Reader reader = Files.newBufferedReader(Paths.get(hudDirectory + "/" + getDisplayName().toLowerCase() + ".json"));
                // convert JSON file to map
                Map<?, ?> map = gson.fromJson(reader, Map.class);

                if (map == null || map.isEmpty())
                    return;

                // print map entries
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String key = (String) entry.getKey();
                    String val = (String) entry.getValue();

                    if (key.equalsIgnoreCase("displayname")) {
                        this.setDisplayName(val, false);
                        continue;
                    }

                    if (key.equalsIgnoreCase("visible")) {
                        this.setHidden(val.equalsIgnoreCase("false"));
                        continue;
                    }

                    if (key.equalsIgnoreCase("x")) {
                        this.setX(Integer.parseInt(val));
                        continue;
                    }

                    if (key.equalsIgnoreCase("y")) {
                        this.setY(Integer.parseInt(val));
                        continue;
                    }

                    if (key.equalsIgnoreCase("clamplvl")) {
                        this.setClampLevel(Integer.parseInt(val));
                        continue;
                    }

                    if (key.equalsIgnoreCase("clampX")) {
                        this.clampX = Float.parseFloat(val);
                        continue;
                    }

                    if (key.equalsIgnoreCase("clampY")) {
                        this.clampY = Float.parseFloat(val);
                        continue;
                    }

                    if (key.equalsIgnoreCase("side")) {
                        this.side = Integer.parseInt(val);
                        continue;
                    }

                /*
                for (Value value : valueList) {
                    if (value.getAliases()[0].equalsIgnoreCase((String) entry.getKey())) {
                        if (value.getValue() instanceof Number && !(value.getValue() instanceof Enum)) {
                            if (value.getValue() instanceof Integer)
                                value.setValue(Integer.parseInt(val));
                            else if (value.getValue() instanceof Float)
                                value.setValue(Float.parseFloat(val));
                            else if (value.getValue() instanceof Double)
                                value.setValue(Double.parseDouble(val));
                        } else if (value.getValue() instanceof Boolean) {
                            value.setValue(val.equalsIgnoreCase("true"));
                        } else if (value.getValue() instanceof Enum) {
                            ((EnumValue) value).setEnumValue(val);
                        } else if (value.getValue() instanceof String)
                            value.setValue(val);

                        break;
                    }
                }
                 */

                    for (Value value : valueList) {

                        if (value.getAliases()[0].equalsIgnoreCase((String) entry.getKey())) {
                            if (value.getValue() instanceof Boolean) {
                                value.setValue(Boolean.parseBoolean(val));
                            } else if (value.getValue() instanceof Number && !(value.getValue() instanceof Enum)) {
                                if (value.getValue() instanceof Float)
                                    value.setValue(Float.parseFloat(val));
                                else if (value.getValue() instanceof Double)
                                    value.setValue(Double.parseDouble(val));
                                else if (value.getValue() instanceof Long)
                                    value.setValue(Long.parseLong(val));
                                else if (value.getValue() instanceof Integer)
                                    value.setValue(Integer.parseInt(val));
                            } else if (value.getValue() instanceof Enum) {
                                ((EnumValue) value).setEnumValue(val);
                            } else if (value.getValue() instanceof String) {
                                value.setValue(val);
                            }

                        }
                    }

                }

                // close reader
                reader.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public int getSide() {
        return this.side;
    }

    public int getClampLevel() {
        return this.clampLevel;
    }

    public boolean hasFlag(int flags) {
        return (this.flags & flags) != 0;
    }

    public void addFlag(int flags) {
        this.flags |= flags;
    }

    public static int onlyVisibleInHudEditor = 0x1;

    public void resetToDefaultPos() {
        this.setX(this.defaultX);
        this.setY(this.defaultY);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isHovering(float mouseX1, float mouseX2, float mouseY1, float mouseY2) {
        return this.getX() >= mouseX1 && this.getX() + this.getWidth() <= mouseX2 && this.getY() >= mouseY1 && this.getY() + this.getHeight() <= mouseY2;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setMultiSelectedDragging(boolean selected) {
        this.multiSelectedDragging = selected;
    }

    public boolean isMultiSelectedDragging() {
        return this.multiSelectedDragging;
    }

    public void setDisplayName(String displayName, boolean save) {
        this.displayName = displayName;

        if (save)
            HudManager.getHudManager().saveConfig(this);
    }


    public Value getSettingByName(String alias) {
        for (Value property : valueList) {
            for (String propertyAlias : property.getAliases()) {
                if (alias.equalsIgnoreCase(propertyAlias)) {
                    return property;
                }
            }
        }
        return null;
    }

    public void addSettings(Value... properties) {
        Collections.addAll(this.valueList, properties);
        //       this.settingList.sort(Comparator.comparing(p -> p.getAliases()[0]));
    }

    public enum HudCategory {
        Combat("Combat"),
        World("World"),
        Client("Client"),
        Info("Info");

        String name;

        HudCategory(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}