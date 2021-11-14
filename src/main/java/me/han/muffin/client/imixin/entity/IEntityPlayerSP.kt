package me.han.muffin.client.imixin.entity

import me.han.muffin.client.event.events.client.MotionUpdateEvent

interface IEntityPlayerSP {
    val lastMotionEvent: MotionUpdateEvent

    var isPrevOnGround: Boolean

    var lastReportedPosX: Double
    var lastReportedPosY: Double
    var lastReportedPosZ: Double

    val lastReportedYaw: Float
    val lastReportedPitch: Float

}