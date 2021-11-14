package me.han.muffin.client.preset

abstract class Preset(val name: String) {
    abstract fun onSet()
}