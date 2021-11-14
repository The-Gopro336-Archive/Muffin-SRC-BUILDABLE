package me.han.muffin.client.utils.block

import me.han.muffin.client.core.Globals
import me.han.muffin.client.module.modules.exploits.NoMineAnimationModule
import me.han.muffin.client.utils.extensions.mc.block.*
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.world.searchForNeighbour
import me.han.muffin.client.utils.extensions.mixin.misc.rightClickDelayTimer
import me.han.muffin.client.utils.math.RandomUtils
import me.han.muffin.client.utils.math.rotation.RotationUtils
import net.minecraft.block.BlockSlab
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.function.BiFunction
import kotlin.math.floor
import kotlin.math.max

object BlockUtil {
/*
    fun getVisibleSides(pos: BlockPos): Set<EnumFacing> {
        val visibleSides = EnumSet.noneOf(EnumFacing::class.java)

        val isFullBox = pos.state.isFullBox

        val flooredPosition = Globals.mc.player.flooredPosition
        val eyesPos = (Globals.mc.player.posY + Globals.mc.player.eyeHeight).floorToInt()

        val xDiff = flooredPosition.x - pos.x
        val yDiff = eyesPos - pos.y
        val zDiff = flooredPosition.z - pos.z

        if (!isFullBox) {
            if (xDiff == 0) {
                visibleSides.add(EnumFacing.SOUTH)
                visibleSides.add(EnumFacing.WEST)
            }
            if (zDiff == 0) {
                visibleSides.add(EnumFacing.SOUTH)
                visibleSides.add(EnumFacing.NORTH)
            }
        }

        if (yDiff == 0) {
            visibleSides.add(EnumFacing.UP)
            visibleSides.add(EnumFacing.DOWN)
        } else {
            visibleSides.add(if (yDiff > 0.0) EnumFacing.UP else EnumFacing.DOWN)
        }

        if (xDiff != 0) visibleSides.add(if (xDiff > 0) EnumFacing.EAST else EnumFacing.WEST)
        if (zDiff != 0) visibleSides.add(if (zDiff > 0) EnumFacing.SOUTH else EnumFacing.NORTH)

        return visibleSides
    }
 */

    fun calcStepSize(range: Double, searchAccuracy: Double): Double {
        var accuracy = searchAccuracy
        accuracy += accuracy % 2 // If it is set to uneven it changes it to even. Fixes a bug
        return max(range / accuracy, 0.01)
    }

    fun getCenter(defaultVector: Vec3d): Vec3d {
        val x = floor(defaultVector.x) + 0.5
        val y = floor(defaultVector.y)
        val z = floor(defaultVector.z) + 0.5
        return Vec3d(x, y, z)
    }

    fun isVoidHole(pos: BlockPos): Boolean {
        if (pos.y > 4 || pos.y <= 0) return false
        var blockPos = pos
        for (i in pos.y downTo 0) {
            if (blockPos.block != Blocks.AIR) return false
            blockPos = blockPos.down()
        }
        return true
    }

    fun calculateFaceForPlacement(structurePosition: BlockPos, blockPosition: BlockPos): EnumFacing? {
        val throwingClamp = BiFunction<Int, String, Int> { number: Int, axis: String? ->
            require(!(number < -1 || number > 1)) { String.format("Difference in %s is illegal, " + "positions are too far apart.", axis) }
            number
        }

        val diffX = throwingClamp.apply(structurePosition.x - blockPosition.x, "x-axis")

        when (diffX) {
            1 -> return EnumFacing.WEST
            -1 -> return EnumFacing.EAST
            else -> {
            }
        }

        val diffY = throwingClamp.apply(structurePosition.y - blockPosition.y, "y-axis")

        when (diffY) {
            1 -> return EnumFacing.DOWN
            -1 -> return EnumFacing.UP
            else -> {
            }
        }

        val diffZ = throwingClamp.apply(structurePosition.z - blockPosition.z, "z-axis")
        when (diffZ) {
            1 -> return EnumFacing.NORTH
            -1 -> return EnumFacing.SOUTH
            else -> {
            }
        }
        return null
    }

    fun isCollidingWithTop() =
        Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(0.0, 0.21, 0.0)).size > 0

    @JvmStatic
    fun valid(pos: BlockPos): ValidResult {
        if (!pos.canPlaceNoCollide) return ValidResult.NoEntityCollision
        if (searchForNeighbour(pos, 4) == null) return ValidResult.NoNeighbours

        if (pos.isPlaceable()) {
            val blocks = arrayOf(pos.north(), pos.south(), pos.east(), pos.west(), pos.up(), pos.down())
            for (blockPos in blocks) {
                if (!blockPos.isAir && EnumFacing.values().any { pos.offset(it).canBeClicked }) return ValidResult.Ok
            }
            return ValidResult.NoNeighbours
        }

        return ValidResult.AlreadyBlockThere
    }

    @JvmStatic
    fun place(pos: BlockPos, distance: Double, rotate: Boolean, slab: Boolean, swingArm: Boolean, ignoreSelfNeighbour: Boolean = false): PlaceResult {
        val blockState = pos.state

        val eyesPos = Globals.mc.player.eyePosition
        val replaceable = blockState.isReplaceable
        val isSlab = blockState.block is BlockSlab

        if (!replaceable && !isSlab) return PlaceResult.NotReplaceable

        if (!ignoreSelfNeighbour && searchForNeighbour(pos, 1, range = distance.toFloat()) == null) return PlaceResult.NoNeighbours

        if (!isSlab && valid(pos) != ValidResult.Ok && !replaceable) return PlaceResult.CantPlace
        if (slab && isSlab && !blockState.isFullCube) return PlaceResult.CantPlace

        for (side in EnumFacing.values()) {
            val offsetPos = pos.offset(side)
            val opposite = side.opposite

            if (!offsetPos.canBeClicked) continue

            val hitVec = offsetPos.getHitVec(opposite)

            if (eyesPos.distanceTo(hitVec) > distance) continue

            val neighbourPos = offsetPos.block

            val shouldSneak =
                neighbourPos.onBlockActivated(Globals.mc.world, pos, pos.state, Globals.mc.player, EnumHand.MAIN_HAND, side, 0.0F, 0.0F, 0.0F) ||
                    rightClickableBlock.contains(neighbourPos) || offsetPos.needTileSneak

            //val goodFacings = getVisibleSides(offsetPos)
            //goodFacings.forEach { ChatManager.sendMessage("${goodFacings.size} ${it.name}") }

            if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))
            if (rotate) RotationUtils.faceVectorWithPositionPacket(hitVec)

            val result = Globals.mc.playerController.processRightClickBlock(Globals.mc.player, Globals.mc.world, offsetPos, opposite, hitVec, EnumHand.MAIN_HAND)

            if (result != EnumActionResult.FAIL) {
                if (swingArm) Globals.mc.player.swingArm(EnumHand.MAIN_HAND) else Globals.mc.player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))

                Globals.mc.rightClickDelayTimer = 4

                if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
                if (NoMineAnimationModule.isEnabled) NoMineAnimationModule.resetMining()

                return PlaceResult.Placed
            }

        }

        return PlaceResult.CantPlace
    }

    fun getRandomHitVec(pos: BlockPos, face: EnumFacing): Vec3d {
        var x = pos.x + 0.5
        var y = pos.y + 0.5
        var z = pos.z + 0.5
        x += face.xOffset / 2.0
        z += face.zOffset / 2.0
        y += face.yOffset / 2.0
        if (face != EnumFacing.UP && face != EnumFacing.DOWN) {
            y += RandomUtils.nextDouble(0.49, 0.5)
        } else {
            x += RandomUtils.nextDouble(0.3, -0.3)
            z += RandomUtils.nextDouble(0.3, -0.3)
        }
        if (face == EnumFacing.WEST || face == EnumFacing.EAST) {
            z += RandomUtils.nextDouble(0.3, -0.3)
        }
        if (face == EnumFacing.SOUTH || face == EnumFacing.NORTH) {
            x += RandomUtils.nextDouble(0.3, -0.3)
        }
        return Vec3d(x, y, z)
    }

    enum class ValidResult {
        NoEntityCollision, AlreadyBlockThere, NoNeighbours, Ok
    }

    enum class PlaceResult {
        NotReplaceable, NoNeighbours, CantPlace, Placed
    }

}