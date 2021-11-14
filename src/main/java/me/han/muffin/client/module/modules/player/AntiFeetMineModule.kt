package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.block.BlockUtil
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.entity.PlayerUtil
import me.han.muffin.client.utils.extensions.mc.item.firstByStack
import me.han.muffin.client.utils.extensions.mc.item.hotbarSlots
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.function.Predicate

internal object AntiFeetMineModule: Module("AntiFeetMine", Category.PLAYER, "Protect your feet from people city boss.") {

    private val rotate = Value(true, "Rotate")
    private val delay = NumberValue(1, 0, 10, 1, "Delay")
    private val swingArm = Value(true, "Swing")

    private val timer = Timer()

    init {
        addSettings(rotate, delay, swingArm)
    }

    override fun onEnable() {
    }

    override fun onDisable() {
    }

    inline fun <reified B : Block, T : Slot> Iterable<T>.firstBlock(predicate: Predicate<ItemStack>? = null) =
        firstByStack { itemStack ->
            itemStack.item.let { it is ItemBlock && it.block is B } && (predicate == null || predicate.test(itemStack))
        }

    fun <T : Slot> Iterable<T>.firstBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
        firstByStack { itemStack ->
            itemStack.item.let { it is ItemBlock && it.block == block } && (predicate == null || predicate.test(itemStack))
        }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        val slot = Globals.mc.player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)?.hotbarSlot ?: -1
        if (slot == -1) return

        val centerPos = Globals.mc.player.flooredPosition
        val blocks = ArrayList<BlockPos>()

        when (PlayerUtil.getLocalPlayerFacing()) {
            PlayerUtil.FacingDirection.East -> {
                val eastBlock = centerPos.east().east()
                val east2Block = eastBlock.east()

                blocks.add(eastBlock)
                blocks.add(eastBlock.up())
                blocks.add(east2Block)
                blocks.add(east2Block.up())
            }
            PlayerUtil.FacingDirection.North -> {
                val northBlock = centerPos.north().north()
                val north2Block = northBlock.north()

                blocks.add(northBlock)
                blocks.add(northBlock.up())
                blocks.add(north2Block)
                blocks.add(north2Block.up())
            }
            PlayerUtil.FacingDirection.South -> {
                val southBlock = centerPos.south().south()
                val south2Block = southBlock.south()

                blocks.add(southBlock)
                blocks.add(southBlock.up())
                blocks.add(south2Block)
                blocks.add(south2Block.up())
            }
            PlayerUtil.FacingDirection.West -> {
                val westBlock = centerPos.west().west()
                val west2Block = westBlock.west()

                blocks.add(westBlock)
                blocks.add(westBlock.up())
                blocks.add(west2Block)
                blocks.add(west2Block.up())
            }
            else -> {
            }
        }

        val posToFill = blocks.firstOrNull { BlockUtil.valid(it) == BlockUtil.ValidResult.Ok } ?: return

        val lastSlot = Globals.mc.player.inventory.currentItem
        InventoryUtils.swapSlot(slot)

        if (rotate.value) addMotion { rotate(RotationUtils.getRotationTo(posToFill.toVec3dCenter())) }

        if (timer.passedTicks(delay.value)) {
            BlockUtil.place(posToFill, 5.0, rotate = false, slab = false, swingArm = swingArm.value)
            timer.reset()
        }

        InventoryUtils.swapSlot(lastSlot)
    }

}