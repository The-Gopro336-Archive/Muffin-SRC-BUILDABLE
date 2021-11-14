package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.manager.managers.HoleManager.isInHole
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.combat.AutoCrystalHelper
import me.han.muffin.client.module.modules.combat.AutoCrystalModule
import me.han.muffin.client.module.modules.combat.AutoTotemModule
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.InventoryUtils.InventoryHotbar
import me.han.muffin.client.utils.combat.CombatUtils
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.entity.MovementUtils.realSpeed
import me.han.muffin.client.utils.extensions.mc.entity.isFakeOrSelf
import me.han.muffin.client.utils.extensions.mc.entity.realHealth
import me.han.muffin.client.utils.extensions.mc.item.moveToSlot
import me.han.muffin.client.utils.extensions.mc.item.offhandSlot
import me.han.muffin.client.utils.extensions.mc.item.removeHoldingItem
import me.han.muffin.client.utils.timer.TickTimer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.entity.projectile.EntityTippedArrow
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.item.Item
import net.minecraft.item.ItemSword
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

internal object OffhandCrystalModule: Module("OffhandCrystal", Category.PLAYER, "Place crystals on offhand.") {

    private val noTotemMode = EnumValue(NoTotemMode.Keep, "NoTotemMode")
    private val priority = EnumValue(Priority.Inventory, "Priority")
    private val autoDisable = Value(false, "AutoDisable")
    private val antiInvDesync = Value(true, "AntiDesync")

    private val totemHealth = NumberValue(15.5f, 0.1f, 32.0f, 0.1f, "TotemHealth")

    private val crystalCheck = Value(false, "CheckCrystal")
    private val swordGap = Value(false, "SwordGap")
    private val autoEnableCA = Value(true, "AutoEnableCA")
    private val switchBackToGap = Value(true, "SwitchBackGap")

    private val holeOverrideHP = NumberValue(4.0F, 0F, 20.0F, 0.2F, "HoleOverrideHP")
    private val noArmourHoleHP = NumberValue(10.0F, 0F, 20.0F, 0.2F, "NoArmourHoleHP")

    private val targetRange = NumberValue(8.0, 0.0, 12.0, 0.2, "TargetRange")
    private val targetLethalHP = NumberValue({ targetRange.value > 0F },18F, 0F, 36F, 0.2F, "TargetLethalHP")
    private val overrideTargetHP = NumberValue({ targetRange.value > 0.0 && targetLethalHP.value > 0F }, 8F, 0F, 30.0F, 0.1F, "TargetOverrideHP")

    private val damageCheck = Value(false, "DamageCheck")
    private val playerDamage = Value({ damageCheck.value },true, "PlayerDamage")
    private val crystalDamage = Value({ damageCheck.value },true, "CrystalDamage")
    private val crystalScale = NumberValue({ damageCheck.value },11.0F, 0.0F, 20.0F, 0.1F, "CrystalScale")
    private val fallingDamage = Value({ damageCheck.value },true, "FallingDamage")

    private val fallingCheck = Value(true, "FallingCheck")

    private var isCAAutoSwitch = false

    private var isOriginallyGapOffHand = false

    private var numOfItems = 0
    private var preferredItemSlot = 0

    private var maxDamage = 0.0F
    private val damageTimer = TickTimer()

    private enum class NoTotemMode {
        Keep, Gapple, None
    }

    private enum class Priority {
        Hotbar, Inventory, All
    }

    init {
        addSettings(
            noTotemMode, priority,
            autoDisable, antiInvDesync,
            totemHealth, crystalCheck,
            swordGap, switchBackToGap, autoEnableCA,
            holeOverrideHP, noArmourHoleHP,
            targetRange, targetLethalHP, overrideTargetHP,
            damageCheck, playerDamage, crystalDamage, crystalScale, fallingDamage,
            fallingCheck
        )
    }

    override fun getHudInfo(): String = numOfItems.toString()

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck() || Globals.mc.player.isDead || Globals.mc.player.health <= 0.0) {

            if (autoDisable.value) {
                if (autoEnableCA.value) {
                    if (AutoCrystalModule.isEnabled) {
                        AutoCrystalModule.disable()
                    }
                }
                disable()
            }
            return
        }

        updateDamage()

        if (AutoTotemModule.isDoingSwordGap) return

        if (antiInvDesync.value && !AutoReplenish.INSTANCE.tasks.hasNext() && !Globals.mc.player.inventory.itemStack.isEmpty) { // If player is holding an in inventory
            if (Globals.mc.currentScreen !is GuiContainer) {// If inventory is open (playing moving item)
                removeHoldingItem() // If inventory is not open (ex. inventory desync)
            }
        }

        if (Globals.mc.currentScreen != null) return

        if (OffhandGapModule.isEnabled && OffhandGapModule.compatibility.value) return

        if (swordGap.value && Globals.mc.player.heldItemMainhand.item is ItemSword) {
            if (Globals.mc.gameSettings.keyBindUseItem.isKeyDown && !Globals.mc.player.isPotionActive(MobEffects.WEAKNESS))  {
                switchItemToOffHand(OffhandItems.Gap, getFormatPriority())
            } else {
                switchItemToOffHand(OffhandItems.Crystal, getFormatPriority())
            }
            return
        }
        switchItemToOffHand(OffhandItems.Crystal, getFormatPriority())
    }

    override fun onEnable() {
        if (fullNullCheck()) return

        isOriginallyGapOffHand = false

        if (AutoCrystalModule.swapMode.value != AutoCrystalModule.SwapMode.None) {
            isCAAutoSwitch = true
            AutoCrystalModule.swapMode.setValue(AutoCrystalModule.SwapMode.None)
        } else {
            isCAAutoSwitch = false
        }

        if (switchBackToGap.value && Globals.mc.player.heldItemOffhand.item == Items.GOLDEN_APPLE) {
            isOriginallyGapOffHand = true
        }

        if (autoEnableCA.value) {
            AutoCrystalModule.enable()
        }

    }

    override fun onDisable() {
        if (fullNullCheck()) return

        if (isCAAutoSwitch) {
            AutoCrystalModule.swapMode.value = AutoCrystalModule.SwapMode.Normal
            isCAAutoSwitch = false
        }

        if (autoEnableCA.value) {
            AutoCrystalModule.disable()
        }

        if (OffhandGapModule.isEnabled && OffhandGapModule.compatibility.value) {
            isOriginallyGapOffHand = false
            return
        }

        if (!isOriginallyGapOffHand) {
            switchItemToOffHand(OffhandItems.Totem, InventoryHotbar.All)
        } else if (switchBackToGap.value && canSwitch() && isOriginallyGapOffHand) {
            isOriginallyGapOffHand = false
            switchItemToOffHand(OffhandItems.Gap, InventoryHotbar.Inventory)
        }

        if (noTotemMode.value == NoTotemMode.Gapple && !findRequiredItems(OffhandItems.Totem, InventoryHotbar.All))
            switchItemToOffHand(OffhandItems.Gap, InventoryHotbar.Inventory)

        if (noTotemMode.value == NoTotemMode.None && findRequiredItems(OffhandItems.Totem, InventoryHotbar.All))
            switchItemToOffHand(OffhandItems.Air, InventoryHotbar.Inventory)

    }

    private fun switchItemToOffHand(items: OffhandItems, inv: InventoryHotbar) {
        val inventoryItems = getRequireItems(items)

        if (findRequiredItems(items, inv) && canSwitch() && Globals.mc.player.heldItemOffhand.item != inventoryItems) {
            moveToSlot(preferredItemSlot, 45)
        } else if (!canSwitch() && findRequiredItems(OffhandItems.Totem, InventoryHotbar.All) && Globals.mc.player.offhandSlot.stack.item != Items.TOTEM_OF_UNDYING) {
            moveToSlot(preferredItemSlot, 45)
        }

        Globals.mc.playerController.updateController()

    }

    private fun findRequiredItems(items: OffhandItems, inv: InventoryHotbar): Boolean {
        val inventoryItems = getRequireItems(items)

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

    private fun hasValidHealth(hp: Float): Boolean {
        var health = hp

        if (targetRange.value > 0.0 && targetLethalHP.value > 0.0 && overrideTargetHP.value > 0.0F) {
            for (player in AutoCrystalHelper.getLinkedTargetListDamage(targetRange.value)) {
                if (player.realHealth < targetLethalHP.value) health = overrideTargetHP.value
            }
        }

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

    private val nextFallDist: Float get() = Globals.mc.player.fallDistance - Globals.mc.player.motionY.toFloat()

    private fun canSwitch(): Boolean {
        return hasValidHealth(totemHealth.value) && (!crystalCheck.value || !isCrystalsAABBEmpty())
    }

    private fun updateDamage() {
        maxDamage = 0.0F
        if (!damageCheck.value) return

        val box = AxisAlignedBB(
            Globals.mc.player.posX - 8.0, Globals.mc.player.posY - 6.0, Globals.mc.player.posZ - 8.0,
            Globals.mc.player.posX + 8.0, Globals.mc.player.posY + 4.0, Globals.mc.player.posZ + 8.0
        )

        if (playerDamage.value) maxDamage = max(getPlayerDamage(box), maxDamage)
        if (crystalDamage.value) maxDamage = max(getCrystalDamage(), maxDamage)
        if (fallingDamage.value && nextFallDist > 3.0F) maxDamage = max(ceil(nextFallDist - 3.0F), maxDamage)

        
    }

    private fun getPlayerDamage(box: AxisAlignedBB): Float {
        return Globals.mc.world.getEntitiesWithinAABB(EntityPlayer::class.java, box) { it?.isFakeOrSelf() == false }
            ?.maxOfOrNull { CombatUtils.calcDamageFromPlayer(it, true) } ?: 0.0F
    }

    private fun getCrystalDamage(): Float {
        return (AutoCrystalHelper.crystalList.maxOfOrNull { it.second.selfDamage } ?: 0.0F).pow(crystalScale.value / 10.0F)
    }

//    private fun getArrowDamage(): Float {
//        val box = AxisAlignedBB(
//            Globals.mc.player.posX - 4.0, Globals.mc.player.posY - 4.0, Globals.mc.player.posZ - 4.0,
//            Globals.mc.player.posX + 4.0, Globals.mc.player.posY + 6.0, Globals.mc.player.posZ + 4.0
//        )
//        return Globals.mc.world.getEntitiesWithinAABB(EntityArrow::class.java, box)?.maxOfOrNull {
//            ceil(it.realSpeed * it.damage)
//
//
//        }
//    }
//
//    private fun getTippedArrowDamage(arrow: EntityTippedArrow): Float {
//
//    }

    private fun getOffhandCurrentItem(): OffhandItems {
        return when (Globals.mc.player.heldItemOffhand.item) {
            Items.GOLDEN_APPLE -> OffhandItems.Gap
            Items.END_CRYSTAL -> OffhandItems.Crystal
            Items.EXPERIENCE_BOTTLE -> OffhandItems.ExperienceBottle
            Items.TOTEM_OF_UNDYING -> OffhandItems.Totem
            Items.AIR -> OffhandItems.Air
            else -> OffhandItems.Crystal
        }
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
        return Globals.mc.world.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB(pos)).filterIsInstance<EntityEnderCrystal>().isEmpty()
    }

    private fun isCrystalsAABBEmpty(): Boolean {
        val localFlooredPos = Globals.mc.player.flooredPosition
        return isEmpty(localFlooredPos.add(1, 0, 0)) &&
                isEmpty(localFlooredPos.add(-1, 0, 0)) &&
                isEmpty(localFlooredPos.add(0, 0, 1)) &&
                isEmpty(localFlooredPos.add(0, 0, -1)) &&
                isEmpty(localFlooredPos)
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