package me.han.muffin.client.utils.entity

import net.minecraft.entity.Entity
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.*
import net.minecraft.entity.monster.*
import net.minecraft.entity.passive.*
import net.minecraft.entity.projectile.EntityEvokerFangs
import net.minecraft.entity.projectile.EntityFireball
import net.minecraft.entity.projectile.EntityShulkerBullet

object EntityTypeUtils {

    fun isIronGolem(entity: Entity) = entity is EntityIronGolem && entity.rotationPitch == 0.0f

    fun isWolf(entity: Entity) = entity is EntityWolf && !entity.isAngry

    fun isRabbit(entity: Entity) = entity is EntityRabbit && entity.rabbitType != 99

    fun isChillPolarBear(entity: Entity) = entity is EntityPolarBear && entity.rotationPitch == 0.0f && entity.revengeTimer <= 0

    fun isMonster(entity: Entity) =
        entity is EntityCreeper || entity is EntityIllusionIllager || entity is EntitySkeleton || entity is EntityZombie && entity !is EntityPigZombie || entity is EntityBlaze || entity is EntitySpider || entity is EntityWitch || entity is EntitySlime || entity is EntitySilverfish || entity is EntityGuardian || entity is EntityEndermite || entity is EntityGhast || entity is EntityEvoker || entity is EntityShulker || entity is EntityWitherSkeleton || entity is EntityStray || entity is EntityVex || entity is EntityVindicator || entity is EntityPolarBear && !isChillPolarBear(entity) || entity is EntityWolf && !isWolf(entity) || entity is EntityPigZombie && !isChillZombiePigman(entity) || entity is EntityEnderman && !isChillEnderman(entity) || entity is EntityRabbit && !isRabbit(entity) || entity is EntityIronGolem && !isIronGolem(entity)


    fun isChillEnderman(entity: Entity) = entity is EntityEnderman && !entity.isScreaming

    fun isChillAggressiveAnimals(entity: Entity) =
        entity is EntityWolf && isWolf(entity) || entity is EntityPolarBear && isChillPolarBear(entity) || entity is EntityIronGolem && isIronGolem(entity) || entity is EntityEnderman && isChillEnderman(entity) || entity is EntityPigZombie && isChillZombiePigman(entity)

    fun isHugeEntity(entity: Entity) = entity is EntityDragon || entity is EntityWither || entity is EntityGiantZombie

    fun isAirEntity(entity: Entity) =
        entity is EntityEnderCrystal || entity is EntityEvokerFangs || entity is EntityShulkerBullet || entity is EntityFallingBlock || entity is EntityFireball || entity is EntityEnderEye || entity is EntityEnderPearl


    fun isVehicle(entity: Entity) = entity is EntityBoat || entity is EntityMinecart

    fun isAnimals(entity: Entity) =
        entity is EntityPig || entity is EntityParrot || entity is EntityCow || entity is EntitySheep || entity is EntityChicken || entity is EntitySquid || entity is EntityBat || entity is EntityVillager || entity is EntityOcelot || entity is EntityHorse || entity is EntityLlama || entity is EntityMule || entity is EntityDonkey || entity is EntitySkeletonHorse || entity is EntityZombieHorse || entity is EntitySnowman || entity is EntityRabbit && isRabbit(entity)


    fun isChillZombiePigman(entity: Entity) = entity is EntityPigZombie && entity.rotationPitch == 0.0f && entity.revengeTimer <= 0


}