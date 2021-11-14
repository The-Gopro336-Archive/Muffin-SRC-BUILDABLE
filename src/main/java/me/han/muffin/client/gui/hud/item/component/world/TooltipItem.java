package me.han.muffin.client.gui.hud.item.component.world;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.hud.item.HudItem;
import me.han.muffin.client.utils.render.RenderUtils;
import me.han.muffin.client.utils.timer.Timer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TooltipItem extends HudItem {

    public TooltipItem() {
        super("ContainerTooltip", HudCategory.World, 2, 50);
        setWidth(80);
        setHeight(60);
    }


    private static final ResourceLocation RESOURCE_INVENTORY = new ResourceLocation("textures/gui/container/generic_54.png");
    public Map<EntityPlayer, ItemStack> spiedPlayers = new ConcurrentHashMap<>();
    public Map<EntityPlayer, Timer> playerTimers = new ConcurrentHashMap<>();
    //    private int textRadarY = 0;
    Timer timer = new Timer();

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (Globals.mc.player == null || Globals.mc.world == null) return;

        int x = -4 + getX();
        int y = 10 + getY();
        //   textRadarY = 0;

        for (EntityPlayer player : Globals.mc.world.playerEntities) {
            if (player == null || player.getHeldItemMainhand().isEmpty() || !(player.getHeldItemMainhand().getItem() instanceof ItemShulkerBox))
                continue;

            ItemStack mainHandStack = player.getHeldItemMainhand();
            spiedPlayers.put(player, mainHandStack);
        }

        for (EntityPlayer player : Globals.mc.world.playerEntities) {
            Timer playerTimer = playerTimers.get(player);

            if (spiedPlayers.get(player) == null) continue;

            if (player.getHeldItemMainhand().isEmpty() || !(player.getHeldItemMainhand().getItem() instanceof ItemShulkerBox)) {
                playerTimer = playerTimers.get(player);

                if (playerTimer == null) {
                    timer.reset();
                    playerTimers.put(player, timer);
                } else if (playerTimer.passed(2 * 1000)) {
                    continue;
                }

            } else if (player.getHeldItemMainhand().getItem() instanceof ItemShulkerBox && playerTimer != null) {
                playerTimer.reset();
                playerTimers.put(player, playerTimer);
            }

            ItemStack stack = spiedPlayers.get(player);
            renderShulkerToolTip(stack, x, y, player.getName());
            //       textRadarY = (y += 18 + 60) - 10 - getY() + 2;
        }



    }

    public void renderShulkerToolTip(ItemStack stack, int x, int y, String name) {
        NBTTagCompound blockEntityTag;
        NBTTagCompound tagCompound = stack.getTagCompound();

        if (tagCompound != null && tagCompound.hasKey("BlockEntityTag", 10) && (blockEntityTag = tagCompound.getCompoundTag("BlockEntityTag")).hasKey("Items", 9)) {
            GlStateManager.pushMatrix();
            GlStateManager.enableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            Globals.mc.getTextureManager().bindTexture(RESOURCE_INVENTORY);

            RenderUtils.drawTexturedRect(x, y, 0, 0, 176, 16, 500);
            RenderUtils.drawTexturedRect(x, y + 16, 0, 16, 176, 54 + 3, 500);
            RenderUtils.drawTexturedRect(x, y + 16 + 54, 0, 160, 176, 8, 500);

            GlStateManager.disableDepth();
            int red = Muffin.getInstance().getFontManager().getPublicRed();
            int green = Muffin.getInstance().getFontManager().getPublicGreen();
            int blue = Muffin.getInstance().getFontManager().getPublicBlue();
            Color color = new Color(red, green, blue, 255);

            Globals.mc.fontRenderer.drawStringWithShadow(name == null ? stack.getDisplayName() : name, x + 8, y + 6, color.getRGB());
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableColorMaterial();
            GlStateManager.enableLighting();
            NonNullList<ItemStack> nonNullList = NonNullList.withSize(27, ItemStack.EMPTY);

            ItemStackHelper.loadAllItems(blockEntityTag, nonNullList);
            for (int i = 0; i < nonNullList.size(); ++i) {
                int iX = x + i % 9 * 18 + 8;
                int iY = y + i / 9 * 18 + 18;
                ItemStack itemStack = nonNullList.get(i);
                Globals.mc.getRenderItem().zLevel = 501.0f;
                Globals.mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, iX, iY);
                Globals.mc.getRenderItem().renderItemOverlayIntoGUI(Globals.mc.fontRenderer, itemStack, iX, iY, null);
                Globals.mc.getRenderItem().zLevel = 0.0f;
            }
            GlStateManager.disableLighting();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.popMatrix();
        }
    }

}