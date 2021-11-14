package me.han.muffin.client.manager.managers;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.font.MinecraftFontRenderer;
import me.han.muffin.client.gui.font.util.Opacity;
import me.han.muffin.client.module.modules.other.ColorControl;
import me.han.muffin.client.module.modules.other.FontsModule;
import me.han.muffin.client.utils.color.ColourUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private MinecraftFontRenderer font = Muffin.getInstance().getTtfFontManager().getCFont("PSans 19");

    private MinecraftFontRenderer guiFont = Muffin.getInstance().getTtfFontManager().getCFont("PSans 14");
    private MinecraftFontRenderer tabCustomFont = Muffin.getInstance().getTtfFontManager().getCFont("PSans 14");

    private float h;

    private int color;

    private final Opacity hue = new Opacity(0);
    private float width;

    private int publicRed;
    private int publicGreen;
    private int publicBlue;

    public static Map<String, MinecraftFontRenderer> systemFontCache = new HashMap<>();

    public String systemFont = null;

    public void drawString(String text, int x, int y) {
        if (font != null) {
            font.drawString(text, x, y, color);
        } else {
            Globals.mc.fontRenderer.drawString(text, x, y, color);
        }
        h += 20 - width;
    }

    public void drawString(String text, int x, int y, int color) {
        if (font != null) {
            font.drawString(text, x, y, color);
        } else {
            Globals.mc.fontRenderer.drawString(text, x, y, color);
        }
    }

    public void drawStringWithShadow(String text, int x, int y) {
        if (font != null) {
            font.drawStringWithShadow(text, x, y, color);
        } else {
            Globals.mc.fontRenderer.drawStringWithShadow(text, x, y, color);
        }
        h += 20 - width;
    }

    public float drawStringWithShadow(String text, float x, float y, int color) {
        if (font != null) {
            return font.drawStringWithShadow(text, x, y, color);
        }

        return Globals.mc.fontRenderer.drawStringWithShadow(text, x, y, color);
    }

    public void drawGrayStringWithShadow(String text, int x, int y) {
        if (font != null) {
            font.drawStringWithShadow(text, x, y, ColourUtils.toRGBA(160, 160, 160, 255));
        } else {
            Globals.mc.fontRenderer.drawStringWithShadow(text, x, y, ColourUtils.toRGBA(185, 185, 185, 255));
        }
    }

    public void drawStringWithShadowCentered(String msg, float x, float y, int color) {
        float offsetX = getStringWidth(msg) / 2f;
        float offsetY = getStringHeight() / 2f;
        drawStringWithShadow(msg, x - offsetX, y - offsetY, color);
    }

    public int getStringWidth(String text) {
        if (font != null) {
            return font.getStringWidth(text);
        }
        return Globals.mc.fontRenderer.getStringWidth(text);
    }

    public int getStringHeight() {
        if (font != null) {
            return font.getHeight();
        }

        return Globals.mc.fontRenderer.FONT_HEIGHT;
    }

    public void setColour() {
        h = hue.getOpacity();
        float speed = ColorControl.INSTANCE.rainbowSpeed.getValue();
        float brightness = ColorControl.INSTANCE.rainbowBrightness.getValue();

        hue.interp(256, speed - 1);

        if (hue.getOpacity() > 255) {
            hue.setOpacity(0);
        }

        width = ColorControl.INSTANCE.rainbowWidth.getValue();

        if (ColorControl.INSTANCE.moduleListMode.getValue() == ColorControl.ModuleListMode.RAINBOW) {
            if (h > 255) h = 0;
            final Color rainB = Color.getHSBColor(h / 255.0f, brightness, 1.0f);
            color = rainB.getRGB();

            publicRed = rainB.getRed();
            publicGreen = rainB.getGreen();
            publicBlue = rainB.getBlue();

        } else if (ColorControl.INSTANCE.moduleListMode.getValue() == ColorControl.ModuleListMode.STATIC) {

            publicRed = Muffin.getInstance().guiManager.getModuleListRed();
            publicGreen = Muffin.getInstance().guiManager.getModuleListGreen();
            publicBlue = Muffin.getInstance().guiManager.getModuleListBlue();

            color = ColourUtils.toRGBA(publicRed, publicGreen, publicBlue, 255);
        }
    }

    public void setFont(FontsModule.FontType font, int guiFontSize) {
        if (systemFont == null) {
            if (font == FontsModule.FontType.Roboto) {
                this.font = Muffin.getInstance().getTtfFontManager().getCFont("Roboto 19");
                guiFont = Muffin.getInstance().getTtfFontManager().getCFont("Roboto " + guiFontSize);
                tabCustomFont = Muffin.getInstance().getTtfFontManager().getCFont("Roboto 17");
            } else if (font == FontsModule.FontType.ProductSans) {
                this.font = Muffin.getInstance().getTtfFontManager().getCFont("PSans 19");
                guiFont = Muffin.getInstance().getTtfFontManager().getCFont("PSans " + guiFontSize);
                tabCustomFont = Muffin.getInstance().getTtfFontManager().getCFont("PSans 17");
            } else if (font == FontsModule.FontType.Segoe) {
                this.font = Muffin.getInstance().getTtfFontManager().getCFont("Segoe 19");
                guiFont = Muffin.getInstance().getTtfFontManager().getCFont("Segoe " + guiFontSize);
                tabCustomFont = Muffin.getInstance().getTtfFontManager().getCFont("Segoe 17");
            } else if (font == FontsModule.FontType.Default) {
                this.font = null;
                guiFont = null;
                tabCustomFont = null;
            }
        } else {
            setSystemFont(guiFontSize);
        }
    }

    public void setSystemFont(int size) {
        if (systemFont == null) return;

        final int currentSize = size * 2;

        final MinecraftFontRenderer current38 = getNewFont(systemFont, 38);
        final MinecraftFontRenderer currentCustom = getNewFont(systemFont, currentSize);
        final MinecraftFontRenderer current34 = getNewFont(systemFont, 34);

        this.font = current38;
        guiFont = currentCustom;
        tabCustomFont = current34;
    }

    private MinecraftFontRenderer getNewFont(String systemFont, int size) {
        final String keyFormat = systemFont + " " + size;

        final MinecraftFontRenderer cacheIn = systemFontCache.get(keyFormat);
        if (cacheIn != null) return cacheIn;

        final MinecraftFontRenderer newFont = new MinecraftFontRenderer(new Font(systemFont, Font.PLAIN, size));
        systemFontCache.put(keyFormat, newFont);

        return newFont;
    }

    public void setFontStyle(MinecraftFontRenderer.FontStyle style) {
        if (font != null && guiFont != null && tabCustomFont != null) {
            this.font.setFontStyle(style);
            guiFont.setFontStyle(style);
            tabCustomFont.setFontStyle(style);
        }
    }

    public String getCorrectFont(FontsModule.FontType font) {
        String fonts = "default";

        if (font == FontsModule.FontType.Roboto) {
            fonts = "Roboto";
        } else if (font == FontsModule.FontType.ProductSans) {
            fonts = "PSans";
        } else if (font == FontsModule.FontType.Segoe) {
            fonts = "Segoe";
        }

        return fonts;
    }

    public int getPublicRed() {
        return publicRed;
    }

    public int getPublicGreen() {
        return publicGreen;
    }

    public int getPublicBlue() {
        return publicBlue;
    }

    public int getColor() {
        return color;
    }

    public MinecraftFontRenderer getGuiFont() {
        return guiFont;
    }

    public MinecraftFontRenderer getTabCustomFont() {
        return tabCustomFont;
    }

    public MinecraftFontRenderer getFont() {
        return font;
    }

}