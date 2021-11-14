package me.han.muffin.client.gui.click.item.element;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.click.ClickGui;
import me.han.muffin.client.gui.click.item.Item;
import me.han.muffin.client.module.modules.other.ClickGUI;
import me.han.muffin.client.utils.color.ColourUtils;
import me.han.muffin.client.utils.render.RenderUtils;
import me.han.muffin.client.value.EnumValue;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;

public class EnumButton extends Item {
    private EnumValue property;

    public EnumButton(EnumValue property) {
        super(property.getAliases()[0]);
        this.property = property;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        RenderUtils.drawRect(
                x + 2,
                y,
                x + width,
                y + height,
                !isHovering(mouseX, mouseY) ? ColourUtils.toRGBA(this.red, this.green, this.blue, 135) : ColourUtils.toRGBA(this.red, this.green, this.blue, 165)
        );

        RenderUtils.rectangle(x, y, x + 1, y + height, ColourUtils.toRGBA(this.red, this.green, this.blue, 165));

        if (ClickGui.getClickGui().guiFont != null)
            ClickGui.getClickGui().guiFont.drawStringWithShadow(String.format("%s\2477 %s", getName(), property.getFixedValue()), x + 2.3F, y + 4f, getDisabledSettingTextColour());
        else
            Globals.mc.fontRenderer.drawStringWithShadow(String.format("%s\2477 %s", getName(), property.getFixedValue()), x + 2.3F, y + 4f, getDisabledSettingTextColour());

    }

    @Override
    public void processMouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.processMouseClicked(mouseX, mouseY, mouseButton);
        if (isHovering(mouseX, mouseY)) {
            if (mouseButton == 0) {
                Globals.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                property.increment();
            } else if (mouseButton == 1) {
                Globals.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                property.decrement();
            }
        }
    }

    @Override
    public void update() {
        super.update();
        this.setHidden(!property.isVisible());
    }

    @Override
    public int getHeight() {
        return 11 + ClickGUI.INSTANCE.getGuiHeight().getValue();
    }

}
