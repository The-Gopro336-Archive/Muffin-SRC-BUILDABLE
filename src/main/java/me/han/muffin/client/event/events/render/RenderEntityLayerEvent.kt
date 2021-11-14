package me.han.muffin.client.event.events.render

import me.han.muffin.client.event.EventCancellable
import net.minecraft.client.renderer.entity.layers.LayerRenderer
import net.minecraft.entity.EntityLivingBase

data class RenderEntityLayerEvent(var entity: EntityLivingBase, var layer: LayerRenderer<*>): EventCancellable()