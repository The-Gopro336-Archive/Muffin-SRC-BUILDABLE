package me.han.muffin.client.gui.click.item.element;

import com.mojang.realmsclient.gui.ChatFormatting;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.click.ClickGui;
import me.han.muffin.client.gui.click.item.Item;
import me.han.muffin.client.utils.color.ColourUtils;
import me.han.muffin.client.utils.render.RenderUtils;
import me.han.muffin.client.value.StringValue;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;

public class StringButton extends Item {
    private final StringValue value;
    private boolean isTyping = false;

    public StringButton(StringValue value) {
        super(value.getAliases()[0]);
        this.value = value;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        String buttonName = value.getAliases()[0] + " " + ChatFormatting.GRAY + value.getValue();


        if (isTyping)
            RenderUtils.drawRect(x + 2, y, x + width, y + height, (!isHovering(mouseX, mouseY) ? ColourUtils.toRGBA(this.red, this.green, this.blue, 135) : ColourUtils.toRGBA(this.red, this.green, this.blue, 165)));
        else
            RenderUtils.drawRect(x + 2, y, x + width, y + height, (!isHovering(mouseX, mouseY) ? 0x11555555 : 0x88555555));

        RenderUtils.rectangle(x, y, x + 1, y + height, ColourUtils.toRGBA(this.red, this.green, this.blue, 165));

        if (ClickGui.getClickGui().guiFont != null)
            ClickGui.getClickGui().guiFont.drawStringWithShadow(buttonName, x + 2.3F, y + 4f, getDisabledSettingTextColour());
        else
            Globals.mc.fontRenderer.drawStringWithShadow(buttonName, x + 2.3F, y + 4f, getDisabledSettingTextColour());

    }

    @Override
    public void processMouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.processMouseClicked(mouseX, mouseY, mouseButton);
        if (isHovering(mouseX, mouseY) && mouseButton == 0) {
            isTyping = true;
            Globals.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        isTyping = false;
        value.setValue(value.getValue());
    }


    @Override
    public void processKeyPressed(char character, int key) {
        super.processKeyPressed(character, key);
        if (!isTyping)
            return;

        if (key == Keyboard.KEY_RETURN) {
            isTyping = false;
        }

        if (key == Keyboard.KEY_BACK && !Keyboard.isRepeatEvent() && this.value.getValue().length() > 0) {
            this.value.setValue(this.value.getValue().substring(0, value.getValue().length() - 1));
            return;
        }

        if (isTyping && ChatAllowedCharacters.isAllowedCharacter(character)) {
            value.setValue(value.getValue() + character);
        }

    }

    @Override
    public void update() {
        super.update();
        this.setHidden(!value.isVisible());
    }

}