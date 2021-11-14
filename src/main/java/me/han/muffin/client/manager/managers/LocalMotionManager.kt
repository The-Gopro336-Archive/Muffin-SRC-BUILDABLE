package me.han.muffin.client.manager.managers

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.StrafeEvent
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.render.entity.RenderEntityEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.extensions.kotlin.floorToInt
import me.han.muffin.client.utils.extensions.kotlin.toRadian
import me.han.muffin.client.utils.extensions.mc.utils.multiply
import me.han.muffin.client.utils.extensions.mixin.netty.*
import me.han.muffin.client.utils.math.RandomUtils
import me.han.muffin.client.utils.math.pattern.AIMING_PATTERNS
import me.han.muffin.client.utils.math.rotation.Vec2f
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

object LocalMotionManager {
    private val motionMap = TreeMap<Module, Motion>(compareByDescending { it.modulePriority })

    var serverSidePosition: Vec3d = Vec3d.ZERO; private set
    var prevServerSidePosition: Vec3d = Vec3d.ZERO; private set

    var serverSideRotation = Vec2f.ZERO; private set
    var prevServerSideRotation = Vec2f.ZERO; private set

    var horizontalFacing = EnumFacing.NORTH; private set

    var clientSideYaw = Vec2f.ZERO; private set
    var clientSidePitch = Vec2f.ZERO; private set

    var rotationRandomX = RandomUtils.random.nextDouble(); private set
    var rotationRandomY = RandomUtils.random.nextDouble(); private set
    var rotationRandomZ = RandomUtils.random.nextDouble(); private set

    init {
       Muffin.getInstance().eventManager.addEventListener(this)
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        // Update patterns
        for (pattern in AIMING_PATTERNS) pattern.update()

        if (motionMap.isNotEmpty()) {
            motionMap.values.first().apply(event) // Apply the packet from the module that has the highest priority
            motionMap.clear()
        }
    }

//    @Listener
//    private fun onPlayerStrafe(event: StrafeEvent) {
//        val fixedVelocity = fixVelocity(Vec3d(event.strafe.toDouble(), event.up.toDouble(), event.forward.toDouble()), event.friction)
//        Globals.mc.player.setVelocity(Globals.mc.player.motionX + fixedVelocity.x, Globals.mc.player.motionY + fixedVelocity.y, Globals.mc.player.motionZ + fixedVelocity.z)
//        event.cancel()
//    }

    @Listener // (priority = ListenerPriority.HIGHEST)
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (RandomUtils.random.nextDouble() > 0.6) rotationRandomX = RandomUtils.random.nextDouble()
        if (RandomUtils.random.nextDouble() > 0.6) rotationRandomY = RandomUtils.random.nextDouble()
        if (RandomUtils.random.nextDouble() > 0.6) rotationRandomZ = RandomUtils.random.nextDouble()

        prevServerSidePosition = serverSidePosition
        prevServerSideRotation = serverSideRotation

        horizontalFacing = EnumFacing.byHorizontalIndex(((serverSideRotation.x * 4.0f / 360.0f) + 0.5).floorToInt() and 3)
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (EntityUtil.fullNullCheck()) return

        if (event.packet is SPacketPlayerPosLook) {
            var yaw = event.packet.yaw
            var pitch = event.packet.pitch

            if (event.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.Y_ROT)) yaw += Globals.mc.player.rotationYaw
            if (event.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.X_ROT)) pitch += Globals.mc.player.rotationPitch

            serverSideRotation = Vec2f(yaw, pitch)
        }
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.POST) return

        if (event.isCanceled || event.packet !is CPacketPlayer) return

        if (event.packet.moving) {
            serverSidePosition = Vec3d(event.packet.x, event.packet.y, event.packet.z)
        }

        if (event.packet.rotating) {
            serverSideRotation = Vec2f(event.packet.yaw, event.packet.pitch)
            Globals.mc.player?.let { player -> player.rotationYawHead = event.packet.yaw }
        }

    }


    @Listener
    private fun onRenderRotation(event: RenderEntityEvent) {
        if (event.entity == null || Globals.mc.currentScreen is GuiInventory || event.entity != Globals.mc.player || event.entity.isRiding) return

        when (event.stage) {
            EventStageable.EventStage.PRE -> {
                with(event.entity) {
                    clientSideYaw = Vec2f(prevRotationYaw, rotationYaw)
                    clientSidePitch = Vec2f(prevRotationPitch, rotationPitch)

                    prevRotationYaw = prevServerSideRotation.x
                    rotationYaw = serverSideRotation.x
                    prevRotationPitch = prevServerSideRotation.y
                    rotationPitch = serverSideRotation.y
                }
            }
            EventStageable.EventStage.POST -> {
                with(event.entity) {
                    prevRotationYaw = clientSideYaw.x
                    rotationYaw = clientSideYaw.y
                    prevRotationPitch = clientSidePitch.x
                    rotationPitch = clientSidePitch.y
                }
            }
            else -> {
            }
        }

    }

    fun addMotionBase(caller: Module, motion: Motion) {
        if (motion.isEmpty()) return
        motionMap[caller] = motion
    }

    fun Module.addMotion(motion: Motion) {
        addMotionBase(this, motion)
    }

    inline fun Module.addMotion(block: Motion.Builder.() -> Unit) {
        Motion.Builder().apply(block).build()?.let {
            addMotion(it)
        }
    }

    /**
     * Fix velocity
     */
    fun fixVelocity(movementInput: Vec3d, speed: Float): Vec3d {
        serverSideRotation.fixedSensitivity().let { rotation ->
            val yaw = rotation.x
            val d = movementInput.lengthSquared()

            return if (d < 1.0E-7) {
                Vec3d.ZERO
            } else {
                val vec = (if (d > 1.0) movementInput.normalize() else movementInput).multiply(speed.toDouble())

                val f = sin(yaw.toRadian())
                val g = cos(yaw.toRadian())

                Vec3d(vec.x * g.toDouble() - vec.z * f.toDouble(), vec.y, vec.z * g.toDouble() + vec.x * f.toDouble())
            }
        }
    }

    class Motion(var pos: Vec3d? = null, var onGround: Boolean? = null, var rotation: Vec2f? = null) {
        fun isEmpty(): Boolean {
            return pos == null && onGround == null && rotation == null
        }

        fun apply(event: MotionUpdateEvent) {
            if (isEmpty()) return

            this.onGround?.let {
                event.location.isOnGround = it
            }

            this.pos?.let {
                event.location.x = it.x
                event.location.y = it.y
                event.location.z = it.z
            }

            this.rotation?.let { targetRotation ->
                if (targetRotation.x.isNaN() || targetRotation.y.isNaN() || targetRotation.y > 90 || targetRotation.y < -90) return

                targetRotation.fixedSensitivity().let { fixedRotation ->
                    val fixedPitch = fixedRotation.y.coerceIn(-90F..90F)

                    event.rotation.x = fixedRotation.x
                    event.rotation.y = fixedPitch

                    event.rotating = true
                }
            }

        }

        class Builder {
            private var moving: Boolean? = null
            private var onGround: Boolean? = null
            private var rotating: Boolean? = null
            private var position: Vec3d? = null
            private var rotation: Vec2f? = null

            private var empty = true

            fun move(position: Vec3d) {
                this.position = position
                this.moving = true
                this.empty = false
            }

            fun rotate(rotation: Vec2f) {
                this.rotation = rotation
                this.rotating = true
                this.empty = false
            }

            fun ground(factor: Boolean) {
                this.onGround = factor
                this.empty = false
            }

            fun ground() {
                ground(true)
            }

            fun noGround() {
                ground(false)
            }

            fun cancelMove() {
                this.position = null
                this.moving = false
                this.empty = false
            }

            fun cancelRotate() {
                this.rotation = null
                this.rotating = false
                this.empty = false
            }

            fun build() =
                if (!empty) Motion(position, onGround, rotation) else null
        }
    }

}