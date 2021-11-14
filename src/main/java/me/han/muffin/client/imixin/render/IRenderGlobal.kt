package me.han.muffin.client.imixin.render

import net.minecraft.client.renderer.DestroyBlockProgress

interface IRenderGlobal {
    val damagedBlocks: Map<Int, DestroyBlockProgress>
}