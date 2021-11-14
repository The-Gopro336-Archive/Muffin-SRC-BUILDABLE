package me.han.muffin.client.event.events.render.entity;

import me.han.muffin.client.event.EventCancellable;
import net.minecraft.tileentity.TileEntityEnderChest;

public class RenderTileEntityEnderChestEvent extends EventCancellable {
    private final EventStage stage;
    private final TileEntityEnderChest enderChest;
    private final double x;
    private final double y;
    private final double z;
    private final float partialTicks;
    private final int destroyStage;
    private final float alpha;

    public RenderTileEntityEnderChestEvent(EventStage stage, TileEntityEnderChest enderChest, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        this.stage = stage;
        this.enderChest = enderChest;
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

    public TileEntityEnderChest getEnderChest() {
        return enderChest;
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
