package me.han.muffin.client.event.events.render.entity;

import me.han.muffin.client.event.EventCancellable;
import net.minecraft.tileentity.TileEntityShulkerBox;

public class RenderTileEntityShulkerBoxEvent extends EventCancellable {
    private final EventStage stage;
    private final TileEntityShulkerBox shulkerBox;
    private final double x;
    private final double y;
    private final double z;
    private final float partialTicks;
    private final int destroyStage;
    private final float alpha;

    public RenderTileEntityShulkerBoxEvent(EventStage stage, TileEntityShulkerBox shulkerBox, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        this.stage = stage;
        this.shulkerBox = shulkerBox;
        this.x = x;
        this.y = y;
        this.z = z;
        this.partialTicks = partialTicks;
        this.destroyStage = destroyStage;
        this.alpha = alpha;
    }

    @Override
    public EventStage getStage() {
        return stage;
    }

    public TileEntityShulkerBox getShulkerBox() {
        return shulkerBox;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public int getDestroyStage() {
        return destroyStage;
    }

    public float getAlpha() {
        return alpha;
    }

}
