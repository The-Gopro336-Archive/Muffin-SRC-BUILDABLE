package me.han.muffin.client.utils;

import me.han.muffin.client.core.Globals;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public class Location {

    private double x;
    private double y;
    private final double oldY;
    private double z;
    private boolean ground;
    private boolean moving;

    public Location(double x, double y, double z, boolean ground, boolean moving) {
        this.x = x;
        this.y = y;
        this.oldY = y;
        this.z = z;
        this.ground = ground;
        this.moving = moving;
    }

    public Location(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.oldY = y;
        this.z = z;
        this.ground = true;
        this.moving = false;
    }

    public Location(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.oldY = y;
        this.z = z;
        this.ground = true;
        this.moving = false;
    }

    public Location add(int x, int y, int z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Location add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Location subtract(int x, int y, int z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;

        return this;
    }

    public Location subtract(double x, double y, double z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;

        return this;
    }

    public Block getBlock() {
        return Globals.mc.world.getBlockState(this.toBlockPos()).getBlock();
    }

    public boolean isOnGround() {
        return this.ground;
    }

    public Location setOnGround(boolean ground) {
        this.ground = ground;
        return this;
    }

    public boolean isMoving() {
        return this.moving;
    }

    public Location setMoving(boolean moving) {
        this.moving = moving;
        return this;
    }

    public double getX() {
        return x;
    }

    public Location setX(double x) {
        this.x = x;
        return this;
    }

    public double getY() {
        return y;
    }

    public Location setY(double y) {
        this.y = y;
        return this;
    }

    public double getOldY() {
        return oldY;
    }

    public double getZ() {
        return z;
    }

    public Location setZ(double z) {
        this.z = z;
        return this;
    }

    public static Location fromBlockPos(BlockPos blockPos) {
        return new Location(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public BlockPos toBlockPos() {
        return new BlockPos(getX(), getY(), getZ());
    }

}
