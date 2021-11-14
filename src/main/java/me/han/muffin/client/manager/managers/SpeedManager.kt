package me.han.muffin.client.manager.managers

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.entity.MovementUtils.realSpeed
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.utils.extensions.mixin.misc.tickLength
import me.han.muffin.client.utils.extensions.mixin.misc.timer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future

object SpeedManager {
    private val speedMap = ConcurrentHashMap<Entity, SpeedData>()

    private val threadMap = hashMapOf<Thread, Future<*>?>(
        Thread { updateEntitySpeedMap() } to null
    )

    private val updateExecutor = Executors.newCachedThreadPool()

    fun initListener() {
        Muffin.getInstance().eventManager.addEventListener(this)
    }

    val Entity.speedKmh: Double get() = speedMap[this]?.kmh ?: 0.0
    val Entity.speedMps: Double get() = speedMap[this]?.mps ?: 0.0
    val Entity.speedReal: Double get() = speedMap[this]?.realSpeed ?: 0.0

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        for ((thread, future) in threadMap) {
            if (future?.isDone == false) continue
            threadMap[thread] = updateExecutor.submit(thread)
        }
    }

    private fun updateEntitySpeedMap() {
        if (EntityUtil.fullNullCheck()) return

        val tempSpeedMap = hashMapOf<Entity, SpeedData>()
        val tps = 1000.0 / Globals.mc.timer.tickLength

        for (entity in Globals.mc.world.loadedEntityList.sortedBy { Globals.mc.player.getDistanceSq(it) }) {
            if (entity == null || entity !is EntityLivingBase || !entity.isAlive) continue

            val realSpeed = entity.realSpeed
            val kmh = realSpeed * 3.6 * tps
            val mps = realSpeed * tps

            tempSpeedMap[entity] = SpeedData(kmh, mps, realSpeed)
        }

        speedMap.clear()
        speedMap.putAll(tempSpeedMap)
    }

    data class SpeedData(val kmh: Double, val mps: Double, val realSpeed: Double)

}