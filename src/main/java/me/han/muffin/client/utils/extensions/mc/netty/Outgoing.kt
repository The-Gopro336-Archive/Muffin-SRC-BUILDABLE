package me.han.muffin.client.utils.extensions.mc.netty

import net.minecraft.entity.Entity
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.math.Vec3d

fun Entity.toPacketPosition(onGround: Boolean = this.onGround): CPacketPlayer.Position {
    return CPacketPlayer.Position(this.posX, this.posY, this.posZ, onGround)
}

fun Entity.toPacketPosition(xOffset: Double, yOffset: Double, zOffset: Double, onGround: Boolean = this.onGround): CPacketPlayer.Position {
    return CPacketPlayer.Position(this.posX + xOffset, this.posY + yOffset, this.posZ + zOffset, onGround)
}

fun Entity.toPacketPosition(vector: Vec3d, onGround: Boolean = this.onGround): CPacketPlayer.Position {
    return CPacketPlayer.Position(this.posX + vector.x, this.posY + vector.y, this.posZ + vector.z, onGround)
}