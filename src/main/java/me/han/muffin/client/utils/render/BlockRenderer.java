package me.han.muffin.client.utils.render;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import net.minecraft.util.math.BlockPos;

import java.awt.*;

public class BlockRenderer {

    private float[] hue;
    private int rgb = 255;
    private int red = 255;
    private int green = 255;
    private int blue = 255;
    private int rainbowred = 255;
    private int rainbowgreen = 255;
    private int rainbowblue = 255;

    public void setColor() {
        if (Globals.mc.player == null || Globals.mc.world == null)
            return;

        hue = new float[]{(float) (System.currentTimeMillis() % 12520L) / 12520.0f};
        rgb = Color.HSBtoRGB(hue[0], 1.0f, 1.0f);
        red = Muffin.getInstance().getFontManager().getPublicRed();
        green = Muffin.getInstance().getFontManager().getPublicGreen();
        blue = Muffin.getInstance().getFontManager().getPublicBlue();
        rainbowred = rgb >> 16 & 0xFF;
        rainbowgreen = rgb >> 8 & 0xFF;
        rainbowblue = rgb & 0xFF;
    }

    /*
    public void drawBox(BlockPos pos, int red, int green, int blue, int alpha) {
        if (Globals.mc.player == null || Globals.mc.world == null)
            return;

        ColorControl colorControl = (ColorControl) Muffin.getInstance().getModuleManager().getModule(ColorControl.class);

        if (colorControl.boxMode.getValue() == ColorControl.BoxMode.Solid) {
            RenderUtils.drawBlockESP(pos, red, green, blue, alpha);
        } else if (colorControl.boxMode.getValue() == ColorControl.BoxMode.Full) {
            RenderUtils.drawBlockOutlineESP(pos, red, green, blue, 255, 1.5f);
            RenderUtils.drawBlockESP(pos, red, green, blue, alpha);
        }

    }

    public void drawBox(BlockPos pos, int alpha) {
        if (Globals.mc.player == null || Globals.mc.world == null)
            return;

        ColorControl colorControl = (ColorControl) Muffin.getInstance().getModuleManager().getModule(ColorControl.class);

        if (colorControl.boxMode.getValue() == ColorControl.BoxMode.Solid) {
            if (RenderUtils.isInViewFrustrum(pos))
                RenderUtils.drawBlockESP(pos, red, green, blue, alpha);
        } else if (colorControl.boxMode.getValue() == ColorControl.BoxMode.Full) {
            if (RenderUtils.isInViewFrustrum(pos)) {
                RenderUtils.drawBlockOutlineESP(pos, red, green, blue, 255, 1.5f);
                RenderUtils.drawBlockESP(pos, red, green, blue, alpha);
            }
        }
    }
     */

    public void drawSolid(BlockPos pos, int red, int green, int blue, int alpha) {
        RenderUtils.drawBlockESP(pos, red, green, blue, alpha);
    }

    public void drawFull(BlockPos pos, int alpha) {
        RenderUtils.drawBlockESP(pos, red, green, blue, alpha);
        RenderUtils.drawBlockOutlineESP(pos, red, green, blue, 255, 1.5f);
    }

    public void drawFull(BlockPos pos, int red, int green, int blue, int alpha, float width) {
        RenderUtils.drawBlockFullESP(pos, red, green, blue, alpha, width);
    }


}