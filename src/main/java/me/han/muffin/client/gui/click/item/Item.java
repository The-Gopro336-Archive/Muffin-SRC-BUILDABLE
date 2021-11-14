package me.han.muffin.client.gui.click.item;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.gui.Panel;
import me.han.muffin.client.gui.click.ClickGui;
import me.han.muffin.client.module.modules.other.ClickGUI;
import me.han.muffin.client.utils.color.Colour;
import me.han.muffin.client.utils.color.ColourUtils;

public class Item {
    private final String label;
    protected float x, y;
    protected int width, height;

    public int red;
    public int green;
    public int blue;
    public int textAlpha;
    private boolean hidden;

    public Colour settingEnabledTextColour = new Colour(0, 0,0, 0);
    public Colour settingDisabledTextColour = new Colour(0, 0,0, 0);

    public Item(String label) {
        this.label = label;
    }

    public void setLocation(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    }

    public void processMouseClicked(int mouseX, int mouseY, int mouseButton) {
    }

    public void processMouseReleased(int mouseX, int mouseY, int releaseButton) {
    }

    public void processKeyPressed(char character, int key) {
    }

    public void onGuiClosed() {
    }

    public void update() {
    }

    public void onUpdateScreen() {
    }

    public void onInitGui() {
    }

    public void updatePosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public String getName() {
        return label;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public boolean setHidden(boolean hidden) {
        this.hidden = hidden;
        return this.hidden;
    }

    public void updateColor() {
        red = Muffin.getInstance().getFontManager().getPublicRed();
        green = Muffin.getInstance().getFontManager().getPublicGreen();
        blue = Muffin.getInstance().getFontManager().getPublicBlue();
    }

    public void updateTextColor() {
        settingEnabledTextColour = ClickGUI.INSTANCE.getSettingEnabledTextColour();
        settingDisabledTextColour = ClickGUI.INSTANCE.getSettingDisabledTextColour();
    }

    public int getEnabledSettingTextColour() {
        return ColourUtils.toRGBA(settingEnabledTextColour.getR(), settingEnabledTextColour.getG(), settingEnabledTextColour.getB(), settingEnabledTextColour.getA());
    }

    public int getDisabledSettingTextColour() {
        return ColourUtils.toRGBA(settingDisabledTextColour.getR(), settingDisabledTextColour.getG(), settingDisabledTextColour.getB(), settingDisabledTextColour.getA());
    }

    protected boolean isHovering(int mouseX, int mouseY) {
        for (Panel panel : ClickGui.getClickGui().getPanels()) {
            if (panel.drag) {
                return false;
            }
        }

        return mouseX >= getX() && mouseX < getX() + getWidth() && mouseY >= getY() && mouseY < getY() + height;
    }

}
