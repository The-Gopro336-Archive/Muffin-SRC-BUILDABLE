package me.han.muffin.client.utils.extensions.mc.world

import me.han.muffin.client.core.Globals
import me.han.muffin.client.manager.managers.LocalHotbarManager.serverSideItem
import me.han.muffin.client.module.modules.exploits.NoMineAnimationModule
import me.han.muffin.client.utils.extensions.mc.block.*
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.math.PlaceInfo
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
import me.han.muffin.client.utils.math.rotation.RotationUtils
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameType
import java.lang.Thread.sleep
import java.util.*
import java.util.function.BooleanSupplier

fun searchForNeighbour(
    pos: BlockPos,
    attempts: Int = 3, range: Float = 4.25f,
    visibleSideCheck: Boolean = false,
    sides: Array<EnumFacing> = EnumFacing.values()
) = searchNeighbour(Globals.mc.player.eyePosition, pos, attempts, range, visibleSideCheck, sides, hashSetOf())

private fun searchNeighbour(
    eyePos: Vec3d, pos: BlockPos,
    attempts: Int, range: Float,
    visibleSideCheck: Boolean,
    sides: Array<EnumFacing>, toIgnore: HashSet<Pair<BlockPos, EnumFacing>>): PlaceInfo?
{
    for (side in sides) {
        val result = checkNeighbour(eyePos, pos, side, range, visibleSideCheck, true, toIgnore)
        if (result != null) return result
    }

    if (attempts > 1) {
        for (side in sides) {
            val newPos = pos.offset(side)
            if (!newPos.isPlaceable()) continue
            return searchNeighbour(eyePos, newPos, attempts - 1, range, visibleSideCheck, sides, toIgnore) ?: continue
        }
    }

    return null
}

private fun checkNeighbour(
    eyePos: Vec3d, pos: BlockPos,
    side: EnumFacing, range: Float,
    visibleSideCheck: Boolean, checkReplaceable: Boolean,
    toIgnore: HashSet<Pair<BlockPos, EnumFacing>>?): PlaceInfo?
{
    val offsetPos = pos.offset(side)
    val oppositeSide = side.opposite

    if (toIgnore?.add(offsetPos to oppositeSide) == false) return null

    val hitVec = offsetPos.getHitVec(oppositeSide)
    val dist = eyePos.distanceTo(hitVec)

    if (dist > range) return null
    if (visibleSideCheck && !offsetPos.getVisibleSides(true).contains(oppositeSide)) return null
    if (checkReplaceable && offsetPos.state.isReplaceable) return null
    if (!pos.isPlaceable()) return null

    val hitVecOffset = oppositeSide.hitVecOffset
    return PlaceInfo(offsetPos, oppositeSide, dist, hitVecOffset, hitVec, pos)
}

val BlockPos.miningSide: EnumFacing? get() {
    val eyePos = Globals.mc.player.eyePosition

    return this.getVisibleSides()
        .filter { !this.offset(it).isFullBox }
        .minByOrNull { eyePos.distanceTo(this.getHitVec(it)) }
}

val BlockPos.closestVisibleSide: EnumFacing? get() {
    val eyePos = Globals.mc.player.eyePosition

    return this.getVisibleSides()
        .minByOrNull { eyePos.squareDistanceTo(this.getHitVec(it)) }
}

fun BlockPos.getClosestVisibleSideStrict(assumeAir: Boolean = false): EnumFacing? {
    val eyePos = Globals.mc.player.eyePosition

    return this.getVisibleSidesStrict(assumeAir)
        .minByOrNull { eyePos.squareDistanceTo(this.getHitVec(it)) }
}

fun BlockPos.getVisibleSides(assumeAirAsFullBox: Boolean = false): Set<EnumFacing>  {
    val visibleSides = EnumSet.noneOf(EnumFacing::class.java)

    val blockCenter = this.toVec3dCenter()
    val isFullBox = assumeAirAsFullBox && this.isAir || this.state.isFullBox
    val eyesPos = Globals.mc.player.eyePosition

    return visibleSides
        .checkAxis(eyesPos.x - blockCenter.x, EnumFacing.WEST, EnumFacing.EAST, !isFullBox)
        .checkAxis(eyesPos.y - blockCenter.y, EnumFacing.DOWN, EnumFacing.UP, true)
        .checkAxis(eyesPos.z - blockCenter.z, EnumFacing.NORTH, EnumFacing.SOUTH, !isFullBox)
}

fun BlockPos.getVisibleSidesStrict(assumeAirAsFullBox: Boolean = false): List<EnumFacing>  {
    return this.getVisibleSides(assumeAirAsFullBox)
        .filter { !this.offset(it).isFullBox }
}

private fun EnumSet<EnumFacing>.checkAxis(diff: Double, negativeSide: EnumFacing, positiveSide: EnumFacing, bothIfInRange: Boolean) =
    this.apply {
        when {
            diff < -0.5 -> {
                add(negativeSide)
            }
            diff > 0.5 -> {
                add(positiveSide)
            }
            else -> {
                if (bothIfInRange) {
                    add(negativeSide)
                    add(positiveSide)
                }
            }
        }
    }


fun buildStructure(
    center: BlockPos,
    structureOffset: Array<BlockPos>,
    placeSpeed: Float, attempts: Int, range: Float,
    visibleSideCheck: Boolean, packetRotate: Boolean, swingArm: Boolean,
    block: BooleanSupplier
) {
    val emptySet = emptySet<BlockPos>()
    val placed = HashSet<BlockPos>()

    var placeCount = 0
    var lastInfo = getStructurePlaceInfo(center, structureOffset, emptySet, attempts, range, visibleSideCheck)

    while (lastInfo != null) {
        if (!block.asBoolean) return

        val placingInfo = getStructurePlaceInfo(center, structureOffset, placed, attempts, range, visibleSideCheck) ?: lastInfo

        placeCount++
        placed.add(placingInfo.placedPos)

        doPlace(placingInfo, placeSpeed, packetRotate, swingArm)

        if (placeCount >= 4) {
            sleep(100L)
            placeCount = 0
            placed.clear()
        }

        lastInfo = getStructurePlaceInfo(center, structureOffset, emptySet, attempts, range, visibleSideCheck)
    }
}

private fun getStructurePlaceInfo(
    center: BlockPos,
    structureOffset: Array<BlockPos>,
    toIgnore: Set<BlockPos>,
    attempts: Int, range: Float,
    visibleSideCheck: Boolean
): PlaceInfo? {
    for (offset in structureOffset) {
        val pos = center.add(offset)
        if (toIgnore.contains(pos)) continue
        if (!pos.isPlaceable()) continue
        return searchForNeighbour(pos, attempts, range, visibleSideCheck) ?: continue
    }
    if (attempts > 1) return getStructurePlaceInfo(center, structureOffset, toIgnore, attempts - 1, range, visibleSideCheck)
    return null
}

private fun doPlace(placeInfo: PlaceInfo, placeSpeed: Float, rotate: Boolean, swingArm: Boolean) {
    val rotation = RotationUtils.getRotationTo(placeInfo.hitVec)
    val rotationPacket = CPacketPlayer.PositionRotation(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ, rotation.x, rotation.y, Globals.mc.player.onGround)
    val placePacket = placeInfo.toPlacePacket(EnumHand.MAIN_HAND)

    if (rotate) {
        Globals.mc.player.connection.sendPacket(rotationPacket)
        sleep((40.0F / placeSpeed).toLong())
    }

    Globals.mc.player.connection.sendPacket(placePacket)
    if (swingArm) Globals.mc.player.swingArm(EnumHand.MAIN_HAND) else Globals.mc.player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
    sleep((10.0F / placeSpeed).toLong())
}

fun placeBlock(pos: BlockPos, side: EnumFacing, hand: EnumHand = EnumHand.MAIN_HAND, packet: Boolean = false, packetRotate: Boolean, swingArm: Boolean = false, noGhost: Boolean = true) {
    val eyesPos = Globals.mc.player.eyePosition

    val hitVec = pos.getHitVec(side)
    val hitVecOffset = side.hitVecOffset
    val dist = eyesPos.distanceTo(hitVec)
    val placedPos = pos.offset(side)

    placeBlock(PlaceInfo(pos, side, dist, hitVecOffset, hitVec, placedPos), hand, packet, packetRotate, swingArm, noGhost)
}

fun placeBlock(placeInfo: PlaceInfo, hand: EnumHand = EnumHand.MAIN_HAND, packet: Boolean = false, packetRotate: Boolean, swingArm: Boolean = false, noGhost: Boolean = true) {
    if (!placeInfo.placedPos.isPlaceable()) return

    if (packetRotate) RotationUtils.faceVectorWithPositionPacket(placeInfo.hitVec)

    if (packet) {
        Globals.mc.player.connection.sendPacket(placeInfo.toPlacePacket(hand))
        if (swingArm) Globals.mc.player.swingArm(hand) else Globals.mc.player.connection.sendPacket(CPacketAnimation(hand))

        val itemStack = Globals.mc.player.serverSideItem
        val block = (itemStack.item as? ItemBlock?)?.block ?: return
        val metaData = itemStack.metadata
        val blockState = block.getStateForPlacement(Globals.mc.world, placeInfo.pos, placeInfo.side, placeInfo.hitVecOffset.x.toFloat(), placeInfo.hitVecOffset.y.toFloat(), placeInfo.hitVecOffset.z.toFloat(), metaData, Globals.mc.player, EnumHand.MAIN_HAND)
        val soundType = blockState.block.getSoundType(blockState, Globals.mc.world, placeInfo.pos, Globals.mc.player)
        Globals.mc.world.playSound(Globals.mc.player, placeInfo.pos, soundType.placeSound, SoundCategory.BLOCKS, (soundType.getVolume() + 1.0f) / 2.0f, soundType.getPitch() * 0.8f)
    } else {
        Globals.mc.playerController.processRightClickBlock(Globals.mc.player, Globals.mc.world, placeInfo.pos, placeInfo.side, placeInfo.hitVec, hand)
        if (swingArm) Globals.mc.player.swingArm(hand) else Globals.mc.player.connection.sendPacket(CPacketAnimation(hand))
    }

    if (noGhost && Globals.mc.playerController.currentGameType != GameType.CREATIVE)
        Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, placeInfo.pos, placeInfo.side))

    if (NoMineAnimationModule.isEnabled) NoMineAnimationModule.resetMining()
}


private fun PlaceInfo.toPlacePacket(hand: EnumHand) =
    CPacketPlayerTryUseItemOnBlock(this.pos, this.side, hand, this.hitVecOffset.x.toFloat(), this.hitVecOffset.y.toFloat(), this.hitVecOffset.z.toFloat())