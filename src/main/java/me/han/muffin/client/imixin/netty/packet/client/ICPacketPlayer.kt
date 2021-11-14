package me.han.muffin.client.imixin.netty.packet.client

interface ICPacketPlayer {
    fun setX(x: Double)
    fun setY(x: Double)
    fun setZ(x: Double)

    fun setYaw(yaw: Float)
    fun setPitch(pitch: Float)

    fun setOnGround(onGround: Boolean)

    var rotating: Boolean
    var moving: Boolean

}