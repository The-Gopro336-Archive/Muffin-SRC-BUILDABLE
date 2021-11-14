package me.han.muffin.client.event.events.entity.living

import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.DamageSource

data class EntityDeathEvent(val entity: EntityLivingBase, val source: DamageSource)