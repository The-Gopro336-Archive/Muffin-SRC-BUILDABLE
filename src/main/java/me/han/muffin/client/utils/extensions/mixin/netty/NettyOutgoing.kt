package me.han.muffin.client.utils.extensions.mixin.netty

import me.han.muffin.client.imixin.netty.packet.client.*
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.*
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand

var CPacketAnimation.packetHand: EnumHand
    get() = this.hand
    set(value) {
        (this as ICPacketAnimation).setHand(value)
    }

var CPacketChatMessage.packetMessage: String
    get() = this.message
    set(value) {
        (this as ICPacketChatMessage).setMessage(value)
    }

var CPacketCloseWindow.windowId: Int
    get() = (this as ICPacketCloseWindow).windowId
    set(value) {
        (this as ICPacketCloseWindow).windowId = value
    }

var CPacketCustomPayload.data: PacketBuffer
    get() = this.bufferData
    set(value) {
        (this as ICPacketCustomPayLoad).setData(value)
    }

var CPacketPlayer.x: Double
    get() = this.getX(0.0)
    set(value) {
        (this as ICPacketPlayer).setX(value)
    }

var CPacketPlayer.y: Double
    get() = this.getY(0.0)
    set(value) {
        (this as ICPacketPlayer).setY(value)
    }

var CPacketPlayer.z: Double
    get() = this.getZ(0.0)
    set(value) {
        (this as ICPacketPlayer).setZ(value)
    }

var CPacketPlayer.yaw: Float
    get() = this.getYaw(0.0F)
    set(value) {
        (this as ICPacketPlayer).setYaw(value)
    }

var CPacketPlayer.pitch: Float
    get() = this.getPitch(0.0F)
    set(value) {
        (this as ICPacketPlayer).setPitch(value)
    }

var CPacketPlayer.onGround: Boolean
    get() = this.isOnGround
    set(value) {
        (this as ICPacketPlayer).setOnGround(value)
    }

var CPacketPlayer.moving: Boolean
    get() = (this as ICPacketPlayer).moving
    set(value) {
        (this as ICPacketPlayer).moving = value
    }

var CPacketPlayer.rotating: Boolean
    get() = (this as ICPacketPlayer).rotating
    set(value) {
        (this as ICPacketPlayer).rotating = value
    }

var CPacketPlayerTryUseItemOnBlock.placedBlockDirection: EnumFacing
    get() = this.direction
    set(value) {
        (this as ICPacketPlayerTryUseItemOnBlock).placedBlockDirection = value
    }

var CPacketUseEntity.id: Int
    get() = (this as ICPacketUseEntity).entityID
    set(value) {
        (this as ICPacketUseEntity).entityID = value
    }

var CPacketUseEntity.packetAction: CPacketUseEntity.Action
    get() = this.action
    set(value) {
        (this as ICPacketUseEntity).setAction(value)
    }

var CPacketVehicleMove.packetX: Double
    get() = this.x
    set(value) {
        (this as ICPacketVehicleMove).setX(value)
    }

var CPacketVehicleMove.packetY: Double
    get() = this.y
    set(value) {
        (this as ICPacketVehicleMove).setY(value)
    }

var CPacketVehicleMove.packetZ: Double
    get() = this.z
    set(value) {
        (this as ICPacketVehicleMove).setZ(value)
    }

var CPacketVehicleMove.packetYaw: Float
    get() = this.yaw
    set(value) {
        (this as ICPacketVehicleMove).setYaw(value)
    }

var CPacketVehicleMove.packetPitch: Float
    get() = this.pitch
    set(value) {
        (this as ICPacketVehicleMove).setPitch(value)
    }