package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.world.OnItemPlaceEvent
import me.han.muffin.client.event.events.world.block.OnBlockRemovedByPlayerEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.value.Value
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object NoGlitchBlocksModule: Module("NoGlitchBlocks", Category.PLAYER, "Make sure no ghost block.") {
    private val place = Value(true, "Place")
    private val destroy = Value(true, "Destroy")

    init {
        addSettings(place, destroy)
    }

    @Listener
    private fun onItemPlace(event: OnItemPlaceEvent) {
         if (place.value && Globals.mc.player.onGround) event.cancel()
    }

    @Listener
    private fun onPlayerDestroy(event: OnBlockRemovedByPlayerEvent) {
        if (destroy.value) event.cancel()
    }


}