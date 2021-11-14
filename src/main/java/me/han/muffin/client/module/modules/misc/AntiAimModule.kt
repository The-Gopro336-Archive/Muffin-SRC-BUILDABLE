package me.han.muffin.client.module.modules.misc

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.math.rotation.Vec2f
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object AntiAimModule : Module("AntiAim", Category.MISC, "CsGo HvH boy.") {
    private val yawMode = EnumValue(Yaw.Static, "Yaw")
    private val pitchMode = EnumValue(Pitch.Down, "Pitch")
    private val yawAdd = NumberValue(0.0F, 0.0F, 180.0F, 1.0F, "YawAdd")
    private val pitchAdd = NumberValue(0.0F, 0.0F, 180.0F, 1.0F, "PitchAdd")
    private val spinSpeed = NumberValue(1.0F, 1.0F, 100.0F, 1.0F, "SpinSpeed")

    private enum class Yaw {
        Off, Static, Zero, Spin, Jitter, Test
    }

    private enum class Pitch {
        Off, Static, Zero, Up, Down, Jitter, Headless, Test
    }

    private var rotation = Vec2f(0.0F, 0.0F)
    private var prevRotation = Vec2f(0.0F, 0.0F)

    init {
        addSettings(yawMode, pitchMode, yawAdd, spinSpeed)
    }

    override fun getHudInfo(): String {
        return "${yawMode.fixedValue} ${ChatFormatting.GRAY}| ${ChatFormatting.WHITE}${pitchMode.fixedValue}"
    }

    override fun onEnable() {
        if (fullNullCheck()) return
        prevRotation = Vec2f(Globals.mc.player.rotationYaw, Globals.mc.player.rotationPitch)
    }

    override fun onDisable() {
        prevRotation = Vec2f(0F, 0F)
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return
        if (fullNullCheck()) return

        if (Globals.mc.gameSettings.keyBindAttack.isKeyDown || Globals.mc.gameSettings.keyBindUseItem.isKeyDown) return

       // if (Globals.mc.playerController.isHittingBlock || Globals.mc.player.isActiveItemStackBlocking || Globals.mc.player.isHandActive) return

        val derpRotations = Vec2f(Globals.mc.player.rotationYaw + (Math.random() * 360 - 180).toFloat(), (Math.random() * 180 - 90).toFloat())
        val currentRotation = Vec2f.ZERO

        when (yawMode.value) {
            Yaw.Static -> currentRotation.x = Globals.mc.player.rotationYaw + yawAdd.value.toFloat()
            Yaw.Zero -> currentRotation.x = prevRotation.x
            Yaw.Spin -> {
                currentRotation.x += spinSpeed.value
                if (currentRotation.x >= 360F) currentRotation.x = 0F
            }
            Yaw.Jitter -> currentRotation.x = (if (Globals.mc.player.ticksExisted % 2 == 0) 90 else -90).toFloat()
            Yaw.Test -> currentRotation.x = derpRotations.x
            else -> currentRotation.x = 0F
        }

         when (pitchMode.value) {
             Pitch.Static -> currentRotation.y = pitchAdd.value - 90
             Pitch.Zero -> currentRotation.y = prevRotation.y
             Pitch.Up -> currentRotation.y = -90F
             Pitch.Down -> currentRotation.y = 90F
             Pitch.Jitter -> {
                 currentRotation.y += 30.0f
                 if (currentRotation.y > 90.0f) currentRotation.y = -90.0f
                 if (currentRotation.y < -90.0F) currentRotation.y = 90.0F
             }
             Pitch.Headless -> currentRotation.y = 180F
             Pitch.Test -> currentRotation.y = derpRotations.y
            else -> currentRotation.y = 0F
        }

        addMotion { rotate(currentRotation) }
    }

}