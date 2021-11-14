package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.core.Globals
import me.han.muffin.client.core.Globals.mc
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.ClientTickEvent
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.manager.managers.ItemManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.player.AutoReplenish
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.entity.WeaponUtils
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.utils.extensions.mc.item.*
import me.han.muffin.client.utils.extensions.mixin.entity.syncCurrentPlayItem
import me.han.muffin.client.utils.timer.TickTimer
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.block.Block
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.ContainerPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.inventory.Slot
import net.minecraft.item.*
import net.minecraft.network.play.server.SPacketConfirmTransaction
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*
import java.util.function.Predicate

object AutoTotemModule: Module("AutoTotem", Category.COMBAT, "Automatically equip Totem of Undying in the off-hand.") {

    private val modeSetting = EnumValue(TotemMode.Crystal, "Mode")
    private val delay = NumberValue(0.0, 0.0, 20.0, 0.1, "Delay")
    private val superDelay = NumberValue({ modeSetting.value == TotemMode.Pvp32k }, 1, 0, 10, 1, "32k-Delay")
    private val antiDesync = Value(true, "AntiDesync")
    private val pauseWhenContainer = Value(true, "PauseInContainer")
    private val pauseWhenInventory = Value(true, "PauseInInventory")
    private val swordGap = Value(true, "SwordGap")
    private val health = NumberValue(6f, 0f, 20f, 0.5f, "Health")
    private val ncpStrict = Value(false, "NCPStrict")

    private val hotbarTotem = Value({ modeSetting.value == TotemMode.Pvp32k }, true, "32k-Hotbar")
    private val autoSwitchIfGetDistance = Value({ modeSetting.value == TotemMode.Pvp32k }, true, "32k-AutoSwitch")
    private val range = NumberValue({ modeSetting.value == TotemMode.Pvp32k }, 0.1, 0.0, 10.0, 0.1, "32k-SwitchRange")
    private val timer = Timer()

    private var timer32k = 0
    var isDoingSwordGap = false

    private val transactionLog = HashMap<Short, Boolean>()
    private val movingTimer = TickTimer()

    private enum class TotemMode {
        Crystal, Pvp32k
    }

    init {
        addSettings(modeSetting, pauseWhenContainer, pauseWhenInventory, health, delay, superDelay, antiDesync, swordGap, ncpStrict, hotbarTotem, autoSwitchIfGetDistance, range)
    }

    override fun getHudInfo(): String = ItemManager.totemStack.count.toString()

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (fullNullCheck() || event.stage != EventStageable.EventStage.PRE) return

        if (event.packet !is SPacketConfirmTransaction || event.packet.windowId != 0 || !transactionLog.containsKey(event.packet.actionNumber)) return
        transactionLog[event.packet.actionNumber] = event.packet.wasAccepted()
        if (!transactionLog.containsValue(false)) movingTimer.reset(-175L) // If all the click packets were accepted then we reset the timer for next moving
    }

    private fun switchToType(slot: Int) {
        transactionLog.clear()
        transactionLog.putAll(moveToSlot(slot, 45).associate { it to false })
        Globals.mc.playerController.syncCurrentPlayItem()
        movingTimer.reset()
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return
        if (fullNullCheck() || !Globals.mc.player.isAlive) return

        if (modeSetting.value != TotemMode.Pvp32k) {
            autoSwitchIfGetDistance.value = false
            hotbarTotem.value = false
        }

        if (!Globals.mc.player.inventory.itemStack.isEmpty && !AutoReplenish.INSTANCE.tasks.hasNext()) { // If player is holding an in inventory
            if (mc.currentScreen is GuiContainer) {// If inventory is open (playing moving item)
                movingTimer.reset() // delay for 5 ticks
            } else { // If inventory is not open (ex. inventory desync)
                removeHoldingItem()
            }
            return
        }

        if (pauseWhenContainer.value && mc.currentScreen is GuiContainer && mc.currentScreen !is GuiInventory) return
        if (pauseWhenInventory.value && mc.currentScreen is GuiInventory) return

        if (swordGap.value && Globals.mc.player.heldItemMainhand.item is ItemSword && Globals.mc.gameSettings.keyBindUseItem.isKeyDown && !shouldSwitchToTotem())  {
            if (Globals.mc.player.heldItemOffhand.item !is ItemAppleGold) findItem(Items.GOLDEN_APPLE).ifPresent { switchToType(it) }
            isDoingSwordGap = true
            return
        } else {
            isDoingSwordGap = false
        }

    }

    fun <T : Slot> Iterable<T>.firstBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
        firstByStack { itemStack ->
            itemStack.item.let { it is ItemBlock && it.block == block } && (predicate == null || predicate.test(itemStack))
        }

    @Listener
    private fun onClientTick(event: ClientTickEvent) {
        if (fullNullCheck() || !Globals.mc.player.isAlive) return

        if (pauseWhenContainer.value && mc.currentScreen is GuiContainer && mc.currentScreen !is GuiInventory) return
        if (pauseWhenInventory.value && mc.currentScreen is GuiInventory) return

        if (modeSetting.value == TotemMode.Pvp32k) {
            if (hotbarTotem.value) {
                if (mc.player.inventory.getStackInSlot(0).item != Items.TOTEM_OF_UNDYING) {
                    for (i in 9 until 35) {
                        val stack = mc.player.inventory.getStackInSlot(i)
                        if (stack.item != Items.TOTEM_OF_UNDYING) continue
                        mc.playerController.windowClick(
                            mc.player.inventoryContainer.windowId,
                            i,
                            0,
                            ClickType.SWAP,
                            mc.player as EntityPlayer
                        )
                        break
                    }
                }
            }

            if (autoSwitchIfGetDistance.value) {
                val totemSlot = InventoryUtils.findItem(Items.TOTEM_OF_UNDYING)
                if (totemSlot == -1 || WeaponUtils.isSuperWeapon(Globals.mc.player.heldItemMainhand)) return
                for (entity in Globals.mc.world.playerEntities) {
                    if (EntityUtil.isntValid(entity, range.value)) continue
                    if (WeaponUtils.isSuperWeapon(entity.heldItemMainhand) && Globals.mc.player.inventory.currentItem != totemSlot) {
                        InventoryUtils.swapSlot(totemSlot)
                        break
                    }
                }
            }

            if (timer32k > superDelay.value) {
                timer32k--
                return
            }

            val inv = Globals.mc.player.inventory.mainInventory
            if (Globals.mc.player.offhandSlot.stack.item != Items.TOTEM_OF_UNDYING) {
                var inventoryIndex = 0
                while (inventoryIndex < inv.size) {
                    if (inv[inventoryIndex] != ItemStack.EMPTY) {
                        if (inv[inventoryIndex].item == Items.TOTEM_OF_UNDYING) {
                            replace32KTotem(inventoryIndex)
                            break
                        }
                    }
                    inventoryIndex++
                }
                timer32k = 3
            }
        }

        if (modeSetting.value == TotemMode.Crystal) {
            if (!movingTimer.tick(200L, false)) return // Delays 4 ticks by default
            if (getOffhand().item == Items.TOTEM_OF_UNDYING || !shouldSwitchToTotem() && !getOffhand().isEmpty) return

            if (timer.passedTicks(delay.value.toInt())) {
                findItem(Items.TOTEM_OF_UNDYING).ifPresent {
                    if (ncpStrict.value) {
                        Globals.mc.player.isSprinting = !Globals.mc.player.isSprinting

                        // TODO: instead of setting velocity to 0
                        // TODO: why don we just slow down player to normal walking speed and stop motionY from grinding
                        Globals.mc.player.setVelocity(0.0, 0.0, 0.0)
                    }
                    switchToType(it)
                }
                timer.reset()
            }
        }

    }

    private fun shouldSwitchToTotem(): Boolean {
        return health.value > 0.0 && health.value >= Globals.mc.player.health + Globals.mc.player.absorptionAmount
    }

    private fun findItem(item: Item): OptionalInt {
        for (i in 9..44) {
            if (Globals.mc.player.inventoryContainer.getSlot(i).stack.item == item) {
                return OptionalInt.of(i)
            }
        }
        return OptionalInt.empty()
    }

    private fun getOffhand(): ItemStack {
        return Globals.mc.player.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND)
    }

    private fun replace32KTotem(inventoryIndex: Int) {
        if (Globals.mc.player.openContainer is ContainerPlayer) {
            clickSlot(windowId = 0, if (inventoryIndex < 9) inventoryIndex + 36 else inventoryIndex, type = ClickType.PICKUP)
            clickSlot(windowId = 0, 45, type = ClickType.PICKUP)
            clickSlot(windowId = 0, if (inventoryIndex < 9) inventoryIndex + 36 else inventoryIndex, type = ClickType.PICKUP)
        }
    }

}