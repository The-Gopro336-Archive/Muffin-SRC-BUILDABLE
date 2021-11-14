package me.han.muffin.client.gui.tabgui;

import me.han.muffin.client.gui.MuffinGuiScreen;

import java.io.IOException;

public class TabGui extends MuffinGuiScreen {

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return super.doesGuiPauseGame();
    }

}