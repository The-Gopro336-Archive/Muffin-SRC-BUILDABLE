package me.han.muffin.client.gui.click.item.element;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.click.ClickGui;
import me.han.muffin.client.gui.click.item.Item;
import me.han.muffin.client.gui.util.GuiUtils;
import me.han.muffin.client.utils.color.ColourUtils;
import me.han.muffin.client.value.NumberValue;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;

public class UnclampedSlider extends Item {

    private final NumberValue numberProperty;
    private boolean dragging = false;
    private float currentWidth;
    private double value;

    public UnclampedSlider(NumberValue numberProperty) {
        super(numberProperty.getAliases()[0]);
        this.numberProperty = numberProperty;
        this.value =  numberProperty.getValue().doubleValue();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (ClickGui.getClickGui().guiFont != null)
            ClickGui.getClickGui().guiFont.drawStringWithShadow(this.numberProperty.getAliases()[0] + ": " + GuiUtils.INSTANCE.roundSlider(this.value), (this.getX() + 2.3f), (this.getY() + 4f), isHovering(mouseX, mouseY) ? ColourUtils.toRGBA(210, 210, 210, 235) : ColourUtils.Colors.WHITE);
        else
            Globals.mc.fontRenderer.drawStringWithShadow(this.numberProperty.getAliases()[0] + ": " + GuiUtils.INSTANCE.roundSlider(this.value), (this.getX() + 2.3f), (this.getY() + 4f), isHovering(mouseX, mouseY) ? ColourUtils.toRGBA(210, 210, 210, 235) : ColourUtils.Colors.WHITE);

    }

    @Override
    public void processMouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.processMouseClicked(mouseX, mouseY, mouseButton);
        if (isHovering(mouseX, mouseY) && mouseButton == 0) {
            Globals.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.dragging = true;
        }
    }

    @Override
    public void processMouseReleased(int mouseX, int mouseY, int releaseButton) {
        super.processMouseReleased(mouseX, mouseY, releaseButton);
        if (this.dragging && releaseButton == 0) {
            this.dragging = false;
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        dragging = false;
    }

    @Override
    public void update() {
        super.update();
        this.setHidden(!numberProperty.isVisible());
    }

    @Override
    public int getHeight() {
        return 11;
    }

}