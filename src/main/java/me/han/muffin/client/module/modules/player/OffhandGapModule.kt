package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.manager.managers.HoleManager.isInHole
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.combat.AutoCrystalModule
import me.han.muffin.client.module.modules.combat.AutoTotemModule
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.InventoryUtils.InventoryHotbar
import me.han.muffin.client.utils.extensions.mc.entity.realHealth
import me.han.muffin.client.utils.extensions.mc.item.moveToSlot
import me.han.muffin.client.utils.extensions.mc.item.removeHoldingItem
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.item.Item
import net.minecraft.item.ItemAppleGold
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.concurrent.atomic.AtomicInteger

object OffhandGapModule: Module("OffhandGap", Category.PLAYER, "Place gapples on offhand and pvp for you.") {

    private val priority = EnumValue(Priority.Inventory, "Priority")
    val compatibility = Value(true, "Compatibility")
    private val autoDisable = Value(false, "AutoDisable")
    private val antiInvDesync = Value(true, "AntiDesync")

    private val health = NumberValue(15.0f, 0.1f, 36.0f, 0.1f, "TotemHealth")
    private val holeOverrideHP = NumberValue(4.0F, 0F, 30.0F, 0.2F, "HoleOverrideHP")
    private val noArmourHoleHP = NumberValue(10.0F, 0F, 20.0F, 0.2F, "NoArmourHoleHP")

    private val crystalCheck = Value(false, "CheckCrystal")
    private val fallingCheck = Value(true, "FallingCheck")

    private val autoEat = Value(false, "AutoEat")
    private val autoEatHp = NumberValue({ autoEat.value }, 16f, 0.1f, 36.0f, 0.1f, "AutoEatHp")

    private var numOfItems = 0
    private var preferredItemSlot = 0

    private var eatingGap = false

    init {
        addSettings(priority, compatibility, autoDisable, antiInvDesync, health, holeOverrideHP, noArmourHoleHP, crystalCheck, fallingCheck, holeOverrideHP, autoEat, autoEatHp)
    }

    private enum class Priority {
        Hotbar, Inventory, All
    }

    override fun getHudInfo(): String = numOfItems.toString()

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck() || Globals.mc.player.isDead || Globals.mc.player.health <= 0.0) {
            if (autoDisable.value) disable()
            return
        }

        if (AutoTotemModule.isDoingSwordGap) return

        if (antiInvDesync.value && !AutoReplenish.INSTANCE.tasks.hasNext() && !Globals.mc.player.inventory.itemStack.isEmpty) { // If player is holding an in inventory
            if (Globals.mc.currentScreen !is GuiContainer) {// If inventory is open (playing moving item)
                removeHoldingItem() // If inventory is not open (ex. inventory desync)
            }
        }

        if (Globals.mc.currentScreen == null) {
            switchItemToOffHand(OffhandItems.Gap, getFormatPriority())
        }

        if (autoEat.value) {
            if (eatingGap && !Globals.mc.player.isHandActive) {
                eatingGap = false
                KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindUseItem.keyCode, false)
                return
            }

            if (eatingGap) {
                return
            }

            if (!healthCheck(autoEatHp.value) && isHoldingGapple()) {
                Globals.mc.player.activeHand = EnumHand.OFF_HAND
                eatingGap = true
                KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindUseItem.keyCode, true)
                Globals.mc.player.connection?.sendPacket(CPacketPlayerTryUseItem(EnumHand.OFF_HAND))
            }

        }

    }

    private fun isHoldingGapple(): Boolean {
        return Globals.mc.player.heldItemOffhand.item is ItemAppleGold
    }

    override fun onDisable() {
        if (fullNullCheck()) return

        switchItemToOffHand(OffhandItems.Totem, InventoryHotbar.All)
    }

    private fun switchItemToOffHand(items: OffhandItems, inv: InventoryHotbar) {
        val inventoryItems = getRequireItems(items)

        if (findRequiredItems(items, inv) && canSwitch() && Globals.mc.player.heldItemOffhand.item != inventoryItems) {
            moveToSlot(preferredItemSlot, 45)
        } else if (!canSwitch() && findRequiredItems(OffhandItems.Totem, InventoryHotbar.All) && Globals.mc.player.heldItemOffhand.item != Items.TOTEM_OF_UNDYING) {
            moveToSlot(preferredItemSlot, 45)
        }

    }

    private fun findRequiredItems(items: OffhandItems, inv: InventoryHotbar): Boolean {
        val inventoryItems: Item = getRequireItems(items)

        numOfItems = 0
        val preferredItemSlotStackSize = AtomicInteger()
        preferredItemSlotStackSize.set(Int.MIN_VALUE)
        InventoryUtils.getSlots(inv).forEach { (slotKey, slotValue) ->
            var numOfItemsInStack = 0
            if (slotValue.item == inventoryItems) {
                numOfItemsInStack = slotValue.count
                if (preferredItemSlotStackSize.get() < numOfItemsInStack) {
                    preferredItemSlotStackSize.set(numOfItemsInStack)
                    preferredItemSlot = slotKey
                }
            }
            numOfItems += numOfItemsInStack
        }

        if (Globals.mc.player.heldItemOffhand.item == inventoryItems)
            numOfItems += Globals.mc.player.heldItemOffhand.count

        if (Globals.mc.player.heldItemMainhand.item == inventoryItems)
            numOfItems += Globals.mc.player.heldItemMainhand.count

        return numOfItems != 0
    }

    private val nextFallDist get() = Globals.mc.player.fallDistance - Globals.mc.player.motionY.toFloat()

    private fun healthCheck(hp: Float): Boolean {
        var health = hp

        if (Globals.mc.player.isInHole) {
            health = if (AutoCrystalModule.checkArmourBreakable(Globals.mc.player, false, 0.0)) noArmourHoleHP.value else holeOverrideHP.value
        }

        if (!Globals.mc.player.isPotionActive(MobEffects.RESISTANCE)) health += 3

        return if (fallingCheck.value) {
            Globals.mc.player.realHealth >= health && nextFallDist < 5
        } else {
            Globals.mc.player.realHealth >= health
        }
    }

    private fun canSwitch(): Boolean {
        if (crystalCheck.value) return healthCheck(health.value) && !isCrystalsAABBEmpty()
        return healthCheck(health.value)
    }

    private enum class OffhandItems {
        Gap, Crystal, ExperienceBottle, Air, Totem
    }

    private fun getRequireItems(items: OffhandItems): Item {
        return when (items) {
            OffhandItems.Gap -> Items.GOLDEN_APPLE
            OffhandItems.Crystal -> Items.END_CRYSTAL
            OffhandItems.ExperienceBottle -> Items.EXPERIENCE_BOTTLE
            OffhandItems.Air -> Items.AIR
            else -> Items.TOTEM_OF_UNDYING
        }
    }

    private fun isEmpty(pos: BlockPos): Boolean {
        val crystalsInAABB =
            Globals.mc.world.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB(pos)).filterIsInstance<EntityEnderCrystal>()

        return crystalsInAABB.isEmpty()
    }

    private fun isCrystalsAABBEmpty(): Boolean {
        return isEmpty(Globals.mc.player.position.add(1, 0, 0)) &&
                isEmpty(Globals.mc.player.position.add(-1, 0, 0)) &&
                isEmpty(Globals.mc.player.position.add(0, 0, 1)) &&
                isEmpty(Globals.mc.player.position.add(0, 0, -1)) &&
                isEmpty(Globals.mc.player.position)
    }

    private fun getFormatPriority(): InventoryHotbar {
        return when (priority.value) {
            Priority.Hotbar -> InventoryHotbar.Hotbar
            Priority.Inventory -> InventoryHotbar.Inventory
            Priority.All -> InventoryHotbar.All
            else -> InventoryHotbar.Inventory
        }
    }


}