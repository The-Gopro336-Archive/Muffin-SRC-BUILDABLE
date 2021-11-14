package me.han.muffin.client.event.events.world.block;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.EventCancellable;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.math.BlockPos;

public class DestroyBlockEvent extends EventCancellable {

    private final PlayerControllerMP controller;
    private final Block block;
    private BlockPos pos;

    public DestroyBlockEvent(PlayerControllerMP controller, BlockPos pos) {
        this.controller = controller;
        this.pos = pos;
        block = Globals.mc.world.getBlockState(pos).getBlock();
    }

    public PlayerControllerMP getController() {
        return controller;
    }

    public Block getBlock() {
        return block;
    }

    public BlockPos getPos() {
        return pos;
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }
}
