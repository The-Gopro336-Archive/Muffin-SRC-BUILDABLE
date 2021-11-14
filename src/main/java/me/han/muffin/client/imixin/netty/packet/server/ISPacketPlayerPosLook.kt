package me.han.muffin.client.imixin.netty.packet.server

interface ISPacketPlayerPosLook {

    fun setX(x: Double)
    fun setY(y: Double)
    fun setZ(z: Double)

    fun setYaw(yaw: Float)
    fun setPitch(pitch: Float)

}