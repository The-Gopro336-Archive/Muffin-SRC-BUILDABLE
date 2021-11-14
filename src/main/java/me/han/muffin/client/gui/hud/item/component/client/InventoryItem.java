package me.han.muffin.client.gui.hud.item.component.client;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.hud.item.HudItem;
import me.han.muffin.client.utils.color.ColourUtils;
import me.han.muffin.client.utils.render.RenderUtils;
import me.han.muffin.client.value.EnumValue;
import me.han.muffin.client.value.NumberValue;
import me.han.muffin.client.value.Value;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class InventoryItem extends HudItem {
    private final EnumValue<Mode> mode = new EnumValue<>(Mode.Client, "Mode");
    public final Value<Boolean> hotbar = new Value<>(false, "Hotbar");
    public final Value<Boolean> xCarry = new Value<>(false, "XCarry");
    public final Value<Boolean> background = new Value<>(true, "Background");
    public final NumberValue<Float> scale = new NumberValue<>(1.0f, 0.0f, 10.0f, 1.0f,"Scale");

    private static final ResourceLocation RESOURCE_INVENTORY = new ResourceLocation("textures/gui/container/generic_54.png");

    public InventoryItem() {
        super("Inventory", HudCategory.Client, 2, 15);
        addSettings(mode, hotbar, xCarry, background, scale);
    }

    private enum Mode {
        TexturePack, Gray, Client
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks){
        super.drawScreen(mouseX, mouseY, partialTicks);

        GlStateManager.pushMatrix();

        GlStateManager.scale(scale.getValue(), scale.getValue(), scale.getValue());

        if (background.getValue()) {

            int red = Muffin.getInstance().getFontManager().getPublicRed();
            int green = Muffin.getInstance().getFontManager().getPublicGreen();
            int blue = Muffin.getInstance().getFontManager().getPublicBlue();

            GlStateManager.disableDepth();

            if (mode.getValue() == Mode.Gray) {
                RenderUtils.drawRect(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x75101010); // background
            } else if (mode.getValue() == Mode.TexturePack) {
                Globals.mc.renderEngine.bindTexture(RESOURCE_INVENTORY);
                GlStateManager.color(1, 1, 1, 1);
                Globals.mc.ingameGUI.drawTexturedModalRect(getX(), getY() - 1, 7, 17, 162, 54);
            } else if (mode.getValue() == Mode.Client) {
                RenderUtils.drawRect(getX(), getY(), getX() + getWidth(), getY() + getHeight(), ColourUtils.toRGBA(red, green, blue, 105)); // background
            }

            if (xCarry.getValue())
                RenderUtils.drawRect(getX() + getWidth(), getY(), getX() + getWidth() + 32, getY() + 36, 0x75101010); // background

            if (hotbar.getValue())
                RenderUtils.drawRect(getX(), getY() + 53, getX() + getWidth(), getY() + 71, 0x75101010); // background

            GlStateManager.enableDepth();

        }

        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);

        GlStateManager.pushMatrix();
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();

        Globals.mc.getRenderItem().zLevel = 0.0F;

        for (int i = 0; i < 27; i++) {
            ItemStack itemStack = Globals.mc.player.inventory.mainInventory.get(i + 9);
            int offsetX = getX() + i % 9 * 18;
            int offsetY = getY() + i / 9 * 18;
            Globals.mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, offsetX + 2, offsetY);
            Globals.mc.getRenderItem().renderItemOverlays(Globals.mc.fontRenderer, itemStack, offsetX + 2, offsetY);
        }

        if (hotbar.getValue()) {
            for (int i = 0; i < 9; i++) {
                ItemStack itemStack = Globals.mc.player.inventory.mainInventory.get(i);
                int offsetX = getX() + (i % 9) * 18;
                int offsetY = getY() + 53;
                Globals.mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, offsetX + 2, offsetY);
                Globals.mc.getRenderItem().renderItemOverlays(Globals.mc.fontRenderer, itemStack, offsetX + 2, offsetY);
            }
        }

        if (xCarry.getValue()) {

            for (int i = 1; i < 5; i++) {
                ItemStack itemStack = Globals.mc.player.inventoryContainer.getInventory().get(i);

                int offsetX = getX() + 2;
                int offsetY = getY();

                switch (i) {
                    case 1:
                    case 2:
                        offsetX += 128 + (i * 16);
                        break;
                    case 3:
                    case 4:
                        offsetX += 128 + ((i - 2) * 16);
                        offsetY += 16;
                        break;
                }

                Globals.mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, offsetX, offsetY);
                Globals.mc.getRenderItem().renderItemOverlays(Globals.mc.fontRenderer, itemStack, offsetX, offsetY);
            }
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GlStateManager.popMatrix();

        setWidth(18 * 9);
        setHeight(18 * (hotbar.getValue() ? 4 : 3));

        Globals.mc.getRenderItem().zLevel = 0.0F;
        GlStateManager.popMatrix();
    }


}