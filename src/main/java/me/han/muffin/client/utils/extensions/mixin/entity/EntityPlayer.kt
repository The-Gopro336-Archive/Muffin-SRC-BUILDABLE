package me.han.muffin.client.utils.extensions.mixin.entity

import me.han.muffin.client.imixin.entity.IEntityPlayer
import net.minecraft.entity.player.EntityPlayer

var EntityPlayer.speedInAir: Float
    get() = (this as IEntityPlayer).speedInAir
    set(value) {
        (this as IEntityPlayer).speedInAir = value
    }