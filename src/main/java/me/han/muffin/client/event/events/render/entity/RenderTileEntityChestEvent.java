package me.han.muffin.client.event.events.render.entity;

import me.han.muffin.client.event.EventCancellable;
import net.minecraft.tileentity.TileEntityChest;

public class RenderTileEntityChestEvent extends EventCancellable {
    private final EventStage stage;
    private final TileEntityChest chest;
    private final double x;
    private final double y;
    private final double z;
    private final float partialTicks;
    private final int destroyStage;
    private final float alpha;

    public RenderTileEntityChestEvent(EventStage stage, TileEntityChest chest, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        this.stage = stage;
        this.chest = chest;
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

    public TileEntityChest getChest() {
        return chest;
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
