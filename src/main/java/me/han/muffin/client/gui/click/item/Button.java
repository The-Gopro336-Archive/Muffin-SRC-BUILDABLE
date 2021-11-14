package me.han.muffin.client.gui.click.item;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.Panel;
import me.han.muffin.client.gui.click.ClickGui;
import me.han.muffin.client.module.modules.other.ClickGUI;
import me.han.muffin.client.utils.color.Colour;
import me.han.muffin.client.utils.color.ColourUtils;
import me.han.muffin.client.utils.render.RenderUtils;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;

import java.awt.*;

public class Button extends Item {

    private boolean state;
    private float fadeAlpha;
    Color rgb;

    int bRed;
    int bGreen;
    int bBlue;
    int alphaTest;
    int gray;
    Colour buttonEnabledTextColour = new Colour(0,0,0, 0);
    Colour buttonDisabledTextColour = new Colour(0,0,0, 0);
  //  int lightGray;

    public Button(String label) {
        super(label);
        this.height = 15;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
/*
        RenderUtils.drawGradientRect(
                x,
                y,
                x + width,
                y + height,
                getState()
                        ? (!isHovering(mouseX, mouseY)
                        ? ColourUtils.toRGBA(this.red, this.green, this.blue, 140)
                        : ColourUtils.toRGBA(this.red, this.green, this.blue, 165))
                        : !isHovering(mouseX, mouseY)
                        ? 0x33555555
                        : 0x88555555,
                getState()
                        ? (!isHovering(mouseX, mouseY)
                        ? ColourUtils.toRGBA(this.red, this.green, this.blue, 140)
                        : ColourUtils.toRGBA(this.red, this.green, this.blue, 165))
                        : !isHovering(mouseX, mouseY)
                        ? 0x55555555
                        : 0x99555555);
 */

        if (ClickGUI.INSTANCE.getButtonAnimation().getValue()) {
            boolean isDecreasing = false;
            int decreasingRed = 0;
            int decreasingGreen = 0;
            int decreasingBlue = 0;
            int decreasingAlpha = 0;


            if (isHovering(mouseX, mouseY) && getState()) {
                gray = 0;
                //     lightGray = 0;

                if (bRed < red)
                    bRed += 20 * partialTicks;
                if (bRed > red)
                    bRed = red;

                if (bGreen < green)
                    bGreen += 20 * partialTicks;
                if (bGreen > green)
                    bGreen = green;

                if (bBlue < blue)
                    bBlue += 20 * partialTicks;
                if (bBlue > blue)
                    bBlue = blue;

                if (alphaTest < 175)
                    alphaTest += 20 * partialTicks;
                if (alphaTest > 175)
                    alphaTest = 175;

                isDecreasing = false;
                decreasingRed = bRed;
                decreasingGreen = bGreen;
                decreasingBlue = bBlue;
                decreasingAlpha = alphaTest;
                rgb = new Color(bRed, bGreen, bBlue, alphaTest);
            } else if (!isHovering(mouseX, mouseY) && getState()) {
                gray = 0;
                //     lightGray = 0;

                if (bRed > red) {
                    bRed -= 20 * partialTicks;
                }
                if (bRed < red)
                    bRed = red;

                if (bGreen > green) {
                    bGreen -= 20 * partialTicks;
                }
                if (bGreen < green)
                    bGreen = green;

                if (bBlue > blue) {
                    bBlue -= 20 * partialTicks;
                }
                if (bBlue < blue)
                    bBlue = blue;

                if (alphaTest > 150) {
                    alphaTest -= 20 * partialTicks;
                }

                if (alphaTest < 150)
                    alphaTest = 150;

                isDecreasing = true;
                decreasingRed = bRed;
                decreasingGreen = bGreen;
                decreasingBlue = bBlue;
                decreasingAlpha = alphaTest;
                rgb = new Color(bRed, bGreen, bBlue, alphaTest);
            }


            if (isHovering(mouseX, mouseY) && !getState()) {
                bRed = 0;
                bGreen = 0;
                bBlue = 0;

                if (gray < 80)
                    gray += 20 * partialTicks;
                if (gray > 80)
                    gray = 80;

                if (alphaTest < 175)
                    alphaTest += 25 * partialTicks;
                if (alphaTest > 175)
                    alphaTest = 175;

                isDecreasing = false;
                decreasingRed = gray;
                decreasingGreen = gray;
                decreasingBlue = gray;
                decreasingAlpha = alphaTest;
                rgb = new Color(gray, gray, gray, alphaTest);

            } else if (!isHovering(mouseX, mouseY) && !getState()) {
                bRed = 0;
                bGreen = 0;
                bBlue = 0;
                if (gray > 22)
                    gray -= 3 * partialTicks;
                if (gray < 22)
                    gray = 22;

                if (alphaTest > 90)
                    alphaTest -= 5 * partialTicks;

                if (alphaTest < 90)
                    alphaTest = 90;

                isDecreasing = true;

                decreasingRed = gray;
                decreasingGreen = gray;
                decreasingBlue = gray;
                decreasingAlpha = alphaTest;

                rgb = new Color(gray, gray, gray, alphaTest);
            }

            if (isDecreasing) {
                if (bRed > decreasingRed)
                    bRed -= 2 * partialTicks;
                if (bRed < decreasingRed)
                    bRed = decreasingRed;

                if (bGreen > decreasingGreen)
                    bGreen -= 2 * partialTicks;
                if (bGreen < decreasingGreen)
                    bGreen = decreasingGreen;

                if (bBlue > decreasingBlue)
                    bBlue -= 2 * partialTicks;
                if (bBlue < decreasingBlue)
                    bBlue = decreasingBlue;

                if (alphaTest > decreasingAlpha)
                    alphaTest -= 2 * partialTicks;
                if (alphaTest < decreasingAlpha)
                    alphaTest = decreasingAlpha;

                rgb = new Color(bRed, bGreen, bBlue, alphaTest);
            } /*else {
            if (bRed < decreasingRed)
                bRed += 2 * partialTicks;
            if (bRed > decreasingRed)
                bRed = decreasingRed;

            if (bGreen < decreasingGreen)
                bGreen -= 2 * partialTicks;
            if (bGreen > decreasingGreen)
                bGreen = decreasingGreen;

            if (bBlue < decreasingBlue)
                bBlue -= 2 * partialTicks;
            if (bBlue > decreasingBlue)
                bBlue = decreasingBlue;

            if (alphaTest < decreasingAlpha)
                alphaTest -= 2 * partialTicks;
            if (alphaTest > decreasingAlpha)
                alphaTest = decreasingAlpha;

        }
        */

            //    rgb = new Color(bRed, bGreen, bBlue, alphaTest);
        } else {
            if (isHovering(mouseX, mouseY) && getState()) {
                rgb = new Color(red, green, blue, 175);
            } else if (!isHovering(mouseX, mouseY) && getState()) {
                rgb = new Color(red, green, blue, 150);
            }

            if (isHovering(mouseX, mouseY) && !getState()) {
                rgb = new Color(80, 80, 80, 175);

            } else if (!isHovering(mouseX, mouseY) && !getState()) {
                rgb = new Color(22, 22, 22, 90);
            }

        }

        buttonEnabledTextColour = ClickGUI.INSTANCE.getButtonEnabledTextColour();
        buttonDisabledTextColour = ClickGUI.INSTANCE.getButtonDisabledTextColour();

        if (ClickGUI.INSTANCE.getButtonRect().getValue())
        RenderUtils.drawRect(
                x - 1,
                y - 0.7F,
                (x + width) + 0.3F,
                (y + height) - 4,
                ColourUtils.toRGBA(
                        rgb.getRed(),
                        rgb.getGreen(),
                        rgb.getBlue(),
                        rgb.getAlpha()
                )
        );


/*
        RenderUtils.drawRect(
                x - 1,
                y - 0.7F,
                (x + width) + 0.3F,
                (y + height) - 4,
                clicked
                        ? (!isHovering(mouseX, mouseY)
                        ? ColourUtils.toRGBA(this.red, this.green, this.blue, 160)
                        : ColourUtils.toRGBA(this.red, this.green, this.blue, 175))
                        : !isHovering(mouseX, mouseY)
                        ? ColourUtils.toRGBA(23, 23, 23, 150)
                        : ColourUtils.toRGBA(80, 80, 80, 170)
        );
 */

/*
        RenderUtils.drawRect(
                x - 1,
                y - 0.7F,
                (x + width) + 0.3F,
                (y + height) - 4,
                getState()
                        ? (!isHovering(mouseX, mouseY)
                        ? ColourUtils.toRGBA(this.red, this.green, this.blue, 165)
                        : ColourUtils.toRGBA(this.red, this.green, this.blue, getFade()))
                        : !isHovering(mouseX, mouseY)
                        ? ColourUtils.toRGBA(23, 23, 23, 150)
                        : ColourUtils.toRGBA(80, 80, 80, getFade())
        );
 */

/*
        if (ClickGui.getClickGui().guiFont != null)
            ClickGui.getClickGui().guiFont.drawStringWithShadow(
                    getName(),
                    x + 2.3F,
                    y + 3f,
                    ColourUtils.toRGBA(buttonTextColour.getR(), buttonTextColour.getG(), buttonTextColour.getB(), textAlpha));
        else
            Globals.mc.fontRenderer.drawStringWithShadow(
                    getName(),
                    x + 2.3F,
                    y + 2.8f,
                    ColourUtils.toRGBA(buttonTextColour.getR(), buttonTextColour.getG(), buttonTextColour.getB(), textAlpha));
 */

        if (ClickGui.getClickGui().guiFont != null)
            ClickGui.getClickGui().guiFont.drawStringWithShadow(
                    getName(),
                    x + 2.3F,
                    y + 3f,
                    getState()
                            ? isHovering(mouseX, mouseY)
                            ? ColourUtils.toRGBA(buttonEnabledTextColour.getR(), buttonEnabledTextColour.getG(), buttonEnabledTextColour.getB(), 195)
                            : ColourUtils.toRGBA(buttonEnabledTextColour.getR(), buttonEnabledTextColour.getG(), buttonEnabledTextColour.getB(), textAlpha)
                            : isHovering(mouseX, mouseY)
                            ? ColourUtils.toRGBA(buttonDisabledTextColour.getR(), buttonDisabledTextColour.getG(), buttonDisabledTextColour.getB(), 180)
                            : ColourUtils.toRGBA(buttonDisabledTextColour.getR(), buttonDisabledTextColour.getG(), buttonDisabledTextColour.getB(), textAlpha));
        else
            Globals.mc.fontRenderer.drawStringWithShadow(
                    getName(),
                    x + 2.3F,
                    y + 2.8f,
                    getState()
                            ? isHovering(mouseX, mouseY)
                            ? ColourUtils.toRGBA(buttonEnabledTextColour.getR(), buttonEnabledTextColour.getG(), buttonEnabledTextColour.getB(), 195)
                            : ColourUtils.toRGBA(buttonEnabledTextColour.getR(), buttonEnabledTextColour.getG(), buttonEnabledTextColour.getB(), textAlpha)
                            : isHovering(mouseX, mouseY)
                            ? ColourUtils.toRGBA(buttonDisabledTextColour.getR(), buttonDisabledTextColour.getG(), buttonDisabledTextColour.getB(), 180)
                            : ColourUtils.toRGBA(buttonDisabledTextColour.getR(), buttonDisabledTextColour.getG(), buttonDisabledTextColour.getB(), textAlpha));


/*
        if (ClickGui.getClickGui().guiFont != null)
            ClickGui.getClickGui().guiFont.drawStringWithShadow(
                    getName(),
                    x + 2.3F,
                    y + 2f,
                    getState()
                            ? isHovering(mouseX, mouseY)
                            ? ColourUtils.toRGBA(this.red, this.green, this.blue, 195)
                            : ColourUtils.toRGBA(this.red, this.green, this.blue, 255)
                            : isHovering(mouseX, mouseY)
                            ? ColourUtils.toRGBA(this.red, this.green, this.blue, 180)
                            : 0xFFAAAAAA);
        else
            Globals.mc.fontRenderer.drawStringWithShadow(
                    getName(),
                    x + 2.3F,
                    y + 2f,
                    getState()
                            ? isHovering(mouseX, mouseY)
                            ? ColourUtils.toRGBA(this.red, this.green, this.blue, 195)
                            : ColourUtils.toRGBA(this.red, this.green, this.blue, 255)
                            : isHovering(mouseX, mouseY)
                            ? ColourUtils.toRGBA(this.red, this.green, this.blue, 180)
                            : 0xFFAAAAAA);
 */

    }

    public void updateFade(final int delta) {

        int unHoverColorNoStateAlpha = 150;
        int unHoverColorNoStateRGB = 23;
        int hoveredColorNoStateRGB = 80;
        int hoveredColorNoStateAlpha = 170;

        int unHoverColorStatedAlpha = 160;
        int hoveredColorStatedAlpha = 175;

        if (getState()) {
            if (fadeAlpha < 150)
                fadeAlpha += 10F * delta;
            if (fadeAlpha > 150)
                fadeAlpha = 150;
        } else {
            if (fadeAlpha > 160)
                fadeAlpha -= 10F * delta;
            if (fadeAlpha < 160)
                fadeAlpha = 160;
        }

    }


    public int getFade() {
        return (int) fadeAlpha;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        bRed = 0;
        bGreen = 0;
        bBlue = 0;
        alphaTest = 0;
        gray = 0;
    }

    @Override
    public void processMouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.processMouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0 && isHovering(mouseX, mouseY)) {
            state = !state;
            toggle();
            Globals.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public void onInitGui() {
        super.onInitGui();
        bRed = 0;
        bGreen = 0;
        bBlue = 0;
        alphaTest = 0;
        gray = 0;
      //  textAlpha = 0;
    }

    public void toggle() {
    }

    public boolean getState() {
        return state;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    protected boolean isHovering(int mouseX, int mouseY) {
        for (Panel panel : ClickGui.getClickGui().getPanels()) {
            if (panel.drag) {
                return false;
            }
        }

        return mouseX >= getX() && mouseX < getX() + getWidth() && mouseY >= getY() && mouseY < getY() + height - 3;
    }

}