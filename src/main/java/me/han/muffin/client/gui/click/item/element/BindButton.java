package me.han.muffin.client.gui.click.item.element;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.click.ClickGui;
import me.han.muffin.client.gui.click.item.Item;
import me.han.muffin.client.module.Module;
import me.han.muffin.client.module.modules.other.ClickGUI;
import me.han.muffin.client.utils.client.BindUtils;
import me.han.muffin.client.utils.color.ColourUtils;
import me.han.muffin.client.utils.render.RenderUtils;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;
import org.lwjgl.input.Keyboard;

public class BindButton extends Item {
    private final Module module;
    private boolean accepting = false;

    public BindButton(Module module) {
        super(module.getName());
        this.module = module;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        String keyName;
        if (!accepting) {
      //      keyName = "Bind " + Keyboard.getKeyName(module.getBind());
            keyName = "Bind " + BindUtils.INSTANCE.getFormattedKeyBind(module.getBind());
            RenderUtils.drawRect(x + 2, y, x + width, y + height, (!isHovering(mouseX, mouseY) ? 0x11555555 : 0x88555555));
        } else {
            keyName = "Press a key...";
            RenderUtils.drawRect(x + 2, y, x + width, y + height, (!isHovering(mouseX, mouseY) ? ColourUtils.toRGBA(red, green, blue, 135) : ColourUtils.toRGBA(red, green, blue, 165)));
        }

        RenderUtils.rectangle(x, y, x + 1, y + height, ColourUtils.toRGBA(red, green, blue, 165));

        if (ClickGui.getClickGui().guiFont != null)
            ClickGui.getClickGui().guiFont.drawStringWithShadow(keyName, x + 2.3F, y + 4f, getDisabledSettingTextColour());
        else
            Globals.mc.fontRenderer.drawStringWithShadow(keyName, x + 2.3F, y + 4f, getDisabledSettingTextColour());

    }

    @Override
    public void processMouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.processMouseClicked(mouseX, mouseY, mouseButton);
        if (isHovering(mouseX, mouseY) && mouseButton == 0) {
            accepting = true;
            Globals.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }

        if (mouseButton == 0) return;

        if (accepting) {
            module.setBind(-mouseButton);
            accepting = false;
        }
    }

    @Override
    public void processKeyPressed(char character, int key) {
        super.processKeyPressed(character, key);
        if (accepting) {
            if (key == ClickGUI.INSTANCE.getBind() || key == Keyboard.KEY_DELETE || key == Keyboard.KEY_ESCAPE) {
                module.setBind(Keyboard.KEY_NONE);
            } else {
                module.setBind(key);
            }
            accepting = false;
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
        //setHidden(!setting.isVisible());
    }

    @Override
    public int getHeight() {
        return 11 + ClickGUI.INSTANCE.getGuiHeight().getValue();
    }

}
