package me.han.muffin.client.utils.math

import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

data class PlaceInfo(val pos: BlockPos, val side: EnumFacing, val dist: Double, val hitVecOffset: Vec3d, val hitVec: Vec3d, val placedPos: BlockPos) {

//    constructor(pos: BlockPos, side: EnumFacing, placedPos: BlockPos) : this() {
//        val eyesPos = Globals.mc.player.getPositionEyes(1.0F)
//        val hitVec = pos.getHitVec(side)
//        val hitVecOffset = side.hitVecOffset
//
//        this(pos, side, eyesPos.distanceTo(hitVec), hitVecOffset, hitVec, placedPos)
//    }

    override fun toString(): String {
        return "pos = $pos, side = $side, placedPos = $placedPos"
    }

}