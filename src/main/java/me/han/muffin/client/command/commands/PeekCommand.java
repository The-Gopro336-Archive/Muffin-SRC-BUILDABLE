package me.han.muffin.client.command.commands;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.command.Command;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.events.render.Render2DEvent;
import me.han.muffin.client.manager.managers.ChatManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.client.gui.inventory.GuiShulkerBox;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityShulkerBox;
import net.minecraft.util.math.RayTraceResult;
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener;

public class PeekCommand extends Command {
    private String entity;

    public PeekCommand() {
        super(new String[]{"peek"});
    }

    @Override
    public String dispatch() {
        try {
            Muffin.getInstance().getEventManager().addEventListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Peeking Shulker Box";
    }

    @Listener
    public void onRender2D(Render2DEvent event) {
        try {
            ItemStack stack = null;

            if (this.entity != null) {
                EntityPlayer target = null;

                for (Entity e : Globals.mc.world.loadedEntityList) {
                    if(e != null) {
                        if(e instanceof EntityPlayer && e.getName().equalsIgnoreCase(this.entity)) {
                            target = (EntityPlayer) e;
                        }
                    }
                }

                if (target != null) {
                    stack = getHeldShulker(target);

                    if (stack == null) {
                        ChatManager.sendMessage("\"" + target.getName() + "\" is not holding a shulker box");
                        this.entity = null;
                        Muffin.getInstance().getEventManager().removeEventListener(this);
                        return;
                    }
                } else {
                    ChatManager.sendMessage("\"" + this.entity + "\" is not within range");
                }
                this.entity = null;
            } else {
                final RayTraceResult ray = Globals.mc.objectMouseOver;

                if(ray != null) {
                    if(ray.entityHit instanceof EntityItemFrame) {
                        final EntityItemFrame itemFrame = (EntityItemFrame) ray.entityHit;
                        if(itemFrame.getDisplayedItem() != null && itemFrame.getDisplayedItem().getItem() instanceof ItemShulkerBox) {
                            stack = itemFrame.getDisplayedItem();
                        }else{
                            stack = getHeldShulker(Globals.mc.player);
                        }
                    }else{
                        stack = getHeldShulker(Globals.mc.player);
                    }
                }else{
                    stack = getHeldShulker(Globals.mc.player);
                }
            }

            if(stack != null) {
                final Item item = stack.getItem();

                if (item instanceof ItemShulkerBox) {
                    if (Block.getBlockFromItem(item) instanceof BlockShulkerBox) {
                        final BlockShulkerBox shulkerBox = (BlockShulkerBox) Block.getBlockFromItem(item);
                        final NBTTagCompound tag = stack.getTagCompound();
                        if (tag != null && tag.hasKey("BlockEntityTag", 10)) {
                            final NBTTagCompound entityTag = tag.getCompoundTag("BlockEntityTag");

                            final TileEntityShulkerBox te = new TileEntityShulkerBox();
                            te.setWorld(Globals.mc.world);
                            te.readFromNBT(entityTag);
                            Globals.mc.displayGuiScreen(new GuiShulkerBox(Globals.mc.player.inventory, te));
                        } else {
                            ChatManager.sendMessage("This shulker box is empty");
                        }
                    }

                } else {
                    ChatManager.sendMessage("Please hold a shulker box");
                }
            }else{
                ChatManager.sendMessage("Please hold a shulker box");
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        Muffin.getInstance().getEventManager().removeEventListener(this);
    }

    private ItemStack getHeldShulker(EntityPlayer entity) {
        if (!entity.getHeldItemMainhand().isEmpty() && entity.getHeldItemMainhand().getItem() instanceof ItemShulkerBox) {
            return entity.getHeldItemMainhand();
        }
        if (!entity.getHeldItemMainhand().isEmpty() && entity.getHeldItemOffhand().getItem() instanceof ItemShulkerBox) {
            return entity.getHeldItemOffhand();
        }
        return null;
    }

}
