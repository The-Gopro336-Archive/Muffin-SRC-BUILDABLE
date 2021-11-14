package me.han.muffin.client.manager.managers

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.utils.block.HoleInfo
import me.han.muffin.client.utils.block.HoleUtils
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.extensions.kotlin.square
import me.han.muffin.client.utils.extensions.mc.block.isAir
import me.han.muffin.client.utils.timer.Timer
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.function.Predicate

object HoleManager {
    val holesMap = ConcurrentHashMap<BlockPos, HoleInfo>()
    var holeInfos = emptyList<HoleInfo>()

    private val threadMap = hashMapOf<Thread, Future<*>?>(
        Thread { updateHolesData() } to null,
        Thread { removeInvalidHoles() } to null
    )

    private val updateExecutor = Executors.newCachedThreadPool()
    private val updateTimer = Timer()

    private const val RADIUS = 16
    private const val RADIUS_SQ = 16 * 16

    private const val MAX_RADIUS = 32

    fun initListener() {
        Muffin.getInstance().eventManager.addEventListener(this)
    }

    val holeInfosNear: List<HoleInfo> get() = holeInfos.sortedBy { Globals.mc.player?.flooredPosition?.distanceSq(it.origin) }

    val holeInfosNearFiltered: List<HoleInfo> get() = holeInfos.asSequence().filter {
        val origin = it.origin
        origin.isAir && origin.up().isAir && origin.up(2).isAir && !origin.down().isAir
    }.sortedBy { Globals.mc.player?.flooredPosition?.distanceSq(it.origin) }.toList()

    val Entity.isInHole: Boolean get() = this.holeInfo.isHole
    val Entity.isInHoleOne: Boolean get() = this.holeInfo.isOne

    val Entity.holeInfo: HoleInfo get() = this.flooredPosition.holeInfo

    val BlockPos.holeInfo: HoleInfo get() {
        return holesMap.computeIfAbsent(this) { HoleUtils.checkHoleTest(it) }
    }

    fun getHoleBelow(pos: BlockPos, yRange: Int): HoleInfo? {
        return getHoleBelow(pos, yRange) { true }
    }

    fun getHoleBelow(pos: BlockPos, yRange: Int, filter: Predicate<HoleInfo>): HoleInfo? {
        for (yOffset in 0..yRange) {
            val offsetPos = pos.down(yOffset)
            val holeInfo = offsetPos.holeInfo
            if (!holeInfo.isHole || !filter.test(holeInfo)) continue
            return holeInfo
        }
        return null
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        updateHoles()
    }

    private fun updateHoles() {
//        if (updateThread == null || !updateThread!!.isAlive || updateThread!!.isInterrupted) {
//            updateThread = thread(start = false) { updateHolesData() }
//            removeThread = thread(start = false) { removeInvalidHoles() }
//
//            updateExecutor.execute(updateThread!!)
//        }

        for ((thread, future) in threadMap) {
            if (future?.isDone == false) continue // Skip if the previous thread isn't done
            threadMap[thread] = updateExecutor.submit(thread)
        }

    }

    private fun updateHolesData() {
        if (EntityUtil.fullNullCheck()) return

        val flooredPosition = Globals.mc.player.flooredPosition
        val tempHoles = hashMapOf<BlockPos, HoleInfo>()
        val checkedPos = hashSetOf<BlockPos>()

        if (!updateTimer.passed(5.0)) return

        for (x in -RADIUS..RADIUS) for (y in -RADIUS..RADIUS) for (z in -RADIUS..RADIUS) {
            val pos = flooredPosition.add(x, y, z)

            if (checkedPos.contains(pos) || pos.distanceSq(flooredPosition) > RADIUS_SQ || !Globals.mc.world.worldBorder.contains(pos)) continue

            val holeResult = HoleUtils.checkHoleTest(pos)
            if (!holeResult.isHole) {
                holesMap.remove(pos)?.let { holesMap.keys.removeAll(it.holePos) }
                checkedPos.add(pos)
                continue
            }

            holeResult.holePos.forEach {
                tempHoles[it] = holeResult
                checkedPos.add(it)
            }
        }


        holesMap.clear()
        holesMap.putAll(tempHoles)

        updateTimer.reset()
    }

    private fun removeInvalidHoles() {
        if (EntityUtil.fullNullCheck()) return

        val playerPos = Globals.mc.player.flooredPosition

        holesMap.keys.removeIf { !HoleUtils.checkHoleTest(it).isHole || it.distanceSq(playerPos) > MAX_RADIUS.square }
        updateHoleInfos()
    }

    private fun updateHoleInfos() {
        holeInfos = holesMap.values.asSequence().distinct().filter { it.isHole }.toList()
    }

}