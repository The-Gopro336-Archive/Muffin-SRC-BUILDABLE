package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.world.block.RightClickBlockEvent
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.extensions.kotlin.square
import me.han.muffin.client.utils.extensions.mc.item.firstByStack
import me.han.muffin.client.utils.extensions.mc.item.hotbarSlots
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.item.Item
import net.minecraft.item.ItemPickaxe
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.tileentity.TileEntityHopper
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object HopperBreakerModule : Module("HopperBreaker", Category.COMBAT, "Break nearby hopper around you.") {

    private val autoSwitch = Value(true, "AutoSwitch")
    private val switchBack = Value(false, "SwitchBack")
    private val rotate = Value(true,"Rotate")
    private val breakOwn = Value(false,"BreakOwn")
    private val range = NumberValue(5.0, 1.0, 10.0, 0.1, "Range")
    private val delay = NumberValue(5.0, 1.0, 20.0, 0.1, "Delay")

    private val hoppersPlaced = HashSet<BlockPos>()
    private val timer = Timer()

    init {
        addSettings(autoSwitch, switchBack, rotate, breakOwn, range, delay)
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        val hoppers = Globals.mc.world.loadedTileEntityList
            .filterIsInstance<TileEntityHopper>()
            .sortedBy { Globals.mc.player.getDistanceSq(it.xPos, it.yPos, it.zPos) }
        //     .sortedBy { it.getDistanceSq(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ) }
        if (hoppers.isEmpty()) return


        if (!timer.passed(delay.value)) return

        for (hopper in hoppers) {
            val hopperPos = hopper.pos

            if (!breakOwn.value && hoppersPlaced.contains(hopperPos)) continue

            if (Globals.mc.player.getDistanceSq(hopperPos.x.toDouble(), hopperPos.y.toDouble(), hopperPos.z.toDouble()) >= range.value.square) continue

            val lastSlot = Globals.mc.player.inventory.currentItem

            if (autoSwitch.value) {
                Globals.mc.player.hotbarSlots.firstByStack { it.item is ItemPickaxe }?.let {
                    InventoryUtils.swapSlot(it.hotbarSlot)
                }
            }

            if (rotate.value) addMotion { rotate(RotationUtils.getRotationTo(hopperPos.toVec3dCenter())) }

            Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, hopper.pos, EnumFacing.UP))
            Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, hopper.pos, EnumFacing.UP))
            Globals.mc.player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))

            if (autoSwitch.value && switchBack.value) InventoryUtils.swapSlot(lastSlot)
        }

        timer.reset()

    }

    override fun onDisable() {
        if (fullNullCheck()) return
        hoppersPlaced.clear()
    }

    @Listener
    private fun onRightClickBlock(event: RightClickBlockEvent) {
        if (Globals.mc.player.inventory.getStackInSlot(Globals.mc.player.inventory.currentItem).item == Item.getItemById(154)) {
            hoppersPlaced.add(Globals.mc.objectMouseOver.blockPos.offset(Globals.mc.objectMouseOver.sideHit))
        }
    }

}