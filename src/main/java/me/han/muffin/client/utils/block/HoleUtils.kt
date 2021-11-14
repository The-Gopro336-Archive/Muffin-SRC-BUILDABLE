package me.han.muffin.client.utils.block

import me.han.muffin.client.core.Globals
import me.han.muffin.client.manager.managers.HoleManager.isInHole
import me.han.muffin.client.utils.combat.CrystalUtils
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.entity.MovementUtils.speed
import me.han.muffin.client.utils.extensions.mc.block.*
import me.han.muffin.client.utils.math.VectorUtils.toBlockPos
import me.han.muffin.client.utils.math.VectorUtils.toVec3d
import net.minecraft.block.Block
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import kotlin.math.abs
import kotlin.math.round

object HoleUtils {
    val surroundOffset = arrayOf(
        BlockPos(0, -1, 0), // Down
        BlockPos(0, 0, -1), // North
        BlockPos(1, 0, 0),  // East
        BlockPos(0, 0, 1),  // South
        BlockPos(-1, 0, 0)  // West
    )

    val surroundTargets = arrayOf(Vec3d(0.0, 0.0, 0.0),
        Vec3d(1.0, 1.0, 0.0), Vec3d(0.0, 1.0, 1.0),
        Vec3d(-1.0, 1.0, 0.0), Vec3d(0.0, 1.0, -1.0),
        Vec3d(1.0, 0.0, 0.0), Vec3d(0.0, 0.0, 1.0),
        Vec3d(-1.0, 0.0, 0.0), Vec3d(0.0, 0.0, -1.0),
        Vec3d(1.0, 1.0, 0.0), Vec3d(0.0, 1.0, 1.0),
        Vec3d(-1.0, 1.0, 0.0), Vec3d(0.0, 1.0, -1.0))

    private val OFFSETS_2x2 = linkedSetOf(Vec3i(0, 0, 0), Vec3i(1, 0, 0), Vec3i(0, 0, 1), Vec3i(1, 0, 1))

    private val holeOffsetCheck1 = arrayOf(
        BlockPos(0, 0, 0),
        BlockPos(0, 1, 0)
    )

    private val holeOffset2X = arrayOf(
        BlockPos(0, 0, 0),
        BlockPos(1, 0, 0),
    )

    private val holeOffsetCheck2X = arrayOf(
        *holeOffset2X,
        BlockPos(0, 1, 0),
        BlockPos(1, 1, 0),
    )

    private val holeOffset2Z = arrayOf(
        BlockPos(0, 0, 0),
        BlockPos(0, 0, 1),
    )

    private val holeOffsetCheck2Z = arrayOf(
        *holeOffset2Z,
        BlockPos(0, 1, 0),
        BlockPos(0, 1, 1),
    )

    private val surroundOffset2X = arrayOf(
        BlockPos(0, -1, 0),
        BlockPos(1, -1, 0),
        BlockPos(-1, 0, 0),
        BlockPos(0, 0, -1),
        BlockPos(0, 0, 1),
        BlockPos(1, 0, -1),
        BlockPos(1, 0, 1),
        BlockPos(2, 0, 0)
    )

    private val surroundOffset2Z = arrayOf(
        BlockPos(0, -1, 0),
        BlockPos(0, -1, 1),
        BlockPos(0, 0, -1),
        BlockPos(-1, 0, 0),
        BlockPos(1, 0, 0),
        BlockPos(-1, 0, 1),
        BlockPos(1, 0, 1),
        BlockPos(0, 0, 2)
    )

    private val holeOffset4 = arrayOf(
        BlockPos(0, 0, 0),
        BlockPos(0, 0, 1),
        BlockPos(1, 0, 0),
        BlockPos(1, 0, 1)
    )

    private val holeOffsetCheck4 = arrayOf(
        *holeOffset4,
        BlockPos(0, 1, 0),
        BlockPos(0, 1, 1),
        BlockPos(1, 1, 0),
        BlockPos(1, 1, 1)
    )

    private val surroundOffset4 = arrayOf(
        BlockPos(0, -1, 0),
        BlockPos(0, -1, 1),
        BlockPos(1, -1, 0),
        BlockPos(1, -1, 1),
        BlockPos(-1, 0, 0),
        BlockPos(-1, 0, 1),
        BlockPos(0, 0, -1),
        BlockPos(1, 0, -1),
        BlockPos(0, 0, 2),
        BlockPos(1, 0, 2),
        BlockPos(2, 0, 0),
        BlockPos(2, 0, 1)
    )

    fun checkHoleTest(pos: BlockPos): HoleInfo {
        if (pos.y !in 1..255 || !pos.isAir || pos.down().isAir) return HoleInfo.empty(pos)
        return checkHole1(pos) ?: checkHole2(pos) ?: checkHole4(pos) ?: HoleInfo.empty(pos)
    }

    private fun checkHole1(pos: BlockPos): HoleInfo? {
        if (!checkAir(holeOffsetCheck1, pos)) return null
        return checkType(pos, HoleType.Bedrock, HoleType.Obsidian, surroundOffset, arrayOf(pos), AxisAlignedBB(pos), 0.5, 0.5)
    }

    private fun checkHole2(pos: BlockPos): HoleInfo? {
        var x = true

        if (!pos.add(1, 0, 0).isAir) {
            if (!pos.add(0, 0, 1).isAir) return null
            else x = false
        }

        val checkArray = if (x) holeOffsetCheck2X else holeOffsetCheck2Z
        if (!checkAir(checkArray, pos)) return null

        val surroundOffset = if (x) surroundOffset2X else surroundOffset2Z
        val array = if (x) holeOffset2X.offset(pos) else holeOffset2Z.offset(pos)
        val centerX = if (x) 1.0 else 0.5
        val centerZ = if (x) 0.5 else 1.0

        val boundingBox = if (x) {
            AxisAlignedBB(
                pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                pos.x + 2.0, pos.y + 1.0, pos.z + 1.0
            )
        } else {
            AxisAlignedBB(
                pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                pos.x + 1.0, pos.y + 1.0, pos.z + 2.0
            )
        }

        return checkType(pos, HoleType.Two, HoleType.Two, surroundOffset, array, boundingBox, centerX, centerZ)
    }

    private fun checkHole4(pos: BlockPos): HoleInfo? {
        if (!checkAir(holeOffsetCheck4, pos)) return null

        val array = holeOffset4.offset(pos)
        val first = array[1]
        val second = array[2]
        val third = array[3]

        if (!first.isAir || !first.isAir || !first.isAir || !second.isAir || !second.isAir || !third.isAir || !third.isAir) {
            return null
        }

        val boundingBox = AxisAlignedBB(
            pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
            pos.x + 2.0, pos.y + 1.0, pos.z + 2.0
        )

        return checkType(pos, HoleType.Four, HoleType.Four, surroundOffset4, array, boundingBox, 1.0, 1.0)
    }

    private fun checkType(
        pos: BlockPos,
        expectType: HoleType,
        obbyType: HoleType,
        surroundOffset: Array<BlockPos>,
        holePos: Array<BlockPos>,
        boundingBox: AxisAlignedBB, centerX: Double, centerZ: Double
    ): HoleInfo? {
        val surroundPos = surroundOffset.offset(pos)
        val type = checkSurroundPos(surroundPos, expectType, obbyType)

        return if (type == HoleType.None) {
            null
        } else {
            HoleInfo(pos, pos.toVec3d(centerX, 0.0, centerZ), boundingBox, holePos, surroundPos, type)
        }
    }

    private fun checkAir(array: Array<BlockPos>, pos: BlockPos) = array.all { pos.add(it).isAir }
    private fun Array<BlockPos>.offset(pos: BlockPos) = Array(this.size) { pos.add(this[it]) }

    private fun checkSurroundPos(array: Array<BlockPos>, expectType: HoleType, obbyType: HoleType): HoleType {
        var type = expectType

        for (pos in array) {
            val blockState = pos.state
            when {
                blockState.block == Blocks.BEDROCK -> continue
                blockState.block != Blocks.AIR && CrystalUtils.isResistant(blockState) -> type = obbyType
                else -> return HoleType.None
            }
        }

        return type
    }

    fun isBurrowed(entity: Entity): Boolean {
        val playerPos = BlockPos(entity.posX.getMiddlePosition(), entity.posY, entity.posZ.getMiddlePosition())
        return (playerPos.block == Blocks.OBSIDIAN || playerPos.block == Blocks.ENDER_CHEST || playerPos.block == Blocks.ANVIL) && playerPos.down().isFullBox
    }

    fun isPlayerInHole(player: EntityPlayer): Boolean {
        val floored = player.flooredPosition
        val state = floored.state

        if (state.block != Blocks.AIR || floored.up().block != Blocks.AIR || floored.down().block == Blocks.AIR) return false

        val touchingBlocks = arrayOf(floored.north(), floored.south(), floored.east(), floored.west())

        var validHorizontalBlocks = 0
        for (touching in touchingBlocks) {
            val touchingState = touching.state
            if (touchingState.block != Blocks.AIR && touchingState.isFullBlock) validHorizontalBlocks++
        }

        return validHorizontalBlocks >= 4
    }

    fun fullCheckInHole(player: EntityPlayer) = (player.isInHole || isBurrowed(player))

    private fun checkBlock(block: Block): Boolean {
        return block == Blocks.BEDROCK || block == Blocks.OBSIDIAN || block == Blocks.ENDER_CHEST || block == Blocks.ANVIL
    }

    fun EntityPlayerSP.centerPlayer(teleport: Boolean): Boolean {
        val center = Vec3d(round(this.posX + 0.5) - 0.5, this.posY, round(this.posZ + 0.5) - 0.5)
        val centered = isCentered(center.toBlockPos())

        if (!centered) {
            if (teleport) {
                val posX = Globals.mc.player.posX + (center.x - this.posX).coerceIn(-0.2, 0.2)
                val posZ = Globals.mc.player.posZ + (center.z - this.posZ).coerceIn(-0.2, 0.2)
                Globals.mc.player.setPosition(posX, Globals.mc.player.posY, posZ)
            } else {
                this.motionX = (center.x - this.posX) / 2.0
                this.motionZ = (center.z - this.posZ) / 2.0

                val speed = this.speed
                if (speed > 0.2805) {
                    val multiplier = 0.2805 / speed
                    this.motionX *= multiplier
                    this.motionZ *= multiplier
                }
            }

        }

        return centered
    }

    fun Entity.isCentered(pos: BlockPos) =
        this.isCentered(pos.toVec3d(0.5, 0.0, 0.5))
        // this.posX in pos.x + 0.31..pos.x + 0.69 && this.posZ in pos.z + 0.31..pos.z + 0.69

    fun Entity.isCentered(vec: Vec3d): Boolean =
        abs(this.posX - vec.x) < 0.2 && abs(this.posZ - vec.z) < 0.2


}