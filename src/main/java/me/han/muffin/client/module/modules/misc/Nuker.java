package me.han.muffin.client.module.modules.misc;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.client.MotionUpdateEvent;
import me.han.muffin.client.event.events.world.block.ClickBlockEvent;
import me.han.muffin.client.manager.managers.BlockManager;
import me.han.muffin.client.module.Module;
import me.han.muffin.client.utils.entity.PlayerUtil;
import me.han.muffin.client.utils.math.VectorUtils;
import me.han.muffin.client.utils.math.rotation.RotationUtils;
import me.han.muffin.client.utils.math.rotation.Vec2f;
import me.han.muffin.client.value.EnumValue;
import me.han.muffin.client.value.NumberValue;
import me.han.muffin.client.value.Value;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener;

//TODO: REWRITE THIS
public class Nuker extends Module {

    public final EnumValue<Modes> Mode = new EnumValue<>(Modes.Survival, "Mode");
    public final Value<Boolean> ClickSelect = new Value<>(false, "Click Select");
    public final Value<Boolean> Flatten = new Value<>(false, "Flatten");
    public final Value<Boolean> Rotates = new Value<>(true, "Rotates");
    public final Value<Boolean> Raytrace = new Value<>(true, "Raytrace");
    public final NumberValue<Float> Range = new NumberValue<>(3.0f, 0.0f, 10.0f, 1.0f, "Range");

    public static Nuker INSTANCE;

    public enum Modes {
        Survival,
        Creative,
    }

    public Nuker() {
        super("Nuker", Category.MISC, "Survival nuker to mine out blocks.");
        INSTANCE = this;
        addSettings(Mode, ClickSelect, Flatten, Rotates, Raytrace, Range);
    }

    private Block _clickSelectBlock = null;

    @Override
    public void onEnable() {
        _clickSelectBlock = null;
    }

    @Listener
    private void onClickBlock(ClickBlockEvent event) {
        IBlockState state = Globals.mc.world.getBlockState(event.getPos());
        if (state.getBlock() == Blocks.AIR) return;
        _clickSelectBlock = state.getBlock();
    }

    @Listener
    private void onMotionUpdate(MotionUpdateEvent event) {
        if (event.getStage().equals(EventStageable.EventStage.PRE)) {

            if (ClickSelect.getValue()) {
                if (_clickSelectBlock == null) return;
            }

            BlockPos selectedBlock = null;

            if (BlockManager.GetCurrBlock() != null) {

                // calculate rotations to the block
                final Vec2f rotations = RotationUtils.INSTANCE.getRotationTo(new Vec3d(BlockManager.GetCurrBlock()).add(0.5, - 0.5, 0.5));

                // send packets to face this serverside
                event.getRotation().setX(rotations.getX());
                event.getRotation().setY(rotations.getY());

                // update our breaking animations / sync
                BlockManager.Update(Range.getValue(), Raytrace.getValue());
                return;
            }

            final float range = Range.getValue();

            for (BlockPos pos : VectorUtils.INSTANCE.getBlockPosInSphere(Globals.mc.player.getPositionEyes(1F), range)) {
                if (Flatten.getValue() && pos.getY() < PlayerUtil.getPlayerPosFloored(Globals.mc.player).getY()) continue;

                IBlockState state = Globals.mc.world.getBlockState(pos);

                if (ClickSelect.getValue()) {
                    if (_clickSelectBlock != null) {
                        if (state.getBlock() != _clickSelectBlock)
                            continue;
                    }
                }

                if (Mode.getValue() == Modes.Creative) {
                    Globals.mc.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, EnumFacing.UP));
                    continue;
                }

                if (state.getBlock() == Blocks.BEDROCK || state.getBlock() == Blocks.AIR || state.getBlock() == Blocks.WATER)
                    continue;

                if (selectedBlock == null) {
                    selectedBlock = pos;
                    continue;
                } else {
                    double dist = pos.getDistance((int) Globals.mc.player.posX, (int) Globals.mc.player.posY, (int) Globals.mc.player.posZ);

                    if (selectedBlock.getDistance((int) Globals.mc.player.posX, (int) Globals.mc.player.posY, (int) Globals.mc.player.posZ) < dist)
                        continue;

                    if (dist <= Range.getValue())
                        selectedBlock = pos;
                }
            }

            if (selectedBlock == null)
                return;

            if (Mode.getValue() != Modes.Creative) BlockManager.SetCurrentBlock(selectedBlock);
        }
    }

}