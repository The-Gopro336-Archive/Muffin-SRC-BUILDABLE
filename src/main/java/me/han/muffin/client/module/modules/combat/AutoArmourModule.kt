package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.utils.extensions.mc.item.firstItem
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.exploits.XCarryModule
import me.han.muffin.client.module.modules.player.AutoReplenish
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.client.BindUtils
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.extensions.mc.item.armorSlots
import me.han.muffin.client.utils.extensions.mc.item.hotbarSlots
import me.han.muffin.client.utils.extensions.mc.item.removeHoldingItem
import me.han.muffin.client.utils.extensions.mixin.entity.syncCurrentPlayItem
import me.han.muffin.client.utils.math.rotation.Vec2f
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.BindValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.init.Enchantments
import net.minecraft.init.Items
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemElytra
import net.minecraft.item.ItemExpBottle
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketHeldItemChange
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.util.EnumHand
import net.minecraft.util.math.RayTraceResult
import org.lwjgl.input.Keyboard
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*

/**
 * @author han
 * Created by han on 19/9/2020
 * Updated by han on 9/10/2020
 */
internal object AutoArmourModule: Module("AutoArmour", Category.COMBAT, "Automatically equip best armour from inventory.", 290) {
    private val delay = NumberValue(50.0f, 0.0f, 1000.0f, 1.0f, "Delay")
    private val factor = NumberValue(2, 1, 15, 1, "Factor")

    private val curse = Value(false, "Curse")
    private val antiInvDesync = Value(true, "AntiDesync")
    private val expPause = Value(false, "ExpPause")
    private val preferElytra = Value(false, "PreferElytra")
    private val elytraReplace = Value(false, "ElytraReplace")
    val chestSwap = BindValue(Keyboard.KEY_NONE, "ChestSwap")

    private val stopExpWhenFull = Value(false, "StopThrowFull")

    private val expKeyboard = BindValue(Keyboard.KEY_NONE, "ExpKeyboard")
    private val expFactor = NumberValue({ expKeyboard.value != 0 }, 1, 1, 5, 1, "ExpFactor")
    private val alwaysLookDown = Value(true, "AlwaysLookDown")

    private val mending = Value(true, "Mending")
    private val onlyWhileGhost = Value({ mending.value }, false, "GhostOnly")
    private val playerFullCheck = Value(false, "FullCheckPlayer")
    private val mendingDurability = NumberValue({ mending.value }, 80, 1, 100, 1, "MendingDurability")
    private val autoSwitch = Value(true, "AutoSwitch")
    private val switchDurability = NumberValue({ autoSwitch.value }, 10, 1, 80, 1, "SwitchDurability")

    private val timer = Timer()

    private val waitingSlots = ArrayDeque<Task>()
    private val doneSlots = arrayListOf<Int>()
    private val switchedSlots = arrayListOf<Int>()

    private var isDoingGhostExp = false
    private var expSlot = -1

    private val startThrowTimer = Timer()
    private var canThrow = false


    init {
        addSettings(
            delay,
            curse,
            factor,
            stopExpWhenFull,
            antiInvDesync,
            expPause,
            preferElytra,
            elytraReplace,
            chestSwap,
            expKeyboard,
            expFactor,
            alwaysLookDown,
            mending,
            onlyWhileGhost,
            playerFullCheck,
            mendingDurability,
            autoSwitch,
            switchDurability
        )
    }


    override fun onEnable() {
    }

    override fun onDisable() {
        waitingSlots.clear()
        doneSlots.clear()
        switchedSlots.clear()
    }

    @Listener
    private fun onServerDisconnect(event: ServerEvent.Disconnect) {
        waitingSlots.clear()
        doneSlots.clear()
        switchedSlots.clear()
    }

    private fun doLookDown() {
        addMotion { rotate(Vec2f(Globals.mc.player.rotationYaw, 90F)) }
    }

    /**
     * do auto armour stuff
     * @param stack our armour slots check items
     * @param slot our equipment slots
     * @param armorSlot our target armour slots
     */
    private fun doAutoArmour(stack: ItemStack, slot: EntityEquipmentSlot, armorSlot: Int, shouldTakeOff: Boolean) {
        if (waitingSlots.isEmpty() && (!expPause.value || isHoldingExp())) {
            if (autoSwitch.value) {
                if (!stack.isEmpty) {
                    val dmg = EntityUtil.getArmorPct(stack)
                    if (dmg < switchDurability.value) {
                        val foundSlot = InventoryUtils.findArmourSlot(slot, true, curse.value, preferElytra.value, XCarryModule.isEnabled, switchDurability.value)
                        replaceArmourSlot(armorSlot, foundSlot)
                        return
                    }
                }
            }

            if (mending.value && shouldTakeOff && isValidToMend() && (isSafe() || EntityUtil.isSafe(Globals.mc.player, 1.0, false))) {
                if (alwaysLookDown.value) doLookDown()
                if (!stack.isEmpty) {
                    val durability = EntityUtil.getArmorPct(stack)
                    if (durability >= mendingDurability.value) {
                        takeOutArmour(armorSlot)
                    }
                }
                return
            }

            if (stack.item == Items.AIR && waitingSlots.isEmpty()) {
                val foundSlot = InventoryUtils.findArmourSlot(slot, true, curse.value, preferElytra.value, XCarryModule.isEnabled, 0)
                if (!expPause.value || isHoldingExp()) putArmour(armorSlot, foundSlot)
            }

        }

        if (timer.passed(delay.value.toDouble())) {
            if (waitingSlots.isNotEmpty()) for (i in 0 until factor.value) waitingSlots.pollFirst()?.run()
            timer.reset()
        }

    }

    private fun isArmourFull(stack: ItemStack): Boolean {
        return EntityUtil.getArmorPct(stack) == 100
    }

    private fun isHoldingExp() = Globals.mc.player.heldItemMainhand.item is ItemExpBottle || Globals.mc.player.heldItemOffhand.item is ItemExpBottle

    private fun doGhostExpCheck(): Boolean {
        return onlyWhileGhost.value || isHoldingExp()
    }

    private fun isValidToMend(): Boolean {
        return isDoingGhostExp || (isHoldingExp() && (!onlyWhileGhost.value && Globals.mc.gameSettings.keyBindUseItem.isKeyDown))
    }

    private fun isValidToMend(stack: ItemStack): Boolean {
         val hasMending = EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, stack) > 0
         return hasMending && (isDoingGhostExp || (isHoldingExp() && (!onlyWhileGhost.value && Globals.mc.gameSettings.keyBindUseItem.isKeyDown)))
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (stopExpWhenFull.value) {
            if (!doGhostExpCheck() || !isValidToMend()) return

            when (event.packet) {
                is CPacketHeldItemChange -> {
                    if (event.packet.slotId == expSlot) isDoingGhostExp = true
                }
                is CPacketPlayerTryUseItem -> {
                    isDoingGhostExp = true

                    val container = Globals.mc.player.inventoryContainer

                    val helm = EntityUtil.getArmorPct(container.getSlot(5).stack)
                    val chest = EntityUtil.getArmorPct(container.getSlot(6).stack)
                    val legging = EntityUtil.getArmorPct(container.getSlot(7).stack)
                    val feet = EntityUtil.getArmorPct(container.getSlot(8).stack)

                    if (helm > 99 && chest > 99 && legging > 99 && feet > 99) event.cancel()
                }
            }
        }

    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (fullNullCheck()) return

        if (Globals.mc.player.isCreative || Globals.mc.player.capabilities.isCreativeMode) return
        if (Globals.mc.currentScreen is GuiContainer && Globals.mc.currentScreen !is GuiInventory) return

        if (event.stage == EventStageable.EventStage.PRE) {
            expSlot = Globals.mc.player.hotbarSlots.firstItem(Items.EXPERIENCE_BOTTLE)?.hotbarSlot ?: -1

            if (antiInvDesync.value && !AutoReplenish.INSTANCE.tasks.hasNext() && !Globals.mc.player.inventory.itemStack.isEmpty) { // If player is holding an in inventory
                if (Globals.mc.currentScreen is GuiContainer) { // If inventory is open (playing moving item)
                    timer.resetTimeSkipTo(150L) // Wait for 3 extra ticks if player is moving item
                } else {
                    removeHoldingItem() // If inventory is not open (ex. inventory desync)
                }
                return
            }

            if (BindUtils.checkIsClicked(expKeyboard.value) && hasBlockUnder() && expSlot != -1) {
                canThrow = true
                if (alwaysLookDown.value) doLookDown()
                isDoingGhostExp = true
            } else {
                canThrow = false
                startThrowTimer.reset()
                isDoingGhostExp = false
            }

        } else if (event.stage == EventStageable.EventStage.POST) {
            if (canThrow && startThrowTimer.passed(75.0)) {
                isDoingGhostExp = true

                val isEqualToExpSlot = Globals.mc.player.inventory.currentItem != expSlot
                val lastSlot = Globals.mc.player.inventory.currentItem

                if (isEqualToExpSlot) Globals.mc.player.connection?.sendPacket(CPacketHeldItemChange(expSlot))
                for (i in 0 until expFactor.value) Globals.mc.player.connection?.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))

                if (isEqualToExpSlot) InventoryUtils.swapSlot(lastSlot)
            }

            val armourSlot = Globals.mc.player.armorSlots

            val helm = armourSlot[0].stack
            val chest = armourSlot[1].stack
            val legging = armourSlot[2].stack
            val feet = armourSlot[3].stack

            val shouldNotTakeOff = isArmourFull(helm) && isArmourFull(chest) && isArmourFull(legging) && isArmourFull(feet)
            doAutoArmour(helm, EntityEquipmentSlot.HEAD, 5, !shouldNotTakeOff)
            doAutoArmour(chest, EntityEquipmentSlot.CHEST, 6, !shouldNotTakeOff)
            doAutoArmour(legging, EntityEquipmentSlot.LEGS, 7, !shouldNotTakeOff)
            doAutoArmour(feet, EntityEquipmentSlot.FEET, 8, !shouldNotTakeOff)

            replaceElytra()
        }

    }

    /**
     * Put armour into armour slot
     * @param targetSlot the armour slot that we want to place to
     * @param inventorySlot the armour slot we found in inventory / backpack
     */
    private fun putArmour(targetSlot: Int, inventorySlot: Int) {
        if (waitingSlots.isEmpty() && inventorySlot != -1) {
            doneSlots.remove(inventorySlot)
            if (inventorySlot in 1..4) {
                waitingSlots.add(Task(inventorySlot))
                waitingSlots.add(Task(targetSlot))
            } else {
                waitingSlots.add(Task(inventorySlot, true))
            }
        }
    }

    /**
     * take out armour from our armour slots
     * @param armourSlot target armour slots
     */
    private fun takeOutArmour(armourSlot: Int) {
        if (waitingSlots.isEmpty()) {
            var targetInventorySlot = -1
            for (slots in InventoryUtils.findEmptySlots(InventoryUtils.InventoryHotbar.All, XCarryModule.isEnabled)) {
                if (doneSlots.contains(targetInventorySlot)) continue
                targetInventorySlot = slots
                doneSlots.add(slots)
            }

            if (targetInventorySlot != -1 && armourSlot != -1) {
                if (targetInventorySlot in 1..4) {
                    waitingSlots.add(Task(armourSlot))
                    waitingSlots.add(Task(targetInventorySlot))
                } else {
                    waitingSlots.add(Task(armourSlot, true))
                }
            }
        }
    }

    var isDoingStuff = false
    private fun future(slot: Int): Boolean {
        if (isDoingStuff) return false

        if (preferElytra.value && slot == 6) {
            if (Globals.mc.player.inventoryContainer.getSlot(slot).stack.item is ItemElytra) {
                return false
            }
            for (i in 9 until 45) {
                val stack = Globals.mc.player.inventoryContainer.getSlot(i).stack
                if (stack != ItemStack.EMPTY && stack.item is ItemElytra && stack.count == 1 && stack.maxDamage - stack.itemDamage > 5) {
                    val isChestEmpty = Globals.mc.player.inventoryContainer.getSlot(slot).stack == ItemStack.EMPTY
                    if (!isChestEmpty) waitingSlots.add(Task(slot))
                    waitingSlots.add(Task(i, true))
                    if (!isChestEmpty) waitingSlots.add(Task(slot))
                    return true
                }
            }
        }

        var armourDurability = -1
        var bestDamage = -1
        val priority = "protection"

        return false
    }

    /**
     * replace armour with inventory armour
     * @param targetSlot the armour slot that we want to place to
     * @param inventorySlot the armour slot we found in inventory / backpack
     */
    private fun replaceArmourSlot(targetSlot: Int, inventorySlot: Int) {
        if (waitingSlots.isEmpty()) {
            if (inventorySlot != -1) {
                waitingSlots.add(Task(inventorySlot))
                waitingSlots.add(Task(targetSlot, inventorySlot in 1..4))
                waitingSlots.add(Task(inventorySlot))
            }
        }
    }

    private fun replaceElytra() {
        if (expPause.value && isHoldingExp()) return

        if (elytraReplace.value && !Globals.mc.player.inventoryContainer.getSlot(6).stack.isEmpty) {

            val stack = Globals.mc.player.inventoryContainer.getSlot(6).stack

            if (stack.item is ItemElytra) {

                if (!ItemElytra.isUsable(stack) && EntityUtil.getArmorPct(stack) < 3) {

                    for (i in Globals.mc.player.inventoryContainer.inventory.indices) {
                        /// @see: https://wiki.vg/Inventory, 0 is crafting slot, and 5,6,7,8 are Armor slots
                        if (i == 0 || i == 5 || i == 6 || i == 7 || i == 8) continue

                        val s = Globals.mc.player.inventoryContainer.inventory[i]

                        if (s.item != Items.AIR) {
                            if (s.item is ItemElytra && ItemElytra.isUsable(s)) {
                                Globals.mc.playerController.windowClick(0, i, 0, ClickType.PICKUP, Globals.mc.player)
                                Globals.mc.playerController.windowClick(0, 6, 0, ClickType.PICKUP, Globals.mc.player)
                                Globals.mc.playerController.windowClick(0, i, 0, ClickType.PICKUP, Globals.mc.player)
                                break
                            }
                        }

                    }

                }
            }

        }
    }

    private fun isSafe(): Boolean {
        if (!playerFullCheck.value) {
            val closest = EntityUtil.findClosestTarget(8.0) ?: return true
            return Globals.mc.player.getDistanceSq(closest) >= 64
        }
        return Globals.mc.world.playerEntities.all {
            !EntityUtil.isntValid(it, 8.0)
        }
    }

    private fun hasBlockUnder(): Boolean {
        val posVec = Globals.mc.player.positionVector
        val result = Globals.mc.world.rayTraceBlocks(posVec, posVec.add(0.0, -5.33, 0.0), false, true, false)
        return result != null && result.typeOfHit == RayTraceResult.Type.BLOCK
    }

    fun getArmorValue(itemStack: ItemStack): Float {
        val item = itemStack.item
        return if (item !is ItemArmor) -1.0F else item.damageReduceAmount * getProtectionModifier(itemStack)
    }

    private fun getProtectionModifier(itemStack: ItemStack): Float {
        for (i in 0 until itemStack.enchantmentTagList.tagCount()) {
            val id = itemStack.enchantmentTagList.getCompoundTagAt(i).getInteger("id")
            val level = itemStack.enchantmentTagList.getCompoundTagAt(i).getInteger("lvl")
            if (id != 0) continue
            return 1f + 0.04F * level
        }
        return 1f
    }

    class Task {
        private val slot: Int
        private val update: Boolean
        private val quickClick: Boolean

        constructor() {
            update = true
            slot = -1
            quickClick = false
        }

        constructor(slot: Int) {
            this.slot = slot
            quickClick = false
            update = false
        }

        constructor(slot: Int, quickClick: Boolean) {
            this.slot = slot
            this.quickClick = quickClick
            update = false
        }

        fun run() {
            if (slot != -1) Globals.mc.playerController.windowClick(Globals.mc.player.inventoryContainer.windowId, slot, 0, if (quickClick) ClickType.QUICK_MOVE else ClickType.PICKUP, Globals.mc.player)
            if (update) Globals.mc.playerController.syncCurrentPlayItem()
        }

        val isSwitching get() = !update
    }

}