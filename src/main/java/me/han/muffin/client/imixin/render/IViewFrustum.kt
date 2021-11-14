package me.han.muffin.client.imixin.render

import net.minecraft.client.renderer.chunk.RenderChunk
import net.minecraft.util.math.BlockPos

interface IViewFrustum {
    fun getRenderChunkVoid(pos: BlockPos): RenderChunk
}