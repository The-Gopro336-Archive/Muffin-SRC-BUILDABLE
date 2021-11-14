package me.han.muffin.client.utils.extensions.mc.block

import me.han.muffin.client.core.Globals
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.utils.extensions.mc.world.getVisibleSides
import me.han.muffin.client.utils.extensions.mc.world.isVisible
import me.han.muffin.client.utils.extensions.mixin.render.damagedBlocks
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
import net.minecraft.block.*
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.item.EntityXPOrb
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.round

val BlockPos.state: IBlockState get() = Globals.mc.world.getBlockState(this)
val BlockPos.material: Material get() = this.state.material
val BlockPos.block: Block get() = this.state.block

val BlockPos.isAir: Boolean get() = Globals.mc.world.isAirBlock(this)
val BlockPos.isLiquid: Boolean get() = this.state.isLiquid
val BlockPos.canBeClicked: Boolean get() = this.block.canCollideCheck(this.state, false) && Globals.mc.world.worldBorder.contains(this)

//val BlockPos.isReplaceable: Boolean get() = this.state.isReplaceable

fun BlockPos.getHitVec(facing: EnumFacing): Vec3d {
    val vec = facing.directionVec
    return Vec3d(vec.x * 0.5 + 0.5 + this.x, vec.y * 0.5 + 0.5 + this.y, vec.z * 0.5 + 0.5 + this.z)
}

val EnumFacing.hitVecOffset: Vec3d get() {
    val vec = this.directionVec
    return Vec3d(vec.x * 0.5 + 0.5, vec.y * 0.5 + 0.5, vec.z * 0.5 + 0.5)
}

val BlockPos.collisionBox: AxisAlignedBB get() = this.state.getCollisionBoundingBox(Globals.mc.world, this) ?: AxisAlignedBB(this)
val BlockPos.selectedBox: AxisAlignedBB get() = this.state.getSelectedBoundingBox(Globals.mc.world, this) ?: AxisAlignedBB(this)

/**
 * Checks is entity collision colliding with block.
 * @return true if it is placeable
 */
val BlockPos.canPlaceNoCollide: Boolean get() =
    !Globals.mc.world.getEntitiesWithinAABB(Entity::class.java, this.collisionBox)
        .any { it !is EntityItem && it !is EntityXPOrb && it.isAlive }

/**
 * Checks if given [this] is able to place block in it
 * @return true playing is not colliding with [this] and there is block below it
 */
fun BlockPos.isPlaceable(ignoreSelfCollide: Boolean = false): Boolean {
    return this.state.isReplaceable && Globals.mc.world.checkNoEntityCollision(this.collisionBox, if (ignoreSelfCollide) Globals.mc.player else null)
}

val BlockPos.isPlaceableForChest: Boolean get() = this.isPlaceable() && !this.down().state.isReplaceable && this.up().isAir

val BlockPos.isInterceptedByOthers: Boolean get() = Globals.mc.world.loadedEntityList.any {
    it != Globals.mc.player && it.isAlive && this.collisionBox.intersects(it.entityBoundingBox)
}

fun BlockPos.isVisible(entity: Entity = Globals.mc.player, tolerance: Double = 1.0): Boolean = Globals.mc.world.isVisible(entity, this, tolerance)

val BlockPos.hasNeighbour: Boolean get() = EnumFacing.values().any { !this.offset(it).state.isReplaceable }

val BlockPos.firstSide: EnumFacing? get() = EnumFacing.values().firstOrNull {
    val offsetPos = this.offset(it)
    offsetPos.canBeClicked && !offsetPos.isPlaceable()
}

val BlockPos.firstSideStrict: EnumFacing? get() = this.getVisibleSides().firstOrNull {
    val offsetPos = this.offset(it)
    offsetPos.canBeClicked && !offsetPos.isPlaceable()
}

val BlockPos.possibleFacings: HashSet<EnumFacing> get() {
    val availableFacings = hashSetOf<EnumFacing>()

    for (facing in EnumFacing.values()) {
        val offsetPos = this.offset(facing)
        if (offsetPos.canBeClicked && !offsetPos.isPlaceable()) availableFacings.add(facing)
    }

    return availableFacings
}

val BlockPos.blockHardness: Float get() = this.state.getBlockHardness(Globals.mc.world, this)

val BlockPos.blockDamage: Int get() =
    Globals.mc.renderGlobal.damagedBlocks.values.firstOrNull { it.position == this }?.partialBlockDamage ?: 0

val BlockPos.obbCenter: Vec3d get() {
    val bb = this.state.getBoundingBox(Globals.mc.world, this)
    return Vec3d(
        bb.minX + (bb.maxX - bb.minX) / 2.0,
        bb.minY + (bb.maxY - bb.minY) / 2.0,
        bb.minZ + (bb.maxZ - bb.minZ) / 2.0
    )
}

val BlockPos.distanceToCenter: Double get() = Globals.mc.player.eyePosition.distanceTo(this.toVec3dCenter())

fun Double.getMiddlePosition(): Double {
    var positionFinal = round(this)
    if (round(this) > this) positionFinal -= 0.5 else if (round(this) <= this) positionFinal += 0.5
    return positionFinal
}

val BlockPos.needTileSneak: Boolean get() {
    return Globals.mc.world.loadedTileEntityList.any { it.pos == this } ||
            block is BlockBed || block is BlockNote ||
            block is BlockDoor || block is BlockTrapDoor ||
            block is BlockFenceGate || block is BlockButton || block is BlockAnvil ||
            block is BlockWorkbench || block is BlockCake || block is BlockRedstoneDiode
}

val IBlockState.isFullBox: Boolean get() = Globals.mc.world?.let {
    this.getCollisionBoundingBox(it, BlockPos.ORIGIN)
} == Block.FULL_BLOCK_AABB

val BlockPos.isFullBox: Boolean get() = Globals.mc.world?.let {
    collisionBox
} == Block.FULL_BLOCK_AABB

val IBlockState.isBlacklisted: Boolean get() = blockBlacklist.contains(this.block)
val IBlockState.isLiquid: Boolean get() = this.material.isLiquid
val IBlockState.isWater: Boolean get() = this.block == Blocks.WATER
val IBlockState.isReplaceable: Boolean get() = this.material.isReplaceable