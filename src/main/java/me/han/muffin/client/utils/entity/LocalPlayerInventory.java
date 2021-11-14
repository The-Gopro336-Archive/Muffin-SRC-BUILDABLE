package me.han.muffin.client.utils.entity;

import me.han.muffin.client.core.Globals;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketClickWindow;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LocalPlayerInventory {

    public static InventoryPlayer getInventory() {
        return Globals.mc.player.inventory;
    }

    public static Container getContainer() {
        return Globals.mc.player.inventoryContainer;
    }

    public static Container getOpenContainer() {
        return Globals.mc.player.openContainer;
    }

    public static int getHotbarSize() {
        return InventoryPlayer.getHotbarSize();
    }

    public static List<InvItem> getMainInventory() {
        AtomicInteger next = new AtomicInteger(0);
        return getInventory()
                .mainInventory
                .stream()
                .map(item -> new InvItem.Base(item, next.getAndIncrement()))
                .collect(Collectors.toList());
    }

    public static List<InvItem> getSlotInventory() {
        return getContainer()
                .inventorySlots
                .stream()
                .map(InvItem.SlotWrapper::new)
                .collect(Collectors.toList());
    }

    public static List<InvItem> getMainInventory(int start, int end) {
        return getMainInventory().subList(start, end);
    }

    public static List<InvItem> getSlotInventory(int start, int end) {
        return getSlotInventory().subList(start, end);
    }

    public static List<InvItem> getSlotStorageInventory() {
        return getSlotInventory(9, 36);
    }

    public static List<InvItem> getHotbarInventory() {
        return getMainInventory(0, getHotbarSize());
    }

    public static InvItem getMouseHeld() {
        return newInvItem(getInventory().getItemStack(), -999);
    }

    public static InvItem getSelected() {
        return getMainInventory().get(getInventory().currentItem);
    }


    public static int getHotbarDistance(InvItem item) {
        int max = LocalPlayerInventory.getHotbarSize() - 1;
        return item.getIndex() > max ? 0 : max - Math.abs(getSelected().getIndex() - item.getIndex());
    }

    public static InvItem newInvItem(ItemStack itemStack, int index) {
        return new InvItem.Base(itemStack, index);
    }


    public static void sendWindowClick(int slotIdIn, int usedButtonIn, ClickType modeIn, ItemStack clickedItemIn) {
        Globals.mc.getConnection()
                .sendPacket(
                        new CPacketClickWindow(
                                0,
                                slotIdIn,
                                usedButtonIn,
                                modeIn,
                                clickedItemIn,
                                getOpenContainer().getNextTransactionID(getInventory())));
    }

    public static ItemStack sendWindowClick(InvItem item, int usedButtonIn, ClickType modeIn) {
        if (item.getIndex() == -1) {
            throw new IllegalArgumentException();
        }
        ItemStack ret;
        sendWindowClick(
                item.getSlotNumber(),
                usedButtonIn,
                modeIn,
                ret =
                        getOpenContainer()
                                .slotClick(item.getSlotNumber(), usedButtonIn, modeIn, Globals.mc.player));
        return ret;
    }

    public abstract static class InvItem implements Comparable<InvItem> {

        public static final InvItem EMPTY =
                new InvItem() {
                    @Override
                    public ItemStack getItemStack() {
                        return ItemStack.EMPTY;
                    }

                    @Override
                    public Item getItem() {
                        return Items.AIR;
                    }

                    @Override
                    public int getIndex() {
                        return -1;
                    }
                };

        public abstract ItemStack getItemStack();

        public Item getItem() {
            return getItemStack().getItem();
        }

        public abstract int getIndex();

        public int getSlotNumber() {
            switch (getIndex()) {
                case 36:
                    return 45;
                default:
                    // TODO: make this work for all container types
                    // 9 = the crafting result, 4x crafting boxes, and 4
                    // 36 = main inventory size
                    int row = getIndex() / 9;
                    int idx = getIndex() % 9;
                    return 9 + 36 - ((row * 9) + (9 - idx));
            }
        }

        public boolean isNull() {
            return getItemStack().isEmpty();
        }

        public boolean nonNull() {
            return !isNull();
        }

        public boolean isEmpty() {
            return getItemStack().isEmpty();
        }

        public boolean nonEmpty() {
            return !isEmpty();
        }

        public boolean isDamageable() {
            return getItemStack().isItemStackDamageable();
        }

        public boolean isItemDamageable() {
            return getItem().isDamageable();
        }

        public boolean isStackable() {
            return getItemStack().isStackable();
        }


        public int getDurability() {
            return isDamageable() ? (getItemStack().getMaxDamage() - getItemStack().getItemDamage()) : 0;
        }

        public int getStackCount() {
            return getItemStack().getCount();
        }

        public int getMaxStackCount() {
            return getItemStack().getMaxStackSize();
        }

        public boolean isStackMaxed() {
            return getStackCount() >= getMaxStackCount();
        }

        public boolean isItemsEqual(InvItem other) {
            return getItemStack().isItemEqualIgnoreDurability(other.getItemStack());
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj
                    || (obj instanceof InvItem
                    && getIndex() == ((InvItem) obj).getIndex()
                    && getItemStack().equals(((InvItem) obj).getItemStack()));
        }

        @Override
        public int hashCode() {
            return Objects.hash(getItemStack(), getIndex());
        }

        @Override
        public int compareTo(InvItem o) {
            return Integer.compare(getIndex(), o.getIndex());
        }

        protected static class Base extends InvItem {

            private final ItemStack itemStack;
            private final int index;

            protected Base(ItemStack itemStack, int index) {
                this.itemStack = itemStack;
                this.index = index;
            }

            @Override
            public ItemStack getItemStack() {
                return itemStack;
            }

            @Override
            public int getIndex() {
                return index;
            }
        }

        protected static class SlotWrapper extends InvItem {

            private final Slot slot;

            protected SlotWrapper(Slot slot) {
                this.slot = slot;
            }

            @Override
            public ItemStack getItemStack() {
                return slot.getStack();
            }

            @Override
            public int getIndex() {
                int row = (getSlotNumber() / 9) - 1;
                int idx = getSlotNumber() % 9;
                return 36 - ((row * 9) + (9 - idx)); // inverse of what is done for ::getIndex()
            }

            @Override
            public int getSlotNumber() {
                return slot.slotNumber;
            }
        }
    }
}