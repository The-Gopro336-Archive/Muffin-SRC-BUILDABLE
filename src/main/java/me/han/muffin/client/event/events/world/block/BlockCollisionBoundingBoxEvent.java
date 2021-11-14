package me.han.muffin.client.event.events.world.block;

import me.han.muffin.client.event.EventCancellable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class BlockCollisionBoundingBoxEvent extends EventCancellable {

    private final BlockPos pos;
    private AxisAlignedBB boundingBox;

    public BlockCollisionBoundingBoxEvent(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }

    public AxisAlignedBB getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(AxisAlignedBB bb) {
        this.boundingBox = bb;
    }

}