package me.han.muffin.client.event.events.world.block;

import net.minecraft.block.Block;
import net.minecraft.util.BlockRenderLayer;

public class CanRenderInLayerEvent {
    private BlockRenderLayer blockRenderLayer;
    private final Block block;

    public CanRenderInLayerEvent(final Block block) {
        this.blockRenderLayer = null;
        this.block = block;
    }

    public BlockRenderLayer getBlockRenderLayer() {
        return blockRenderLayer;
    }

    public void setBlockRenderLayer(BlockRenderLayer blockRenderLayer) {
        this.blockRenderLayer = blockRenderLayer;
    }

    public Block getBlock() {
        return block;
    }

}
