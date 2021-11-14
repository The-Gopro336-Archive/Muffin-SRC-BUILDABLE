package me.han.muffin.client.utils

import me.han.muffin.client.utils.timer.TickTimer
import me.han.muffin.client.utils.timer.TimeUnit
import kotlin.concurrent.thread
import kotlin.reflect.KProperty

open class CachedValue<T>(
    protected val updateTime: Long,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    protected val block: () -> T
) {
    protected var value: T? = null
    protected val timer = TickTimer(timeUnit)

    open fun get(): T {
        val cached = value

        return if (cached == null || timer.tick(updateTime)) {
            block().also { value = it }
        } else {
            cached
        }
    }

    fun update() {
        timer.reset(-updateTime * timer.timeUnit.multiplier)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = get()

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

class AsyncCachedValue<T>(
    updateTime: Long,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    block: () -> T
): CachedValue<T>(updateTime, timeUnit, block) {
    override fun get(): T {
        val cached = value

        return when {
            cached == null -> {
                block().also { value = it }
            }
            timer.tick(updateTime) -> {
                thread {
                    value = block()
                }
                cached
            }
            else -> {
                cached
            }
        }
    }
}