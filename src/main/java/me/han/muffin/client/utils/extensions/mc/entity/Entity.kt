package me.han.muffin.client.utils.extensions.mc.entity

import me.han.muffin.client.core.Globals
import me.han.muffin.client.manager.managers.FriendManager
import me.han.muffin.client.utils.color.ColorUtils
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.extensions.kotlin.square
import me.han.muffin.client.utils.extensions.mc.world.getGroundPos
import me.han.muffin.client.utils.extensions.mc.world.isLiquidBelow
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityAgeable
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.EnumCreatureType
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.monster.*
import net.minecraft.entity.passive.*
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.sqrt

val nearestPlayers: List<EntityPlayer> get() = Globals.mc.world?.playerEntities?.sortedBy { Globals.mc.player?.getDistanceSq(it) } ?: emptyList()

val Entity?.eyePosition: Vec3d get() =
    Vec3d(this?.posX ?: 0.0, (this?.entityBoundingBox?.minY ?: 0.0) + (this?.eyeHeight?.toDouble() ?: 0.0), this?.posZ ?: 0.0)
    // this?.getPositionEyes(1.0F) ?: Vec3d.ZERO

fun Entity.isFakeOrSelf(): Boolean = this == Globals.mc.player || this == Globals.mc.renderViewEntity || this.entityId < 0

fun Entity.getBodyY(heightScale: Double) = this.entityBoundingBox.minY + this.height * heightScale
val Entity.isAlive: Boolean get() = !this.isDead && (this !is EntityLivingBase || this.health.isNaN() || this.health > 0.0F)

/**
 * Allows to get the distance between the current entity and [entity] from the nearest corner of the bounding box
 */
fun Entity.getDistanceToEntityBox(entity: Entity): Double {
    val eyes = this.eyePosition
    val pos = getNearestPointBB(eyes, entity.entityBoundingBox)

    val xDist = abs(pos.x - eyes.x)
    val yDist = abs(pos.y - eyes.y)
    val zDist = abs(pos.z - eyes.z)

    return sqrt(xDist.square + yDist.square + zDist.square)
}

fun getNearestPointBB(eye: Vec3d, box: AxisAlignedBB): Vec3d {
    val origin = doubleArrayOf(eye.x, eye.y, eye.z)
    val destMins = doubleArrayOf(box.minX, box.minY, box.minZ)
    val destMaxs = doubleArrayOf(box.maxX, box.maxY, box.maxZ)

    for (i in 0..2)
        if (origin[i] > destMaxs[i])
            origin[i] = destMaxs[i]
        else if (origin[i] < destMins[i])
            origin[i] = destMins[i]

    return Vec3d(origin[0], origin[1], origin[2])
}

fun EntityPlayer.getPing(): Int {
    val playerInfo = Globals.mc.connection?.getPlayerInfo(uniqueID)
    return playerInfo?.responseTime ?: 0
}

fun Entity.isAnimal(): Boolean = this is EntityAnimal || this is EntitySquid || this is EntityGolem || this is EntityBat
fun Entity.isMob(): Boolean = this is EntityMob || this is EntityVillager|| this is EntitySlime || this is EntityGhast || this is EntityDragon || this is EntityShulker

/**
 * If the mob by default wont attack the player, but will if the player attacks it
 */
fun Entity.isNeutralMob(): Boolean = this is EntityPigZombie || this is EntityWolf || this is EntityEnderman || this is EntityIronGolem

fun Entity.isHostileMob(): Boolean = this.isCreatureType(EnumCreatureType.MONSTER, false) && !this.isNeutralMob()

fun Entity.isPassiveMob(): Boolean = this is EntityAnimal || this is EntityAgeable || this is EntityTameable || this is EntityAmbientCreature || this is EntitySquid

/**
 * If the mob is friendly (not aggressive)
 */
fun Entity.isFriendlyMob(): Boolean =
    this.isCreatureType(EnumCreatureType.CREATURE, false) && !this.isNeutralMob() ||
            this.isCreatureType(EnumCreatureType.AMBIENT, false) ||
            this is EntityVillager ||
            this is EntityIronGolem ||
            this.isNeutralMob() &&
            !this.isMobAggressive()

fun Entity.isMobAggressive(): Boolean = when (this) {
    is EntityPigZombie -> {
        // arms raised = aggressive, angry = either game or we have set the anger cooldown
        this.isArmsRaised || this.isAngry
    }
    is EntityWolf -> {
        this.isAngry && Globals.mc.player != this.owner
    }
    is EntityEnderman -> {
        this.isScreaming
    }
    is EntityIronGolem -> {
        this.revengeTarget != null
    }
    else -> {
        this.isHostileMob()
    }
}

val EntityPlayer.isFriend: Boolean get() = FriendManager.isFriend(ColorUtils.stripColor(name))
val EntityLivingBase.realHealth: Float get() = this.health + this.absorptionAmount //entity.health + entity.absorptionAmount * (entity.health / entity.maxHealth)

val Entity.isLiquidBelow: Boolean get() = Globals.mc.world.isLiquidBelow(this)
val Entity.groundPos: Vec3d get() = Globals.mc.world.getGroundPos(this)

fun Entity.getRelativeBlockPos(xOffset: Int, yOffset: Int, zOffset: Int): BlockPos {
    val playerCoords = this.flooredPosition

    return if (this.posX < 0.0 && this.posZ < 0.0) {
        BlockPos(playerCoords.x + xOffset - 1, playerCoords.y + yOffset, playerCoords.z + zOffset - 1)
    } else if (this.posX < 0.0 && this.posZ > 0.0) {
        BlockPos(playerCoords.x + xOffset - 1, playerCoords.y + yOffset, playerCoords.z + zOffset)
    } else if (this.posX > 0.0 && this.posZ < 0.0) {
        BlockPos(playerCoords.x + xOffset, playerCoords.y + yOffset, playerCoords.z + zOffset - 1)
    } else {
        BlockPos(playerCoords.x + xOffset, playerCoords.y + yOffset, playerCoords.z + zOffset)
    }
}