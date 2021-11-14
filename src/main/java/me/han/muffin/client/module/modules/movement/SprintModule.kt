package me.han.muffin.client.module.modules.movement

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.event.events.entity.CancelSprintEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.Value
import net.minecraft.init.MobEffects
import net.minecraft.network.play.client.CPacketEntityAction
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object SprintModule : Module("Sprint", Category.MOVEMENT, "Automatically sprint.") {

    private val mode = EnumValue(SprintMode.Rage, "Mode")
    private val keepSprinting = Value(true, "KeepSprinting")
    private val keepSprintingPacket = Value(false, "KeepSprintPacket")

    private enum class SprintMode {
        Legit, Rage, Instant
    }

    init {
        addSettings(mode, keepSprinting, keepSprintingPacket)
    }

    override fun getHudInfo(): String = mode.fixedValue

    override fun onDisable() {
        if (fullNullCheck()) return
        Globals.mc.player.isSprinting = false
    }

    @Listener
    private fun onMove(event: MoveEvent) {
        if (fullNullCheck()) return

        if (mode.value == SprintMode.Instant && canSprint()) {
            MovementUtils.strafe(0.2805)
            if (!Globals.mc.player.isSprinting) Globals.mc.player.isSprinting = true
        }

    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (mode.value != SprintMode.Instant) {
            if (canSprint()) {
                if (!Globals.mc.player.isSprinting) Globals.mc.player.isSprinting = true
            }
        }

    }

    private fun isFlying(): Boolean {
        return Globals.mc.player.isElytraFlying && Globals.mc.player.capabilities.isFlying
    }

    @Listener
    private fun onCancelSprint(event: CancelSprintEvent) {
        if (keepSprinting.value) event.shouldCancel = false
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (!keepSprintingPacket.value || event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return
        if (event.packet is CPacketEntityAction && event.packet.action == CPacketEntityAction.Action.STOP_SPRINTING) {
            event.cancel()
        }
    }

    fun canSprint(): Boolean {
        val hunger = Globals.mc.player.foodStats.foodLevel > 6
        val creative = Globals.mc.player.isCreative
        val allowFlying = Globals.mc.player.capabilities.allowFlying

        val firstCheck = hunger || creative || allowFlying

        val movingForward = Globals.mc.player.moveForward > 0.0f

        var moving = MovementUtils.isKeyDown()

        moving = moving && (Globals.mc.player.motionX != 0.0 || Globals.mc.player.motionZ != 0.0)
        moving = moving && mode.value == SprintMode.Rage || mode.value == SprintMode.Instant || movingForward && (mode.value != SprintMode.Rage && mode.value != SprintMode.Instant)

        val collided = Globals.mc.player.collidedHorizontally
        val sneaking = Globals.mc.player.isSneaking

        val isBlind = Globals.mc.player.isPotionActive(MobEffects.BLINDNESS)

        val using = Globals.mc.player.itemInUseCount != 0

        return firstCheck && moving && !collided && !sneaking && !isFlying() && !isBlind
    }


}