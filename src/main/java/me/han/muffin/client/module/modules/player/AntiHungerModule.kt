package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.extensions.mixin.netty.onGround
import me.han.muffin.client.value.Value
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayer
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object AntiHungerModule : Module("AntiHunger", Category.PLAYER, "Reduces hunger lost when moving around.") {
    private val cancelMovementState = Value(true, "NoMoveState")

    init {
        addSettings(cancelMovementState)
    }

    @Listener
    private fun onPacketSend(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (cancelMovementState.value && event.packet is CPacketEntityAction) {
            if (event.packet.action == CPacketEntityAction.Action.START_SPRINTING || event.packet.action == CPacketEntityAction.Action.STOP_SPRINTING) {
                event.cancel()
            }
        }

        if (event.packet is CPacketPlayer) {
            // Trick the game to think that tha player is flying even if he is on ground. Also check if the player is flying with the Elytra.
            event.packet.onGround = (Globals.mc.player.fallDistance <= 0 || Globals.mc.playerController.isHittingBlock) && Globals.mc.player.isElytraFlying
        }
    }

}