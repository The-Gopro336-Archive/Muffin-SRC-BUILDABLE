package me.han.muffin.client.utils.color;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.manager.managers.FriendManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;

import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;

/**
 * Java Code to get a color name from rgb/hex value/awt color
 * <p>
 * The part of looking up a color name from the rgb values is edited from
 * https://gist.github.com/nightlark/6482130#file-gistfile1-java (that has some errors) by Ryan Mast (nightlark)
 *
 * @author Xiaoxiao Li
 */
public class ColourUtils {

    /**
     * Initialize the color list that we have.
     */
    private ArrayList<ColorName> initColorList() {
        ArrayList<ColorName> colorList = new ArrayList<ColorName>();
        colorList.add(new ColorName("AliceBlue", 0xF0, 0xF8, 0xFF));
        colorList.add(new ColorName("AntiqueWhite", 0xFA, 0xEB, 0xD7));
        colorList.add(new ColorName("Aqua", 0x00, 0xFF, 0xFF));
        colorList.add(new ColorName("Aquamarine", 0x7F, 0xFF, 0xD4));
        colorList.add(new ColorName("Azure", 0xF0, 0xFF, 0xFF));
        colorList.add(new ColorName("Beige", 0xF5, 0xF5, 0xDC));
        colorList.add(new ColorName("Bisque", 0xFF, 0xE4, 0xC4));
        colorList.add(new ColorName("Black", 0x00, 0x00, 0x00));
        colorList.add(new ColorName("BlanchedAlmond", 0xFF, 0xEB, 0xCD));
        colorList.add(new ColorName("Blue", 0x00, 0x00, 0xFF));
        colorList.add(new ColorName("BlueViolet", 0x8A, 0x2B, 0xE2));
        colorList.add(new ColorName("Brown", 0xA5, 0x2A, 0x2A));
        colorList.add(new ColorName("BurlyWood", 0xDE, 0xB8, 0x87));
        colorList.add(new ColorName("CadetBlue", 0x5F, 0x9E, 0xA0));
        colorList.add(new ColorName("Chartreuse", 0x7F, 0xFF, 0x00));
        colorList.add(new ColorName("Chocolate", 0xD2, 0x69, 0x1E));
        colorList.add(new ColorName("Coral", 0xFF, 0x7F, 0x50));
        colorList.add(new ColorName("CornflowerBlue", 0x64, 0x95, 0xED));
        colorList.add(new ColorName("Cornsilk", 0xFF, 0xF8, 0xDC));
        colorList.add(new ColorName("Crimson", 0xDC, 0x14, 0x3C));
        colorList.add(new ColorName("Cyan", 0x00, 0xFF, 0xFF));
        colorList.add(new ColorName("DarkBlue", 0x00, 0x00, 0x8B));
        colorList.add(new ColorName("DarkCyan", 0x00, 0x8B, 0x8B));
        colorList.add(new ColorName("DarkGoldenRod", 0xB8, 0x86, 0x0B));
        colorList.add(new ColorName("DarkGray", 0xA9, 0xA9, 0xA9));
        colorList.add(new ColorName("DarkGreen", 0x00, 0x64, 0x00));
        colorList.add(new ColorName("DarkKhaki", 0xBD, 0xB7, 0x6B));
        colorList.add(new ColorName("DarkMagenta", 0x8B, 0x00, 0x8B));
        colorList.add(new ColorName("DarkOliveGreen", 0x55, 0x6B, 0x2F));
        colorList.add(new ColorName("DarkOrange", 0xFF, 0x8C, 0x00));
        colorList.add(new ColorName("DarkOrchid", 0x99, 0x32, 0xCC));
        colorList.add(new ColorName("DarkRed", 0x8B, 0x00, 0x00));
        colorList.add(new ColorName("DarkSalmon", 0xE9, 0x96, 0x7A));
        colorList.add(new ColorName("DarkSeaGreen", 0x8F, 0xBC, 0x8F));
        colorList.add(new ColorName("DarkSlateBlue", 0x48, 0x3D, 0x8B));
        colorList.add(new ColorName("DarkSlateGray", 0x2F, 0x4F, 0x4F));
        colorList.add(new ColorName("DarkTurquoise", 0x00, 0xCE, 0xD1));
        colorList.add(new ColorName("DarkViolet", 0x94, 0x00, 0xD3));
        colorList.add(new ColorName("DeepPink", 0xFF, 0x14, 0x93));
        colorList.add(new ColorName("DeepSkyBlue", 0x00, 0xBF, 0xFF));
        colorList.add(new ColorName("DimGray", 0x69, 0x69, 0x69));
        colorList.add(new ColorName("DodgerBlue", 0x1E, 0x90, 0xFF));
        colorList.add(new ColorName("FireBrick", 0xB2, 0x22, 0x22));
        colorList.add(new ColorName("FloralWhite", 0xFF, 0xFA, 0xF0));
        colorList.add(new ColorName("ForestGreen", 0x22, 0x8B, 0x22));
        colorList.add(new ColorName("Fuchsia", 0xFF, 0x00, 0xFF));
        colorList.add(new ColorName("Gainsboro", 0xDC, 0xDC, 0xDC));
        colorList.add(new ColorName("GhostWhite", 0xF8, 0xF8, 0xFF));
        colorList.add(new ColorName("Gold", 0xFF, 0xD7, 0x00));
        colorList.add(new ColorName("GoldenRod", 0xDA, 0xA5, 0x20));
        colorList.add(new ColorName("Gray", 0x80, 0x80, 0x80));
        colorList.add(new ColorName("Green", 0x00, 0x80, 0x00));
        colorList.add(new ColorName("GreenYellow", 0xAD, 0xFF, 0x2F));
        colorList.add(new ColorName("HoneyDew", 0xF0, 0xFF, 0xF0));
        colorList.add(new ColorName("HotPink", 0xFF, 0x69, 0xB4));
        colorList.add(new ColorName("IndianRed", 0xCD, 0x5C, 0x5C));
        colorList.add(new ColorName("Indigo", 0x4B, 0x00, 0x82));
        colorList.add(new ColorName("Ivory", 0xFF, 0xFF, 0xF0));
        colorList.add(new ColorName("Khaki", 0xF0, 0xE6, 0x8C));
        colorList.add(new ColorName("Lavender", 0xE6, 0xE6, 0xFA));
        colorList.add(new ColorName("LavenderBlush", 0xFF, 0xF0, 0xF5));
        colorList.add(new ColorName("LawnGreen", 0x7C, 0xFC, 0x00));
        colorList.add(new ColorName("LemonChiffon", 0xFF, 0xFA, 0xCD));
        colorList.add(new ColorName("LightBlue", 0xAD, 0xD8, 0xE6));
        colorList.add(new ColorName("LightCoral", 0xF0, 0x80, 0x80));
        colorList.add(new ColorName("LightCyan", 0xE0, 0xFF, 0xFF));
        colorList.add(new ColorName("LightGoldenRodYellow", 0xFA, 0xFA, 0xD2));
        colorList.add(new ColorName("LightGray", 0xD3, 0xD3, 0xD3));
        colorList.add(new ColorName("LightGreen", 0x90, 0xEE, 0x90));
        colorList.add(new ColorName("LightPink", 0xFF, 0xB6, 0xC1));
        colorList.add(new ColorName("LightSalmon", 0xFF, 0xA0, 0x7A));
        colorList.add(new ColorName("LightSeaGreen", 0x20, 0xB2, 0xAA));
        colorList.add(new ColorName("LightSkyBlue", 0x87, 0xCE, 0xFA));
        colorList.add(new ColorName("LightSlateGray", 0x77, 0x88, 0x99));
        colorList.add(new ColorName("LightSteelBlue", 0xB0, 0xC4, 0xDE));
        colorList.add(new ColorName("LightYellow", 0xFF, 0xFF, 0xE0));
        colorList.add(new ColorName("Lime", 0x00, 0xFF, 0x00));
        colorList.add(new ColorName("LimeGreen", 0x32, 0xCD, 0x32));
        colorList.add(new ColorName("Linen", 0xFA, 0xF0, 0xE6));
        colorList.add(new ColorName("Magenta", 0xFF, 0x00, 0xFF));
        colorList.add(new ColorName("Maroon", 0x80, 0x00, 0x00));
        colorList.add(new ColorName("MediumAquaMarine", 0x66, 0xCD, 0xAA));
        colorList.add(new ColorName("MediumBlue", 0x00, 0x00, 0xCD));
        colorList.add(new ColorName("MediumOrchid", 0xBA, 0x55, 0xD3));
        colorList.add(new ColorName("MediumPurple", 0x93, 0x70, 0xDB));
        colorList.add(new ColorName("MediumSeaGreen", 0x3C, 0xB3, 0x71));
        colorList.add(new ColorName("MediumSlateBlue", 0x7B, 0x68, 0xEE));
        colorList.add(new ColorName("MediumSpringGreen", 0x00, 0xFA, 0x9A));
        colorList.add(new ColorName("MediumTurquoise", 0x48, 0xD1, 0xCC));
        colorList.add(new ColorName("MediumVioletRed", 0xC7, 0x15, 0x85));
        colorList.add(new ColorName("MidnightBlue", 0x19, 0x19, 0x70));
        colorList.add(new ColorName("MintCream", 0xF5, 0xFF, 0xFA));
        colorList.add(new ColorName("MistyRose", 0xFF, 0xE4, 0xE1));
        colorList.add(new ColorName("Moccasin", 0xFF, 0xE4, 0xB5));
        colorList.add(new ColorName("NavajoWhite", 0xFF, 0xDE, 0xAD));
        colorList.add(new ColorName("Navy", 0x00, 0x00, 0x80));
        colorList.add(new ColorName("OldLace", 0xFD, 0xF5, 0xE6));
        colorList.add(new ColorName("Olive", 0x80, 0x80, 0x00));
        colorList.add(new ColorName("OliveDrab", 0x6B, 0x8E, 0x23));
        colorList.add(new ColorName("Orange", 0xFF, 0xA5, 0x00));
        colorList.add(new ColorName("OrangeRed", 0xFF, 0x45, 0x00));
        colorList.add(new ColorName("Orchid", 0xDA, 0x70, 0xD6));
        colorList.add(new ColorName("PaleGoldenRod", 0xEE, 0xE8, 0xAA));
        colorList.add(new ColorName("PaleGreen", 0x98, 0xFB, 0x98));
        colorList.add(new ColorName("PaleTurquoise", 0xAF, 0xEE, 0xEE));
        colorList.add(new ColorName("PaleVioletRed", 0xDB, 0x70, 0x93));
        colorList.add(new ColorName("PapayaWhip", 0xFF, 0xEF, 0xD5));
        colorList.add(new ColorName("PeachPuff", 0xFF, 0xDA, 0xB9));
        colorList.add(new ColorName("Peru", 0xCD, 0x85, 0x3F));
        colorList.add(new ColorName("Pink", 0xFF, 0xC0, 0xCB));
        colorList.add(new ColorName("Plum", 0xDD, 0xA0, 0xDD));
        colorList.add(new ColorName("PowderBlue", 0xB0, 0xE0, 0xE6));
        colorList.add(new ColorName("Purple", 0x80, 0x00, 0x80));
        colorList.add(new ColorName("Red", 0xFF, 0x00, 0x00));
        colorList.add(new ColorName("RosyBrown", 0xBC, 0x8F, 0x8F));
        colorList.add(new ColorName("RoyalBlue", 0x41, 0x69, 0xE1));
        colorList.add(new ColorName("SaddleBrown", 0x8B, 0x45, 0x13));
        colorList.add(new ColorName("Salmon", 0xFA, 0x80, 0x72));
        colorList.add(new ColorName("SandyBrown", 0xF4, 0xA4, 0x60));
        colorList.add(new ColorName("SeaGreen", 0x2E, 0x8B, 0x57));
        colorList.add(new ColorName("SeaShell", 0xFF, 0xF5, 0xEE));
        colorList.add(new ColorName("Sienna", 0xA0, 0x52, 0x2D));
        colorList.add(new ColorName("Silver", 0xC0, 0xC0, 0xC0));
        colorList.add(new ColorName("SkyBlue", 0x87, 0xCE, 0xEB));
        colorList.add(new ColorName("SlateBlue", 0x6A, 0x5A, 0xCD));
        colorList.add(new ColorName("SlateGray", 0x70, 0x80, 0x90));
        colorList.add(new ColorName("Snow", 0xFF, 0xFA, 0xFA));
        colorList.add(new ColorName("SpringGreen", 0x00, 0xFF, 0x7F));
        colorList.add(new ColorName("SteelBlue", 0x46, 0x82, 0xB4));
        colorList.add(new ColorName("Tan", 0xD2, 0xB4, 0x8C));
        colorList.add(new ColorName("Teal", 0x00, 0x80, 0x80));
        colorList.add(new ColorName("Thistle", 0xD8, 0xBF, 0xD8));
        colorList.add(new ColorName("Tomato", 0xFF, 0x63, 0x47));
        colorList.add(new ColorName("Turquoise", 0x40, 0xE0, 0xD0));
        colorList.add(new ColorName("Violet", 0xEE, 0x82, 0xEE));
        colorList.add(new ColorName("Wheat", 0xF5, 0xDE, 0xB3));
        colorList.add(new ColorName("White", 0xFF, 0xFF, 0xFF));
        colorList.add(new ColorName("WhiteSmoke", 0xF5, 0xF5, 0xF5));
        colorList.add(new ColorName("Yellow", 0xFF, 0xFF, 0x00));
        colorList.add(new ColorName("YellowGreen", 0x9A, 0xCD, 0x32));
        return colorList;
    }

    /**
     * SubClass of ColorUtils. In order to lookup color name
     *
     * @author Xiaoxiao Li
     */
    public static int toRGBA(double r, double g, double b, double a) {
        return toRGBA((float) r, (float) g, (float) b, (float) a);
    }

    private static int rainbow(double x, double y) {
        double scale = 4.0d / 255.0d;
        double speed = 5000.0d; //time (in ms) per full rainbow cycle
        double d = 0.7071067811865483d; //cos((360° - 45°) + 180°)
        double pos = (x * d + y * d) * scale + (System.currentTimeMillis() % speed) * 2.0d * Math.PI / speed;
        return 0xFF000000
                | MathHelper.clamp(MathHelper.floor(255.0d * (0.5d + Math.sin(0.0d / 3.0d * Math.PI + pos))), 0, 255) << 16
                | MathHelper.clamp(MathHelper.floor(255.0d * (0.5d + Math.sin(2.0d / 3.0d * Math.PI + pos))), 0, 255) << 8
                | MathHelper.clamp(MathHelper.floor(255.0d * (0.5d + Math.sin(4.0d / 3.0d * Math.PI + pos))), 0, 255);
    }

    /**
     * Get the closest color name from our list
     *
     * @param r
     * @param g
     * @param b
     * @return
     */
    public String getColorNameFromRgb(int r, int g, int b) {
        ArrayList<ColorName> colorList = initColorList();
        ColorName closestMatch = null;
        int minMSE = Integer.MAX_VALUE;
        int mse;
        for (ColorName c : colorList) {
            mse = c.computeMSE(r, g, b);
            if (mse < minMSE) {
                minMSE = mse;
                closestMatch = c;
            }
        }

        if (closestMatch != null) {
            return closestMatch.getName();
        } else {
            return "No matched color name.";
        }
    }

    public static Color blend(Color color1, Color color2, double ratio) {
        float r = (float) ratio;
        float ir = (float) 1.0 - r;

        float rgb1[] = new float[3];
        float rgb2[] = new float[3];

        color1.getColorComponents(rgb1);
        color2.getColorComponents(rgb2);

        float red = rgb1[0] * r + rgb2[0] * ir;
        float green = rgb1[1] * r + rgb2[1] * ir;
        float blue = rgb1[2] * r + rgb2[2] * ir;

        if (red < 0) {
            red = 0;
        } else if (red > 255) {
            red = 255;
        }
        if (green < 0) {
            green = 0;
        } else if (green > 255) {
            green = 255;
        }
        if (blue < 0) {
            blue = 0;
        } else if (blue > 255) {
            blue = 255;
        }

        Color color = null;
        try {
            color = new Color(red, green, blue);
        } catch (IllegalArgumentException exp) {
            NumberFormat nf = NumberFormat.getNumberInstance();
            Muffin.LOGGER.info(nf.format(red) + "; " + nf.format(green) + "; " + nf.format(blue));
            exp.printStackTrace();
        }
        return color;
    }

    public static Color blendColors(float[] fractions, Color[] colors, float progress) {
        Color color;
        if (fractions != null) {
            if (colors != null) {
                if (fractions.length == colors.length) {
                    int[] indicies = getFractionIndicies(fractions, progress);

                    if (indicies[0] < 0 || indicies[0] >= fractions.length || indicies[1] < 0 || indicies[1] >= fractions.length) {
                        return colors[0];
                    }
                    float[] range = new float[]{fractions[indicies[0]], fractions[indicies[1]]};
                    Color[] colorRange = new Color[]{colors[indicies[0]], colors[indicies[1]]};

                    float max = range[1] - range[0];
                    float value = progress - range[0];
                    float weight = value / max;

                    color = blend(colorRange[0], colorRange[1], 1f - weight);
                } else {
                    throw new IllegalArgumentException("Fractions and colours must have equal number of elements");
                }
            } else {
                throw new IllegalArgumentException("Colours can't be null");
            }
        } else {
            throw new IllegalArgumentException("Fractions can't be null");
        }
        return color;
    }


    private static int[] getFractionIndicies(float[] fractions, float progress) {
        int[] range = new int[2];

        int startPoint = 0;
        while (startPoint < fractions.length && fractions[startPoint] <= progress) {
            startPoint++;
        }

        if (startPoint >= fractions.length) {
            startPoint = fractions.length - 1;
        }

        range[0] = startPoint - 1;
        range[1] = startPoint;

        return range;
    }

    public static String getHealthColoured(Entity entity, int health) {
        float max = ((EntityLivingBase) entity).getMaxHealth();
        int result = Math.round((health / max) * 100);
        if (result > 75) {
            return TextFormatting.GREEN + "" + health;
        } else if (result > 50) {
            return TextFormatting.YELLOW + "" + health;
        } else if (result > 25) {
            return TextFormatting.RED + "" + health;
        } else {
            return TextFormatting.DARK_RED + "" + health;
        }
    }

    public static float toF(int i) {
        return i / 255F;
    }

    public static float toF(double d) {
        return (float) (d / 255F);
    }

    /**
     * Convert hexColor to rgb, then call getColorNameFromRgb(r, g, b)
     *
     * @param hexColor
     * @return
     */
    public String getColorNameFromHex(int hexColor) {
        int r = (hexColor & 0xFF0000) >> 16;
        int g = (hexColor & 0xFF00) >> 8;
        int b = (hexColor & 0xFF);
        return getColorNameFromRgb(r, g, b);
    }

    public int colorToHex(Color c) {
        return Integer.decode("0x"
                + Integer.toHexString(c.getRGB()).substring(2));
    }

    public String getColorNameFromColor(Color color) {
        return getColorNameFromRgb(color.getRed(), color.getGreen(),
                color.getBlue());
    }

    public class ColorName {

        public int r, g, b;
        public String name;

        public ColorName(String name, int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.name = name;
        }

        public int computeMSE(int pixR, int pixG, int pixB) {
            return ((pixR - r) * (pixR - r) + (pixG - g) * (pixG - g) + (pixB - b) * (pixB - b)) / 3;
        }

        public int getR() {
            return r;
        }

        public int getG() {
            return g;
        }

        public int getB() {
            return b;
        }

        public String getName() {
            return name;
        }
    }

    public static Color getFriendColor(int a) {
        return new Color(90, 255, 255, a);
    }

    public static Colour getFriendColour(int a) {
        return new Colour(90, 255, 255, a);
    }
    
    public static Color getClientColor() {
        int r = Muffin.getInstance().getFontManager().getPublicRed();
        int g = Muffin.getInstance().getFontManager().getPublicGreen();
        int b = Muffin.getInstance().getFontManager().getPublicBlue();
        return new Color(r, g, b);
    }

    public static Color getClientColor(int a) {
        int r = Muffin.getInstance().getFontManager().getPublicRed();
        int g = Muffin.getInstance().getFontManager().getPublicGreen();
        int b = Muffin.getInstance().getFontManager().getPublicBlue();
        return new Color(r, g, b, a);
    }

    public static Colour getClientColour(int a) {
        int r = Muffin.getInstance().getFontManager().getPublicRed();
        int g = Muffin.getInstance().getFontManager().getPublicGreen();
        int b = Muffin.getInstance().getFontManager().getPublicBlue();
        return new Colour(r, g, b, a);
    }

    public static int toRGBAClient(int a) {
        int r = Muffin.getInstance().getFontManager().getPublicRed();
        int g = Muffin.getInstance().getFontManager().getPublicGreen();
        int b = Muffin.getInstance().getFontManager().getPublicBlue();
       // int a = 25;
        return (r << 16) + (g << 8) + (b << 0) + (a << 24);
    }


    public static int toRGBA(int r, int g, int b, int a) {
        return (r << 16) + (g << 8) + (b << 0) + (a << 24);
    }

    public static int toRGBA(float r, float g, float b, float a) {
        return toRGBA((int) (r * 255.f), (int) (g * 255.f), (int) (b * 255.f), (int) (a * 255.f));
    }

    public static int toRGBA(float[] colors) {
        if (colors.length != 4) throw new IllegalArgumentException("colors[] must have a length of 4!");
        return toRGBA(colors[0], colors[1], colors[2], colors[3]);
    }

    public static int toRGBA(double[] colors) {
        if (colors.length != 4) throw new IllegalArgumentException("colors[] must have a length of 4!");
        return toRGBA((float) colors[0], (float) colors[1], (float) colors[2], (float) colors[3]);
    }

    public static Color getColorByRange(Entity entity) {
        if (FriendManager.isFriend(entity.getName())) {
            return new Color(85, 255, 255);
        } else if (Globals.mc.player.getDistance(entity) <= 32) {
            return new Color(170, 0, 0);
        } else if (Globals.mc.player.getDistance(entity) >= 32 && Globals.mc.player.getDistance(entity) <= 96) {
            return new Color(255, 255, 85);
        } else {
            return new Color(0, 170, 0);
        }
    }

    /**
     * Adds color values to a color
     * The values will be clamped in the valid rgb color space (0-255)
     * @param red
     * @param green
     * @param blue
     * @param alpha
     * @param color
     * @return
     */
    public static Color addColor(final int red, final int green, final int blue, final int alpha, final Color color) {
        final int newRed = MathHelper.clamp(color.getRed() + red, 0, 255);
        final int newGreen = MathHelper.clamp(color.getGreen() + green, 0, 255);
        final int newBlue = MathHelper.clamp(color.getBlue() + blue, 0, 255);
        final int newAlpha = MathHelper.clamp(color.getAlpha() + alpha, 0, 255);
        return new Color(newRed, newGreen, newBlue, newAlpha);
    }

    /**
     * Adds color values to a color
     * The values will be clamped in the valid rgb color space (0-255)
     * @param red
     * @param green
     * @param blue
     * @param color
     * @return
     */
    public static Color addColor(final int red, final int green, final int blue, final Color color) {
        final int newRed = MathHelper.clamp(color.getRed() + red, 0, 255);
        final int newGreen = MathHelper.clamp(color.getGreen() + green, 0, 255);
        final int newBlue = MathHelper.clamp(color.getBlue() + blue, 0, 255);
        return new Color(newRed, newGreen, newBlue, color.getAlpha());
    }

    /**
     * Sets the alpha value of a color
     * The value will be clamped in the valid rgb color space (0-255)
     * @param alpha
     * @param color
     * @return
     */
    public static Color setAlpha(final int alpha, final Color color) {
        final int newAlpha = MathHelper.clamp(alpha, 0, 255);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), newAlpha);
    }

    /**
     * Returns the rgb color of a hue
     * @param value
     * @return
     */
    public static Color getColorOfHue(final int value) {
        final float hue = (1.0F - value / 360.0F);
        final int color = Color.HSBtoRGB(hue, 1.0F, 1.0F);
        return new Color(color);
    }

    /**
     * Returns the opposite color of the given color
     * @param color
     * @return
     */
    public static Color getInvertedColor(final Color color) {
        final int red = 255 - color.getRed();
        final int green = 255 - color.getGreen();
        final int blue = 255 - color.getBlue();
        return new Color(red, green, blue, color.getAlpha());
    }

    /**
     * Returns the opposize color of the given color
     * @param color
     * @return
     */
    public static int getInvertedColor(final int color) {
        return getInvertedColor(new Color(color)).getRGB();
    }

    public static Color fade(final Color color) {
        return fade(color, 2, 100);
    }

    public static Color fade(final Color color, final int index, final int count) {
        final float[] hsb = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
        float brightness = Math.abs((System.currentTimeMillis() % 2000L / 1000.0f + index / (float)count * 2.0f) % 2.0f - 1.0f);
        brightness = 0.5f + 0.5f * brightness;
        hsb[2] = brightness % 2.0f;
        return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
    }

    /*
    public Color getColorOf(double value, double minValue, double maxValue) {
        if (value == 0 || minValue == maxValue)
            return Color.WHITE;

        float g = (int)(240 * value / maxValue);
        float r = (int)(240 * value / minValue);

        return (value > 0
                ? Color.FromArgb(240 - g, 255 - (int)(g * ((255 - 155) / 240.0)), 240 - g)
                : Color.FromArgb(255 - (int)(r * ((255 - 230) / 240.0)), 240 - r, 240 - r));
    }
     */

    public static int[] toRGBAArray(int colorBuffer) {
        return new int[]{
                (colorBuffer >> 16 & 255),
                (colorBuffer >> 8 & 255),
                (colorBuffer & 255),
                (colorBuffer >> 24 & 255)
        };
    }


    public static Color rainbow() {
        Color currentColor = new Color(Color.HSBtoRGB((System.nanoTime() + 400000L) / 10000000000F % 1, 1F, 1F));
        return new Color(currentColor.getRed() / 255F * 1F, currentColor.getGreen() / 255f * 1F, currentColor.getBlue() / 255F * 1F, currentColor.getAlpha() / 255F);
    }

    public static class Colors {
        public final static int WHITE = toRGBA(255, 255, 255, 255);
        public final static int BLACK = toRGBA(0, 0, 0, 255);
        public final static int RED = toRGBA(255, 0, 0, 255);
        public final static int GREEN = toRGBA(0, 255, 0, 255);
        public final static int BLUE = toRGBA(0, 0, 255, 255);
        public final static int ORANGE = toRGBA(255, 128, 0, 255);
        public final static int PURPLE = toRGBA(163, 73, 163, 255);
        public final static int GRAY = toRGBA(127, 127, 127, 255);
        public final static int DARK_RED = toRGBA(64, 0, 0, 255);
        public final static int YELLOW = toRGBA(255, 255, 0, 255);
        public final static int AQUA = toRGBA(0, 204, 204, 255);
        public final static int RAINBOW = Integer.MIN_VALUE;
    }

    public static final int changeAlpha(int origColor, int userInputedAlpha) {
        origColor = origColor & 0x00ffffff; //drop the previous alpha value
        return (userInputedAlpha << 24) | origColor; //add the one the user inputted
    }

    public static int toHex(int r, int g, int b) {
        return (0xff << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
    }

}