package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.client.ClientTickEvent
import me.han.muffin.client.manager.managers.FriendManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.InfoUtils
import me.han.muffin.client.utils.PredictMode
import me.han.muffin.client.utils.combat.CrystalUtils
import me.han.muffin.client.utils.combat.DamageData
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.extensions.kotlin.square
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.utils.extensions.mc.entity.nearestPlayers
import me.han.muffin.client.utils.extensions.mc.entity.realHealth
import me.han.muffin.client.utils.extensions.mixin.misc.tickLength
import me.han.muffin.client.utils.extensions.mixin.misc.timer
import me.han.muffin.client.utils.math.MotionTracker
import me.han.muffin.client.utils.math.PredictUtils
import me.han.muffin.client.utils.math.RayTraceUtils
import me.han.muffin.client.utils.math.VectorUtils.distanceTo
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.collections.ArrayList

internal object AutoCrystalHelper: Module("AutoCrystalHelper", Category.HIDDEN, true, true) {

    var target: EntityPlayer? = null
        set(value) {
            motionTracker.target = value
            field = value
        }

    private var lastDamage = 0.0F

    private val motionTracker = MotionTracker(null)

    var placeMap = emptyMap<BlockPos, DamageData>()
    var crystalMap = emptyMap<EntityEnderCrystal, DamageData>()
    var crystalList = emptyList<Pair<EntityEnderCrystal, DamageData>>()

    private val threadMap = hashMapOf<Thread, Future<*>?>(
        Thread { updateTarget() } to null,
        Thread { updatePlacingList() } to null,
        Thread { updateCrystalMap() } to null,
        Thread { updateCrystalList() } to null
    )

    private val threadPool = Executors.newCachedThreadPool()

    @Listener
    private fun onClientTicking(event: ClientTickEvent) {
        if (fullNullCheck()) return

        for ((thread, future) in threadMap) {
            if (future?.isDone == false) continue // Skip if the previous thread isn't done
            threadMap[thread] = threadPool.submit(thread)
        }
    }

    private fun updatePlacingList() {
        if (AutoCrystalModule.isDisabled && CrystalBlocksModule.isDisabled && Globals.mc.player.ticksExisted % 4 != 0) return

        val eyePos = Globals.mc.player.eyePosition
        val cacheList = arrayListOf<Pair<BlockPos, DamageData>>()

        val nearestTarget = target
        val prediction = nearestTarget?.let { getPrediction(it) }

        val blocks = if (AutoCrystalModule.placeMethod.value == AutoCrystalModule.PlaceMethod.Old)
            CrystalUtils.findCrystalBlocks(Globals.mc.player, AutoCrystalModule.currentBestPlaceRange, AutoCrystalModule.placeSelector, AutoCrystalModule.onePointThirteen.value, AutoCrystalModule.strictDirection.value)
        else
            CrystalUtils.findCrystalBlocks(nearestTarget, Globals.mc.player, AutoCrystalModule.currentBestPlaceRange, AutoCrystalModule.onePointThirteen.value, AutoCrystalModule.strictDirection.value)

        for (pos in blocks) {
            val dist = eyePos.distanceTo(pos.toVec3dCenter(0.0, 0.5, 0.0))
            val damage = nearestTarget?.let { CrystalUtils.calculateDamage(pos, it, prediction?.first, prediction?.second) } ?: 0.0F
            val selfDamage = CrystalUtils.calculateDamage(pos, Globals.mc.player)
            cacheList.add(Pair(pos, DamageData(damage, selfDamage, dist)))
        }

        placeMap = LinkedHashMap<BlockPos, DamageData>(cacheList.size).apply {
            putAll(cacheList.sortedByDescending { it.second.targetDamage })
        }

    }

    private fun updateCrystalMap() {
        if (AutoCrystalModule.isDisabled && (Globals.mc.player.ticksExisted - 2) % 4 != 0) return

        val cacheList = arrayListOf<Pair<EntityEnderCrystal, DamageData>>()
        val eyePos = Globals.mc.player.eyePosition
        val currentTarget = target ?: AutoCrystalModule.renderingTarget ?: EntityUtil.findClosestTarget(AutoCrystalModule.enemyRange.value + AutoCrystalModule.maxRange)
        val prediction = currentTarget?.let { getPrediction(it) }

        for (entity in Globals.mc.world.loadedEntityList.toList()) {
            if (!entity.isAlive) continue
            if (entity !is EntityEnderCrystal) continue
            val dist = entity.distanceTo(eyePos)
            if (dist > 16.0F) continue
            val damage = if (currentTarget != null && prediction != null) CrystalUtils.calculateDamage(entity, currentTarget, prediction.first, prediction.second) else 0.0f
            val selfDamage = CrystalUtils.calculateDamage(entity, Globals.mc.player)
            cacheList.add(entity to DamageData(damage, selfDamage, dist))
        }

        crystalMap = LinkedHashMap<EntityEnderCrystal, DamageData>(cacheList.size).apply {
            putAll(cacheList.sortedByDescending { it.second.targetDamage })
        }
    }

    private fun updateCrystalList() {
        crystalList = crystalMap.mapNotNullTo(ArrayList(crystalMap.size)) {
            it.key to it.value
        }.sortedByDescending { it.second.targetDamage }

//        crystalList = ArrayList<Pair<EntityEnderCrystal, DamageData>>(crystalMap.size).apply {
//            addAll(crystalMap.mapTo(this) { it.key to it.value })
//        }.sortedByDescending { it.second.targetDamage }
    }

    private fun getTargetList(range: Double): ArrayList<EntityPlayer> {
        if (Globals.mc.world.loadedEntityList.isNullOrEmpty()) return arrayListOf()

        val entityList = arrayListOf<EntityPlayer>()
        var distance = Double.MAX_VALUE

        for (entity in nearestPlayers) {
            if (entity.name == Globals.mc.player.name || entity == Globals.mc.renderViewEntity || !entity.isAlive || FriendManager.isFriend(entity.name)) continue
            if (Globals.mc.player.getDistanceSq(entity) > range.square) continue
            val dist = entity.getDistance(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ)
            if (distance > dist) {
                distance = dist
                entityList.add(entity)
            }
        }

        return entityList
    }

    private fun getTargetListDamage(range: Double): ArrayList<EntityPlayer> {
        if (Globals.mc.world.loadedEntityList.isNullOrEmpty()) return arrayListOf()

        val entityList = arrayListOf<EntityPlayer>()

        for (entity in nearestPlayers) {
            if (entity.name == Globals.mc.player.name || entity == Globals.mc.renderViewEntity || !entity.isAlive || FriendManager.isFriend(entity.name)) continue
            if (Globals.mc.player.getDistanceSq(entity) > range.square) continue
            entityList.add(entity)
        }

        return entityList
    }

    private fun getMostDamageTarget(range: Double): Pair<EntityPlayer?, Float> {
        if (Globals.mc.world.loadedEntityList.isNullOrEmpty()) return null to 0.0F

        val eyePos = Globals.mc.player.eyePosition
        val tempTargets = arrayListOf<Pair<EntityPlayer, Float>>()
        var targetBlockDamage = Float.MIN_VALUE

        val enemyRange = AutoCrystalModule.enemyRange.value
        val bestPlaceRange = AutoCrystalModule.currentBestPlaceRange
        val placeWallRange = AutoCrystalModule.placeWallRange.value
        val bestConstrictRange = AutoCrystalModule.currentBestConstrictRange

        val minPlaceDamage = AutoCrystalModule.minPlaceDamage.value
        val placeEfficiency = AutoCrystalModule.placeEfficiency.value

        for (entity in nearestPlayers) {
            if (target != null && target != entity) continue

            if (entity.name == Globals.mc.player.name || entity == Globals.mc.renderViewEntity || !entity.isAlive || FriendManager.isFriend(entity.name)) continue
            if (Globals.mc.player.getDistanceSq(entity) > range.square) continue
            // if (target != null && Globals.mc.player.getDistanceSq(entity) >= Globals.mc.player.getDistanceSq(target!!)) continue

            val blocks = if (AutoCrystalModule.placeMethod.value == AutoCrystalModule.PlaceMethod.Old)
                CrystalUtils.findCrystalBlocks(Globals.mc.player, bestPlaceRange, AutoCrystalModule.placeSelector, AutoCrystalModule.onePointThirteen.value, AutoCrystalModule.strictDirection.value)
            else
                CrystalUtils.findCrystalBlocks(entity, Globals.mc.player, bestPlaceRange, AutoCrystalModule.onePointThirteen.value, AutoCrystalModule.strictDirection.value)

            val prediction = getPrediction(entity)

            for (pos in blocks) {
                val hitVec = pos.toVec3dCenter(0.0, 0.5, 0.0)
                val dist = eyePos.distanceTo(hitVec)

                if (dist > bestConstrictRange) continue
                if (entity.distanceTo(hitVec) >= enemyRange) continue

                if (!AutoCrystalModule.maxYOffsetCheck(pos) || !AutoCrystalModule.isValidPos(pos, dist) || AutoCrystalModule.doCollideCheck(pos)) continue

                if (placeWallRange > 0.0) {
                    val hitPos = RayTraceUtils.rayTraceTo(hitVec.subtract(0.0, 0.5, 0.0))?.blockPos ?: pos
                    if (hitPos.distanceTo(pos) > 1.0 && dist > placeWallRange) continue
                }

                val selfDamage = CrystalUtils.calculateDamage(pos, Globals.mc.player)
                if (!AutoCrystalModule.doSuicideCheck(selfDamage)) continue

                val targetDamage = CrystalUtils.calculateDamage(pos, entity, prediction.first, prediction.second)
                val (minPlaceDmg, efficiency) = AutoCrystalModule.getMinDamageAndFactor(AutoCrystalModule.facePlaceMode.value, entity, targetDamage, minPlaceDamage, placeEfficiency, AutoCrystalModule.lethalHealth.value, AutoCrystalModule.lethalMultiplier.value)

                if (targetDamage < minPlaceDmg) continue
                if (placeEfficiency > 0.0 && targetDamage - selfDamage < efficiency) continue

                if (!AutoCrystalModule.checkMaxLocalPlace(entity, selfDamage, targetDamage, minPlaceDmg)) continue
                if (selfDamage > targetDamage && targetDamage < entity.realHealth) continue

                if (targetDamage > targetBlockDamage) {
                    targetBlockDamage = targetDamage
                    tempTargets.add(entity to targetDamage)
                }
            }
        }

        val highestDamageTarget = tempTargets.maxByOrNull { it.second }
        return highestDamageTarget?.first to (highestDamageTarget?.second ?: 0.0F)
    }


    fun getLinkedTargetListDamage(enemyRange: Double): LinkedList<EntityPlayer> {
        return LinkedList(getTargetListDamage(enemyRange))
    }

    private fun getNearestTarget(range: Double, minDamage: Double, lethalHealth: Double, lethalMultiplier: Double, unsafe: Boolean): EntityPlayer? {
        if (AutoCrystalModule.targetPriority.value == AutoCrystalModule.TargetPriority.Damage) return null

        var tempEntity: EntityPlayer? = null
        val linkedList = getTargetList(range)

        for (player in linkedList) {
            if (unsafe && EntityUtil.isSafe(player)) continue

            val areValidToAim =
                player.realHealth < lethalHealth ||
                        AutoCrystalModule.checkArmourBreakable(player, AutoCrystalModule.minDurability.value > 0, AutoCrystalModule.minDurability.value)

            if (Globals.mc.player.getDistanceSq(player) <= (AutoCrystalModule.maxRange + 2).square && areValidToAim) {
                tempEntity = player
                break
            }

            //        if (Globals.mc.player.getDistanceSq(player) <= MathUtils.square(AutoCrystalModule.maxRange + 2) && targetHole == HoleUtils.HoleType.NONE) {
            //            tempEntity = player
            //            break
            //        }

            tempEntity = if (tempEntity == null) {
                player
            } else {
                //            val targetHole = HoleUtils.checkHole(player)
                //            val tempEntityHole = HoleUtils.checkHole(tempEntity)

                //            if (targetHole == HoleUtils.HoleType.NONE && tempEntityHole != HoleUtils.HoleType.NONE) continue
                if (Globals.mc.player.getDistanceSq(player) >= Globals.mc.player.getDistanceSq(tempEntity)) continue
                player
            }

        }


        if (unsafe && tempEntity == null) {
            return getNearestTarget(range, minDamage, lethalHealth, lethalMultiplier, false)
        }

        return tempEntity
    }

    private fun updateTarget() {
        var currentDamage = 0.0F
        val currentTarget: EntityPlayer?

        if (AutoCrystalModule.targetPriority.value == AutoCrystalModule.TargetPriority.MTDamage) {
            val data = getMostDamageTarget(AutoCrystalModule.enemyRange.value + AutoCrystalModule.maxRange)
            currentTarget = data.first
            currentDamage = data.second
        } else {
            currentTarget = getNearestTarget(
                AutoCrystalModule.enemyRange.value + AutoCrystalModule.maxRange,
                AutoCrystalModule.minPlaceDamage.value,
                AutoCrystalModule.lethalHealth.value,
                AutoCrystalModule.lethalMultiplier.value,
                AutoCrystalModule.targetPriority.value == AutoCrystalModule.TargetPriority.Unsafe)
        }

        target = currentTarget
        lastDamage = currentDamage
    }

    private fun getPrediction(entity: Entity) = target?.let {
        val predictTicks = AutoCrystalModule.predictTicks.value
        val pingSync = predictTicks == -1

        val predictedVector = when (AutoCrystalModule.predictMode.value) {
            PredictMode.Normal -> {
                val prediction = PredictUtils.getDiffVector(entity)
                val predictVector = entity.positionVector.add(prediction)
                val predictBB = entity.entityBoundingBox.offset(prediction)
                predictVector to predictBB
            }
            PredictMode.Motion -> {
                val vectorDiff = PredictUtils.getDiffVector(entity, Globals.mc.timer.tickLength.div(1.5F))
                val prediction = PredictUtils.getSquaredNewNormalDistance(entity, vectorDiff)

                val predictBB = entity.entityBoundingBox.offset(
                    if (entity.collidedHorizontally) 0.0 else vectorDiff.x,
                    if (entity.collidedVertically) (if (vectorDiff.y > 0.0) vectorDiff.y else 0.0) else vectorDiff.y,
                    if (entity.collidedHorizontally) 0.0 else vectorDiff.z
                )

                prediction to predictBB
            }
            PredictMode.Interpolation -> {
                val predictBlockPosition = PredictUtils.predictEntityLocation(it, (InfoUtils.ping() / 1000.0).times(20.0))
                predictBlockPosition to PredictUtils.getPredictedBoundingBox(it, predictBlockPosition)
            }
            PredictMode.New -> {
                val result = PredictUtils.getNewPrediction(motionTracker, it, pingSync, predictTicks)
                result.first to result.second
            }
            PredictMode.Rewrite -> {
                val predictedVector = PredictUtils.getRewritePos(it)
                predictedVector to PredictUtils.getPredictedBoundingBox(it, predictedVector)
            }
            else -> it.positionVector to it.entityBoundingBox
        }

        predictedVector
    } ?: entity.positionVector to entity.entityBoundingBox

    fun getPredictionSpecified(entity: Entity) = entity.let {
        val predictTicks = AutoCrystalModule.predictTicks.value
        val pingSync = predictTicks == -1

        val predictedVector = when (AutoCrystalModule.predictMode.value) {
            PredictMode.Normal -> {
                val prediction = PredictUtils.getDiffVector(entity)
                val predictVector = entity.positionVector.add(prediction)
                val predictBB = entity.entityBoundingBox.offset(prediction)
                predictVector to predictBB
            }
            PredictMode.Motion -> {
                val vectorDiff = PredictUtils.getDiffVector(entity, Globals.mc.timer.tickLength.div(1.5F))
                val prediction = PredictUtils.getSquaredNewNormalDistance(entity, vectorDiff)

                val predictBB = entity.entityBoundingBox.offset(
                    if (entity.collidedHorizontally) 0.0 else vectorDiff.x,
                    if (entity.collidedVertically) (if (vectorDiff.y > 0.0) vectorDiff.y else 0.0) else vectorDiff.y,
                    if (entity.collidedHorizontally) 0.0 else vectorDiff.z
                )

                prediction to predictBB
            }
            PredictMode.Interpolation -> {
                val predictBlockPosition = PredictUtils.predictEntityLocation(it, (InfoUtils.ping() / 1000.0).times(20.0))
                predictBlockPosition to PredictUtils.getPredictedBoundingBox(it, predictBlockPosition)
            }
            PredictMode.New -> {
                val result = PredictUtils.getNewPrediction(motionTracker, it, pingSync, predictTicks)
                result.first to result.second
            }
            PredictMode.Rewrite -> {
                val predictedVector = PredictUtils.getRewritePos(it)
                predictedVector to PredictUtils.getPredictedBoundingBox(it, predictedVector)
            }
            else -> it.positionVector to it.entityBoundingBox
        }

        predictedVector
    }

}