package me.han.muffin.client.manager.managers

import me.han.muffin.client.core.Globals
import me.han.muffin.client.utils.extensions.mixin.misc.tickLength
import me.han.muffin.client.utils.extensions.mixin.misc.timer

object TimerManager {
    @JvmStatic var timerSpeed = 1.0F; private set

    fun setTimer(speed: Float) {
        timerSpeed = speed
        //((Globals.mc as IMinecraft).timer as ITimer).tickLength = 50.0F / timerSpeed
    }

    fun setTimerRaw(speed: Float) {
        timerSpeed = 50.0F / speed
        // ((Globals.mc as IMinecraft).timer as ITimer).tickLength = speed
    }

    fun resetTimer() {
        timerSpeed = 1.0F
        Globals.mc.timer.tickLength = 50.0F
    }

}