package me.han.muffin.client.module.modules.combat

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.HoleManager.isInHole
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.block.BlockUtil
import me.han.muffin.client.utils.block.BlockUtil.PlaceResult
import me.han.muffin.client.utils.block.BlockUtil.ValidResult
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.extensions.mc.block.material
import me.han.muffin.client.utils.extensions.mc.item.firstBlock
import me.han.muffin.client.utils.extensions.mc.item.firstByStack
import me.han.muffin.client.utils.extensions.mc.item.hotbarSlots
import me.han.muffin.client.value.Value
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.function.Predicate

internal object AutoSelfTrapModule : Module("SelfTrap", Category.COMBAT, "Automatically place an obsidian over your head.") {
    private val holeCheck = Value(true, "HoleCheck")
    private val disable = Value(false, "Toggles")

    private val trapPos = BlockPos.MutableBlockPos(0, -69, 0)

    init {
        addSettings(holeCheck, disable)
    }

    fun <T : Slot> Iterable<T>.firstBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
        firstByStack { itemStack ->
            itemStack.item.let { it is ItemBlock && it.block == block } && (predicate == null || predicate.test(itemStack))
        }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (holeCheck.value && !Globals.mc.player.isInHole) return

        trapPos.setPos(Globals.mc.player.flooredPosition.up(2))

        if (isSelfTrapped) {
            if (disable.value) toggle()
            return
        }

        val slot = Globals.mc.player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)?.hotbarSlot ?: -1
        if (slot == -1) {
            ChatManager.sendMessage("${ChatFormatting.RED}Missing obsidian from hotbar.")
            disable()
            return
        }

        if (hasStack(Blocks.OBSIDIAN) ) {
            if (Globals.mc.player.onGround) {
                val lastSlot = Globals.mc.player.inventory.currentItem
                InventoryUtils.swapSlot(slot)

                val result = BlockUtil.valid(trapPos)
                if (result == ValidResult.AlreadyBlockThere && !trapPos.material.isReplaceable) {
                    finish(lastSlot)
                    return
                }

                if (result == ValidResult.NoNeighbours) {
                    val noNeighbourArray = arrayOf(trapPos.north(), trapPos.south(), trapPos.east(), trapPos.west(), trapPos.up(), trapPos.down().west())

                    for (noNeighbourPos in noNeighbourArray) {
                        val noNeighbourValid = BlockUtil.valid(noNeighbourPos)
                        if (noNeighbourValid == ValidResult.NoNeighbours || noNeighbourValid == ValidResult.NoEntityCollision) continue
                        val noNeighbourPlace = BlockUtil.place(noNeighbourPos, 5.0, rotate = false, slab = false, swingArm = true)
                        if (noNeighbourPlace == PlaceResult.Placed) {
                            finish(lastSlot)
                            return
                        }
                    }
                    finish(lastSlot)
                    return
                }

                BlockUtil.place(trapPos, 5.0, rotate = false, slab = false, swingArm = true)
                finish(lastSlot)
            }
        }
    }

    private fun finish(lastSlot: Int) {
        if (!slotEqualsBlock(lastSlot, Blocks.OBSIDIAN)) {
            InventoryUtils.swapSlot(lastSlot)
        }
    }

    private fun hasStack(type: Block): Boolean {
        if (Globals.mc.player.inventory.getCurrentItem().item is ItemBlock) {
            val block = Globals.mc.player.inventory.getCurrentItem().item as ItemBlock
            return block.block == type
        }
        return false
    }

    private fun slotEqualsBlock(slot: Int, type: Block): Boolean {
        if (Globals.mc.player.inventory.getStackInSlot(slot).item is ItemBlock) {
            val block = Globals.mc.player.inventory.getStackInSlot(slot).item as ItemBlock
            return block.block == type
        }
        return false
    }

    private val isSelfTrapped: Boolean get() {
        val state = trapPos.block
        return state != Blocks.AIR && state != Blocks.WATER && state != Blocks.LAVA
    }

}