package me.han.muffin.client.utils.entity

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.utils.extensions.kotlin.toRadian
import net.minecraft.entity.Entity
import net.minecraft.init.MobEffects
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.MovementInput
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.sin

object MovementUtils {
    private val frictionValues = ArrayList<Double>()

    private const val MIN_DIF = 0.001F
    private const val AIR_FRICTION = 0.98F
    private const val WATER_FRICTION = 0.89F
    private const val LAVA_FRICTION = 0.535F

    const val BUNNY_SLOPE = 0.66F
    const val SPRINTING_MOD = 1.3F
    const val SNEAK_MOD = 0.3F
    const val ICE_MOD = 2.5
    const val VANILLA_JUMP_HEIGHT = 0.42F
    const val WALK_SPEED = 0.221F
    private const val SWIM_MOD = 0.5203619984250619
    private val DEPTH_STRIDER_VALUES = doubleArrayOf(1.0, 1.4304347400741908, 1.7347825295420374, 1.9217391028296074)
    const val MAX_DIST = 2.149000095319934
    const val BUNNY_DIV_FRICTION = 159.9999985
    const val GRAVITY_MAX = 0.0834
    const val GRAVITY_MIN = 0.0624
    const val GRAVITY_SPAN = 0.021000000000000005
    const val GRAVITY_ODD = 0.05
    const val GRAVITY_VACC = 0.03744f

    val Entity.isMovingSpeed get() = speed > 0.0001
    val Entity.speed get() = hypot(motionX, motionZ)
    val Entity.realSpeed get() = hypot(posX - prevPosX, posZ - prevPosZ)

    val isMovingSpeed get() = Globals.mc.player.speed > 0.0001

    val motionSpeed get() = hypot(Globals.mc.player.motionX, Globals.mc.player.motionZ)

    fun calculateFriction(moveSpeed: Double, lastDist: Double, baseMoveSpeedRef: Double): Double {
        frictionValues.clear()
        frictionValues.add(lastDist - lastDist / 159.9999985)
        frictionValues.add(lastDist - (moveSpeed - lastDist) / 33.3)
        val materialFriction = if (Globals.mc.player.isInWater) 0.89F else if (Globals.mc.player.isInLava) 0.535F else 0.98F
        frictionValues.add(lastDist - baseMoveSpeedRef * (1.0 - materialFriction))
        return frictionValues.minOrNull() ?: 0.0
    }

    fun calcMoveYaw(yawIn: Float = Globals.mc.player.rotationYaw, moveForward: Float = roundedForward, moveString: Float = roundedStrafing): Double {
        var strafe = 90 * moveString
        strafe *= if (moveForward != 0F) moveForward * 0.5F else 1F
        var yaw = yawIn - strafe
        yaw -= if (moveForward < 0F) 180 else 0

        return yaw.toRadian().toDouble()
    }

    private val roundedForward get() = getRoundedMovementInput(Globals.mc.player.moveForward)

    private val roundedStrafing get() = getRoundedMovementInput(Globals.mc.player.moveStrafing)

    private fun getRoundedMovementInput(input: Float) = when {
        input > 0f -> 1f
        input < 0f -> -1f
        else -> 0f
    }

    fun setSpeed(speed: Double) {
        val yaw = calcMoveYaw()
        Globals.mc.player.motionX = -sin(yaw) * speed
        Globals.mc.player.motionZ = cos(yaw) * speed
    }

    fun strafe() {
        strafe(Globals.mc.player.speed)
    }

    fun strafe(speed: Double) {
        if (!isMoving()) return
        setSpeed(speed)
    }

    fun forward(length: Double) {
        val yaw = Globals.mc.player.rotationYaw.toRadian()
        Globals.mc.player.setPosition(Globals.mc.player.posX + -sin(yaw) * length, Globals.mc.player.posY, Globals.mc.player.posZ + cos(yaw) * length)
    }

    fun getDirectionSpeed(speed: Double): DoubleArray {
        val yaw = calcMoveYaw()
        val sin = -sin(yaw) * speed
        val cos = cos(yaw) * speed
        return doubleArrayOf(sin, cos)
    }

    fun getDirectionSpeedNoForward(speed: Double): DoubleArray {
        var forward = 1f

        if (Globals.mc.gameSettings.keyBindLeft.isPressed || Globals.mc.gameSettings.keyBindRight.isPressed ||
            Globals.mc.gameSettings.keyBindBack.isPressed || Globals.mc.gameSettings.keyBindForward.isPressed)
            forward = Globals.mc.player.movementInput.moveForward

        var side = Globals.mc.player.movementInput.moveStrafe
        var yaw = Globals.mc.player.rotationYaw

        if (forward != 0.0f) {
            if (side > 0.0f) {
                yaw += (if (forward > 0.0f) -45 else 45).toFloat()
            } else if (side < 0) {
                yaw += (if (forward > 0.0f) 45 else -45).toFloat()
            }
            side = 0f
            if (forward > 0.0f) {
                forward = 1f
            } else if (forward < 0) {
                forward = -1f
            }
        }
        val sin = sin((yaw + 90.0).toRadian())
        val cos = cos((yaw + 90.0).toRadian())
        val posX = forward * speed * cos + side * speed * sin
        val posZ = forward * speed * sin - side * speed * cos
        return doubleArrayOf(posX, posZ)
    }

    fun setMoveMotionNew(event: MoveEvent, speed: Double) {
        val movementInput = Globals.mc.player.movementInput
        var forward = movementInput.moveForward.toDouble()
        var strafe = movementInput.moveStrafe.toDouble()
        val yaw = Globals.mc.player.rotationYaw

        if (forward == 0.0 && strafe == 0.0) {
            event.x = 0.0
            event.z = 0.0
        }

        if (forward != 0.0 && strafe != 0.0) {
            forward *= sin(Math.PI / 4)
            strafe *= cos(Math.PI / 4)
        }

        event.x = (forward * speed * -sin(yaw.toRadian()) + strafe * speed * cos(yaw.toRadian())) * 0.99
        event.z = (forward * speed * cos(yaw.toRadian()) - strafe * speed * -sin(yaw.toRadian())) * 0.99
    }

    fun setMoveMotionOld(event: MoveEvent, speed: Double) {
        val movementInput = Globals.mc.player.movementInput
        var forward = movementInput.moveForward
        var strafe = movementInput.moveStrafe
        var yaw = Globals.mc.player.rotationYaw

        if (forward == 0.0f && strafe == 0.0f) {
            event.x = 0.0
            event.z = 0.0
        } else if (forward != 0.0f) {
            if (strafe >= 1.0f) {
                yaw += (if (forward > 0.0f) -45 else 45).toFloat()
            } else if (strafe <= -1.0f) {
                yaw += (if (forward > 0.0f) 45 else -45).toFloat()
            }
            strafe = 0.0F
            if (forward > 0.0f) {
                forward = 1.0f
            } else if (forward < 0.0f) {
                forward = -1.0f
            }
//            if (strafe > 0.0F) {
//                strafe = 1.0F
//            } else if (strafe < 0.0F) {
//                strafe = -1.0F
//            }
        }

    //    event.x = forward * speed * cos((yaw + 88.0).toRadian()) + strafe * speed * sin((yaw + 87.9000015258789).toRadian())
    //    event.z = forward * speed * sin((yaw + 88.0).toRadian()) - strafe * speed * cos((yaw + 87.9000015258789).toRadian())
        event.x = forward * speed * cos((yaw + 90.0).toRadian()) + strafe * speed * sin((yaw + 90.0).toRadian())
        event.z = forward * speed * sin((yaw + 90.0).toRadian()) - strafe * speed * cos((yaw + 90.0).toRadian())
        if (forward == 0.0f && strafe == 0.0f) {
            event.x = 0.0
            event.z = 0.0
        }
    }

    //TODO : STRAFING LEFTRIGHT ISSUE

    fun setMoveMotion(event: MoveEvent, speed: Double) {
        val movementInput = Globals.mc.player.movementInput
        val forward = movementInput.moveForward
        val strafe = movementInput.moveStrafe
        if (forward == 0.0f && strafe == 0.0f) {
            event.x = (0.0.also { Globals.mc.player.motionX = it })
            event.z = (0.0.also { Globals.mc.player.motionZ = it })
        } else {
            val yaw = calcMoveYaw()
            event.x = ((-sin(yaw) * speed).also { Globals.mc.player.motionX = it })
            event.z = ((cos(yaw) * speed).also { Globals.mc.player.motionZ = it })
        }
    }

    fun getBaseMoveSpeed(baseSpeed: Double = 0.2873): Double {
        return Globals.mc.player.getActivePotionEffect(MobEffects.SPEED)?.let {
            baseSpeed * (1.0 + 0.2 * (it.amplifier + 1))
        } ?: baseSpeed
    }

    fun getJumpBoostModifier(baseJumpHeight: Double = 0.0): Double {
        if (EntityUtil.isInWater(Globals.mc.player)) return 0.13500000163912773

        return Globals.mc.player.getActivePotionEffect(MobEffects.JUMP_BOOST)?.let {
            baseJumpHeight + ((it.amplifier + 1.0) * 0.1)
        } ?: baseJumpHeight
    }

    fun isOnGround(height: Double): Boolean {
        return Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(0.0, -height, 0.0)).isNotEmpty()
    }

    fun getSpeedEffect(): Int {
        return Globals.mc.player.getActivePotionEffect(MobEffects.SPEED)?.let {
            it.amplifier + 1
        } ?: 0
    }

    fun getJumpEffect(): Int {
        return Globals.mc.player.getActivePotionEffect(MobEffects.JUMP_BOOST)?.let {
            it.amplifier + 1
        } ?: 0
    }

    fun isMoving() = (Globals.mc.player.movementInput.moveForward != 0f || Globals.mc.player.movementInput.moveStrafe != 0f)
    fun isPlayerMoving() = (Globals.mc.player.moveForward != 0f || Globals.mc.player.moveStrafing != 0f)

    fun isKeyDown() = Globals.mc.gameSettings.keyBindForward.isKeyDown ||
            Globals.mc.gameSettings.keyBindBack.isKeyDown ||
            Globals.mc.gameSettings.keyBindLeft.isKeyDown ||
            Globals.mc.gameSettings.keyBindRight.isKeyDown

    fun isMoveInputKeyDown() =
        Globals.mc.player.movementInput.forwardKeyDown ||
        Globals.mc.player.movementInput.backKeyDown ||
        Globals.mc.player.movementInput.rightKeyDown ||
        Globals.mc.player.movementInput.leftKeyDown

    fun isAllMoveInputKeyDown() =
        Globals.mc.player.movementInput.forwardKeyDown ||
                Globals.mc.player.movementInput.backKeyDown ||
                Globals.mc.player.movementInput.rightKeyDown ||
                Globals.mc.player.movementInput.leftKeyDown ||
                Globals.mc.player.movementInput.jump ||
                Globals.mc.player.movementInput.sneak

    fun hasMotion(): Boolean {
        return Globals.mc.player.motionX != 0.0 && Globals.mc.player.motionZ != 0.0 && Globals.mc.player.motionY != 0.0
    }

    fun fallPacket(): Double {
        var i = Globals.mc.player.posY
        while (i > getGroundLevel()) {
            if (i < getGroundLevel()) {
                i = getGroundLevel()
            }
            Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, i, Globals.mc.player.posZ, true))
            i -= 8.0
        }
        return i
    }

    fun ascendPacket() {
        var i = getGroundLevel()
        while (i < Globals.mc.player.posY) {
            if (i > Globals.mc.player.posY) {
                i = Globals.mc.player.posY
            }
            Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, i, Globals.mc.player.posZ, true))
            i += 8.0
        }
    }

    fun getGroundLevel(): Double {
        for (i in round(Globals.mc.player.posY).toInt() downTo 1) {
            val box = Globals.mc.player.entityBoundingBox.expand(0.0, 0.0, 0.0).contract(0.0, i - 1.0, 0.0).setMaxY(i.toDouble())
            if (!Globals.mc.world.checkBlockCollision(box) || box.minY > Globals.mc.player.posY) {
                continue
            }
            return i.toDouble()
        }
        return 0.0
    }

    fun MovementInput.resetMove() {
        moveForward = 0.0F
        moveStrafe = 0.0F
        forwardKeyDown = false
        backKeyDown = false
        leftKeyDown = false
        rightKeyDown = false
    }

    fun MovementInput.resetJumpSneak() {
        jump = false
        sneak = false
    }

}