package me.han.muffin.client.event.events.client

import me.han.muffin.client.module.Module

class ModuleEvent(val type: Type, val module: Module) {
    enum class Type {
        Enable, Disable
    }
}