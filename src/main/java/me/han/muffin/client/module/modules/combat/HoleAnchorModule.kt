package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateMultiplierEvent
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.event.events.movement.PlayerUpdateMoveStateEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.manager.managers.HoleManager
import me.han.muffin.client.manager.managers.HoleManager.isInHole
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.movement.SpeedModule
import me.han.muffin.client.utils.block.HoleUtils.isCentered
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.entity.MovementUtils.resetMove
import me.han.muffin.client.utils.entity.MovementUtils.speed
import me.han.muffin.client.utils.extensions.kotlin.square
import me.han.muffin.client.utils.extensions.kotlin.toRadian
import me.han.muffin.client.utils.extensions.mc.block.isAir
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.MovementInputFromOptions
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

internal object HoleAnchorModule: Module("HoleAnchor", Category.PLAYER, "Anchor you when you are above a hole.") {

    private val disableStrafe = Value(false,"DisableStrafe")
    private val airStrafe = Value(true, "AirStrafe")
    private val range = NumberValue(2.5F, 0.0F, 5.0F, 0.1F, "Range")
    private val dragTicks = NumberValue(3, 1, 5, 1, "DragTicks")

    private var holeCenter: Vec3d? = null
    private var stuckTicks = 0
    private var canDrag = false

    init {
        addSettings(disableStrafe, airStrafe, range, dragTicks)
    }

    override fun onDisable() {
        if (fullNullCheck()) return

        holeCenter = null
        stuckTicks = 0
    }

    override fun onToggle() {
        canDrag = false
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return
        if (event.packet is SPacketPlayerPosLook) disable()
    }

    @Listener
    private fun onPlayerUpdateMoveState(event: PlayerUpdateMoveStateEvent) {
        if (fullNullCheck()) return
        if (event.movementInput is MovementInputFromOptions && holeCenter != null) {
            event.movementInput.resetMove()
        }
    }

    @Listener
    private fun onMotionFactor(event: MotionUpdateMultiplierEvent) {
        if (fullNullCheck() || !canDrag) return
        event.factor = dragTicks.value
    }

    @Listener
    private fun onMoving(event: MoveEvent) {
        if (fullNullCheck()) return

        if (!Globals.mc.player.isAlive) {
            disable()
            return
        }

        val currentSpeed = Globals.mc.player.speed
        if (shouldDisable(currentSpeed)) {
            disable()
            return
        }

        getHole()?.let {
            canDrag = true
            if (disableStrafe.value) SpeedModule.disable()

            if (!Globals.mc.player.isCentered(it)) {
                if (airStrafe.value || Globals.mc.player.onGround) {
                    val playerPos = Globals.mc.player.positionVector

                    val yawRad = RotationUtils.getRotationTo(playerPos, it).x.toRadian()
                    val dist = hypot(it.x - playerPos.x, it.z - playerPos.z)
                    val speed = if (Globals.mc.player.onGround) min(0.2805, dist / 2.0) else currentSpeed + 0.02

                    event.x = -sin(yawRad) * speed
                    event.z = cos(yawRad) * speed

                    if (Globals.mc.player.collidedHorizontally) stuckTicks++ else stuckTicks = 0
                }
            }

        } ?: run {
            canDrag = false
        }

    }

    private fun findHole(): Vec3d? {
        val playerPos = Globals.mc.player.flooredPosition
        val playerPosVec = Globals.mc.player.positionVector
        val squaredRange = range.value.square

        return HoleManager.holeInfos
            .asSequence()
            .filter { it.origin.y < playerPos.y }
            .filter { it.center.squareDistanceTo(playerPosVec) <= squaredRange }
            .filter { (it.origin.y..playerPos.y + 2).all { yOffset -> BlockPos(it.origin.x, yOffset , it.origin.z).isAir }  }
            .minByOrNull { it.center.squareDistanceTo(playerPosVec) }?.center
    }

    private fun shouldDisable(currentSpeed: Double) =
        holeCenter?.let { Globals.mc.player.posY < it.y } ?: false
                || stuckTicks > 5 && currentSpeed < 0.1
                || currentSpeed < 0.01 && Globals.mc.player.isInHole

    private fun getHole() =
        if (Globals.mc.player.ticksExisted % 10 == 0 && Globals.mc.player.flooredPosition != holeCenter) findHole() else holeCenter ?: findHole()

}