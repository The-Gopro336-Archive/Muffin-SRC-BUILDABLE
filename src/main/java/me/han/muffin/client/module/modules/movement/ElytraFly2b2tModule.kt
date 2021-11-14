package me.han.muffin.client.module.modules.movement

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.movement.SmoothElytraEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.manager.managers.TimerManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.entity.MovementUtils.speed
import me.han.muffin.client.utils.extensions.kotlin.toDegree
import me.han.muffin.client.utils.extensions.kotlin.toRadian
import me.han.muffin.client.utils.extensions.mc.entity.groundPos
import me.han.muffin.client.utils.extensions.mixin.netty.onGround
import me.han.muffin.client.utils.extensions.mixin.netty.pitch
import me.han.muffin.client.utils.extensions.mixin.netty.yaw
import me.han.muffin.client.utils.math.RandomUtils
import me.han.muffin.client.utils.math.rotation.Vec2f
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.init.Items
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.network.play.client.CPacketConfirmTeleport
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.server.SPacketEntity
import net.minecraft.network.play.server.SPacketEntityMetadata
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.math.Vec3d
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal object ElytraFly2b2tModule: Module("ElytraFly2b2t", Category.MOVEMENT, "An elytra fly was made for bypass toobeetootee dot org.") {
    private val stopOnGround = Value(true, "StopOnGround")
    private val stopInWater = Value(true, "StopInWater")

    private val ncpStrict = Value(true, "NCPStrict")
    private val antiKick = Value(true, "AntiKick")
    private val accelerateMode = EnumValue(AccelerateMode.Constant, "AccelerateMode")

    private val speed = NumberValue(5.0, 0.1, 50.0, 0.1, "Speed")
    private val upwardSpeed = NumberValue(2.0, 0.0, 10.0, 0.1, "UpwardSpeed")
    private val downwardSpeed = NumberValue(1.0, 0.0, 10.0, 0.1, "DownwardSpeed")

    private val accelSpeed = NumberValue(1.0, 0.0, 10.0, 0.1, "AccelSpeed")
    private val decelSpeed = NumberValue(1.0, 0.0, 10.0, 0.1, "DecelSpeed")

    private var shouldSlow = true
    private var ticksCount = 0

    private var isStandingStillH = false
    private var isStandingStill = false

    private val antiKickTimer = Timer()
    private var moveSpeed = 0.0

    private var packetYaw = 0.0F
    private var packetPitch = 0.0F

    private var lastPos = Vec3d.ZERO
    private var rotation = Vec2f.ZERO
    private var lastRotation = Vec2f.ZERO

    private var elytraIsEquipped = false
    private var elytraDurability = 0
    private var outOfDurability = false

    private val packetTimer = Timer()

    private var isFlying = false

    private var jumpHeightMin = 0.50
    private var isJumpStart = false

    private var fallFlyingTicksCount = 0
    private var currentPhase = MovementState.NOT_STARTED

    private var accelStart = System.currentTimeMillis()

    private var teleportPosition = Vec3d.ZERO
    private var teleportRotation = Vec2f.ZERO

    private enum class MovementState {
        NOT_STARTED, IDLE, MOVING
    }

    private enum class AccelerateMode {
        None, Constant, Interpolation
    }

    init {
        addSettings(stopOnGround, stopInWater, ncpStrict, accelerateMode, antiKick, speed, upwardSpeed, downwardSpeed, accelSpeed, decelSpeed)
    }

    override fun onEnable() {
        if (fullNullCheck()) return

        reset()

        if (Globals.mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).item == Items.ELYTRA) {
            Globals.mc.player.setVelocity(0.0, 0.0, 0.0)
        }
    }

    override fun onDisable() {
        reset()
        shouldSlow = true
        ticksCount = 0
    }

    private fun reset() {
        moveSpeed = 0.0
        fallFlyingTicksCount = 0
        isJumpStart = false
        currentPhase = MovementState.NOT_STARTED
        rotation = Vec2f(0.0F, 0.0F)
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (fullNullCheck()) return
        if (!isElytrable()) return

        when (event.stage) {
            EventStageable.EventStage.PRE -> {
                if (!Globals.mc.player.onGround || !stopOnGround.value) {
                    when (event.packet) {
                        is CPacketPlayer.Position -> {
                            if (event.packet.onGround) {
                                if (Globals.mc.player.posY - Globals.mc.player.groundPos.y > 0.0) {
                                    event.packet.onGround = false
                                }
                            } else {
                                event.cancel()
                            }
                        }
                        is CPacketPlayer.PositionRotation -> {
                            if (event.packet.onGround) {
                                if (Globals.mc.player.posY - Globals.mc.player.groundPos.y > 0.0) {
                                    event.packet.onGround = false
                                }
                            } else {
                                rotation = Vec2f(event.packet.yaw, event.packet.pitch)
                                event.cancel()
                            }
                        }
                        is CPacketPlayer.Rotation -> {
                            rotation = Vec2f(event.packet.yaw, event.packet.pitch)
                            event.cancel()
                        }
                        is CPacketEntityAction -> {
                        }
                    }
                }
            }

            EventStageable.EventStage.POST -> {
                when (event.packet) {
                    is CPacketConfirmTeleport -> {
                        Globals.mc.player.setVelocity(0.0, 0.0, 0.0)
                        accelStart = System.currentTimeMillis()
                        shouldSlow = true
                        moveSpeed = 1.0
                        /* This only sets the position and rotation client side since it is not salted with onGround */
                        Globals.mc.player.setPositionAndRotation(teleportPosition.x, teleportPosition.y, teleportPosition.z, teleportRotation.x, teleportRotation.y)
                        /* Force send the packet */
                        sendForcedPacket(true)
                    }
                }
            }
        }
    }

    @Listener
    private fun onDisconnect(event: ServerEvent.Disconnect) {
        disable()
    }

    private fun doBetterResponse(event: PacketEvent.Receive) {
        if (event.packet !is SPacketPlayerPosLook) return
        var x = event.packet.x
        var y = event.packet.y
        var z = event.packet.z
        var yaw = event.packet.yaw
        var pitch = event.packet.pitch
        if (event.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.X)) {
            x += Globals.mc.player.posX
        }
        if (event.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.Y)) {
            y += Globals.mc.player.posY
        }
        if (event.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.Z)) {
            z += Globals.mc.player.posZ
        }
        if (event.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.X_ROT)) {
            pitch += Globals.mc.player.rotationPitch
        }
        if (event.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.Y_ROT)) {
            yaw += Globals.mc.player.rotationYaw
        }
        Globals.mc.connection?.sendPacket(CPacketPlayer.PositionRotation(x, Globals.mc.player.entityBoundingBox.minY, z, yaw, pitch, false))
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (fullNullCheck()) return

        if (!isElytrable()) return

        when (event.stage) {
            EventStageable.EventStage.PRE -> {
                when (event.packet) {
                    is SPacketPlayerPosLook -> {
                        teleportRotation = Vec2f(event.packet.yaw, event.packet.pitch)
                        teleportPosition = Vec3d(event.packet.x, event.packet.y, event.packet.z)
                        shouldSlow = true
                    }
                    is SPacketEntityMetadata -> {
                        if (event.packet.entityId == Globals.mc.player.entityId) event.cancel()
                    }
                    is SPacketEntity.S15PacketEntityRelMove -> {
                        if (isFlying) event.cancel()
                    }
                    is SPacketEntity.S17PacketEntityLookMove -> {
                        if (isFlying) event.cancel()
                    }
                }
            }
            EventStageable.EventStage.POST -> {
            }
        }
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        when (event.stage) {
            EventStageable.EventStage.PRE -> {
            }
            EventStageable.EventStage.POST -> {

            }
        }
    }

    private fun isElytrable(): Boolean {
        return elytraIsEquipped && elytraDurability > 1 && !outOfDurability
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (fullNullCheck()) return

        doMovementUpdate()

        if ((Globals.mc.player.posY - Globals.mc.player.groundPos.y < jumpHeightMin) && currentPhase != MovementState.NOT_STARTED) {
            reset()
            return
        }
        if (!isElytrable() && currentPhase != MovementState.NOT_STARTED) {
            reset()
            return
        }

        when (currentPhase) {
            MovementState.NOT_STARTED -> calcStartingHeight()
        }

        when (event.stage) {
            EventStageable.EventStage.PRE -> {
                if (!Globals.mc.player.onGround || !stopOnGround.value) {

                }
            }
            EventStageable.EventStage.POST -> {
                if (!Globals.mc.player.onGround || !stopOnGround.value) {

                    for (i in 0 until 3) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_FALL_FLYING))
                    rotation = Vec2f(Globals.mc.player)
                    sendForcedPacket(false)
                    for (i in 0 until 2) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_FALL_FLYING))

                    if (ncpStrict.value && !shouldSlow && (abs(event.location.x) >= 0.05 || abs(event.location.z) >= 0.05)) {
                        val random = 1.0E-8 + 1.0E-8 * (1.0 + RandomUtils.random.nextInt(1 + if (RandomUtils.random.nextBoolean()) RandomUtils.random.nextInt(34) else RandomUtils.random.nextInt(43)))
                        if (Globals.mc.player.onGround || Globals.mc.player.ticksExisted % 2 == 0) {
                            event.location.y += random
                            return
                        }
                        event.location.y -= random
                    }
                }
            }
        }
    }

    @Listener
    private fun onMove(event: MoveEvent) {
        if (fullNullCheck()) return

        if (!isElytrable()) return

        if (!Globals.mc.player.onGround || !stopOnGround.value) {
            Globals.mc.player.isSprinting = false

            if (accelerateMode.value == AccelerateMode.Interpolation) {
                if (shouldSlow) {
                    moveSpeed = 1.0
                    shouldSlow = false
                }
                if (moveSpeed < speed.value) {
                    moveSpeed += accelSpeed.value.div(10)//0.1
                }
                if (moveSpeed - accelSpeed.value.div(10) > speed.value) {
                    moveSpeed -= decelSpeed.value.div(10)
                }
            } else {
                moveSpeed = speed.value
            }

            if (antiKick.value && !MovementUtils.isMoving() && !Globals.mc.player.collided) {
                if (antiKickTimer.passed(1000.0)) {
                    shouldSlow = true
                    ticksCount++
                    Globals.mc.player.motionX += 0.03 * sin((ticksCount * 4.0).toRadian())
                    Globals.mc.player.motionZ += 0.03 * cos((ticksCount * 4.0).toRadian())
                }
                accelStart = System.currentTimeMillis()
            } else {
                antiKickTimer.reset()
                shouldSlow = false
            }

            if (upwardSpeed.value > 0.0 && Globals.mc.player.movementInput.jump) {
                event.y = upwardSpeed.value.also { Globals.mc.player.motionY = it }
            } else if (downwardSpeed.value > 0.0 && Globals.mc.player.movementInput.sneak) {
                event.y = -downwardSpeed.value.also { Globals.mc.player.motionY = it }
            } else if (ncpStrict.value) {
                if (Globals.mc.player.ticksExisted % 32 == 0 && !shouldSlow && (abs(event.x) >= 0.05 || abs(event.z) >= 0.05)) {
                    moveSpeed -= moveSpeed / 2.0 * 0.1
                    Globals.mc.player.motionY = -2.0E-4
                    event.y = 0.006200000000000001
                } else {
                    event.y = (-2.0E-4).also { Globals.mc.player.motionY = it }
                }
            } else {
                event.y = 0.0.also { Globals.mc.player.motionY = it }
            }

            isFlying = !Globals.mc.player.onGround

            val yawRad = MovementUtils.calcMoveYaw()
            packetYaw = yawRad.toDegree().toFloat()

            if (accelerateMode.value == AccelerateMode.Constant) {
                if (MovementUtils.isMoving()) {
                    if (Globals.mc.player.speed < 9.90F) {
                        val accelVec = Globals.mc.player.lookVec.scale(((System.currentTimeMillis() - accelStart) / (10000 - (accelSpeed.value.div(100.0) * 10000 - 1))))
                        event.x = if (shouldSlow) 0.5 else accelVec.x
                        event.z = if (shouldSlow) 0.5 else accelVec.z
                    } else {
                        event.x = if (shouldSlow) 0.5 else event.x
                        event.z = if (shouldSlow) 0.5 else event.z
                    }
                } else {
                    accelStart = System.currentTimeMillis()
                }
            }

            event.x *= if (shouldSlow) 0.5 else moveSpeed
            event.z *= if (shouldSlow) 0.5 else moveSpeed
        }
    }

    private fun doMovementUpdate() {
        val armorSlot = Globals.mc.player.inventory.armorInventory[2]
        elytraIsEquipped = armorSlot.item == Items.ELYTRA

        if (elytraIsEquipped) {
            val oldDurability = elytraDurability
            elytraDurability = armorSlot.maxDamage - armorSlot.itemDamage

            if (!Globals.mc.player.onGround && oldDurability != elytraDurability) {
                if (elytraDurability <= 1 && !outOfDurability) {
                    outOfDurability = true
                }
            }
        } else {
            elytraDurability = 0
        }

        if (!Globals.mc.player.onGround && elytraDurability <= 1 && outOfDurability) {
            holdPlayer()
        } else if (outOfDurability) {
            outOfDurability = false
        }

        isStandingStillH = Globals.mc.player.movementInput.moveForward == 0.0F && Globals.mc.player.movementInput.moveStrafe == 0.0F
        isStandingStill = isStandingStillH && !Globals.mc.player.movementInput.jump && !Globals.mc.player.movementInput.sneak
    }

    private fun holdPlayer() {
        TimerManager.resetTimer()
        Globals.mc.player.setVelocity(0.0, -0.01, 0.0)
    }

    @Listener
    private fun onSmoothElytra(event: SmoothElytraEvent) {
        event.isWorldRemote = false
    }

    /**
     * Calculate the starting height. Constantly update the position as we would in vanilla until the correct criteria
     * is met. Should only be called from the state NOT_STARTED.
     */
    private fun calcStartingHeight() {
        /* We are in the air at least 0.5 above the ground and have an elytra equipped */
        if (!Globals.mc.player.onGround && Globals.mc.player.inventory.armorInventory[2].item == Items.ELYTRA && (Globals.mc.player.posY - Globals.mc.player.groundPos.y >= jumpHeightMin)) {
            /* Start at pos X.5 or higher */
            if (isJumpStart && (Globals.mc.player.posY - Globals.mc.player.posY.toInt() >= jumpHeightMin)) {
                /* Jumping start */
                currentPhase = MovementState.IDLE
            } else if (!isJumpStart) {
                /* Falling start */
                currentPhase = MovementState.IDLE
            }
        }
    }

    /**
     * @param forceSendPosRot: If we should force send the position and rotation regardless of if it is our current position/rotation
     *
     * Sends a packet salted with onGround == true to allow it to be sent in PacketEvent.Send.
     *
     * By default, send the position and rotation (if this function is called we ALWAYS need to send a packet). If not,
     * send either the position or both the position and rotation, whichever is appropriate.
     */
    private fun sendForcedPacket(forceSendPosRot: Boolean) {
        /* Determine which packet we need to send: position, rotation, or positionRotation */
        if (Globals.mc.player.posX != lastPos.x || Globals.mc.player.posY != lastPos.y || Globals.mc.player.posZ != lastPos.z || forceSendPosRot) {
            if (rotation.x != lastRotation.x || rotation.y != lastRotation.y || forceSendPosRot) {
                /* Position and rotation need to be sent to the server */
                Globals.mc.player.connection.sendPacket(CPacketPlayer.PositionRotation(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ, Globals.mc.player.rotationYaw, Globals.mc.player.rotationPitch, true))
            } else {
                /* Position needs to be sent to the server */
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ, true))
            }
        } else {
            /* Position and rotation need to be sent to the server */
            Globals.mc.player.connection.sendPacket(CPacketPlayer.PositionRotation(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ, Globals.mc.player.rotationYaw, Globals.mc.player.rotationPitch, true))
        }

        lastRotation = rotation
        lastPos = Globals.mc.player.positionVector
    }

    /*
    private fun setSpeed(yaw: Double, boosting: Boolean) {
        val acceleratedSpeed = getSpeed(boosting)
        Globals.mc.player.setVelocity(sin(-yaw) * acceleratedSpeed, Globals.mc.player.motionY, cos(yaw) * acceleratedSpeed)
    }

    private fun getSpeed(boosting: Boolean): Double {
        return when {
            boosting -> (if (ncpStrict.value) min(horizontalSpeed.value, 2.0f) else horizontalSpeed.value).toDouble()
            accelerateTime.value != 0.0f && accelerateStartSpeed.value != 100 -> {
                speedPercentage = when {
                    Globals.mc.gameSettings.keyBindSprint.isKeyDown -> 100.0f
                    speedPercentage >= 100.0f -> accelerateStartSpeed.value.toFloat()
                    else -> min(speedPercentage + (100.0f - accelerateStartSpeed.value.toFloat()) / (accelerateTime.value * 20), 100.0f)
                }
                horizontalSpeed.value * (speedPercentage / 100.0) * (cos((speedPercentage / 100.0) * PI) * -0.5 + 0.5)
            }
            else -> horizontalSpeed.value.toDouble()
        }
    }
     */

}