package me.han.muffin.client.gui.click.item.element;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.click.ClickGui;
import me.han.muffin.client.gui.click.item.Item;
import me.han.muffin.client.module.modules.other.ClickGUI;
import me.han.muffin.client.utils.client.BindUtils;
import me.han.muffin.client.utils.color.ColourUtils;
import me.han.muffin.client.utils.render.RenderUtils;
import me.han.muffin.client.value.BindValue;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;
import org.lwjgl.input.Keyboard;

public class BindValueButton extends Item {
    private final BindValue value;
    private boolean accepting = false;

    public BindValueButton(BindValue value) {
        super(value.getAliases()[0]);
        this.value = value;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        String keyName;
        if (!this.accepting) {
         //   keyName = value.getAliases()[0] + " " + Keyboard.getKeyName(value.getValue());
            keyName = value.getAliases()[0] + " " + BindUtils.INSTANCE.getFormattedKeyBind(value.getValue());
            RenderUtils.drawRect(x + 2, y, x + width, y + height, (!isHovering(mouseX, mouseY) ? 0x11555555 : 0x88555555));
        } else {
            keyName = "Press a key...";
            RenderUtils.drawRect(x + 2, y, x + width, y + height, (!isHovering(mouseX, mouseY) ? ColourUtils.toRGBA(this.red, this.green, this.blue, 135) : ColourUtils.toRGBA(this.red, this.green, this.blue, 165)));
        }

        RenderUtils.rectangle(x, y, x + 1, y + height, ColourUtils.toRGBA(this.red, this.green, this.blue, 165));

        if (ClickGui.getClickGui().guiFont != null)
            ClickGui.getClickGui().guiFont.drawStringWithShadow(keyName, x + 2.3F, y + 4f, getDisabledSettingTextColour());
        else
            Globals.mc.fontRenderer.drawStringWithShadow(keyName, x + 2.3F, y + 4f, getDisabledSettingTextColour());

    }

    @Override
    public void processMouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.processMouseClicked(mouseX, mouseY, mouseButton);
        if (this.isHovering(mouseX, mouseY) && mouseButton == 0) {
            this.accepting = true;
            Globals.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }

        if (mouseButton == 0) return;
        if (this.accepting) {
            value.setValue(-((Integer) mouseButton));
            this.accepting = false;
        }
    }

    @Override
    public void processKeyPressed(char character, int key) {
        super.processKeyPressed(character, key);
        if (this.accepting) {
            if (key == ClickGUI.INSTANCE.getBind() || key == (Integer) Keyboard.KEY_DELETE || key == (Integer) Keyboard.KEY_ESCAPE) {
                value.setValue((Integer) Keyboard.KEY_NONE);
            } else {

                value.setValue((Integer) key);
            }
            this.accepting = false;
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        accepting = false;
    }

    @Override
    public void update() {
        super.update();
        this.setHidden(!this.value.isVisible());
    }

    @Override
    public int getHeight() {
        return 11 + ClickGUI.INSTANCE.getGuiHeight().getValue();
    }

}