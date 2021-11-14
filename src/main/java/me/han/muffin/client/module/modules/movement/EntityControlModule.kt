package me.han.muffin.client.module.modules.movement

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.entity.HorseSaddledEvent
import me.han.muffin.client.event.events.entity.PigTravelEvent
import me.han.muffin.client.event.events.entity.SteerEntityEvent
import me.han.muffin.client.module.Module
import net.minecraft.entity.passive.EntityPig
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object EntityControlModule : Module("EntityControl", Category.MOVEMENT, "Allow you to control a rideable entity.") {

    override fun getHudInfo(): String? {
        if (Globals.mc.player.ridingEntity != null) return Globals.mc.player.ridingEntity!!.name
        return null
    }

    @Listener
    private fun onPigTravel(event: PigTravelEvent) {
        if (fullNullCheck() || Globals.mc.player.ridingEntity == null) return

        val moving = Globals.mc.player.movementInput.moveForward != 0.0F || Globals.mc.player.movementInput.moveStrafe != 0.0F || Globals.mc.player.movementInput.jump

        Globals.mc.player.ridingEntity?.let {
            if (it is EntityPig && !moving && it.onGround) {
                event.cancel()
            }
        }

    }

    @Listener
    private fun onSteerEntity(event: SteerEntityEvent) {
        event.cancel()
    }

    @Listener
    private fun onHorseSaddled(event: HorseSaddledEvent) {
        event.cancel()
    }

}