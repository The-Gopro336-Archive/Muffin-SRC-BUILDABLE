package me.han.muffin.client.utils.timer

open class Timer {
    private var time = -1L

    fun passed(ms: Double): Boolean {
        return System.currentTimeMillis() >= ms + time
    }

    fun passedTicks(ticks: Int): Boolean {
        return passed((ticks * 50).toDouble())
    }

    fun passedSeconds(seconds: Int): Boolean {
        return passed((seconds * 1000).toDouble())
    }

    fun passedAPS(clicks: Int): Boolean {
        return passed((1000L / clicks).toDouble())
    }

    fun reset() {
        time = System.currentTimeMillis()
    }

    fun sleep(time: Long): Boolean {
        if (currentTime() >= time) {
            reset()
            return true
        }
        return false
    }

    fun resetTimeSkipTo(ms: Long) {
        time = System.currentTimeMillis() + ms
    }

    fun currentTime(): Long {
        return System.currentTimeMillis() - time
    }

    fun getTime(): Long {
        return time
    }

    fun setTime(time: Long) {
        this.time = time
    }

}