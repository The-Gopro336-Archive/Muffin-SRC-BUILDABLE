package me.han.muffin.client.module.modules.movement

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.client.TravelEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityBoat
import net.minecraft.entity.passive.AbstractHorse
import net.minecraft.entity.passive.EntityHorse
import net.minecraft.entity.passive.EntityPig
import net.minecraft.world.chunk.EmptyChunk
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.cos
import kotlin.math.sin

object EntitySpeedModule: Module("EntitySpeed", Category.MOVEMENT, "Abuse client-sided movement to shape sound barrier breaking rideables.") {
    private val speed = NumberValue(1F, 0.1F, 10.0F, 0.1F, "Speed")
    private val antiStuck = Value(false, "AntiStuck")
    private val flight = Value(false, "Flight")
    private val glideSpeed = NumberValue({ flight.value },0.1F, 0.0F, 1.0F, 0.01F,"GlideSpeed")
    private val verticalSpeed = NumberValue({ flight.value },1.0F, 0.0F, 5.0F, 0.1F,"VerticalSpeed")

    init {
        addSettings(speed, antiStuck, flight, glideSpeed, verticalSpeed)
    }


    @Listener
    private fun onTravel(event: TravelEvent) {
        if (fullNullCheck()) return

        Globals.mc.player.ridingEntity?.let {
            if (it is EntityPig || it is AbstractHorse || it is EntityBoat && it.controllingPassenger == Globals.mc.player) {
                steerEntity(it)
                if (flight.value) fly(it)
                event.cancel()
            }
        }

    }


    private fun steerEntity(entity: Entity) {
        val yawRad = MovementUtils.calcMoveYaw()

        val motionX = -sin(yawRad) * speed.value
        val motionZ = cos(yawRad) * speed.value

        if (MovementUtils.isMoving() && !isBorderingChunk(entity, motionX, motionZ)) {
            entity.motionX = motionX
            entity.motionZ = motionZ
        } else {
            entity.motionX = 0.0
            entity.motionZ = 0.0
        }

        if (entity is EntityHorse || entity is EntityBoat) {
            entity.rotationYaw = Globals.mc.player.rotationYaw

            // Make sure the boat doesn't turn etc (params: isLeftDown, isRightDown, isForwardDown, isBackDown)
            if (entity is EntityBoat) entity.updateInputs(false, false, false, false)
        }
    }


    private fun fly(entity: Entity) {
        if (!entity.isInWater) entity.motionY = -glideSpeed.value.toDouble()
        if (Globals.mc.gameSettings.keyBindJump.isKeyDown) entity.motionY += verticalSpeed.value / 2.0
    }

    private fun isBorderingChunk(entity: Entity, motionX: Double, motionZ: Double): Boolean {
        return antiStuck.value && Globals.mc.world.getChunk((entity.posX + motionX).toInt() shr 4, (entity.posZ + motionZ).toInt() shr 4) is EmptyChunk
    }

}