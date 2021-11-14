package me.han.muffin.client.module.modules.movement

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.entity.player.PlayerApplyCollisionEvent
import me.han.muffin.client.event.events.entity.player.PlayerPushEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.extensions.mc.utils.component1
import me.han.muffin.client.utils.extensions.mc.utils.component2
import me.han.muffin.client.utils.extensions.mc.utils.component3
import me.han.muffin.client.utils.extensions.mixin.entity.isInWeb
import me.han.muffin.client.utils.extensions.mixin.netty.packetMotionX
import me.han.muffin.client.utils.extensions.mixin.netty.packetMotionY
import me.han.muffin.client.utils.extensions.mixin.netty.packetMotionZ
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.MoverType
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraft.init.Blocks
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.server.*
import net.minecraft.util.SoundCategory
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.text.DecimalFormat
import kotlin.math.abs

internal object VelocityModule: Module("Velocity", Category.MOVEMENT, "Take the amount of velocity you want.") {

    private val mode = EnumValue(Mode.Simple, "Mode")
    private val liquid = Value(true, "Liquid")
    private val blocks = Value(true, "Blocks")
    private val pushable = Value(false, "Pushable")
    private val fishingHooks = Value(true, "FishingHooks")
    private val explosions = Value(true, "Explosions")
    private val pistons = Value(false, "Pistons")

    private val horizontal = NumberValue(0, 0, 100, 1, "Horizontal")
    private val vertical = NumberValue(0, 0, 100, 1, "Vertical")

    private val velocityTimer = Timer()
    private var velocityInput = false

    private val pistonResetTimer = Timer()
    private var hasSoundReceived = false
    private var hasBlockAction = false

    private enum class Mode {
        Simple, AAC
    }

    init {
        addSettings(mode, liquid, blocks, pushable, fishingHooks, explosions, pistons, horizontal, vertical)
    }

    private val shouldCancelVelocity get() = horizontal.value == 0 && vertical.value == 0

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (mode.value == Mode.Simple) Globals.mc.player.entityCollisionReduction = 1.0F

        if (pistons.value) {
            val upPos = Globals.mc.player.flooredPosition.up().block
            if (upPos == Blocks.PISTON_EXTENSION || upPos == Blocks.PISTON_HEAD) {
                hasSoundReceived = true
                hasBlockAction = true
            }
            if (hasSoundReceived && hasBlockAction && (upPos != Blocks.PISTON_EXTENSION && upPos != Blocks.PISTON_HEAD)) {
                hasSoundReceived = false
                hasBlockAction = false
            }
        }

        if (Globals.mc.player.isInWater || Globals.mc.player.isInLava || Globals.mc.player.isInWeb) {
            return
        }

        when (mode.value) {
            Mode.AAC -> if (velocityInput && velocityTimer.passed(80.0)) {
                Globals.mc.player.motionX *= horizontal.value
                Globals.mc.player.motionZ *= horizontal.value
                velocityInput = false
            }
        }

    }

//    @Listener
//    private fun onMoving(event: MoveEvent) {
//        if (fullNullCheck() || !pistons.value || event.type != MoverType.PISTON && event.type != MoverType.SHULKER_BOX) return
//        event.cancel()
//    }


    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (fishingHooks.value && event.packet is SPacketEntityStatus) {
            if (event.packet.opCode == 31.toByte()) {
                val entity = event.packet.getEntity(Globals.mc.world)
                if (entity is EntityFishHook) {
                    if (entity.caughtEntity == Globals.mc.player) {
                        event.cancel()
                    }
                }
            }
        }

        if (pistons.value && event.packet is SPacketSoundEffect && event.packet.sound == SoundEvents.BLOCK_PISTON_EXTEND && event.packet.category == SoundCategory.BLOCKS) {
            val soundX = Globals.mc.player.posX
            val soundZ = Globals.mc.player.posZ
            if (abs(soundX) in soundX..soundX + 2.0 || abs(soundZ) in soundZ..soundZ + 2.0) {
                hasSoundReceived = true
            }
        }

        if (pistons.value && event.packet is SPacketBlockAction && (event.packet.blockType == Blocks.PISTON || event.packet.blockType == Blocks.STICKY_PISTON)) {
            val localX = Globals.mc.player.posX
            val localZ = Globals.mc.player.posZ
            val (x, y, z) = event.packet.blockPosition
            if (abs(x).toDouble() in localX..localX + 2.0 || abs(z).toDouble() in localZ..localZ + 2.0) {
                hasBlockAction = true
            }
        }

        if (pistons.value && event.packet is SPacketPlayerPosLook && hasSoundReceived && hasBlockAction) {
            event.cancel()
            hasSoundReceived = false
            hasBlockAction = false
        }

        if (event.packet is SPacketEntityVelocity) {
            if (Globals.mc.world.getEntityByID(event.packet.entityID) != Globals.mc.player) return
            velocityTimer.reset()

            when (mode.value) {
                Mode.Simple -> {
                    if (shouldCancelVelocity) {
                        event.cancel()
                    } else {
                        event.packet.packetMotionX *= horizontal.value / 100
                        event.packet.packetMotionY *= vertical.value / 100
                        event.packet.packetMotionZ *= horizontal.value / 100
                    }
                }
                Mode.AAC -> velocityInput = true
            }
        }

        if (explosions.value && event.packet is SPacketExplosion) {
            if (shouldCancelVelocity) {
                event.cancel()
            } else {
                event.packet.packetMotionX *= horizontal.value / 100
                event.packet.packetMotionY *= vertical.value / 100
                event.packet.packetMotionZ *= horizontal.value / 100
            }
        }

    }


    private val df = DecimalFormat("0.0")

    override fun getHudInfo(): String {
        return "H" + df.format(horizontal.value) + "% " +
                ChatFormatting.GRAY +
                "| " +
                ChatFormatting.WHITE +
                "V" + df.format(vertical.value) + "%"
    }


    @Listener
    private fun onPlayerPush(event: PlayerPushEvent) {
        if (blocks.value && event.type == PlayerPushEvent.Type.BLOCK) {
            event.cancel()
        }
        if (liquid.value && event.type == PlayerPushEvent.Type.LIQUID) {
            event.cancel()
        }
    }

    @Listener
    private fun onPlayerApplyCollision(event: PlayerApplyCollisionEvent) {
        if (pushable.value) return
        event.cancel()
    }


}