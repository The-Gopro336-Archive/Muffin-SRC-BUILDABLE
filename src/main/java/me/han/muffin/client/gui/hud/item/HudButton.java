package me.han.muffin.client.gui.hud.item;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.click.item.Button;
import me.han.muffin.client.gui.click.item.Item;
import me.han.muffin.client.gui.click.item.element.*;
import me.han.muffin.client.module.modules.other.ClickGUI;
import me.han.muffin.client.utils.timer.Timer;
import me.han.muffin.client.value.*;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public class HudButton extends Button {

    private final HudItem hudItem;
    private final List<Item> items = new ArrayList<>();
    private boolean subOpen;
    public final Timer buttonTimer = new Timer();

    public HudButton(HudItem hudItem) {
        super(hudItem.getDisplayName());
        this.hudItem = hudItem;

        if (!hudItem.valueList.isEmpty()) {
            for (Value property : hudItem.valueList) {
                if (property.getValue() instanceof Boolean) {
                    items.add(new BooleanButton(property));
                }
                if (property instanceof EnumValue) {
                    items.add(new EnumButton((EnumValue) property));
                }
                if (property instanceof NumberValue) {
                    items.add(new NumberSlider((NumberValue) property));
                }
                if (property instanceof StringValue) {
                    items.add(new StringButton((StringValue) property));
                }
                if (property instanceof ColourValue) {
                    items.add(new ColorButton((ColourValue) property));
                }
                if (property instanceof BindValue) {
                    items.add(new BindValueButton((BindValue) property));
                }
            }
        }

    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (!items.isEmpty()) {
     //       RenderUtils.rectangle(x + 73, y, x + 75f, y + height - 4, ColourUtils.toRGBA(this.red, this.green, this.blue,245));
            if (subOpen) {
                float height = 0;
                int ms = 20;

                for (Item item : items) {
                    item.update();

                    if (item.isHidden()) continue;

                    height += 12F + ClickGUI.INSTANCE.getGuiHeight().getValue();
                    if (ClickGUI.INSTANCE.getSettingAnimation().getValue()) if (buttonTimer.passed(ms)) ms += 20; else break;

                    item.setLocation(x + 1, y + height);
                    item.setHeight(12 + ClickGUI.INSTANCE.getGuiHeight().getValue());
                    item.setWidth(width - 2);
                    item.updateColor();
                    item.updateTextColor();
                    item.drawScreen(mouseX, mouseY, partialTicks);

                }

            }
        }
    }

    @Override
    public void processMouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.processMouseClicked(mouseX, mouseY, mouseButton);
        if (!items.isEmpty()) {
            if (mouseButton == 0 && isHovering(mouseX, mouseY)) {
            }

            if (mouseButton == 1 && isHovering(mouseX, mouseY)) {
                subOpen = !subOpen;
                Globals.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                buttonTimer.reset();
            }

            if (subOpen) {
                for (Item item : items) {
                    if (item.isHidden())
                        continue;

                    item.processMouseClicked(mouseX, mouseY, mouseButton);
                }
            }

        }
    }

    @Override
    public void processMouseReleased(int mouseX, int mouseY, int releaseButton) {
        super.processMouseReleased(mouseX, mouseY, releaseButton);

        if (subOpen) {
            for (Item item : items) {

                if (item.isHidden()) continue;
                item.processMouseReleased(mouseX, mouseY, releaseButton);
            }
        }

    }

    @Override
    public void processKeyPressed(char character, int key) {
        super.processKeyPressed(character, key);
        if (subOpen) {
            for (Item item : items) {

                if (item.isHidden())
                    continue;

                item.processKeyPressed(character, key);

            }
        }
    }

    @Override
    public void onInitGui() {
        super.onInitGui();
        if (ClickGUI.INSTANCE.getPanelAnimation().getValue())
            buttonTimer.reset();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        if (ClickGUI.INSTANCE.getPanelAnimation().getValue())
            buttonTimer.reset();
    }

    public int getHeight() {

        if (subOpen) {
            int height = 13 + ClickGUI.INSTANCE.getGuiHeight().getValue();
            int ms = 20;
            for (Item item : items) {
                if (item.isHidden()) continue;
                if (ClickGUI.INSTANCE.getSettingAnimation().getValue()) if (buttonTimer.passed(ms)) ms += 20; else break;
                height += item.getHeight() + 1;
            }

            return height + 2;
        } else {

            //    if (fade < 14) {
            //       fade += 2 * Globals.mc.getRenderPartialTicks();
            //   }
            //     if (fade > 14) {
            ///         fade = 14;
            //     }

            //      return fade;
            return 14 + ClickGUI.INSTANCE.getGuiHeight().getValue();
        }
    }

    public void toggle() {
        hudItem.setHidden(!hudItem.isHidden());
    }

    public boolean getState() {
        return !hudItem.isHidden();
    }

}