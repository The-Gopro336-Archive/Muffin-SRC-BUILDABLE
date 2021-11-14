package me.han.muffin.client.event.events.render;

import me.han.muffin.client.event.EventCancellable;
import net.minecraft.util.BlockRenderLayer;

public class RenderBlockLayerEvent extends EventCancellable {
    private final EventStage stage;
    private final BlockRenderLayer renderLayer;
    private final double partialTicks;

    public RenderBlockLayerEvent(EventStage stage, BlockRenderLayer renderLayer, double partialTicks) {
        this.stage = stage;
        this.renderLayer = renderLayer;
        this.partialTicks = partialTicks;
    }

    @Override
    public EventStage getStage() {
        return stage;
    }

    public BlockRenderLayer getRenderLayer() {
        return renderLayer;
    }

    public double getPartialTicks() {
        return partialTicks;
    }

}