package me.han.muffin.client.command

class Argument(val name: String?) {
    lateinit var value: String
    var present = false
}