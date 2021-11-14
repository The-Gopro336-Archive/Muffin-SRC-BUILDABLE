package me.han.muffin.client.imixin

import net.minecraft.util.Session
import net.minecraft.util.Timer

interface IMinecraft {
    val timer: Timer
    var rightClickDelayTimer: Int
    val renderPartialTicksPaused: Float

    fun setSession(session: Session)

}