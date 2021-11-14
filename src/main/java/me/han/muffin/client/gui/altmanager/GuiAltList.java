package me.han.muffin.client.gui.altmanager;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.manager.AccountManager;
import net.minecraft.client.gui.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiAltList extends GuiScreen {
	public boolean deleteMenuOpen = false;
	private boolean valid;
	private boolean checkedValid;

	public static final ResourceLocation SERVER_SELECTION = new ResourceLocation("textures/gui/server_selection.png");

	private GuiScreen previousScreen;

	public GuiAltList(GuiScreen previousScreen) {
		this.previousScreen = previousScreen;
	}

	public FontRenderer getLocalFontRenderer() {
		return fontRenderer;
	}

	public void onGuiClosed() {
		super.onGuiClosed();
	}

	private SlotAlt tSlot;

	public void initGui() {
		valid = false;
		checkedValid = false;
		new Thread(() -> {
			valid = !YggdrasilPayload.isOffline() && YggdrasilPayload.sessionValid();
			checkedValid = true;
		}).start();

		buttonList.clear();
		buttonList.add(new GuiButton(5, width / 2 - 105, height - 47, 100, 20, "Direct Login"));
		buttonList.add(new GuiButton(2, width / 2 + 5, height - 47, 100, 20, "Login"));

		buttonList.add(new GuiButton(1, width / 2 - 105, height - 26, 66, 20, "Add"));
		buttonList.add(new GuiButton(3, width / 2 - 33, height - 26, 66, 20, "Remove"));
		buttonList.add(new GuiButton(4, width / 2 + 39, height - 26, 66, 20, "Cancel"));

		buttonList.add(new GuiButton(6, width / 2 + 105, height - 26, 66, 20, "Edit"));

		tSlot = new SlotAlt(Globals.mc, this);
		tSlot.registerScrollButtons(7, 8);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) {
		if (keyCode == Keyboard.KEY_UP) {
			tSlot.selected--;
		}
		if (keyCode == Keyboard.KEY_DOWN) {
			tSlot.selected++;
		}

		if (keyCode == Keyboard.KEY_ESCAPE) {
			Globals.mc.displayGuiScreen(previousScreen);
		}

		if (keyCode == Keyboard.KEY_RETURN) {
			Account account = AccountManager.altList.get(tSlot.selected);
			try {
				if (account.isPremium()) {
					new Thread(() -> {
						try {
							YggdrasilPayload.login(account.getLabel(), account.getPassword());
						} catch (Exception error) {
							AccountManager.altList.remove(account);
							account.errorMessage = "Invalid username or password.";
						}
					}).start();
				} else {
					YggdrasilPayload.loginOffline(account.getLabel());
				}
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * Handles mouse input.
	 */
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		tSlot.handleMouseInput();
	}

	@Override
	public void confirmClicked(boolean result, int id) {
		super.confirmClicked(result, id);
		if (deleteMenuOpen) {
			deleteMenuOpen = false;
			if (result) {
				AccountManager.altList.remove(id);
			}
			mc.displayGuiScreen(this);
		}
	}

	public void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);
		if (button.id == 1) {
			GuiAltAdd gaa = new GuiAltAdd(this);
			mc.displayGuiScreen(gaa);
		}
		if (button.id == 2) {
			try {
				Account account = AccountManager.altList.get(tSlot.getSelected());
				if (account.isPremium()) {
					try {
						YggdrasilPayload.login(account.getLabel(), account.getPassword());
						account.errorMessage = "";
					} catch (Exception error) {
						account.errorMessage = "Invalid username or password.";
					}
				} else {
					YggdrasilPayload.loginOffline(account.getLabel());
					account.errorMessage = "";
				}
			} catch (Exception ignored) {
			}
		}
		if (button.id == 3) {
			try {
				String s1 = "Delete the alt: " + "\"" + AccountManager.altList.get(tSlot.getSelected()).getLabel() + "\"" + "?";
				String s3 = "Delete";
				String s4 = "Cancel";
				GuiYesNo guiyesno = new GuiYesNo(this, s1, "", s3, s4, tSlot.getSelected());
				deleteMenuOpen = true;
				mc.displayGuiScreen(guiyesno);
			} catch (Exception ignored) {
			}
		}
		if (button.id == 4) {
			mc.displayGuiScreen(new GuiMainMenu());
		}
		if (button.id == 5) {
			mc.displayGuiScreen(new GuiDirectLogin(this));
		}
		if (button.id == 6) {
			mc.displayGuiScreen(new GuiAccountEdit(this, tSlot));
		}
	}

	public void updateScreen() {
		super.updateScreen();
		AccountManager.INSTANCE.checkStatus();
	}

	@Override
	public void drawScreen(int i, int j, float f) {
		tSlot.drawScreen(i, j, f);

		if (tSlot.getSelected() < 0) {
			buttonList.get(1).enabled = false;
			buttonList.get(3).enabled = false;
			buttonList.get(5).enabled = false;
		} else {
			buttonList.get(1).enabled = true;
			buttonList.get(3).enabled = true;
			buttonList.get(5).enabled = true;
		}

		AccountManager.INSTANCE.drawString();

		drawCenteredString(fontRenderer, "Accounts: " + AccountManager.altList.size(), width / 2, 13, 0xFFFFFF);
		fontRenderer.drawStringWithShadow(TextFormatting.GRAY + "Username: " + TextFormatting.RESET + Globals.mc.getSession().getUsername(), 2, 20, 0xFFFFFF);
		renderStats(2, 20 + Globals.mc.fontRenderer.FONT_HEIGHT);
		super.drawScreen(i, j, f);
	}

	private void renderStats(int x, int y) {
		//String line1 = mc.getSession().getUsername();
		String line2 = TextFormatting.GRAY + "Status: " + (YggdrasilPayload.isOffline() ? TextFormatting.RED + "Cracked" : (checkedValid ? (valid ? TextFormatting.GREEN + "Premium" : TextFormatting.YELLOW + "Invalid Session") : TextFormatting.GRAY + "Unknown Status"));
		//int width1 = fontRenderer.getStringWidth(line1);
		int width2 = fontRenderer.getStringWidth(line2);
		//int width = Math.max(width1, width2) + 2;
		//GuiScreen.drawRect(x - width / 2, y - fontRenderer.FONT_HEIGHT * 2 - 2, x + width / 2, y, new Color(0, 0, 0, 70).getRGB());
		//fontRenderer.drawStringWithShadow(line1, x, y - fontRenderer.FONT_HEIGHT * 2 - 1, -1);
		fontRenderer.drawStringWithShadow(line2, x, y, -1);
	}

}