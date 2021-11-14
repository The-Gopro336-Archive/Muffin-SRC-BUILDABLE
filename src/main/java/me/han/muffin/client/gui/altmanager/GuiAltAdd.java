package me.han.muffin.client.gui.altmanager;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.manager.AccountManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiAltAdd extends GuiScreen {

	private final GuiScreen previousScreen;
	
	public GuiTextField usernameBox;
	public GuiPasswordField passwordBox;

	public GuiAltAdd(GuiScreen previousScreen) {
		this.previousScreen = previousScreen;
	}

	@Override
	public void initGui() {
		Keyboard.enableRepeatEvents(true);
		buttonList.add(new GuiButton(1, width / 2 - 100, height / 4 + 96 + 12, "Add"));
		buttonList.add(new GuiButton(2, width / 2 - 100, height / 4 + 96 + 36, "Back"));
		usernameBox = new GuiTextField(0, this.fontRenderer, width / 2 - 100, 76, 200, 20);
		passwordBox = new GuiPasswordField(2, this.fontRenderer, width / 2 - 100, 116, 200, 20);
		usernameBox.setMaxStringLength(200);
		passwordBox.setMaxStringLength(128);
	}

	@Override
	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawString(this.fontRenderer, "Username", width / 2 - 100, 63, 0xa0a0a0);
		drawString(this.fontRenderer, "Password", width / 2 - 100, 104, 0xa0a0a0);
		try{
			usernameBox.drawTextBox();
			passwordBox.drawTextBox();
		} catch(Exception err) {
			err.printStackTrace();
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		usernameBox.mouseClicked(mouseX, mouseY, mouseButton);
		passwordBox.mouseClicked(mouseX, mouseY, mouseButton);
		try {
			super.mouseClicked(mouseX, mouseY, mouseButton);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		super.keyTyped(typedChar, keyCode);

		usernameBox.textboxKeyTyped(typedChar, keyCode);
		passwordBox.textboxKeyTyped(typedChar, keyCode);
		if (typedChar == '\t') {
			if(usernameBox.isFocused()) {
				usernameBox.setFocused(false);
				passwordBox.setFocused(true);
			} else {
				usernameBox.setFocused(true);
				passwordBox.setFocused(false);
			}
		}
		if (typedChar == '\r') {
			actionPerformed(buttonList.get(0));
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);
		if (button.id == 1) {
			new Thread(() -> {
				if (!usernameBox.getText().trim().isEmpty()) {
					Account theAlt;
					if (passwordBox.getText().trim().isEmpty()) {
						theAlt = new Account(usernameBox.getText().trim());
					} else {
						theAlt = new Account(usernameBox.getText().trim(), passwordBox.getText().trim());
					}
					if (!AccountManager.altList.contains(theAlt)) AccountManager.altList.add(theAlt);
					Muffin.getInstance().getAltManagerConfig().saveAccounts();
				}
			}).start();
			Globals.mc.displayGuiScreen(previousScreen);
		} else if (button.id == 2) {
			Globals.mc.displayGuiScreen(previousScreen);
		}

	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		usernameBox.updateCursorCounter();
		passwordBox.updateCursorCounter();
	}

}