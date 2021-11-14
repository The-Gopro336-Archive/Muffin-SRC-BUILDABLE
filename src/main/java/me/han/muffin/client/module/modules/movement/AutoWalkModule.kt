package me.han.muffin.client.module.modules.movement

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.movement.PlayerUpdateMoveStateEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.Value
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityBoat
import net.minecraft.util.MovementInputFromOptions
import net.minecraft.world.chunk.EmptyChunk
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object AutoWalkModule: Module("AutoWalk", Category.MOVEMENT, "Automatically walks somewhere") {
    val mode = EnumValue(AutoWalkMode.FORWARD, "Direction")
    private val antiStuck = Value(true, "AntiStuck")

    enum class AutoWalkMode {
        FORWARD, BACKWARDS
    }

    init {
        addSettings(mode, antiStuck)
    }

    override fun onEnable() {
    }

    override fun onDisable() {
        when (mode.value) {
            AutoWalkMode.FORWARD -> {
                KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindForward.keyCode, false)
            }
            AutoWalkMode.BACKWARDS -> {
                KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindBack.keyCode, false)
            }
        }
    }

    @Listener
    private fun onPlayerUpdateMoveStateEvent(event: PlayerUpdateMoveStateEvent) {
        if (fullNullCheck() || event.movementInput !is MovementInputFromOptions) return

        when (mode.value) {
            AutoWalkMode.FORWARD -> {
                KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindForward.keyCode, true)
                event.movementInput.moveForward++
                Globals.mc.player.ridingEntity?.let {
                    val dir = MovementUtils.getDirectionSpeed(0.47)
                    it.motionX = dir[0]
                    it.motionZ = dir[1]
                }
            }
            AutoWalkMode.BACKWARDS -> {
                KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindBack.keyCode, true)
                event.movementInput.moveForward--
                Globals.mc.player.ridingEntity?.let {
                    val dir = MovementUtils.getDirectionSpeed(-0.47)
                    it.motionX = dir[0]
                    it.motionZ = dir[1]
                }
            }
        }

    }

    @Listener
    private fun onDisconnect(event: ServerEvent.Disconnect) {
        if (event.state == EventStageable.EventStage.PRE) disable()
    }

    private fun isBorderingChunk(entity: Entity, motionX: Double, motionZ: Double): Boolean {
        return antiStuck.value &&
                Globals.mc.world.getChunk((entity.posX + motionX).toInt() shr 4, (entity.posZ + motionZ).toInt() shr 4) is EmptyChunk
    }

}