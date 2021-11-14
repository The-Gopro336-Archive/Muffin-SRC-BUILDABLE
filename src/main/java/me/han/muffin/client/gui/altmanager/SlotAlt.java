package me.han.muffin.client.gui.altmanager;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.manager.AccountManager;
import me.han.muffin.client.utils.color.ColourUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;

public class SlotAlt extends GuiSlot {
	private final GuiAltList aList;
	int selected;

	boolean hasClicked;

	public SlotAlt(Minecraft mc, GuiAltList aList) {
		super(mc, aList.width, aList.height, 32, aList.height - 60, AccountManager.slotHeight);
		this.aList = aList;
		this.selected = -1;
	}
	
	@Override
	protected int getSize()
	{
		return AccountManager.altList.size();
	}

	@Override
	protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
//		int var5 = left + width / 2 - getListWidth() / 2 + 2;
//		int var6 = top + 4 - (int) amountScrolled;
//		mouseX -= var5;
//		int var10000 = mouseY - var6;
//		if (mouseX <= 32) {
//			hasClicked = true;
//		} else {
//			this.selected = slotIndex;
//			if (isDoubleClick) {
//				Account account = AccountManager.altList.get(slotIndex);
//				Muffin.getInstance().getAltManagerConfig().saveAccounts();
//				try {
//					if (account.isPremium()) {
//						try {
//							YggdrasilPayload.login(account.getLabel(), account.getPassword());
//							account.errorMessage = "";
//						} catch (Exception ignored) {
//							account.errorMessage = "Invalid username or password.";
//						}
//					} else {
//						YggdrasilPayload.loginOffline(account.getLabel());
//					}
//				} catch (Exception ignored) {
//				}
//			}
//		}

		this.selected = slotIndex;
		if (isDoubleClick) {
			Account account = AccountManager.altList.get(slotIndex);
			Muffin.getInstance().getAltManagerConfig().saveAccounts();
			try {
				if (account.isPremium()) {
					try {
						YggdrasilPayload.login(account.getLabel(), account.getPassword());
						account.errorMessage = "";
					} catch (Exception ignored) {
						account.errorMessage = "Invalid username or password.";
					}
				} else {
					YggdrasilPayload.loginOffline(account.getLabel());
				}
			} catch (Exception ignored) {
			}
		}
	}

	@Override
	protected boolean isSelected(int slotIndex)
	{
		return this.selected == slotIndex;
	}
	
	protected int getSelected()
	{
		return this.selected;
	}

	@Override
	protected void drawBackground()
	{
		aList.drawDefaultBackground();
	}

	@Override
	public int getSlotHeight() {
		return super.getSlotHeight();
	}

	@Override
	protected void drawSlot(int slotIndex, int xPos, int yPos, int heightIn, int mouseXIn, int mouseYIn, float partialTicks) {
		Account theAlt = AccountManager.altList.get(slotIndex);
		if (theAlt == null) return;

		//theAlt.drawHead(xPos, yPos, 35, heightIn);
		if (theAlt.getUsername() != null && !theAlt.getUsername().isEmpty()) AccountManager.INSTANCE.drawImage(theAlt.getUsername(), xPos, yPos);

		int xAddon = xPos + 42;

		aList.drawString(aList.getLocalFontRenderer(), theAlt.getUsername(), xAddon, yPos + 1, 0xFFFFFF);

		if (theAlt.isPremium()) {
			aList.drawString(aList.getLocalFontRenderer(), "Premium", xAddon + aList.getLocalFontRenderer().getStringWidth(theAlt.getLabel()) + 5, yPos + 4, 0x00FF00);
			aList.drawString(aList.getLocalFontRenderer(), theAlt.getLabel(), xAddon, yPos + aList.getLocalFontRenderer().FONT_HEIGHT, 0xFFFFFF);
		} else {
			aList.drawString(aList.getLocalFontRenderer(), "Non-Premium", xAddon + aList.getLocalFontRenderer().getStringWidth(theAlt.getLabel()) + 5, yPos + 4, 0x990000);
		}

		aList.drawString(aList.getLocalFontRenderer(), theAlt.errorMessage, xAddon, yPos + 18, ColourUtils.Colors.RED);


//		if (mc.gameSettings.touchscreen || isMouseYWithinSlotBounds(mouseYIn) && getSlotIndexFromScreenCoords(mouseXIn, mouseYIn) == slotIndex) {
//			mc.getTextureManager().bindTexture(GuiAltList.SERVER_SELECTION);
//			RenderUtils.drawRect(xPos, yPos, xPos + 32, yPos + 32, ColourUtils.toRGBA(144, 144, 144, 80));
//			GlStateUtils.resetColour();
//
//			float xDiff = mouseX - xPos;
//			int yDiff = mouseY - yPos;
//
//			if (!DG.f$E(this.f$d).f$I) {
//				Gui.drawModalRectWithCustomSizedTexture(xPos, yPos, 0.0F, xDiff < 32 && xDiff > 16 ? 32.0F : 0.0F, 32, 32, 256.0F, 256.0F);
//				if (hasClicked && xDiff < 32 && xDiff > 16) {
//					DG.f$E(this.f$d).f$E(var1);
//					DG.f$E(this.f$d).f$M = var1;
//					hasClicked = false;
//				}
//			}
//		}


/*
		try {
			aList.drawCenteredString(aList.getLocalFontRenderer(), AccountManager.altList.get(slotIndex).getLabel(), width / 2, yPos + 2, 0xFFAAAAAA);
			aList.drawCenteredString(aList.getLocalFontRenderer(), theAlt.isPremium() ? theAlt.getPassword().replaceAll("(?s).", "*") : "Not Available", width / 2, yPos + 15, 0xFFAAAAAA);
		} catch (AccountException e) {
			e.printStackTrace();
		}
 */
	}

}