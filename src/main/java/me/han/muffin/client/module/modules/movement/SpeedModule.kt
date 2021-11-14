package me.han.muffin.client.module.modules.movement

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.JumpEvent
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.manager.managers.TimerManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.misc.TimerModule
import me.han.muffin.client.utils.block.BlockUtil
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.entity.MovementUtils.speed
import me.han.muffin.client.utils.extensions.kotlin.ceilToInt
import me.han.muffin.client.utils.extensions.kotlin.floorToInt
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.extensions.mixin.entity.isInWeb
import me.han.muffin.client.utils.extensions.mixin.entity.speedInAir
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.init.Blocks
import net.minecraft.init.MobEffects
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

internal object SpeedModule: Module("Speed", Category.MOVEMENT, "Allow you to control your movement.") {
    val mode = EnumValue(Mode.Strafe, "Mode")
    private val vanillaSpeed = NumberValue({ mode.value == Mode.Vanilla }, 2.0f, 0.1f, 10.0f, 0.1f, "VanillaSpeed")
    private val testStrict = Value(true, "TestStrict")
    private val strictLevel = NumberValue(1, 1, 4, 1, "StrictLevel")
    private val resolveLagBack = EnumValue(LagBackSelection.Punch, "ResolveLagBack")
    private val strictY = Value(true, "StrictY")
    private val autoJump = Value(true, "AutoJump")
    private val speedWhenSneak = Value(false, "SpeedWhenSneak")
    private val useTimer = Value(false, "UseTimer")
    private val strictTimer = Value(false, "StrictTimer")
    private val strictAccelerate = Value({ mode.value == Mode.Strafe || mode.value == Mode.TestStrafe || mode.value == Mode.StrafeStrict },false, "StrictAccelerate")
    private val speedInWater = Value(false, "SpeedInWater")

    private val bypass = Value(false, "Bypass")
    private val zoomBoost = NumberValue(1.0, 0.0, 5.0, 0.1, "ZoomBoost")
    private val airBoost = Value(false, "AirBoost")
    private val stepHeightAddon = NumberValue({ mode.value == Mode.YPortTest }, 1.5F, 0.0F, 3.0F, 0.1F, "StepHeightAddon")
    private val speedValue = NumberValue({ mode.value != Mode.Vanilla && mode.value != Mode.MiniHop && mode.value != Mode.YPortTest }, 1.533, 1.000, 2.5000, 0.001, "Speed")
    private val speedAddon = NumberValue({ mode.value == Mode.YPortTest || mode.value == Mode.MiniHop }, 3.2, 0.1, 10.0, 0.1, "SpeedAddon")

    private var moveSpeed = 0.0
    private var lastDist = 0.0

    private var baseMoveSpeed = 0.0

    var stage = 0

    var prevOnGround = false
    private var timerDelay = 0
    private var ncpTimer = 0L
    private var boost = true

    private var canBoostStrafe = false

    private val bHopTimer = Timer()
    private var doSlow = false

    private var shouldPause = false

    private val startTimer = Timer()
    private val gayHopTimer = Timer()

    enum class Mode {
        Vanilla, BHop, LowHop, MiniHop, Strafe, Alerithe, TestStrafe, StrafeStrict, NCPHop, GayHop, OnGround, YPortTest
    }

    private enum class LagBackSelection {
        Off, Ground, Punch
    }

    init {
        addSettings(
            mode, vanillaSpeed,
            resolveLagBack,
            testStrict, strictLevel, strictY,
            autoJump, speedWhenSneak,
            useTimer, strictTimer, strictAccelerate,
            speedInWater, bypass, speedValue, speedAddon, stepHeightAddon, zoomBoost, airBoost
        )
    }

    override fun getHudInfo(): String = mode.value.toString()

    override fun onEnable() {
        if (fullNullCheck()) return

        TimerManager.resetTimer()

        moveSpeed = MovementUtils.getBaseMoveSpeed()
        lastDist = 0.0
        baseMoveSpeed = MovementUtils.getBaseMoveSpeed()

        prevOnGround = true

        if (mode.value == Mode.Strafe) stage = 0
        if (mode.value == Mode.OnGround) stage = 2
        if (mode.value == Mode.TestStrafe || mode.value == Mode.StrafeStrict) stage = 4

        timerDelay = 0
    }

    override fun onDisable() {
        if (fullNullCheck()) return

        Globals.mc.player.stepHeight = 0.6F
        Globals.mc.player.jumpMovementFactor = 0.02F
        moveSpeed = MovementUtils.getBaseMoveSpeed()
        Globals.mc.player.speedInAir = 0.02F
        TimerManager.resetTimer()
    }

    @Listener
    private fun onMove(event: MoveEvent) {
        if (shouldPause()) {
            moveSpeed = 0.0
            shouldPause = true
            return
        }

        if (shouldPause) {
            moveSpeed = 0.0
            shouldPause = false
            return
        }

        ++timerDelay
        timerDelay %= 5

        if (mode.value == Mode.Vanilla) {
            if (autoJump.value && MovementUtils.isMoving() && Globals.mc.player.onGround) {
                Globals.mc.player.motionY = MovementUtils.getJumpBoostModifier(if (strictY.value) 0.399399995803833 else 0.41999998688697815)
                event.y = Globals.mc.player.motionY
            }
            MovementUtils.setMoveMotionNew(event, vanillaSpeed.value / 10.toDouble())
        }

        if (mode.value == Mode.BHop) {
            if (timerDelay != 0) {
                TimerManager.resetTimer()
            } else if (MovementUtils.isMoving()) {
                TimerManager.setTimer(if (strictTimer.value) 1.0888F else 1.3F)
                event.x = (1.0199999809265137.let { Globals.mc.player.motionX *= it; Globals.mc.player.motionX })
                event.z = (1.0199999809265137.let { Globals.mc.player.motionZ *= it; Globals.mc.player.motionZ })
            }

            if (MathUtils.round(Globals.mc.player.posY - Globals.mc.player.posY.toInt().toDouble(), 3) == MathUtils.round(0.138, 3)) {
                Globals.mc.player.motionY -= 0.08 + MovementUtils.getJumpBoostModifier()
                event.y -= 0.09316090325960147 + MovementUtils.getJumpBoostModifier()
                Globals.mc.player.posY -= 0.09316090325960147 + MovementUtils.getJumpBoostModifier()
            }

            if (Globals.mc.player.onGround && MovementUtils.isMoving() && bHopTimer.passed(300.0)) {
                Globals.mc.player.motionY =  MovementUtils.getJumpBoostModifier(if (strictY.value) 0.399399995803833 else 0.41999998688697815)
                event.y = Globals.mc.player.motionY
                moveSpeed = baseMoveSpeed * speedValue.value //* 1.901
                doSlow = true
                bHopTimer.reset()
            } else {
                if (doSlow || Globals.mc.player.collidedHorizontally) {
                    moveSpeed -= 0.7 * baseMoveSpeed.also { moveSpeed = it }
                    doSlow = false
                } else {
                    moveSpeed -= moveSpeed / 159.0
                }
            }

            moveSpeed = max(moveSpeed, baseMoveSpeed)
            MovementUtils.setMoveMotionNew(event, moveSpeed)
        }

        if (mode.value == Mode.Strafe) {
            if (strictLevel.value == 2 && Globals.mc.player.fallDistance > 4.0f) return

            if (timerDelay != 0) {
                TimerManager.resetTimer()
            } else if (MovementUtils.isMoving()) {
                TimerManager.setTimer(if (strictTimer.value) 1.0888F else 1.3F)
                Globals.mc.player.motionX *= 1.0199999809265137
                Globals.mc.player.motionX *= 1.0199999809265137
            }

            if (Globals.mc.player.onGround && MovementUtils.isMoving()) {
                stage = 2
            }

            if (strictLevel.value != 2 && MathUtils.round(Globals.mc.player.posY - Globals.mc.player.posY.toInt().toDouble(), 3) == MathUtils.round(0.138, 3)) {
                Globals.mc.player.motionY -= 0.08
                event.y -= 0.09316090325960147
                Globals.mc.player.posY -= 0.09316090325960147
            } else if (strictLevel.value == 2 && MathUtils.round(Globals.mc.player.posY - Globals.mc.player.posY.toInt().toDouble(), 3) == MathUtils.round(0.138, 3)) {
                Globals.mc.player.motionY -= 1.0
                event.y -= 0.09316090325960147
                Globals.mc.player.posY -= 0.09316090325960147
            }

            if (stage == 1 && Globals.mc.player.collidedVertically && (Globals.mc.player.moveForward != 0.0f || Globals.mc.player.moveStrafing != 0.0f)) {
                moveSpeed = 1.35 * baseMoveSpeed - 0.01
            } else if (stage == 2 && MovementUtils.isMoving() && Globals.mc.player.collidedVertically && !EntityUtil.isInWater(Globals.mc.player) && Globals.mc.player.onGround) {
                if (autoJump.value) {
                    handleYHeight(event)
                } else if (Globals.mc.gameSettings.keyBindJump.isKeyDown) {
                    handleYHeight(event)
                }
            } else if (stage == 3) {
                val difference = 0.66 * (lastDist - baseMoveSpeed)
                moveSpeed = lastDist - difference

                if (strictLevel.value != 1 && strictLevel.value != 4 && moveSpeed < 0.3) moveSpeed = 0.3
                if (strictLevel.value == 2 && useTimer.value) TimerManager.setTimer((if (boost) 1.125f else 1.088f))
            } else {
                val collidingList = Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(0.0, Globals.mc.player.motionY, 0.0))
                if ((collidingList.size > 0 || Globals.mc.player.collidedVertically) && stage > 0) {
                    stage = if (MovementUtils.isMoving()) 1 else 0
                }
                moveSpeed = if (strictAccelerate.value) {
                    MovementUtils.calculateFriction(moveSpeed, lastDist, baseMoveSpeed)
                } else {
                    lastDist - lastDist / 159.0
                }
            }

            moveSpeed = max(moveSpeed, baseMoveSpeed)

            when (strictLevel.value) {
                2 -> moveSpeed = max(moveSpeed, baseMoveSpeed)
                3 ->  moveSpeed = min(moveSpeed, if (Globals.mc.player.isPotionActive(MobEffects.SPEED)) 0.718 else 0.573) // 0.551
                4 -> {
                    if (System.currentTimeMillis() - ncpTimer > 2500L) ncpTimer = System.currentTimeMillis()
                    moveSpeed = min(moveSpeed, if (System.currentTimeMillis() - ncpTimer > 1250L) 0.455 else 0.44)
                }
            }

            if (Globals.mc.player.hurtTime > 7 && zoomBoost.value > 0.0) moveSpeed = max(moveSpeed + zoomBoost.value.div(10.0), baseMoveSpeed)

            if (stage > 0) {
                if (EntityUtil.isInWater(Globals.mc.player)) moveSpeed = 0.1
                MovementUtils.setMoveMotionNew(event, moveSpeed)
                if (airBoost.value) Globals.mc.player.jumpMovementFactor = 0.029F
            }

            if (MovementUtils.isMoving()) ++stage
        }

        if (mode.value == Mode.TestStrafe || mode.value == Mode.StrafeStrict) {
            if (Globals.mc.player.moveForward == 0.0F && Globals.mc.player.moveStrafing == 0.0F) return

            if (useTimer.value) TimerManager.setTimer(1.0888F)

            if (stage == 1 && (Globals.mc.player.moveForward != 0.0f || Globals.mc.player.moveStrafing != 0.0f)) {
                moveSpeed = 1.35 * baseMoveSpeed - 0.01
            } else if (stage == 2 && (Globals.mc.player.moveForward != 0.0f || Globals.mc.player.moveStrafing != 0.0f)) {
                Globals.mc.player.motionY = MovementUtils.getJumpBoostModifier(if (BlockUtil.isCollidingWithTop()) if (strictY.value) 0.399399995803833 else 0.41999998688697815 else if (strictY.value) 0.399399995803833 else 0.41999998688697815)
                event.y = Globals.mc.player.motionY

                if (strictAccelerate.value && mode.value == Mode.StrafeStrict) {
                    moveSpeed = min(2.14 * baseMoveSpeed, max(baseMoveSpeed * 1.78, lastDist * 1.78))
                } else {
                    moveSpeed *= if (mode.value == Mode.TestStrafe) if (canBoostStrafe) 1.6835 else 1.395
                    else if (mode.value == Mode.StrafeStrict && canBoostStrafe) 1.420 else 1.395
                }

            } else if (stage == 3) {
                val difference = if (strictAccelerate.value && mode.value == Mode.StrafeStrict) {
                        (0.74 + 0.02 * MovementUtils.getJumpEffect()) * (lastDist - baseMoveSpeed)
                    } else {
                        0.66 * (lastDist - baseMoveSpeed)
                    }
                moveSpeed = lastDist - difference
                canBoostStrafe = !canBoostStrafe
            } else {
                val collidingBoxes = Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(0.0, Globals.mc.player.motionY, 0.0))
                if ((collidingBoxes.size > 0 || Globals.mc.player.collidedVertically) && stage > 0) {
                    stage = if (Globals.mc.player.moveForward != 0.0F || Globals.mc.player.moveStrafing != 0.0F) 1 else 0
                }
                moveSpeed = if (strictAccelerate.value) {
                    MovementUtils.calculateFriction(moveSpeed, lastDist, baseMoveSpeed)
                } else {
                    lastDist - lastDist / 159.0
                }

            }

            moveSpeed = max(moveSpeed, baseMoveSpeed)

            if (mode.value == Mode.StrafeStrict && !strictAccelerate.value) {
                if (System.currentTimeMillis() - ncpTimer > 2500L) ncpTimer = System.currentTimeMillis()
                moveSpeed = min(moveSpeed, if (System.currentTimeMillis() - ncpTimer > 1250L) 0.465 else 0.44)
            }

            if (zoomBoost.value > 0.0 && Globals.mc.player.hurtTime > 7) {
                moveSpeed = max(moveSpeed + zoomBoost.value.div(10.0), baseMoveSpeed)
            }

            MovementUtils.setMoveMotionOld(event, moveSpeed)
            if (Globals.mc.player.moveForward != 0.0F || Globals.mc.player.moveStrafing != 0.0F) ++stage
        }

        if (mode.value == Mode.GayHop) {
            if (!gayHopTimer.passed(100.0)) return
            gayHopTimer.reset()

            //if (Globals.mc.player.moveForward == 0.0F && Globals.mc.player.moveStrafing == 0.0F) moveSpeed = baseMoveSpeed

            if (MathUtils.round(Globals.mc.player.posY - Globals.mc.player.posY.toInt().toDouble(), 3) == MathUtils.round(0.4, 3)) {
                event.y = (0.31 + MovementUtils.getJumpBoostModifier()).also { Globals.mc.player.motionY = it }
            } else if (MathUtils.round(Globals.mc.player.posY - Globals.mc.player.posY.toInt().toDouble(), 3) == MathUtils.round(0.71, 3)) {
                event.y = (0.04 + MovementUtils.getJumpBoostModifier()).also { Globals.mc.player.motionY = it }
            } else if (MathUtils.round(Globals.mc.player.posY - Globals.mc.player.posY.toInt().toDouble(), 3) == MathUtils.round(0.75, 3)) {
                event.y = (-0.2 + MovementUtils.getJumpBoostModifier()).also { Globals.mc.player.motionY = it }
            }

            if (Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(0.0, -0.56, 0.0)).size > 0 && MathUtils.round(Globals.mc.player.posY - Globals.mc.player.posY.toInt().toDouble(), 3) == MathUtils.round(0.55, 3)) {
                event.y = (-0.14 + MovementUtils.getJumpBoostModifier()).also { Globals.mc.player.motionY = it }
            }

            /*
            if (stage != 1 || !Globals.mc.player.collidedVertically || (Globals.mc.player.moveForward == 0.0F && Globals.mc.player.moveStrafing == 0.0F)) {
                if (stage == 2 && Globals.mc.player.collidedVertically && (Globals.mc.player.moveForward != 0.0F && Globals.mc.player.moveStrafing != 0.0F)) {
                    event.y = ((if (BlockUtil.isCollidingWithTop()) 0.2 else 0.4) + MovementUtils.getJumpBoostModifier()).also { Globals.mc.player.motionY = it }
                    moveSpeed *= 2.149
                } else if (stage == 3) {
                    val difference = 0.66 * (lastDist - baseMoveSpeed)
                    moveSpeed = lastDist - difference
                } else {
                    if (Globals.mc.player.collidedVertically && stage > 0) {
                        if (1.35 * baseMoveSpeed - 0.01 > )
                    }
                }
            }
             */


            if (timerDelay != 0) {
                TimerManager.resetTimer()
            } else if (MovementUtils.isMoving()) {
                TimerManager.setTimer(if (strictTimer.value) 1.0888F else 1.3F)
                Globals.mc.player.motionX *= 1.0199999809265137
                Globals.mc.player.motionZ *= 1.0199999809265137
            }

            if (Globals.mc.player.onGround && MovementUtils.isMoving()) {
                stage = 2
            }

            if (MathUtils.round(Globals.mc.player.posY - (Globals.mc.player.posY).toInt().toDouble(), 3) == MathUtils.round(0.138, 3)) {
                Globals.mc.player.motionY -= 0.08
                event.y -= - 0.09316090325960147
                Globals.mc.player.posY -= 0.09316090325960147
            }

            if (stage == 1 && MovementUtils.isMoving()) {
                stage = 2
                moveSpeed = 1.38 * baseMoveSpeed - 0.01
            } else if (stage == 2) {
                stage = 3
                Globals.mc.player.motionY = 0.3994
                event.y = Globals.mc.player.motionY
                moveSpeed *= 2.149
            } else if (stage == 3) {
                stage = 4
                val difference = 0.66 * (lastDist - baseMoveSpeed)
                moveSpeed = lastDist - difference
            } else {
                if (Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(0.0, Globals.mc.player.motionY, 0.0)).size > 0 || Globals.mc.player.collidedVertically) {
                    stage = if (MovementUtils.isMoving()) 1 else 0
                }
                moveSpeed = lastDist - lastDist / 159.0
            }

            moveSpeed = max(moveSpeed, baseMoveSpeed)
            MovementUtils.setMoveMotionNew(event, moveSpeed)
        }

        if (mode.value == Mode.OnGround) {
            if (canSpeed() && (Globals.mc.player.onGround || stage == 3)) {
                if (!Globals.mc.player.collidedHorizontally && Globals.mc.player.moveForward != 0.0f || Globals.mc.player.moveStrafing != 0.0f) {
                    if (stage == 2) {
                        //     speed *= 1.64;
                        moveSpeed *= speedValue.value * 1.3
                        stage = 3
                    } else if (stage == 3) {
                        stage = 2
                        val difference = 0.99 * (lastDist - baseMoveSpeed)
                        moveSpeed = lastDist - difference
                    } else {
                        val collidingList = Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(0.0, Globals.mc.player.motionY, 0.0))
                        if (collidingList.size > 0 || Globals.mc.player.collidedVertically) {
                            stage = 1
                        }
                    }
                } else {
                    TimerManager.resetTimer()
                }
                moveSpeed = max(moveSpeed, baseMoveSpeed)
                MovementUtils.setMoveMotionNew(event, moveSpeed)
            }
        }

        if (mode.value == Mode.LowHop) {
            if (Globals.mc.player.onGround && (Globals.mc.player.moveForward != 0.0f || Globals.mc.player.moveStrafing != 0.0f))
                if (useTimer.value) TimerManager.setTimer(1.0888F) else TimerManager.resetTimer()

            when (MathUtils.round(Globals.mc.player.posY - Globals.mc.player.posY.toInt().toDouble(), 3)) {
                MathUtils.round(0.4, 3) -> {
                    Globals.mc.player.motionY = 0.31
                    event.y = 0.31
                }
                MathUtils.round(0.71, 3) -> {
                    Globals.mc.player.motionY = 0.05
                    event.y = 0.05
                }
                MathUtils.round(0.75, 3) -> {
                    Globals.mc.player.motionY = -0.5
                    event.y = -0.5
                }
                MathUtils.round(0.55, 3) -> {
                    Globals.mc.player.motionY = -0.2
                    event.y = -0.2
                }
                MathUtils.round(0.41, 3) -> {
                    Globals.mc.player.motionY = -0.2
                    event.y = -0.2
                }
            }

            if (stage == 1 && (Globals.mc.player.moveForward != 0.0f || Globals.mc.player.moveStrafing != 0.0f)) {
                moveSpeed = 2.0 * baseMoveSpeed - 0.01
            } else if (stage == 2 && (Globals.mc.player.moveForward != 0.0f || Globals.mc.player.moveStrafing != 0.0f)) {
                Globals.mc.player.motionY = 0.4199
                event.y = Globals.mc.player.motionY
                moveSpeed *= if (Globals.mc.player.hurtResistantTime <= 3) 1.47 else 2.1
            } else if (stage == 3) {
                val difference = 0.66 * (lastDist - baseMoveSpeed)
                moveSpeed = lastDist - difference
            } else {
                if (Globals.mc.player.collidedVertically && stage > 0) {
                    stage = if (MovementUtils.isMoving()) 1 else 0
                }
                moveSpeed = lastDist - lastDist / 200.0
            }

            moveSpeed = max(moveSpeed, baseMoveSpeed)
            MovementUtils.setMoveMotion(event, moveSpeed)
            if (MovementUtils.isMoving()) ++stage
        }

    }



    private fun shouldPause(): Boolean {
        if (fullNullCheck()) return true

        if (!speedInWater.value && (Globals.mc.player.isInWater || Globals.mc.player.isInLava || EntityUtil.isInWater(Globals.mc.player) || EntityUtil.isAboveWater(Globals.mc.player))) {
            return true
        }

        if (Globals.mc.player.isOnLadder || Globals.mc.player.isInWeb || Globals.mc.player.isEntityInsideOpaqueBlock) {
            return true
        }

        if (!speedWhenSneak.value && Globals.mc.player.isSneaking) {
            return true
        }

        return false
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (shouldPause()) return

        if (!MovementUtils.isMoveInputKeyDown()) {
            Globals.mc.player.motionX = 0.0
            Globals.mc.player.motionZ = 0.0
            moveSpeed = 0.0
            return
        }

        if (event.stage == EventStageable.EventStage.PRE) {

            lastDist = hypot(Globals.mc.player.posX - Globals.mc.player.lastTickPosX, Globals.mc.player.posZ - Globals.mc.player.lastTickPosZ)
            baseMoveSpeed = MovementUtils.getBaseMoveSpeed()

            if (mode.value == Mode.NCPHop) {
                if (useTimer.value) TimerManager.setTimer(1.0865F)

                if (MovementUtils.isKeyDown()) {
                    if (Globals.mc.player.onGround) {
                        Globals.mc.player.motionY = 0.41999998688697815
                        Globals.mc.player.speedInAir = 0.0223F
                    }
                    MovementUtils.strafe()
                } else {
                    Globals.mc.player.motionX = 0.0
                    Globals.mc.player.motionZ = 0.0
                }
            }

            if (mode.value == Mode.Alerithe) {
                if (!startTimer.passed(100.0)) return

                if (Globals.mc.player.moveForward == 0.0F && Globals.mc.player.moveStrafing == 0.0F) {
                    Globals.mc.player.motionX = 0.0
                    Globals.mc.player.motionZ = 0.0
                    return
                }

                if (!Globals.mc.player.movementInput.jump && Globals.mc.player.onGround) {
                    if (Globals.mc.player.ticksExisted % 2 == 0)
                        event.location.y += (if (BlockUtil.isCollidingWithTop()) 0.2 else 0.424999997 + MovementUtils.getJumpBoostModifier())

                    if (useTimer.value) {
                        val timerSpeed = if (Globals.mc.player.ticksExisted % 3 == 0) 1.3F else 1.0F
                        TimerManager.setTimer(timerSpeed)
                    }
                    Globals.mc.player.motionX *= if (Globals.mc.player.ticksExisted % 2 == 0) 2.0 else 0.705
                    Globals.mc.player.motionZ *= if (Globals.mc.player.ticksExisted % 2 == 0) 2.0 else 0.705
                }

            }

            if (mode.value == Mode.OnGround) {
                if (stage == 3) event.location.y += 0.4
            }

            if (testStrict.value) {
                //     event.location.y = Globals.mc.player.posY + 1.8995E-35
                if (event.location.y % 0.015625 == 0.0) {
                    event.location.y += 5.3424E-4
                    //event.setY(event.getY() + 0.00611131);
                    event.location.isOnGround = false
                }
                if (Globals.mc.player.motionY > 0.3) {
                    event.location.isOnGround = true
                }
            }

            if (mode.value == Mode.YPortTest) {
                if (StepModule.isDisabled && stepHeightAddon.value > 0.0F) Globals.mc.player.stepHeight = 0.6F + stepHeightAddon.value

                if (MovementUtils.isMoving() && !Globals.mc.player.collidedHorizontally) {
                    if (useTimer.value) TimerManager.setTimer(1.0888F)
                    if (Globals.mc.player.onGround) {
                        Globals.mc.player.motionY = 0.41999998688697815
                        MovementUtils.strafe(Globals.mc.player.speed + speedAddon.value.div(10))
                    } else {
                        Globals.mc.player.motionY = -1.0
                        TimerManager.resetTimer()
                    }
                }
            }

            if (mode.value == Mode.MiniHop) {
                if (MovementUtils.isMoving()) {
                    if (Globals.mc.player.onGround && !Globals.mc.player.movementInput.jump) {
                        Globals.mc.player.motionY += 0.1
                        val multiplier = 1.8
                        Globals.mc.player.motionX *= multiplier
                        Globals.mc.player.motionZ *= multiplier
                        val maxSpeed = 0.66
                        if (Globals.mc.player.speed > maxSpeed) {
                            Globals.mc.player.motionX = Globals.mc.player.motionX / Globals.mc.player.speed * maxSpeed
                            Globals.mc.player.motionZ = Globals.mc.player.motionZ / Globals.mc.player.speed * maxSpeed
                        }
                    }
                    MovementUtils.strafe()
                }
            }

        }
    }


    private fun isValidForPhyCalc(): Boolean {
        val collisionBoxes = Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.grow(0.1, 0.0, 0.1))
        var isStepEnabled = false
        if (StepModule.isEnabled && collisionBoxes.isNotEmpty()) isStepEnabled = true
        return Globals.mc.player.onGround && !isStepEnabled && !EntityUtil.isInWater(Globals.mc.player) && !EntityUtil.isAboveWater(Globals.mc.player)
    }

    @Listener
    private fun onJump(event: JumpEvent) {
        if (
            mode.value == Mode.Alerithe ||
            mode.value == Mode.MiniHop ||
            mode.value == Mode.YPortTest ||
            mode.value == Mode.OnGround ||
            !autoJump.value ||
            !MovementUtils.isMoving() && Globals.mc.gameSettings.keyBindJump.isKeyDown
        ) return
        event.cancel()
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (event.packet is SPacketPlayerPosLook) {
            when (resolveLagBack.value) {
                LagBackSelection.Ground -> Globals.mc.player.connection?.sendPacket(CPacketPlayer(true))
                LagBackSelection.Punch -> {
                    Globals.mc.player.connection?.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                    Globals.mc.player.connection?.sendPacket(CPacketPlayerTryUseItemOnBlock(BlockPos(-1, -1, -1), EnumFacing.DOWN, EnumHand.MAIN_HAND, 0F, 0F, 0F))
                    Globals.mc.player.connection?.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                }
            }

            Globals.mc.player.motionX *= 0.0
            Globals.mc.player.motionZ *= 0.0
            Globals.mc.player.jumpMovementFactor = 0.0F

            if (TimerModule.isDisabled) TimerManager.resetTimer()

            startTimer.reset()
            moveSpeed = MovementUtils.getBaseMoveSpeed()
            lastDist = 0.0
            timerDelay = 0
            stage = if (mode.value == Mode.TestStrafe || mode.value == Mode.StrafeStrict) 4 else -4
        }

    }

    private fun handleYHeight(event: MoveEvent) {
        Globals.mc.player.motionY = MovementUtils.getJumpBoostModifier(if (strictY.value) 0.399399995803833 else 0.41999998688697815)
        event.y = Globals.mc.player.motionY
        Globals.mc.player.isAirBorne = true

        if (!bypass.value) {
            if (strictLevel.value == 2) {
                moveSpeed *= if (boost) 1.685 else 1.533 // + if (Globals.mc.player.hurtTime > 0) zoomBoost.value.div(10) else 0.0
                boost = !boost
                // moveSpeed *= if (boost) 1.685 else 1.533 + if (zoom.value && Globals.mc.player.hurtResistantTime >= 18) 0.45 else 0.0
            } else {
                moveSpeed *= speedValue.value //+ if (Globals.mc.player.hurtTime > 0) zoomBoost.value.div(10) else 0.0
                // moveSpeed *= speedValue.value + if (zoom.value && Globals.mc.player.hurtResistantTime >= 18) 0.41 else 0.0
            }
        }
    }


    private fun canSpeed(): Boolean {
        val blockBelow = BlockPos(Globals.mc.player.posX, Globals.mc.player.posY - 1.0, Globals.mc.player.posZ).block
        return Globals.mc.player.onGround &&
            !Globals.mc.gameSettings.keyBindJump.isPressed &&
            blockBelow != Blocks.STONE_STAIRS &&
            blockBelow != Blocks.OAK_STAIRS &&
            blockBelow != Blocks.SANDSTONE_STAIRS &&
            blockBelow != Blocks.NETHER_BRICK_STAIRS &&
            blockBelow != Blocks.SPRUCE_STAIRS &&
            blockBelow != Blocks.STONE_BRICK_STAIRS &&
            blockBelow != Blocks.BIRCH_STAIRS &&
            blockBelow != Blocks.JUNGLE_STAIRS &&
            blockBelow != Blocks.ACACIA_STAIRS &&
            blockBelow != Blocks.BRICK_STAIRS &&
            blockBelow != Blocks.DARK_OAK_STAIRS &&
            blockBelow != Blocks.QUARTZ_STAIRS &&
            blockBelow != Blocks.RED_SANDSTONE_STAIRS &&
            BlockPos(Globals.mc.player.posX, Globals.mc.player.posY + 2.0, Globals.mc.player.posZ).block == Blocks.AIR
    }

    private fun getBlock(bb: AxisAlignedBB): BlockPos? {
        val y = bb.minY.toInt()
        for (x in bb.minX.floorToInt() until bb.maxX.ceilToInt()) for (z in bb.minZ.floorToInt() until bb.maxZ.ceilToInt()) return BlockPos(x, y, z)
        return null
    }

    private fun getBlock(offset: Double): BlockPos? {
        return getBlock(Globals.mc.player.entityBoundingBox.offset(0.0, offset, 0.0))
    }

}