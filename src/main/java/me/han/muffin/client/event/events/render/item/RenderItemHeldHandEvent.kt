package me.han.muffin.client.event.events.render.item

import me.han.muffin.client.event.EventCancellable
import net.minecraft.entity.Entity
import java.util.function.Function

class RenderItemHeldHandEvent(entity: Entity, function: Function<RenderItemHeldHandEvent, *>, noClue: Boolean): EventCancellable()