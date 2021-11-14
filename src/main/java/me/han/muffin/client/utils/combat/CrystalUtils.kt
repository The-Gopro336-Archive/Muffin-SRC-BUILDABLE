package me.han.muffin.client.utils.combat

import me.han.muffin.client.core.Globals
import me.han.muffin.client.module.modules.combat.AutoCrystalModule
import me.han.muffin.client.module.modules.player.FreecamModule
import me.han.muffin.client.utils.block.BlockHelper
import me.han.muffin.client.utils.extensions.kotlin.floorToInt
import me.han.muffin.client.utils.extensions.kotlin.square
import me.han.muffin.client.utils.extensions.mc.block.*
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.utils.extensions.mc.utils.rayTraceBlockC
import me.han.muffin.client.utils.extensions.mc.utils.xLength
import me.han.muffin.client.utils.extensions.mc.utils.yLength
import me.han.muffin.client.utils.extensions.mc.world.getVisibleSidesStrict
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.math.VectorUtils
import me.han.muffin.client.utils.math.VectorUtils.distanceTo
import me.han.muffin.client.utils.math.VectorUtils.toBlockPos
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
import me.han.muffin.client.utils.math.rotation.Vec2f
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.item.EntityXPOrb
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.MobEffects
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.util.DamageSource
import net.minecraft.util.NonNullList
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.EnumDifficulty
import net.minecraft.world.Explosion
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object CrystalUtils {

    fun getPlacePos(target: EntityLivingBase?, center: Entity?, radius: Float, onePointThirteen: Boolean, strictDirection: Boolean): List<BlockPos> {
        if (center == null) return emptyList()
        val centerPos = if (center == Globals.mc.player) center.eyePosition else center.positionVector
        return VectorUtils.getBlockPosInSphere(centerPos, radius).filter { canPlace(it, target, onePointThirteen, strictDirection) }
    }

    private fun checkItemsColliding(pos: BlockPos): Boolean {
        val placingBB = getCrystalPlacingBB(pos)
        return Globals.mc.world.getEntitiesWithinAABBExcludingEntity(null, placingBB).any {
            it != null && (it is EntityItem || it is EntityXPOrb) && it.isAlive
        }
    }

    fun canPlaceCrystal(pos: BlockPos, multiPlace: Boolean = true, onePointThirteen: Boolean = false, strictDirection: Boolean = false): Boolean {
        val posUp = pos.up()
        val posUp2 = posUp.up()

        if (!canPlaceOn(pos)) return false
        if (!onePointThirteen && !posUp2.isAir || !posUp.isAir) return false
        if (checkItemsColliding(pos) || strictDirection && pos.getVisibleSidesStrict().isEmpty()) return false

        val firstBB = AxisAlignedBB(posUp)
        if (Globals.mc.world.checkBlockCollision(firstBB)) return false

        for (entity in Globals.mc.world.getEntitiesWithinAABB(Entity::class.java, firstBB)) {
            if (!entity.isAlive || FreecamModule.isEnabled && entity == FreecamModule.cameraGuy || !multiPlace && entity is EntityEnderCrystal) continue
            return false
        }

        if (!onePointThirteen) {
            val secondBB = AxisAlignedBB(posUp2)
            if (Globals.mc.world.checkBlockCollision(secondBB)) return false
            for (entity in Globals.mc.world.getEntitiesWithinAABB(Entity::class.java, secondBB)) {
                if (!entity.isAlive || FreecamModule.isEnabled && entity == FreecamModule.cameraGuy || !multiPlace && entity is EntityEnderCrystal) continue
                return false
            }
        }

        return true
/*
        if (onePointThirteen) {
            val boost = pos.add(0, 1, 0)
            val boost2 = pos.add(0, 2, 0)

            val crystalBB = boost2.add(1, 1, 1)

            val placeable = if (multiPlace) {
                Globals.mc.world.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB(boost, crystalBB)).isEmpty()
            } else {
                Globals.mc.world.getEntitiesWithinAABB(EntityPlayer::class.java, AxisAlignedBB(boost, crystalBB)).isEmpty()
            }

            return (pos.block == Blocks.BEDROCK || pos.block == Blocks.OBSIDIAN) &&
                    boost.block == Blocks.AIR && placeable
        }

        val placingBB = getCrystalPlacingBB(pos.up())

        val placeable = if (multiPlace) Globals.mc.world.getEntitiesWithinAABBExcludingEntity(null, placingBB).isEmpty()
        else Globals.mc.world.getEntitiesWithinAABB(EntityPlayer::class.java, placingBB).isEmpty() &&
                Globals.mc.world.getEntitiesWithinAABB(EntityOtherPlayerMP::class.java, placingBB).isEmpty() &&
                Globals.mc.world.getEntitiesWithinAABB(EntityPlayerSP::class.java, placingBB).isEmpty() &&
                Globals.mc.world.getEntitiesWithinAABB(EntityPlayerMP::class.java, placingBB).isEmpty()

        return (pos.block == Blocks.BEDROCK || pos.block == Blocks.OBSIDIAN) &&
                !Globals.mc.world.checkBlockCollision(placingBB) && placeable
 */
    }

    fun canPlace(pos: BlockPos, entity: EntityLivingBase? = null, onePointThirteen: Boolean = false, strictDirection: Boolean = false): Boolean {
        if (!canPlaceOn(pos)) return false

        val posUp1 = pos.up()
        if (entity != null && getCrystalPlacingBB(pos).grow(-1.0E-4, 0.0, -1.0E-4).intersects(entity.entityBoundingBox)) return false

        val posUp2 = posUp1.up()
        if (onePointThirteen && !posUp1.isAir) {
            return false
        } else if (!isValidMaterial(posUp1.material) || !isValidMaterial(posUp2.material)) {
            return false
        }

        return !checkItemsColliding(pos) && (!strictDirection || pos.getVisibleSidesStrict().isNotEmpty())
    }

    fun findCrystalBlocks(target: EntityLivingBase?, center: EntityPlayer?, range: Double, onePointThirteen: Boolean, strictDirection: Boolean): List<BlockPos> {
        if (center == null) return emptyList()
        val positions = NonNullList.create<BlockPos>()
        val centerPos = if (center == Globals.mc.player) center.eyePosition else center.positionVector
        positions.addAll(VectorUtils.getBlockPosInSphere(centerPos, range.toFloat()).filter { canPlace(it, target, onePointThirteen, strictDirection) })
        return positions
    }

    fun findCrystalBlocks(player: EntityPlayer?, range: Double, multiPlace: Boolean, onePointThirteen: Boolean, strictDirection: Boolean): List<BlockPos> {
        if (player == null) return emptyList()
        val positions = NonNullList.create<BlockPos>()
        val centerPos = if (player == Globals.mc.player) player.eyePosition else player.positionVector
        positions.addAll(VectorUtils.getBlockPosInSphere(centerPos, range.toFloat()).filter { canPlaceCrystal(it, multiPlace, onePointThirteen, strictDirection) })
        return positions
    }

    fun canPlaceOn(pos: BlockPos): Boolean {
        val block = pos.block
        return block == Blocks.BEDROCK || block == Blocks.OBSIDIAN
    }

    fun isValidMaterial(material: Material) = !material.isLiquid && material.isReplaceable

    fun getCrystalPlacingBB(pos: BlockPos) = AxisAlignedBB(
        pos.x + 0.001, pos.y + 1.0, pos.z + 0.001,
        pos.x + 0.999, pos.y + 3.0, pos.z + 0.999
    )

    fun getCrystalBB(pos: BlockPos): AxisAlignedBB = AxisAlignedBB(
        pos.x - 0.5, pos.y - 0.5, pos.z - 0.5,
        pos.x + 1.5, pos.y + 2.0, pos.z + 1.5
    )

    fun canPlaceCollide(pos: BlockPos): Boolean {
        val placingBB = getCrystalPlacingBB(pos)
        // world.getEntitiesWithinAABB(Entity::class.java, AxisAlignedBB(pos.up(), pos.add(1, 1, 1))).firstOrNull { !it.isDead } == null
        return Globals.mc.world.getEntitiesWithinAABBExcludingEntity(null, placingBB).all {
            !it.isAlive || FreecamModule.isEnabled && it == FreecamModule.cameraGuy
        }
    }

    fun canPlaceCollideAntiSurround(pos: BlockPos): Boolean {
        val placingBB = getCrystalPlacingBB(pos)
        return Globals.mc.world.getEntitiesWithinAABB(EntityPlayer::class.java, placingBB).all {
            !it.isAlive || FreecamModule.isEnabled && it == FreecamModule.cameraGuy
        }
    }

    fun getCrystalList(range: Double): List<EntityEnderCrystal> {
        return getCrystalList(Globals.mc.player.positionVector, range)
    }

    fun getCrystalList(center: Vec3d, range: Double): List<EntityEnderCrystal> =
        Globals.mc.world.loadedEntityList.filterIsInstance<EntityEnderCrystal>()
            .filter { entity -> entity.isAlive && entity.distanceTo(center) <= range }

    fun canBreakWeakness(): Boolean {
        val mainHandItem = Globals.mc.player.heldItemMainhand.item
        val strengthAmp = Globals.mc.player.getActivePotionEffect(MobEffects.STRENGTH)?.amplifier ?: 0

        return !Globals.mc.player.isPotionActive(MobEffects.WEAKNESS) ||
                strengthAmp >= 1 || mainHandItem is ItemSword || mainHandItem is ItemTool
    }

    private fun calcRawDamage(vector: Vec3d, defaultBox: AxisAlignedBB, entityBB: AxisAlignedBB, distance: Float, explosionSize: Float): Float {
        // val distance = vector.distanceTo(entityPos)
        val factor = (1.0F - distance) * getExposureAmount(vector, defaultBox, entityBB) // MinecraftBlockInstance.getBlockDensity(Globals.mc.world, vector, entityBB, true, true, true, true)
        // return floor((factor * factor + factor) * 42.0F + 1.0F)
        return (factor * factor + factor) / 2.0F * 7.0F * explosionSize + 1.0F
    }

    fun calculateDamage(vec: Vec3d, entity: EntityLivingBase, size: Float = 6.0F, entityPos: Vec3d? = entity.positionVector, entityBB: AxisAlignedBB? = entity.entityBoundingBox): Float {
        if (entity is EntityPlayer && entity.isCreative) return 0.0F

        val isProtocol = AutoCrystalModule.onePointThirteen.value
        val explosionSize = size * 2.0F
        val entityVector = entityPos ?: entity.positionVector
        val posBelow = vec.toBlockPos(0, -1, 0)

        // val scaledDistance = entity.getDistance(vec.x, vec.y, vec.z) / explosionSize
        // val scaledDistance = sqrt(vec.squareDistanceTo(entityVector) / explosionSize)

        val scaledDistance = if (isProtocol) sqrt(vec.squareDistanceTo(entityVector).toFloat() / explosionSize) else (vec.distanceTo(entityVector).toFloat() / explosionSize)
        if (scaledDistance > 1.0F) return 0.0F

        var damage = if (isResistant(posBelow.state) && vec.y - entityVector.y > 1.5652173822904127) {
            1.0F
        } else {
            calcRawDamage(vec, entity.entityBoundingBox, entityBB ?: entity.entityBoundingBox, scaledDistance, explosionSize)
        }

        damage = CombatUtils.calcDamage(entity, damage, getDamageSource(vec))
        if (entity is EntityPlayer) damage *= Globals.mc.world.difficulty.id * 0.5F //damage = getDamageDifficulty(damage)

        return max(damage, 0F)
    }

    private fun getDamageDifficulty(damage: Float): Float {
        when (Globals.mc.world.difficulty) {
            EnumDifficulty.PEACEFUL -> return 0.0f
            EnumDifficulty.EASY -> return min(damage / 2.0f + 1.0f, damage)
            EnumDifficulty.HARD -> return damage * 3.0f / 2.0f
        }
        return damage
    }

    private fun getDamageSource(damagePos: Vec3d) =
        DamageSource.causeExplosionDamage(Explosion(Globals.mc.world, Globals.mc.player, damagePos.x, damagePos.y, damagePos.z, 6.0F, false, true))

    fun calculateDamage(crystal: EntityEnderCrystal, entity: EntityLivingBase, entityPos: Vec3d? = entity.positionVector, entityBB: AxisAlignedBB? = entity.entityBoundingBox) =
        calculateDamage(crystal.positionVector, entity, 6.0F, entityPos, entityBB)

    fun calculateDamage(pos: BlockPos, entity: EntityLivingBase, entityPos: Vec3d? = entity.positionVector, entityBB: AxisAlignedBB? = entity.entityBoundingBox) =
        calculateDamage(pos.toVec3dCenter(0.0, 0.5, 0.0), entity, 6.0F, entityPos, entityBB)

    fun calculateBedDamage(pos: BlockPos, entity: EntityLivingBase, entityPos: Vec3d? = entity.positionVector, entityBB: AxisAlignedBB? = entity.entityBoundingBox) =
        calculateDamage(pos.toVec3dCenter(), entity, 5.0F, entityPos, entityBB)

    /*
    @JvmStatic
    fun getBestStrictFacing(blockPos: BlockPos, checks: Boolean): PlaceRotation? {
        val eyesPos = Vec3d(Globals.mc.player.posX, Globals.mc.player.entityBoundingBox.minY + Globals.mc.player.getEyeHeight(), Globals.mc.player.posZ)

        var placeRotation: PlaceRotation? = null

        for (side in EnumFacing.values()) {
            val neighbour = blockPos.offset(side)
            val dirVec = Vec3d(side.directionVec)

            val posVec = blockPos.toVec3d().add(0.0, 0.5, 0.0)
            val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
            val hitVec = posVec.add(Vec3d(dirVec.x * 0.5, dirVec.y * 0.5, dirVec.z * 0.5))

            if (checks && (eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) || Globals.mc.world.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null)) continue
            val rotation = RotationUtils.getRotationTo(hitVec, true).toVec2f()
            val rotationVector = RotationUtils.getVectorForRotation(rotation)
            val vector = eyesPos.add(rotationVector.x * 4, rotationVector.y * 4, rotationVector.z * 4)
            val rayTrace = Globals.mc.world.rayTraceBlocks(eyesPos, vector, false, false, true)
            if (rayTrace?.typeOfHit != RayTraceResult.Type.BLOCK || rayTrace.blockPos != neighbour) continue
            if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(placeRotation.rotation)) {
                placeRotation = PlaceRotation(PlaceInfo(neighbour, side.opposite, hitVec), rotation)
            }
        }

        return placeRotation //?: if (blockPos.y > Globals.mc.player.posY + Globals.mc.player.getEyeHeight()) PlaceRotation(PlaceInfo(blockPos, EnumFacing.DOWN, hitVec), rotation) else EnumFacing.UP
    }
     */

    private val samplePointsCache = HashMap<Vec2f, Array<Vec3d>>()

    private fun getSamplePoints(defaultBox: AxisAlignedBB, boundingBox: AxisAlignedBB) = getExposureSamplePoints(defaultBox).let { array ->
        Array(array.size) {
            array[it].add(boundingBox.minX, boundingBox.minY, boundingBox.minZ)
        }
    }

    private fun getExposureSamplePoints(box: AxisAlignedBB): Array<Vec3d> {
        val width = MathUtils.round(box.xLength.toFloat(), 2)
        val height = MathUtils.round(box.yLength.toFloat(), 2)

        return samplePointsCache.getOrPut(Vec2f(width, height)) {
            val gridMultiplierXZ = 1.0 / (width * 2.0 + 1.0)
            val gridMultiplierY = 1.0 / (height * 2.0 + 1.0)

            val gridXZ = width * gridMultiplierXZ
            val gridY = height * gridMultiplierY

            val sizeXZ = (1.0 / gridMultiplierXZ).floorToInt() + 1
            val sizeY = (1.0 / gridMultiplierY).floorToInt() + 1
            val sizeXZSq = sizeXZ.square

            val xzOffset = (1.0 - gridMultiplierXZ * (sizeXZ - 1)) / 2.0

            Array(sizeXZSq * sizeY) {
                val xzIndex = it % sizeXZSq
                val xIndex = xzIndex % sizeXZ

                val yIndex = it / sizeXZSq
                val zIndex = xzIndex / sizeXZ

                Vec3d(gridXZ * xIndex + xzOffset, gridY * yIndex, gridXZ * zIndex + xzOffset)
            }
        }
    }

    private fun getExposureAmount(vec: Vec3d, defaultBox: AxisAlignedBB, boundingBox: AxisAlignedBB): Float {
        val array = getSamplePoints(defaultBox, boundingBox)
        val count = array.count {
            Globals.mc.world.rayTraceBlockC(it, vec) == null
        }
        return count / array.size.toFloat()
    }

    /**
     * @return true if the block are hard
     */
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")
    fun isResistant(state: IBlockState): Boolean {
        return !state.isLiquid && state.block.getExplosionResistance(null) >= 19.7
    }

    fun getMostDistanced(pos: BlockPos, player: EntityPlayer): Double {
        var distance = abs(player.posY - pos.up().y) + abs(player.posX - pos.x) + abs(player.posZ - pos.z)
        if (BlockHelper.getRayTraceToClosest(Vec3d(pos.add(0.5, 1.0, 0.5)), player.positionVector) == RayTraceResult.Type.BLOCK) {
            distance = -1.0
        }
        return distance
    }

    fun getMostDistanced(crystal: EntityEnderCrystal, player: EntityPlayer): Double {
        var distance = (abs(player.posY - crystal.posY) + abs(player.posX - crystal.posX) + abs(player.posZ - crystal.posZ))
        if (BlockHelper.getRayTraceToClosest(crystal.positionVector, player.positionVector) == RayTraceResult.Type.BLOCK) {
            distance = -1.0
        }
        return distance
    }

}