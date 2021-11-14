package me.han.muffin.client.gui.hud.item.component.client;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.hud.item.HudItem;
import me.han.muffin.client.utils.color.ColourUtils;
import me.han.muffin.client.utils.render.GlStateUtils;
import me.han.muffin.client.value.NumberValue;
import me.han.muffin.client.value.Value;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemAir;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ArmourItem extends HudItem {
    private final Value<Boolean> durability = new Value<>(true, "Durability");
    private final NumberValue<Float> scale = new NumberValue<>(1.0F, 0.25F, 2.0F, 0.05F, "Scale");

    public ArmourItem() {
        super("Armour", HudCategory.Client, 50, 50);
        addSettings(durability);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawArmour(getX() + 6, getY() + 12);
        setWidth(83);

        if (durability.getValue()) setHeight(22 + Muffin.getInstance().getFontManager().getStringHeight());
        else setHeight(22);
    }

    private void drawArmour(int posX, int posY) {

        GlStateUtils.matrix(true);
        GlStateUtils.depth(true);
        RenderHelper.enableGUIStandardItemLighting();

        for (int i = 3; i >= 0; --i) {
            final ItemStack stack = Globals.mc.player.inventory.armorInventory.get(i);

            if (stack.getItem() instanceof ItemAir) continue;

            int betterY = 0;

            if (Globals.mc.player.isInsideOfMaterial(Material.WATER) && Globals.mc.player.getAir() > 0 && !Globals.mc.player.capabilities.isCreativeMode) {
                betterY = 10;
            } else if (Globals.mc.player.getRidingEntity() != null && !Globals.mc.player.capabilities.isCreativeMode) {
                if (Globals.mc.player.getRidingEntity() instanceof EntityLivingBase) {
                    betterY = -20 + (int) Math.ceil((((EntityLivingBase) Globals.mc.player.getRidingEntity()).getMaxHealth() - 1.0f) / 20.0f) * 10;
                } else {
                    betterY = -20;
                }
            } else if (Globals.mc.player.capabilities.isCreativeMode) {
                betterY = (Globals.mc.player.isRidingHorse() ? 20 : 13);
            }

            int newPosY = posY - betterY;
            Globals.mc.getRenderItem().zLevel = 200F;
            Globals.mc.getRenderItem().renderItemIntoGUI(stack, posX, newPosY);
            Globals.mc.getRenderItem().renderItemOverlays(Globals.mc.fontRenderer, stack, posX, newPosY);
            posX += 18;
            Globals.mc.getRenderItem().zLevel = 0F;

            String s = stack.getCount() > 1 ? stack.getCount() + "" : "";

            Muffin.getInstance().getFontManager().drawStringWithShadow(
                    s,
                    posX + 17 - Muffin.getInstance().getFontManager().getStringWidth(s),
                    newPosY + 9,
                    0xffffff);

            GlStateUtils.lighting(false);

            if (durability.getValue()) {
                float green = ((float) stack.getMaxDamage() - (float) stack.getItemDamage()) / (float) stack.getMaxDamage();
                float red = 1 - green;
                int dmg = 100 - (int) (red * 100);

                Muffin.getInstance().getFontManager().drawStringWithShadow(dmg + "",
                        posX - 10 - Muffin.getInstance().getFontManager().getStringWidth(dmg + "") / 2,
                        newPosY - 9, ColourUtils.toHex((int) (red * 255), (int) (green * 255), 0));
            }

        }

        RenderHelper.disableStandardItemLighting();
        GlStateUtils.matrix(false);
    }

    private boolean isEyeInWater() {
        Vec3d eyePos = Globals.mc.player.getPositionEyes(1f);
        BlockPos flooredEyePos = new BlockPos(Math.floor(eyePos.x), Math.floor(eyePos.y), Math.floor(eyePos.z));
        Block block = Globals.mc.world.getBlockState(flooredEyePos).getBlock();
        return block == Blocks.WATER || block == Blocks.FLOWING_WATER;
    }

    private void doFuture(int posX, int posY) {

        if (Globals.mc.currentScreen instanceof GuiChat) return;

        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 3; i >= 0; --i) {
            ItemStack itemStack = Globals.mc.player.inventory.armorInventory.get(i);
            if (!(itemStack.getItem() instanceof ItemAir)) {
                int n4;

                if (Globals.mc.player.isInsideOfMaterial(Material.WATER) && Globals.mc.player.getAir() > 0 && !Globals.mc.player.capabilities.isCreativeMode) {
                    n4 = 65;
                } else if (Globals.mc.player.getRidingEntity() != null && !Globals.mc.player.capabilities.isCreativeMode) {
                    if (Globals.mc.player.getRidingEntity() instanceof EntityLivingBase) {
                        EntityLivingBase entityLivingBase = (EntityLivingBase) Globals.mc.player.getRidingEntity();
                        n4 = 45 + (int)Math.ceil((entityLivingBase.getMaxHealth() - 1.0f) / 20.0f) * 10;
                    } else {
                        n4 = 45;
                    }
                } else {
                    n4 = Globals.mc.player.capabilities.isCreativeMode ? (Globals.mc.player.isRidingHorse() ? 45 : 38) : 55;
                }

                Globals.mc.getRenderItem().renderItemIntoGUI(itemStack, posX, posY - n4);
                int n5 = posX;
                posX += 18;
                Globals.mc.getRenderItem().renderItemOverlays(Globals.mc.fontRenderer, itemStack, n5, posY - n4);
            }
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    private void drawArmor(int posX, int posY) {

        GlStateManager.pushMatrix();
        GlStateManager.enableTexture2D();

        int iteration = 0;
        int y = posY + (durability.getValue() ? 10 : 0) - (isEyeInWater() ? 10 : 0);
        // int y = posY + (durability.getValue() ? 10 : 0) - (Globals.mc.player.isInWater() ? 10 : 0);

        for (ItemStack is : Globals.mc.player.inventory.armorInventory) {
            iteration++;

            if (is.isEmpty())
                continue;

            int x = posX - 100 + (9 - iteration) * 20 + 2;
            GlStateManager.enableDepth();

            Globals.mc.getRenderItem().zLevel = 200F;
            int n = 15;
            for (int i = 3; i > 0; --i) {
                if (!(Globals.mc.player.inventory.armorInventory.get(i).getItem() instanceof ItemAir)) {
                    Globals.mc.getRenderItem().renderItemAndEffectIntoGUI(is, x, y);
                    Globals.mc.getRenderItem().renderItemOverlayIntoGUI(Globals.mc.fontRenderer, is, x + n, y, "");
                    n += 18;
                }
            }
            Globals.mc.getRenderItem().zLevel = 0F;

            GlStateManager.enableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();

            String s = is.getCount() > 1 ? is.getCount() + "" : "";

            Muffin.getInstance().getFontManager().drawStringWithShadow(s, x + 19 - 2 - Muffin.getInstance().getFontManager().getStringWidth(s), y + 9, 0xffffff);

            if (durability.getValue()) {
                float green = ((float) is.getMaxDamage() - (float) is.getItemDamage()) / (float) is.getMaxDamage();
                float red = 1 - green;
                int dmg = 100 - (int) (red * 100);

                Muffin.getInstance().getFontManager().drawStringWithShadow(dmg + "", x + 8 - Muffin.getInstance().getFontManager().getStringWidth(dmg + "") / 2, y - 11, ColourUtils.toHex((int) (red * 255), (int) (green * 255), 0));
            }

        }

        GlStateManager.enableDepth();
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();

    }

}