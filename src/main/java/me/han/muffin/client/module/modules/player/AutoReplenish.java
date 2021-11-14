package me.han.muffin.client.module.modules.player;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.client.MotionUpdateEvent;
import me.han.muffin.client.manager.managers.ChatManager;
import me.han.muffin.client.module.Module;
import me.han.muffin.client.utils.TaskChain;
import me.han.muffin.client.utils.entity.LocalPlayerInventory;
import me.han.muffin.client.value.NumberValue;
import me.han.muffin.client.value.Value;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketClickWindow;
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener;

import java.util.Comparator;
import java.util.List;

public class AutoReplenish extends Module {
    private final NumberValue<Integer> durability_threshold = new NumberValue<>(5, 0, 100, 1, "DurabilityTools");
    private final NumberValue<Integer> stack_threshold = new NumberValue<>(10, 0, 64, 1, "StackThreshold");
    private final NumberValue<Integer> tick_delay = new NumberValue<>(1, 0, 20, 1, "TickDelay");
    private final Value<Boolean> no_gui = new Value<>(true, "NoGui");
    private final Value<Boolean> preferSmallStack = new Value<>(true, "PreferSmallStack");

    public TaskChain<Runnable> tasks = TaskChain.empty();
    private long tickCount = 0;
    public static AutoReplenish INSTANCE;

    public AutoReplenish() {
        super("AutoReplenish", Category.PLAYER, "Auto refill stacks when reach a specified amount.");
        addSettings(durability_threshold, stack_threshold, tick_delay, no_gui, preferSmallStack);
        INSTANCE = this;
    }

    private boolean processing(int index) {
        if (tick_delay.getValue() == 0) {
            return true; // process all
        } else if (tick_delay.getValue() < 0) {
            return index < Math.abs(tick_delay.getValue()); // process n tasks per tick
        } else {
            return index == 0 && tickCount % tick_delay.getValue() == 0;
        }
    }

    private boolean isMonitoring(LocalPlayerInventory.InvItem item) {
        return item.isItemDamageable() || item.isStackable();
    }

    private boolean isAboveThreshold(LocalPlayerInventory.InvItem item) {
        return item.isItemDamageable() ? item.getDurability() > durability_threshold.getValue() : item.getStackCount() > stack_threshold.getValue();
    }

    private int getDamageOrCount(LocalPlayerInventory.InvItem item) {
        return item.isNull() ? 0 : item.isItemDamageable() ? item.getDurability() : item.getStackCount();
    }

    private void tryPlacingHeldItem() {
        LocalPlayerInventory.InvItem holding = LocalPlayerInventory.getMouseHeld();
        if (holding.isEmpty()) return;

        LocalPlayerInventory.InvItem item;
        if (holding.isDamageable()) {
            item = LocalPlayerInventory.getSlotStorageInventory()
                    .stream()
                    .filter(LocalPlayerInventory.InvItem::isNull)
                    .findAny()
                    .orElse(LocalPlayerInventory.InvItem.EMPTY);
        } else {
            item = LocalPlayerInventory.getSlotStorageInventory()
                    .stream()
                    .filter(inv -> inv.isNull() || holding.isItemsEqual(inv))
                    .filter(inv -> inv.isNull() || !inv.isStackMaxed())
                    .max(Comparator.comparing(LocalPlayerInventory.InvItem::getStackCount))
                    .orElse(LocalPlayerInventory.InvItem.EMPTY);
        }

        if (item == LocalPlayerInventory.InvItem.EMPTY) {
            click(holding, 0, ClickType.PICKUP);
        } else {
            click(item, 0, ClickType.PICKUP);
            if (LocalPlayerInventory.getMouseHeld().nonEmpty()) throw new RuntimeException();
        }

    }

    @Override
    public void onDisable() {
        Globals.mc.addScheduledTask(() -> {
            tasks = TaskChain.empty();
            tickCount = 0;
        });
    }

    @Listener
    private void onMotionUpdate(MotionUpdateEvent event) {
        if (event.getStage() != EventStageable.EventStage.PRE) return;

        if (fullNullCheck()) return;

        // only process when a gui isn't opened by the player
        if (Globals.mc.currentScreen != null && no_gui.getValue()) return;

        if (tasks.isEmpty()) {
            final List<LocalPlayerInventory.InvItem> slots = LocalPlayerInventory.getSlotStorageInventory();

            tasks =
                    LocalPlayerInventory.getHotbarInventory()
                            .stream()
                            .filter(LocalPlayerInventory.InvItem::nonNull)
                            .filter(this::isMonitoring)
                            .filter(item -> !isAboveThreshold(item))
                            .filter(item -> slots.stream()
                                    .filter(this::isMonitoring)
                                    .filter(inv -> !inv.isItemDamageable() || isAboveThreshold(inv))
                                    .anyMatch(item::isItemsEqual))
                            .max(Comparator.comparingInt(LocalPlayerInventory::getHotbarDistance))
                            .map(hotbarItem -> TaskChain.<Runnable>builder()
                                    .then(() -> {
                                        // pick up item
                                        verifyHotbar(hotbarItem);

                                        LocalPlayerInventory.InvItem slot;

                                        if (preferSmallStack.getValue()) {
                                            slot = slots.stream()
                                                    .filter(LocalPlayerInventory.InvItem::nonNull)
                                                    .filter(this::isMonitoring)
                                                    .filter(hotbarItem::isItemsEqual)
                                                    .filter(inv -> isCompatibleStacks(inv.getItemStack(), hotbarItem.getItemStack()))
                                                    .filter(inv -> !inv.isDamageable() || isAboveThreshold(inv))
                                                    .min(Comparator.comparingInt(this::getDamageOrCount))
                                                    .orElseThrow(RuntimeException::new);
                                        } else {
                                            slot = slots.stream()
                                                    .filter(LocalPlayerInventory.InvItem::nonNull)
                                                    .filter(this::isMonitoring)
                                                    .filter(hotbarItem::isItemsEqual)
                                                    .filter(inv -> isCompatibleStacks(inv.getItemStack(), hotbarItem.getItemStack()))
                                                    .filter(inv -> !inv.isDamageable() || isAboveThreshold(inv))
                                                    .max(Comparator.comparingInt(this::getDamageOrCount))
                                                    .orElseThrow(RuntimeException::new);
                                        }


                            //            ChatManager.INSTANCE.sendMessage("Found slot " + slot.getSlotNumber());
                            //            ChatManager.INSTANCE.sendMessage("Hotbar slot " + hotbarItem.getSlotNumber());

                            //            if (isCompatibleStacks(slot.getItemStack(), hotbarItem.getItemStack())) {
                            //                ChatManager.INSTANCE.sendMessage("Clicked");
                                            click(slot, 0, ClickType.QUICK_MOVE);
                                            Globals.mc.playerController.updateController();
                           //             }

                                    })

                                    .then(this::tryPlacingHeldItem)
                                    .build())
                            .orElse(TaskChain.empty());
        }

        // process the next click task
        int n = 0;
        while (processing(n++) && tasks.hasNext()) {
            try {
                tasks.next().run();
            } catch (Throwable t) {
                tasks = TaskChain.singleton(this::tryPlacingHeldItem);
            }
        }
        ++tickCount;

    }

    private static void verifyHotbar(LocalPlayerInventory.InvItem hotbarItem) {
        LocalPlayerInventory.InvItem current = LocalPlayerInventory.getHotbarInventory().get(hotbarItem.getIndex());
        if (!hotbarItem.isItemsEqual(current)) {
            throw new IllegalArgumentException();
        }
    }

    private boolean isCompatibleStacks(ItemStack from, ItemStack to) {
        if (!from.getItem().equals(to.getItem())) {
            ChatManager.INSTANCE.sendMessage("Not same item.");
            return false;
        }
   //     if (!from.isItemEqual(to) || !ItemStack.areItemStacksEqual(from, to)) {
   //         ChatManager.INSTANCE.sendMessage("Not equals item.");
   //         return false;
   //     }
        if (!from.getDisplayName().equals(to.getDisplayName())) {
            // ChatManager.INSTANCE.sendMessage("Diff name.");
            return false;
        }
        if (from.getItemDamage() != to.getItemDamage()) {
          //   ChatManager.INSTANCE.sendMessage("Not same damage.");
            return false;
        }
        return true;
    }

    private static void clickWindow(int slotIdIn, int usedButtonIn, ClickType modeIn, ItemStack clickedItemIn) {
        Globals.mc.player.connection.sendPacket(
                new CPacketClickWindow(
                        Globals.mc.player.inventoryContainer.windowId,
                        slotIdIn,
                        usedButtonIn,
                        modeIn,
                        clickedItemIn,
                        LocalPlayerInventory.getOpenContainer().getNextTransactionID(LocalPlayerInventory.getInventory())));
    }

    private static ItemStack click(LocalPlayerInventory.InvItem item, int usedButtonIn, ClickType modeIn) {
        if (item.getIndex() == -1) throw new IllegalArgumentException();
        ItemStack ret;
        clickWindow(
                item.getSlotNumber(),
                usedButtonIn,
                modeIn,
                ret = LocalPlayerInventory.getOpenContainer()
                        .slotClick(item.getSlotNumber(), usedButtonIn, modeIn, Globals.mc.player));
        return ret;
    }

}