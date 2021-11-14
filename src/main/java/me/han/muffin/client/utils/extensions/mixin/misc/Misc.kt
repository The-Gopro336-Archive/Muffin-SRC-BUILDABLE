package me.han.muffin.client.utils.extensions.mixin.misc

import me.han.muffin.client.imixin.IItemStack
import me.han.muffin.client.imixin.IItemTool
import me.han.muffin.client.imixin.IMinecraft
import me.han.muffin.client.imixin.ITimer
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemTool
import net.minecraft.util.Session
import net.minecraft.util.Timer

var ItemStack.stackSize: Int
    get() = (this as IItemStack).stackSize
    set(value) {
        (this as IItemStack).stackSize = value
    }

val ItemTool.attackDamage: Float
    get() = (this as IItemTool).attackDamage

val Minecraft.timer: Timer
    get() = (this as IMinecraft).timer

var Minecraft.rightClickDelayTimer: Int
    get() = (this as IMinecraft).rightClickDelayTimer
    set(value) {
        (this as IMinecraft).rightClickDelayTimer = value
    }

val Minecraft.renderPartialTicksPaused: Float
    get() = (this as IMinecraft).renderPartialTicksPaused

var Minecraft.mcSession: Session
    get() = this.session
    set(value) {
        (this as IMinecraft).setSession(value)
    }

var Timer.tickLength: Float
    get() = (this as ITimer).tickLength
    set(value) {
        (this as ITimer).tickLength = value
    }

val Timer.lastSyncSysClock: Long
    get() = (this as ITimer).lastSyncSysClock