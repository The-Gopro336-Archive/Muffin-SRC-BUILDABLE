package me.han.muffin.client.manager.managers;

import me.han.muffin.client.gui.font.MinecraftFontRenderer;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class TTFFontManager {

    public static TTFFontManager INSTANCE;

    private final HashMap<String, MinecraftFontRenderer> cFonts = new HashMap<>();

//    private static final InputStream iStreamProduct = TTFFontManager.class.getClassLoader().getResourceAsStream("/assets/minecraft/font/font.ttf");
//    private static final InputStream iStreamRoboto = TTFFontManager.class.getClassLoader().getResourceAsStream("/assets/minecraft/font/Roboto-Regular.ttf");

    // private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
    // Executors.newFixedThreadPool(8);
    // private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);

    private final MinecraftFontRenderer defaultCFont = new MinecraftFontRenderer(new Font("Verdana", Font.PLAIN, 38));

    public TTFFontManager() {
        INSTANCE = this;
        try {
            for (int i = 5; i < 25; ++i) {
                final InputStream iStreamProduct = getClass().getResourceAsStream("/assets/minecraft/font/font.ttf");
                final InputStream iStreamRoboto = getClass().getResourceAsStream("/assets/minecraft/font/Roboto-Regular.ttf");

                int multipliedSize = i * 2;
                Font roboto = Font.createFont(Font.TRUETYPE_FONT, iStreamRoboto).deriveFont(Font.PLAIN, multipliedSize);
                Font productSans = Font.createFont(Font.TRUETYPE_FONT, iStreamProduct).deriveFont(Font.PLAIN, multipliedSize);

                MinecraftFontRenderer segoe = new MinecraftFontRenderer(new Font("Segoe UI", Font.PLAIN, multipliedSize));
                MinecraftFontRenderer productSansFinal = new MinecraftFontRenderer(productSans);
                MinecraftFontRenderer ruboto = new MinecraftFontRenderer(roboto);

                cFonts.put("Segoe " + i, segoe);
                cFonts.put("PSans " + i, productSansFinal);
                cFonts.put("Roboto " + i, ruboto);
            }
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }
    }

    private static Font getFont(String fontName, float size) {
        try {
            InputStream inputStream = TTFFontManager.class.getResourceAsStream("/assets/salhack/fonts/" + fontName);
            Font awtClientFont = Font.createFont(Font.TRUETYPE_FONT, inputStream).deriveFont(Font.PLAIN, size);
            inputStream.close();
            return awtClientFont;
        } catch (Exception e) {
            e.printStackTrace();
            return new Font("default", Font.PLAIN, (int)size);
        }
    }

    public MinecraftFontRenderer getCFont(String key) {
        return cFonts.getOrDefault(key, defaultCFont);
    }

}