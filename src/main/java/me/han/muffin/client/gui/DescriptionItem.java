package me.han.muffin.client.gui;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.click.ClickGui;
import me.han.muffin.client.gui.font.MinecraftFontRenderer;
import me.han.muffin.client.utils.color.ColourUtils;
import me.han.muffin.client.utils.render.RenderUtils;

public class DescriptionItem {
    private final String text;
    private final int x;
    private final int y;

    public DescriptionItem(String text, int x, int y) {
        this.text = text;
        this.x = x;
        this.y = y;
    }

    public String getText() {
        return text;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void renderDescription() {
        MinecraftFontRenderer fontRenderer = ClickGui.getClickGui().guiFont;

        int width = Globals.mc.fontRenderer.getStringWidth(text);
        int height = Globals.mc.fontRenderer.FONT_HEIGHT;
        int colour = ColourUtils.toRGBA(20, 20, 20, 130);

        if (fontRenderer == null) {
            RenderUtils.rectangle(x, y, x + width + 5, y + height + 3, colour);
            Globals.mc.fontRenderer.drawStringWithShadow(text, x + 5, y + 3, -1);
            return;
        }

        int cWidth = fontRenderer.getStringWidth(text) + 5;
        int cHeight = fontRenderer.getFontHeight();

        RenderUtils.rectangle(x, y, x + cWidth + 5, y + cHeight + 3, colour);

        fontRenderer.drawStringWithShadow(text, x + 5, y + 3, -1);
    }

    /*
    public void renderDescription() {
        FontManager fontManager = Muffin.getInstance().getFontManager();

        int width = Globals.mc.fontRenderer.getStringWidth(text);
        int height = Globals.mc.fontRenderer.FONT_HEIGHT;

        if (fontManager.isDefault()) {
            RenderUtils.drawRect(x, y, x + width + 5, y + height + 3,
                    ColourUtils.toRGBA(20, 20, 20, 130));

            Globals.mc.fontRenderer.drawStringWithShadow(text, x + 5, y + 3, ColourUtils.Colors.WHITE);

            return;
        }

        width = fontManager.getStringWidth(text);
        height = fontManager.getStringHeight();

        RenderUtils.drawRect(x, y, x + width, y + height + 3,
                ColourUtils.toRGBA(20, 20, 20, 130));

        ClickGui.getClickGui().guiFont.drawStringWithShadow(text, x + 5, y + 3, ColourUtils.Colors.WHITE);
    }
     */

}