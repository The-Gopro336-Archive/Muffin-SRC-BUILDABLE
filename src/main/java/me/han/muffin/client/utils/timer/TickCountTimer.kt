package me.han.muffin.client.utils.timer

open class TickCountTimer {
    private var tick = 0

    fun update() {
        tick++
    }

    fun passed(passedTick: Int): Boolean {
        return tick >= passedTick
    }

    fun reset() {
        tick = 0
    }

}