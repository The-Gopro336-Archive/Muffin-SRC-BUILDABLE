package me.han.muffin.client.gui.hud;

import me.han.muffin.client.gui.MuffinGuiScreen;
import me.han.muffin.client.gui.font.AWTFontRenderer;
import me.han.muffin.client.gui.hud.item.HudItem;
import me.han.muffin.client.manager.managers.HudManager;
import me.han.muffin.client.utils.color.ColourUtils;
import me.han.muffin.client.utils.render.RenderUtils;

import java.io.IOException;

public class GuiHud extends MuffinGuiScreen {
    public static GuiHud guiHud;
    private boolean hasClicked = false;
    private boolean dragging = false;
    private int clickedMouseX = 0;
    private int clickedMouseY = 0;

    public static GuiHud getGuiHud() {
        return guiHud == null ? guiHud = new GuiHud() : guiHud;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        AWTFontRenderer.Companion.setAssumeNonVolatile(true);
        this.drawDefaultBackground();

        HudItem lastHovered = null;

        for (HudItem item : HudManager.getHudManager().items) {
            if (!item.isHidden() && item.doRender(mouseX, mouseY, partialTicks)) lastHovered = item;
        }

        if (lastHovered != null) {
            /// Add to the back of the list for rendering
            HudManager.getHudManager().items.remove(lastHovered);
            HudManager.getHudManager().items.add(lastHovered);
        }

        if (hasClicked) {
            final float mouseX1 = Math.min(clickedMouseX, mouseX);
            final float mouseX2 = Math.max(clickedMouseX, mouseX);
            final float mouseY1 = Math.min(clickedMouseY, mouseY);
            final float mouseY2 = Math.max(clickedMouseY, mouseY);

            RenderUtils.drawOutlineRect(mouseX2, mouseY2, mouseX1, mouseY1, 1, ColourUtils.toRGBA(0, 225, 225, 155));
        //    RenderUtils.drawOutlineRect(mouseX2, mouseY2, mouseX1, mouseY1, 1, 0x75056EC6);
     //       RenderUtils.drawRect(mouseX1, mouseY1, mouseX2, mouseY2, 0x56EC6, 205);
            RenderUtils.rectangle(mouseX1, mouseY1, mouseX2, mouseY2, ColourUtils.toRGBA(51, 185, 185, 70));

            HudManager.getHudManager().items.forEach(item -> {
                if (!item.isHidden()) {
                    if (item.isHovering(mouseX1, mouseX2, mouseY1, mouseY2)) item.setSelected(true);
                    else if (item.isSelected()) item.setSelected(false);
                }
            });
        }

        AWTFontRenderer.Companion.setAssumeNonVolatile(false);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (HudItem item : HudManager.getHudManager().items) {
            if (!item.isHidden()) {
                if (item.onMouseClick(mouseX, mouseY, mouseButton))
                    return;
            }
        }

        hasClicked = true;
        clickedMouseX = mouseX;
        clickedMouseY = mouseY;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);

        HudManager.getHudManager().items.forEach(item -> {
            if (!item.isHidden()) {
                item.onMouseRelease(mouseX, mouseY, state);
                item.setMultiSelectedDragging(item.isSelected());
            }
        });

        hasClicked = false;
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        HudManager.getHudManager().items.forEach(item -> item.processKeyPressed(typedChar, keyCode));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        HudManager.getHudManager().items.forEach(HudItem::updateScreen);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        hasClicked = false;
        dragging = false;
        clickedMouseX = 0;
        clickedMouseY = 0;
    }

}