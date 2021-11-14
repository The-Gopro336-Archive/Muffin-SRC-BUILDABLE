package me.han.muffin.client.utils.threading

import me.han.muffin.client.core.Globals

inline fun onMainThread(noinline block: () -> Unit) {
    Globals.mc.addScheduledTask(block)
}