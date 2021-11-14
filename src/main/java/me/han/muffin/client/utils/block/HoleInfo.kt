package me.han.muffin.client.utils.block

import me.han.muffin.client.utils.math.VectorUtils.toVec3d
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

class HoleInfo(
    val origin: BlockPos,
    val center: Vec3d,
    val boundingBox: AxisAlignedBB,
    val holePos: Array<BlockPos>,
    val surroundPos: Array<BlockPos>,
    val type: HoleType
) {
    val isHole = type != HoleType.None
    val isSafe = type != HoleType.Bedrock

    val isOne = type == HoleType.Bedrock || type == HoleType.Obsidian
    val isTwo = type != HoleType.Two
    val isFour = type != HoleType.Four

    override fun equals(other: Any?): Boolean =
        this === other || other is HoleInfo && origin == other.origin

    override fun hashCode(): Int = origin.hashCode()

    companion object {
        private val emptyBlockPosArray = emptyArray<BlockPos>()

        fun empty(pos: BlockPos) = HoleInfo(
            pos,
            pos.toVec3d(0.5, 0.0, 0.5),
            AxisAlignedBB(pos),
            emptyBlockPosArray,
            emptyBlockPosArray,
            HoleType.None
        )
    }

}