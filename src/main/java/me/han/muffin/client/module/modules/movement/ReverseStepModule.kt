package me.han.muffin.client.module.modules.movement

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.MotionUpdateMultiplierEvent
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.exploits.PacketFlyModule
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.extensions.mixin.entity.isInWeb
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

internal object ReverseStepModule: Module("ReverseStep", Category.MOVEMENT, true, "Step down blocks.") {

    private val mode = EnumValue(Mode.Normal, "Mode")
    private val webMode = EnumValue(WebMode.Normal, "WebMode")
    private val speed = NumberValue({ mode.value == Mode.Normal },3.0, 0.1, 5.0, 0.1, "Speed")
    private val dragTicks = NumberValue({ mode.value == Mode.Drag },3, 1, 5, 1, "DragTicks")

    private var wasOnGround = false
    private var shouldFreeze = false
    private var shouldStop = false

    private var ticksLastStop = 0

    private val shouldRunStep get() =
        MovementUtils.isMoving() &&
                !Globals.mc.player.isElytraFlying &&
                !Globals.mc.player.capabilities.isFlying &&
                !Globals.mc.gameSettings.keyBindJump.isKeyDown &&
                !EntityUtil.isInWater(Globals.mc.player) &&
                !EntityUtil.isAboveWater(Globals.mc.player) &&
                !Globals.mc.player.isOnLadder &&
                !Globals.mc.player.isInWater &&
                !Globals.mc.player.isInLava

    init {
        addSettings(mode, webMode, speed, dragTicks)
    }

    private enum class Mode {
        Normal, Drag
    }

    private enum class WebMode {
        Off, Normal
    }

    override fun getHudInfo(): String {
        return mode.fixedValue
    }

    private fun unStep(event: MotionUpdateMultiplierEvent) {
        val range = Globals.mc.player.entityBoundingBox.expand(0.0, -3.0, 0.0).contract(0.0, Globals.mc.player.height.toDouble(), 0.0)
        if (!Globals.mc.world.checkBlockCollision(range)) return

        val collisionBoxes = Globals.mc.world.getCollisionBoxes(Globals.mc.player, range)
        val newY = AtomicReference(0.0)
        collisionBoxes.forEach { newY.set(max(newY.get(), it.maxY)) }

        if (mode.value == Mode.Drag) {
            event.factor = dragTicks.value
            shouldStop = true
            ticksLastStop = 0
        }

    }

    private fun updateUnStep(event: MotionUpdateMultiplierEvent) {
        try {
            if (wasOnGround && !Globals.mc.player.onGround && Globals.mc.player.motionY <= 0.01 && SpeedModule.isDisabled) {
                unStep(event)
            }
        } finally {
            wasOnGround = Globals.mc.player.onGround
        }
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (fullNullCheck() || event.stage != EventStageable.EventStage.PRE) return

        if (webMode.value != WebMode.Off && Globals.mc.player.isInWeb) {
            Globals.mc.player.motionY = if (webMode.value == WebMode.Normal) -3.9200038147008747 else -0.22000000000000003
        }

        if (EntityUtil.isInWater(Globals.mc.player) || Globals.mc.player.isInWater || Globals.mc.player.isInLava || Globals.mc.player.isOnLadder || Globals.mc.player.isSneaking || Globals.mc.player.capabilities.isFlying || Globals.mc.player.isElytraFlying) return

        if (mode.value == Mode.Normal) {
            var y = 0.0
            while (y < 3.5) {
                if (Globals.mc.player.onGround && MovementUtils.isOnGround(y) &&
                    PacketFlyModule.isDisabled && (SpeedModule.isDisabled || SpeedModule.mode.value == SpeedModule.Mode.YPortTest)) {
                    Globals.mc.player.motionY = -speed.value
                    break
                }
                y += 0.1
            }
        }

    }

    @Listener
    private fun onMove(event: MoveEvent) {
        if (mode.value == Mode.Drag && shouldStop) {
            event.x = 0.0
            event.z = 0.0
            ++ticksLastStop
            if (ticksLastStop > dragTicks.value) {
                shouldStop = false
                ticksLastStop = 0
            }
        }
    }

    @Listener
    private fun onMotionUpdateFactor(event: MotionUpdateMultiplierEvent) {
        if (mode.value == Mode.Drag) {
            shouldFreeze = false
            updateUnStep(event)
        }
    }

//    @Listener
//    private fun onPacketSent(event: PacketEvent.Send) {
//        if (fullNullCheck() || event.stage != EventStageable.EventStage.PRE || mode.value != Mode.Drag) return
//        if (!canFall || shouldDisablePacket) return
//
//        if (event.packet !is CPacketPlayer.Position && event.packet !is CPacketPlayer.PositionRotation) return
//
//
//        packets.add(event.packet)
//        // ChatManager.sendMessage("Adding packet: ${event.packet}")
//        event.cancel()
//    }


}