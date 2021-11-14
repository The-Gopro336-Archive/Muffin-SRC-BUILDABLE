package me.han.muffin.client.event.events.render;

import me.han.muffin.client.event.EventStageable;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.ItemStack;

public class RenderItemModelEvent {
    private final EventStageable.EventStage stage;
    private final ItemStack stack;
    private final IBakedModel iBakedModel;

    public RenderItemModelEvent(EventStageable.EventStage stage, ItemStack stack, IBakedModel iBakedModel) {
        this.stage = stage;
        this.stack = stack;
        this.iBakedModel = iBakedModel;
    }

    public EventStageable.EventStage getStage() {
        return stage;
    }

    public ItemStack getStack() {
        return stack;
    }

    public IBakedModel getiBakedModel() {
        return iBakedModel;
    }

}