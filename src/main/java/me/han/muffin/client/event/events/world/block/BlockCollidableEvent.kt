package me.han.muffin.client.event.events.world.block

import me.han.muffin.client.event.EventCancellable
import net.minecraft.block.Block

data class BlockCollidableEvent(val block: Block): EventCancellable()