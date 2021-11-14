package me.han.muffin.client.event.events.world.block;


import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.EventCancellable;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public class PlayerBlockBreakEvent extends EventCancellable {

    private final Block block;
    private final BlockPos brokenPos;

    public PlayerBlockBreakEvent(BlockPos pos) {
        block = Globals.mc.world.getBlockState(pos).getBlock();
        this.brokenPos = pos;
    }

    public Block getBlock() {
        return block;
    }

    public BlockPos getBlockPos() {
        return brokenPos;
    }

}
