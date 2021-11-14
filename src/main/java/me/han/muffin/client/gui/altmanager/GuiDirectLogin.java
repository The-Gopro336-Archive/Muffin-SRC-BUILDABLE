package me.han.muffin.client.gui.altmanager;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiDirectLogin extends GuiScreen {

	public GuiScreen parent;
	public GuiTextField usernameBox;
	public GuiPasswordField passwordBox;

	public GuiDirectLogin(GuiScreen paramScreen)
	{
		this.parent = paramScreen;
	}

	public void initGui() {
		Keyboard.enableRepeatEvents(true);
		buttonList.add(new GuiButton(1, width / 2 - 100, height / 4 + 96 + 12, "Login"));
		buttonList.add(new GuiButton(2, width / 2 - 100, height / 4 + 96 + 36, "Back"));
		usernameBox = new GuiTextField(0, this.fontRenderer, width / 2 - 100, 76 - 25, 200, 20);
		passwordBox = new GuiPasswordField(2, this.fontRenderer, width / 2 - 100, 116 - 25, 200, 20);
		usernameBox.setMaxStringLength(200);
		passwordBox.setMaxStringLength(200);
	}

	public void onGuiClosed()
	{
		Keyboard.enableRepeatEvents(false);
	}

	public void updateScreen() {
		usernameBox.updateCursorCounter();
		passwordBox.updateCursorCounter();
	}

	@Override
	public void mouseClicked(int x, int y, int b) throws IOException {
		usernameBox.mouseClicked(x, y, b);
		passwordBox.mouseClicked(x, y, b);
		super.mouseClicked(x, y, b);
	}

	@Override
	public void actionPerformed(GuiButton button) {
		if (button.id == 1) {
			new Thread(() -> {
				Account account = new Account(usernameBox.getText(), passwordBox.getText());
				if (account.isPremium()) {
					try {
						YggdrasilPayload.login(usernameBox.getText(), passwordBox.getText());
					} catch (Exception ignored) {}
				} else {
					YggdrasilPayload.loginOffline(usernameBox.getText());
				}
				Muffin.getInstance().getAltManagerConfig().saveAccounts();
			}).start();
			Globals.mc.displayGuiScreen(parent);
		} else if (button.id == 2) {
			Globals.mc.displayGuiScreen(parent);
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) {
		usernameBox.textboxKeyTyped(typedChar, keyCode);
		passwordBox.textboxKeyTyped(typedChar, keyCode);
		if (typedChar == '\t') {
			if (usernameBox.isFocused()) {
				usernameBox.setFocused(false);
				passwordBox.setFocused(true);
			} else if (passwordBox.isFocused()) {
				usernameBox.setFocused(false);
				passwordBox.setFocused(false);
			}
		}
		if (typedChar == '\r') {
			actionPerformed(buttonList.get(0));
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawString(this.fontRenderer, "Username", width / 2 - 100, 63 - 25, 0xA0A0A0);
		drawString(this.fontRenderer, "\2474*", width / 2 - 106, 63 - 25, 0xA0A0A0);
		drawString(this.fontRenderer, "Password", width / 2 - 100, 104 - 25, 0xA0A0A0);
		try {
			usernameBox.drawTextBox();
			passwordBox.drawTextBox();
		} catch(Exception err) {
			err.printStackTrace();
		}

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

}