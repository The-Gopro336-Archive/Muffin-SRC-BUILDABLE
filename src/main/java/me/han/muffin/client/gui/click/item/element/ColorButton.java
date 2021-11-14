package me.han.muffin.client.gui.click.item.element;

import me.han.muffin.client.gui.click.item.Item;
import me.han.muffin.client.utils.color.BetterColour;
import me.han.muffin.client.utils.render.RenderUtils;
import me.han.muffin.client.value.ColourValue;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public class ColorButton extends Item {
    private ColourValue value;

    private boolean subOpen;

    private double hueBarWidth = 20;

    private double hueColor;

    private double hueBarDotX, lastHueBarDotX, hueBarDotY, lastHueBarDotY;
    private double colorFieldDotX, lastColorFieldDotX, colorFieldDotY, lastColorFieldDotY;

    private boolean draggingHue, draggingColor;

    private boolean extended, slidingAlpha;

    public ColorButton(ColourValue value) {
        super(value.getAliases()[0]);
        this.value = value;
    }

    public final void rect(double x, double y, double width, double height, boolean filled, Color color) {

        RenderUtils.enableGL2D();
        if (color != null) RenderUtils.glColor(color);
        glBegin(filled ? GL_TRIANGLE_FAN : GL_LINES); {
            glVertex2d(x, y);
            glVertex2d(x + width, y);
            glVertex2d(x + width, y + height);
            glVertex2d(x, y + height);
            if (!filled)  {
                glVertex2d(x, y);
                glVertex2d(x, y + height);
                glVertex2d(x + width, y);
                glVertex2d(x + width, y + height);
            }
        }
        glEnd();
        RenderUtils.disableGL2D();
    }

    public final void rect(double x, double y, double width, double height, boolean filled) {
        rect(x, y, width, height, filled, null);
    }

    public final void rect(double x, double y, double width, double height, Color color) {
        rect(x, y, width, height, true, color);
    }

    public final void rect(double x, double y, double width, double height) {
        rect(x, y, width, height, true, null);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        hueBarDotX = lastHueBarDotX + x;
        hueBarDotY = lastHueBarDotY + y;

        hueColor = ((hueBarDotY / getHeight()) - (getY() / getHeight())) * 360;

        colorFieldDotX = lastColorFieldDotX + x;
        colorFieldDotY = lastColorFieldDotY + y;

        RenderUtils.enableGL2D();
        glBegin(GL_QUADS);{
            RenderUtils.glColor(BetterColour.getHue(hueColor));
            glVertex2d(x, y + getHeight());
            RenderUtils.glColor(Color.BLACK);
            glVertex2d(x + getWidth() - hueBarWidth, y + getHeight());
            RenderUtils.glColor(BetterColour.getHue(hueColor));
            glVertex2d(x + getWidth() - hueBarWidth, y);
            RenderUtils.glColor(Color.WHITE);
            glVertex2d(x, y);
        }
        glEnd();
        RenderUtils.disableGL2D();

        for (int i = 0; i < 360; ++i) {
            rect(x + getWidth() - hueBarWidth, getHeight() * (i / 360.0F) + y, hueBarWidth, 1, BetterColour.getHue((float) i));
           // RenderUtils.rectangle(x, y, x + getWidth() - hueBarWidth, y + getHeight() * (i / 360.0F), BetterColour.getHue((float) i).getRGB());
        }

        if (draggingHue && !draggingColor) {
            hueBarDotX = mouseX - 2.5;
            hueBarDotY = mouseY - 2.5;

            hueBarDotX = MathHelper.clamp(hueBarDotX, x + getWidth() - hueBarWidth, x + getWidth() - 5);
            hueBarDotY = MathHelper.clamp(hueBarDotY, y, y + getHeight() - 5);

            hueColor = ((hueBarDotY / getHeight()) - (y / getHeight())) * 360;

            lastHueBarDotX = hueBarDotX - x;
            lastHueBarDotY = hueBarDotY - y;
            draggingHue = true;
        }

        if (draggingColor && !draggingHue) {
            colorFieldDotX = mouseX - 2.5;
            colorFieldDotY = mouseY - 2.5;

            colorFieldDotX = MathHelper.clamp(colorFieldDotX, x, x + getWidth() - hueBarWidth - 5);
            colorFieldDotY = MathHelper.clamp(colorFieldDotY, y, y + getHeight() - 5);

            value.setValue(BetterColour.getHue((float) hueColor).addColoring((int) ((360 - (((colorFieldDotY / getHeight()) - (y / getHeight())) * 360)) - ((((colorFieldDotX / getWidth()) - (x / getWidth())) * 360)))));

            lastColorFieldDotX = colorFieldDotX - x;
            lastColorFieldDotY = colorFieldDotY - y;
            draggingColor = true;
        }

        RenderUtils.circle(hueBarDotX, hueBarDotY, 3, Color.WHITE);
        RenderUtils.circle(hueBarDotX, hueBarDotY, 3, false, Color.BLACK);

        RenderUtils.circle(colorFieldDotX, colorFieldDotY, 3, Color.WHITE);
        RenderUtils.circle(colorFieldDotX, colorFieldDotY, 3, false, Color.BLACK);

    }

    @Override
    public void processMouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.processMouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            if (colorFieldHovered(mouseX, mouseY)) {
                draggingColor = true;
            }
            if (colorHueBarHovered(mouseX, mouseY)) {
                draggingHue = true;
            }
        }
    }

    @Override
    public void processMouseReleased(int mouseX, int mouseY, int releaseButton) {
        super.processMouseReleased(mouseX, mouseY, releaseButton);
        if (releaseButton == 0) {
            if (draggingColor) {
                draggingColor = false;
            }
            if (draggingHue) {
                draggingHue = false;
            }
        }
    }

    @Override
    public void processKeyPressed(char character, int key) {
        super.processKeyPressed(character, key);
    }

    @Override
    public int getWidth() {
        return super.getWidth();
    }

    @Override
    public int getHeight() {
        return 30;
    }

    private boolean colorFieldHovered(int mouseX, int mouseY) {
        return (mouseX > x && mouseX < x + getWidth() - hueBarWidth) && (mouseY > y && mouseY < y + getHeight());
    }

    private boolean colorHueBarHovered(int mouseX, int mouseY) {
        return (mouseX > x + getWidth() - hueBarWidth && mouseX < x + getWidth()) && (mouseY > y && mouseY < y + getHeight());
    }

    public final double getHueBarDotX() {
        return hueBarDotX;
    }

    public final double getHueBarDotY() {
        return hueBarDotY;
    }

    public final double getColorFieldDotX() {
        return colorFieldDotX;
    }

    public final double getColorFieldDotY() {
        return colorFieldDotY;
    }

    public final void setDotPosition(double hueDotX, double hueDotY, double colorFieldDotX, double colorFieldDotY) {
        this.hueBarDotX = hueDotX;
        this.hueBarDotY = hueDotY;

        this.colorFieldDotX = colorFieldDotX;
        this.colorFieldDotY = colorFieldDotY;

        this.lastHueBarDotX = hueBarDotX - x;
        this.lastHueBarDotY = hueBarDotY - y;

        this.lastColorFieldDotX = colorFieldDotX - x;
        this.lastColorFieldDotY = colorFieldDotY - y;

        hueColor = ((hueBarDotY / getHeight()) - (y / getHeight())) * 360;
        value.setValue(BetterColour.getHue((float) hueColor).addColoring((int) ((360 - (((colorFieldDotY / getHeight()) - (y / getHeight())) * 360)) - ((((colorFieldDotX / getWidth()) - (x / getWidth())) * 360)))));
    }
    
}