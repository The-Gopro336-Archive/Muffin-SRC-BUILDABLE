package me.han.muffin.client.gui.click.item.element;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.click.ClickGui;
import me.han.muffin.client.gui.click.item.Item;
import me.han.muffin.client.gui.util.GuiUtils;
import me.han.muffin.client.module.modules.other.ClickGUI;
import me.han.muffin.client.utils.color.ColourUtils;
import me.han.muffin.client.utils.render.RenderUtils;
import me.han.muffin.client.value.NumberValue;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;

//TODO: middle click to type value
public class NumberSlider extends Item {

    private final NumberValue numberValue;
    private boolean dragging = false;
    private double currentWidth;
    private Number value;
    private double sliderLength = 77.5;

   // private boolean isMiddleClicked = false;

    public NumberSlider(NumberValue numberValue) {
        super(numberValue.getAliases()[0]);
        this.numberValue = numberValue;
        this.value = numberValue.getValue();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        sliderLength = (77.5F + ClickGUI.INSTANCE.getGuiWidth().getValue() * 2) - 2;

        if (dragging) {
            currentWidth = mouseX - getX();
            if (currentWidth < 0) {
                currentWidth = 0;
            } else if (currentWidth > sliderLength) { // 77.5F
                currentWidth = sliderLength; // 77.5F
            }
            updateValueFromWidth();
        }

/*
        if (isMiddleClicked) {
            RenderUtils.drawBorderedRectReliant(
                    x + 1,
                    y + 1,
                    x + width,
                    y + height,
                    1.1f,
                    !isHovering(mouseX, mouseY)
                            ? ColourUtils.toRGBA(red, green, blue, 135)
                            : ColourUtils.toRGBA(red, green, blue, 165),
                    ColourUtils.toRGBA(red, green, blue, 205)
            );
            return;
        }

 */


        RenderUtils.drawBorderedRectReliant(
                x + 2,
                y + 1,
                (float) (x + 2 + getWidthFromValue()),
                y + height,
                1.1f,
                !isHovering(mouseX, mouseY)
                        ? ColourUtils.toRGBA(red, green, blue, 135)
                        : ColourUtils.toRGBA(red, green, blue, 165),
                ColourUtils.toRGBA(red, green, blue, 205));

/*
        RenderUtils.drawRect(
                x,
                y,
                x + currentWidth,
                y + height,
                !isHovering(mouseX, mouseY)
                        ? ColourUtils.toRGBA(red, green, blue, 120)
                        : ColourUtils.toRGBA(red, green, blue, 165));
*/

        RenderUtils.rectangle(x, y, x + 1, y + height, ColourUtils.toRGBA(red, green, blue, 165));

        final String roundedValue = GuiUtils.INSTANCE.roundSlider(value);
        if (roundedValue == null) return;
        final float floatValue = Float.parseFloat(roundedValue);
        final String valuePlaceholder = String.valueOf((floatValue == -0F) ? 0F : floatValue);

        final String placeHolder = numberValue.getAliases()[0] + ": " + valuePlaceholder;
        final float x = getX() + 2.3f;
        final float y = getY() + 4f;
        final int colour = isHovering(mouseX, mouseY) ? ColourUtils.toRGBA(210, 210, 210, 235) : getDisabledSettingTextColour();

        if (ClickGui.getClickGui().guiFont != null)
            ClickGui.getClickGui().guiFont.drawStringWithShadow(placeHolder, x, y, colour);
        else
            Globals.mc.fontRenderer.drawStringWithShadow(placeHolder, x, y, colour);

    }

    @Override
    public void processMouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.processMouseClicked(mouseX, mouseY, mouseButton);
        if (isHovering(mouseX, mouseY) && mouseButton == 0) {
            Globals.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            dragging = true;
        }
/*
        if (isHovering(mouseX, mouseY) && mouseButton == Globals.mc.gameSettings.keyBindPickBlock.getKeyCode()) {
            Globals.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F));
            isMiddleClicked = true;
            dragging = false;
        }

 */

    }

    @Override
    public void processMouseReleased(int mouseX, int mouseY, int releaseButton) {
        super.processMouseReleased(mouseX, mouseY, releaseButton);
        if (dragging && releaseButton == 0) {
            dragging = false;
            getWidthFromValue();
        }
    }

    @Override
    public void onInitGui() {
        super.onInitGui();
        value = numberValue.getValue();
        getWidthFromValue();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        dragging = false;
    }

/*
    @Override
    public void processKeyPressed(char character, int key) {
        super.processKeyPressed(character, key);
        if (!isMiddleClicked) return;

        if (key == Keyboard.KEY_RETURN) {
            isMiddleClicked = false;
        }

        if (key == Keyboard.KEY_BACK && !Keyboard.isRepeatEvent() && numberProperty.getValue().toString().length() > 0) {
            numberProperty.setValue(numberProperty.getValue());
            return;
        }

        if (isMiddleClicked && ChatAllowedCharacters.isAllowedCharacter(character)) {
            numberProperty.setValue(numberProperty.getValue() + character);
        }
    }

 */

    @Override
    public void update() {
        super.update();
        value = numberValue.getValue();
        setHidden(!numberValue.isVisible());
    }

    @Override
    public int getHeight() {
        return 11 + ClickGUI.INSTANCE.getGuiHeight().getValue();
    }


    protected double getWidthFromValue() {
        double val = value.doubleValue();
        val -= getMin();
        val /= getMax() - getMin();
        val *= sliderLength; //77.5F
        currentWidth = GuiUtils.INSTANCE.reCheckSliderRange(val, 0.0F, sliderLength); //77.5F
        return currentWidth;
    }

    protected void updateValueFromWidth() {
        double val = currentWidth / sliderLength; // 77.5F
        val *= getMax() - getMin();
        val += getMin();
        val = GuiUtils.INSTANCE.roundSliderStep(val, getStep());
        val = GuiUtils.INSTANCE.reCheckSliderRange(val, getMin(), getMax());
        value = val;
        Number roundedValue = val;
        if (numberValue.getValue() instanceof Long) {
            numberValue.setValue(roundedValue.longValue());
        } else if (numberValue.getValue() instanceof Integer) {
            numberValue.setValue(roundedValue.intValue());
        } else if (numberValue.getValue() instanceof Float) {
            numberValue.setValue(roundedValue.floatValue());
        } else if (numberValue.getValue() instanceof Double) {
            numberValue.setValue(roundedValue.doubleValue());
        }
    }

    public float getMax() {
        Number inc = numberValue.getMaximum();
        return inc == null ? 100.0F : inc.floatValue();
    }

    public float getMin() {
        Number inc = numberValue.getMinimum();
        return inc == null ? 0.0F : inc.floatValue();
    }

    public float getStep() {
        Number inc = numberValue.getIncrement();
        return inc == null ? 1.0F : inc.floatValue();
    }

}