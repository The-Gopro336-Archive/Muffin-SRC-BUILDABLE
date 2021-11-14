package me.han.muffin.client.utils.extensions.mixin.netty

import me.han.muffin.client.imixin.netty.packet.server.ISPacketCloseWindow
import me.han.muffin.client.imixin.netty.packet.server.ISPacketEntityVelocity
import me.han.muffin.client.imixin.netty.packet.server.ISPacketExplosion
import me.han.muffin.client.imixin.netty.packet.server.ISPacketPlayerPosLook
import net.minecraft.network.play.server.SPacketCloseWindow
import net.minecraft.network.play.server.SPacketEntityVelocity
import net.minecraft.network.play.server.SPacketExplosion
import net.minecraft.network.play.server.SPacketPlayerPosLook

var SPacketCloseWindow.windowId: Int
    get() = (this as ISPacketCloseWindow).windowId
    set(value) {
        (this as ISPacketCloseWindow).windowId = value
    }

var SPacketEntityVelocity.packetMotionX: Int
    get() = this.motionX
    set(value) {
        (this as ISPacketEntityVelocity).setMotionX(value)
    }

var SPacketEntityVelocity.packetMotionY: Int
    get() = this.motionY
    set(value) {
        (this as ISPacketEntityVelocity).setMotionY(value)
    }

var SPacketEntityVelocity.packetMotionZ: Int
    get() = this.motionZ
    set(value) {
        (this as ISPacketEntityVelocity).setMotionZ(value)
    }

var SPacketExplosion.packetMotionX: Float
    get() = this.motionX
    set(value) {
        (this as ISPacketExplosion).setMotionX(value)
    }

var SPacketExplosion.packetMotionY: Float
    get() = this.motionY
    set(value) {
        (this as ISPacketExplosion).setMotionY(value)
    }

var SPacketExplosion.packetMotionZ: Float
    get() = this.motionZ
    set(value) {
        (this as ISPacketExplosion).setMotionZ(value)
    }

var SPacketPlayerPosLook.posX: Double
    get() = this.x
    set(value) {
        (this as ISPacketPlayerPosLook).setX(value)
    }

var SPacketPlayerPosLook.posY: Double
    get() = this.y
    set(value) {
        (this as ISPacketPlayerPosLook).setY(value)
    }

var SPacketPlayerPosLook.posZ: Double
    get() = this.z
    set(value) {
        (this as ISPacketPlayerPosLook).setZ(value)
    }

var SPacketPlayerPosLook.rotationYaw: Float
    get() = this.yaw
    set(value) {
        (this as ISPacketPlayerPosLook).setYaw(value)
    }

var SPacketPlayerPosLook.rotationPitch: Float
    get() = this.pitch
    set(value) {
        (this as ISPacketPlayerPosLook).setPitch(value)
    }