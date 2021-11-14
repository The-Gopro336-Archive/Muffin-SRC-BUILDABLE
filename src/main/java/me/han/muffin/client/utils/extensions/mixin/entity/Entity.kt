package me.han.muffin.client.utils.extensions.mixin.entity

import me.han.muffin.client.imixin.entity.IEntity
import net.minecraft.entity.Entity

var Entity.isInWeb: Boolean
    get() = (this as IEntity).isIsInWeb
    set(value) {
        (this as IEntity).isIsInWeb = value
    }