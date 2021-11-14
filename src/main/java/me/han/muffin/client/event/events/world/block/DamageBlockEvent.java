package me.han.muffin.client.event.events.world.block;


import me.han.muffin.client.event.EventCancellable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class DamageBlockEvent extends EventCancellable {

    private final BlockPos pos;
    private EnumFacing facing;

    public DamageBlockEvent(BlockPos pos, EnumFacing facing) {
        this.pos = pos;
        setDirection(facing);
    }

    public BlockPos getPos() {
        return pos;
    }

    public EnumFacing getDirection() {
        return facing;
    }


    public void setDirection(EnumFacing direction) {
        this.facing = direction;
    }

}