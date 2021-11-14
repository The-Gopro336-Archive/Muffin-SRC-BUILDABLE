package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.entity.OnItemUsePassEvent
import me.han.muffin.client.event.events.world.AllowInteractEvent
import me.han.muffin.client.event.events.world.block.ResetBlockRemovingEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.block.BlockUtil
import me.han.muffin.client.value.BindValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.init.Blocks
import net.minecraft.item.ItemAppleGold
import net.minecraft.item.ItemEndCrystal
import net.minecraft.item.ItemPickaxe
import org.lwjgl.input.Keyboard
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object InteractionTweaksModule : Module("InteractionTweaks", Category.PLAYER, true, "Better interaction than normal.") {

    private val antiHitBox = Value(true, "AntiHitBox")
    private val ignorePickaxe = Value({ antiHitBox.value }, true, "AHB-Pickaxe")
    private val ignoreCrystal = Value({ antiHitBox.value }, false, "AHB-Crystal")
    private val ignoreGApple = Value({ antiHitBox.value }, true, "AHB-GApple")
    private val ignoreWhenSneak = Value({ antiHitBox.value }, false, "AHB-Sneak")

    private val multiTask = Value(true, "MultiTask")
    private val stickyBreak = Value(true, "StickyBreak")

    val liquidInteract = Value(false, "LiquidInteract")
    val liquidInteractBind = BindValue(Keyboard.KEY_NONE, "LiquidBind")

    private val toolObby = Value(false, "ToolObby")
    private val obbyDelay = NumberValue({ toolObby.value }, 5, 0, 15, 1, "PlaceDelay")
    private val obbyRotate = Value({ toolObby.value }, false, "Rotate")

    private var ticks = 0

    init {
        addSettings(
            antiHitBox, ignorePickaxe, ignoreCrystal, ignoreGApple, ignoreWhenSneak,
            multiTask, stickyBreak,
            liquidInteract, liquidInteractBind,
            toolObby, obbyRotate, obbyDelay
        )
    }

    @JvmStatic
    val isLiquidInteractEnabled get() = isEnabled && liquidInteract.value

    @JvmStatic
    fun shouldIgnoreHitBox(): Boolean {
        if (isDisabled || !antiHitBox.value) return false

        val mainHandItem = Globals.mc.player.heldItemMainhand.item

        val isHoldingPickaxe = mainHandItem is ItemPickaxe
        val isHoldingCrystal = mainHandItem is ItemEndCrystal
        val isHoldingGApple = mainHandItem is ItemAppleGold

        val isSneakPressing = Globals.mc.gameSettings.keyBindSneak.isKeyDown

        return (ignorePickaxe.value && isHoldingPickaxe) ||
            (ignoreCrystal.value && isHoldingCrystal) ||
            (ignoreGApple.value && isHoldingGApple) ||
            (ignoreWhenSneak.value && isSneakPressing)
    }

    @Listener
    private fun onResetBlockRemoving(event: ResetBlockRemovingEvent) {
        if (stickyBreak.value && (SpeedMineModule.isDisabled || SpeedMineModule.tweaks.value != SpeedMineModule.Tweaks.PacketInstant || SpeedMineModule.packetInstantMode.value != SpeedMineModule.PacketInstantMode.Old)) event.cancel()
    }

    @Listener
    private fun setMultiTask(event: AllowInteractEvent) {
        if (multiTask.value) event.isUsingItem = false
    }

    @Listener
    private fun onItemUsePass(event: OnItemUsePassEvent) {
        if (!toolObby.value) return

        if (ticks > Globals.mc.player.ticksExisted && ticks != 0) {
            return
        }

        val pos = Globals.mc.objectMouseOver.blockPos.offset(Globals.mc.objectMouseOver.sideHit) ?: return

        val lastSlot = Globals.mc.player.inventory.currentItem
        val obbySlot = InventoryUtils.findBlock(Blocks.OBSIDIAN)

        if (obbySlot == -1) {
            return
        }

        InventoryUtils.swapSlot(obbySlot)
        BlockUtil.place(pos, Globals.mc.playerController.blockReachDistance.toDouble(), obbyRotate.value, slab = false, swingArm = false)
        InventoryUtils.swapSlot(lastSlot)
        ticks = Globals.mc.player.ticksExisted + obbyDelay.value
    }


}