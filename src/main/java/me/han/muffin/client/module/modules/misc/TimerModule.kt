package me.han.muffin.client.module.modules.misc

import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.manager.managers.TimerManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.network.LagCompensator
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.network.play.server.SPacketPlayerPosLook
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.round

object TimerModule: Module("Timer", Category.MISC, "Speed up your game.") {
    private val tpsSync = Value(false, "TPSSync")
    var speed = NumberValue(1.1f, 0.1f, 60f, 0.1f, "Speed")
    var accelerate = Value(false, "Accelerate")

    init {
        addSettings(tpsSync, speed, accelerate)
    }

    /// store this as member to save cpu
    private val df = DecimalFormat("#.#")
    private val timer = Timer()

    private val maxSpeed get() = max(speed.value, 0.1F) //round(EntityUtil.timerSpeed * 100.0) / 100.0 // max(speed.value, 0.1F)

    private var accelerateSpeed = 1.0F

    override fun getHudInfo(): String {
        return (round(TimerManager.timerSpeed * 100.0) / 100.0).toString()
        //if (overrideSpeed != 1.0F) return MathUtils.round(overrideSpeed, 1).toString()
        //return MathUtils.round(maxSpeed, 1).toString()
    }

    override fun onDisable() {
        TimerManager.resetTimer()
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        val tpsSyncTimer = round(TimerManager.timerSpeed * 100.0) / 100.0

        if (fullNullCheck()) {
            TimerManager.resetTimer()
            return
        }

        if (tpsSync.value) {
            if (LagCompensator.tickRate / 20.0 > 0.01) {
                TimerManager.setTimer(LagCompensator.tickRate / 20.0F)
                return
            }
            TimerManager.resetTimer()
            return
        }

        TimerManager.setTimer(maxSpeed)

        if (accelerate.value && timer.passed(2000.0)) {
            accelerateSpeed += 0.1F
            timer.reset()
        }

    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (accelerate.value && event.packet is SPacketPlayerPosLook) {
            accelerateSpeed = 1.0F
        }
    }

}