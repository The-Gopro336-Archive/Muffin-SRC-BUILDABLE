package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.HoleManager.holeInfo
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.block.HoleType
import me.han.muffin.client.utils.combat.CrystalUtils
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.entity.EntityUtil.prevPosVector
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.extensions.mc.block.isAir
import me.han.muffin.client.utils.extensions.mc.block.isNull
import me.han.muffin.client.utils.extensions.mc.block.setNull
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.item.swapToItem
import me.han.muffin.client.utils.math.VectorUtils.distanceTo
import me.han.muffin.client.utils.math.VectorUtils.toBlockPos
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.timer.TickTimer
import me.han.muffin.client.utils.timer.TimeUnit
import me.han.muffin.client.value.NumberValue
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object AutoHoleMineModule: Module("AutoHoleMine", Category.COMBAT, "Mine enemy hole blocks for you.") {
    private val range = NumberValue(5F, 0F, 8F, 0.1F, "Range")
    private val delay = NumberValue(2, 1, 10, 1, "Delay")

    private val timer = TickTimer(TimeUnit.TICKS)
    private val miningPos = BlockPos.MutableBlockPos(0, -69, 0)
    private var target: EntityPlayer? = null

    init {
        addSettings(range, delay)
    }

    override fun getHudInfo(): String? = target?.name

    override fun onDisable() {
        miningPos.setNull()
    }

    override fun onEnable() {
        if (Globals.mc.player == null) return

        target = EntityUtil.findClosestTarget(range.value.toDouble())

        target?.let { player ->
            if (player.holeInfo.type != HoleType.Obsidian) {
                disable()
            } else {
                findHoleBlock(target!!)?.let { miningPos.setPos(it) }
            }
        } ?: run {
            disable()
        }

    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (Globals.mc.player.heldItemMainhand.item != Items.DIAMOND_PICKAXE && !swapToItem(Items.DIAMOND_PICKAXE)) {
            ChatManager.sendMessage("No pickaxe found in hotbar.")
            disable()
            return
        }

        val pos = miningPos

        if (pos.isNull) {
            disable()
        } else {
            target?.let {
                if (it.prevPosVector.distanceTo(pos) > 2.0) {
                    disable()
                    return
                }
            }

            if (pos.isAir) {
                disable()
                return
            }

            val center = pos.toVec3dCenter()
            val rotation = RotationUtils.getRotationTo(center)
            addMotion { rotate(rotation) }

            val diff = Globals.mc.player.eyePosition.subtract(center)
            val normalizedVec = diff.normalize()
            val facing = EnumFacing.getFacingFromVector(normalizedVec.x.toFloat(), normalizedVec.y.toFloat(), normalizedVec.z.toFloat())

            if (timer.tick(delay.value.toLong())) {
                Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, facing))
                Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, facing))
                Globals.mc.player.swingArm(EnumHand.MAIN_HAND)
            }

        }
    }

    private fun findHoleBlock(entity: Entity): BlockPos? {
        val pos = entity.positionVector.toBlockPos()
        var closestPos = 114.514 to BlockPos.ORIGIN
        for (facing in EnumFacing.HORIZONTALS) {
            val offsetPos = pos.offset(facing)
            val dist = Globals.mc.player.distanceTo(offsetPos)
            if (dist > range.value || dist > closestPos.first) continue
            if (offsetPos.block == Blocks.BEDROCK) continue
            if (!checkPos(offsetPos, facing)) continue
            closestPos = dist to offsetPos
        }
        return if (closestPos.second != BlockPos.ORIGIN) closestPos.second else null
    }

    private fun checkPos(pos: BlockPos, facingIn: EnumFacing): Boolean {
        if (CrystalUtils.canPlaceOn(pos.down()) && Globals.mc.world.isAirBlock(pos.up())) return true
        for (facing in EnumFacing.HORIZONTALS) {
            if (facing == facingIn.opposite) continue
            if (!CrystalUtils.canPlace(pos.offset(facing))) continue
            return true
        }
        return false
    }

}
