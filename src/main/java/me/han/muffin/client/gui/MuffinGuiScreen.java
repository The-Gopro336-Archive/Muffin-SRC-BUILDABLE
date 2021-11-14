package me.han.muffin.client.gui;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.module.modules.movement.NoSlowModule;
import me.han.muffin.client.module.modules.player.FreecamModule;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class MuffinGuiScreen extends GuiScreen {

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
    public void updateScreen() {
        super.updateScreen();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public static void updateRotationContainer() {
        if (Globals.mc.player != null && Globals.mc.world != null && isGuiWalkEnabled()) {
            if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
                updateRotationPitch(-2.0f);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
                updateRotationPitch(2.0f);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
                updateRotationYaw(2.0f);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
                updateRotationYaw(-2.0f);
            }
        }
    }

    private static void updateRotationPitch(float pitch) {

        float newRotation = Globals.mc.player.rotationPitch + pitch;

        newRotation = Math.max(newRotation, -90.0f);
        newRotation = Math.min(newRotation, 90.0f);

        if (FreecamModule.INSTANCE.isEnabled() && FreecamModule.INSTANCE.getCameraGuy() != null) {
            float viewPitch = FreecamModule.INSTANCE.getCameraGuy().rotationPitch + pitch;
            viewPitch = Math.max(viewPitch, -90.0f);
            viewPitch = Math.min(viewPitch, 90.0f);
            FreecamModule.INSTANCE.getCameraGuy().rotationPitch = viewPitch;
            return;
        }

        Globals.mc.player.rotationPitch = newRotation;
    }

    private static void updateRotationYaw(float yaw) {

        float newRotation = Globals.mc.player.rotationYaw + yaw;

        // l_NewRotation = Math.min(l_NewRotation, -360.0f);
        // l_NewRotation = Math.max(l_NewRotation, 360.0f);

        if (FreecamModule.INSTANCE.isEnabled() && FreecamModule.INSTANCE.getCameraGuy() != null) {
            FreecamModule.INSTANCE.getCameraGuy().rotationYaw += yaw;
            return;
        }

        Globals.mc.player.rotationYaw = newRotation;
    }

    private static boolean isGuiWalkEnabled() {
        return NoSlowModule.INSTANCE.getInventoryWalk().getValue();
    }

}