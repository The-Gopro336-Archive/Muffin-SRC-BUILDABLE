package me.han.muffin.client.utils.extensions.mc.utils

import net.minecraft.util.math.BlockPos

operator fun BlockPos.component1() = this.x
operator fun BlockPos.component2() = this.y
operator fun BlockPos.component3() = this.z

fun BlockPos.toStringFormat() = "${this.x}, ${this.y}, ${this.z}"