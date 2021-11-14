package me.han.muffin.client.event.events.world

import net.minecraft.world.chunk.Chunk

data class ChunkEvent(val type: ChunkType, val chunk: Chunk) {
    enum class ChunkType {
        LOAD, UNLOAD
    }
}