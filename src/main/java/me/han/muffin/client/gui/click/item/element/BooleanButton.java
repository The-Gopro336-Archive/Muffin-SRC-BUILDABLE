package me.han.muffin.client.gui.click.item.element;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.click.ClickGui;
import me.han.muffin.client.gui.click.item.Item;
import me.han.muffin.client.module.modules.other.ClickGUI;
import me.han.muffin.client.utils.color.ColourUtils;
import me.han.muffin.client.utils.render.RenderUtils;
import me.han.muffin.client.value.Value;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;

public class BooleanButton extends Item {
    private Value property;

    public BooleanButton(Value property) {
        super(property.getAliases()[0]);
        this.property = property;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        if (ClickGUI.INSTANCE.getSettingEnabledRect().getValue())
            RenderUtils.drawRect(
                    x + 2,
                    y,
                    x + width,
                    y + height,
                    getState()
                            ? (!isHovering(mouseX, mouseY)
                            ? ColourUtils.toRGBA(this.red, this.green, this.blue, 135)
                            : ColourUtils.toRGBA(this.red, this.green, this.blue, 165))
                            : !isHovering(mouseX, mouseY)
                            ? 0x11555555
                            : 0x88555555);

        RenderUtils.rectangle(x, y, x + 1, y + height, ColourUtils.toRGBA(this.red, this.green, this.blue, 165));

        if (ClickGui.getClickGui().guiFont != null)
            ClickGui.getClickGui().guiFont.drawStringWithShadow(getName(), x + 2.3F, y + 4f, getState() ? getEnabledSettingTextColour() : getDisabledSettingTextColour());
        else
            Globals.mc.fontRenderer.drawStringWithShadow(getName(), x + 2.3F, y + 4f, getState() ? getEnabledSettingTextColour() : getDisabledSettingTextColour());

    }

    @Override
    public void processMouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.processMouseClicked(mouseX, mouseY, mouseButton);
        if (isHovering(mouseX, mouseY) && mouseButton == 0) {
            toggle();
            Globals.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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

    public void toggle() {
        property.setValue(!(boolean) property.getValue());
    }

    public boolean getState() {
        return (boolean) property.getValue();
    }

}
