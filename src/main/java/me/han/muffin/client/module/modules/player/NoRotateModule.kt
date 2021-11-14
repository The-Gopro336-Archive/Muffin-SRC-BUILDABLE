package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.extensions.mixin.netty.pitch
import me.han.muffin.client.utils.extensions.mixin.netty.rotationPitch
import me.han.muffin.client.utils.extensions.mixin.netty.rotationYaw
import me.han.muffin.client.utils.extensions.mixin.netty.yaw
import me.han.muffin.client.utils.math.rotation.Vec2f
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object NoRotateModule: Module("NoRotate", Category.PLAYER, "Prevents you from processing server rotations.") {
    var preferRotations: Vec2f? = null

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (event.packet !is SPacketPlayerPosLook) return

        if (!Globals.mc.world.isBlockLoaded(BlockPos(Globals.mc.player.lastTickPosX, Globals.mc.player.lastTickPosY, Globals.mc.player.lastTickPosZ), false) || !Globals.mc.world.isBlockLoaded(BlockPos(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ), false)) {
            return
        }

        preferRotations = Vec2f(event.packet.yaw, event.packet.pitch)

        val flags = event.packet.flags
        if (flags.remove(SPacketPlayerPosLook.EnumFlags.Y_ROT)) {
            preferRotations!!.plus(Globals.mc.player.rotationYaw, 0.0F)
        }
        if (flags.remove(SPacketPlayerPosLook.EnumFlags.X_ROT)) {
            preferRotations!!.plus(0.0F, Globals.mc.player.rotationPitch)
        }

        event.packet.rotationPitch = Globals.mc.player.rotationPitch
        event.packet.rotationYaw = Globals.mc.player.rotationYaw
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck() || event.packet !is CPacketPlayer.PositionRotation || preferRotations == null) return
        event.packet.yaw = preferRotations!!.x
        event.packet.pitch = preferRotations!!.y
        preferRotations = null
    }

}