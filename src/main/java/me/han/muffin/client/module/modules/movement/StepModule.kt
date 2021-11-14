package me.han.muffin.client.module.modules.movement

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.*
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.manager.managers.TimerManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.misc.TimerModule
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.stats.StatList
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object StepModule: Module("Step", Category.MOVEMENT, "Step up blocks.") {
    private val mode = EnumValue(Mode.New, "Mode")
    private val disable = Value(false, "Disable")
    private val height = NumberValue(2.5f, 1.0f, 4f, 0.1f, "Height")
    private val timer = Value(false, "Timer")
    private val entityStep = Value(false, "EntityStep")

    // NEW MODE //
    private val newTimer = Timer()
    private var fix = 0
    private var oldY = 0.0

    private var ticksCount = 0
    private var timerSpeed = 1.0F

    private val stepTimer = Timer()

    private val halfStep = doubleArrayOf(0.39, 0.6938)
    private val oneStep = doubleArrayOf(0.41999998688698, 0.7531999805212)
    private val oneHalfStep = doubleArrayOf(0.41999998688698, 0.7531999805212, 1.00133597911214, 1.16610926093821, 1.24918707874468, 1.1707870772188)
    private val twoStep = doubleArrayOf(0.42, 0.78, 0.63, 0.51, 0.90, 1.21, 1.45, 1.43)
    private val twoHalfStep = doubleArrayOf(0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.907)
    private val threeStep = doubleArrayOf(0.42, 0.78, 0.63, 0.51, 0.9, 1.21, 1.45, 1.43, 1.78, 1.63, 1.51, 1.9, 2.21, 2.45, 2.43)
    private val fourStep = doubleArrayOf(0.42, 0.78, 0.63, 0.51, 0.9, 1.21, 1.45, 1.43, 1.78, 1.63, 1.51, 1.9, 2.21, 2.45, 2.43, 2.78, 2.63, 2.51, 2.9, 3.21, 3.45, 3.43)

    private enum class Mode {
        Old, New, Vanilla
    }

    init {
        addSettings(mode, disable, height, timer, entityStep)
    }

    override fun getHudInfo(): String {
        return if (mode.value == Mode.Vanilla) "Vanilla" else "Packet"
    }

    override fun onDisable() {
        if (fullNullCheck()) return

        ticksCount = 0
        timerSpeed = 1F

        Globals.mc.player.stepHeight = 0.6F
        if (TimerModule.isDisabled) TimerManager.resetTimer()
        Globals.mc.player.ridingEntity?.let { it.stepHeight = 1.0F }
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (event.packet is SPacketPlayerPosLook) stepTimer.reset()
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (Globals.mc.player.ridingEntity != null) stepTimer.reset()

        if (timer.value && ticksCount == 0 && TimerModule.isDisabled) TimerManager.resetTimer()
    }

    @Listener
    private fun onStepLanding(event: LandStepEvent) {
        Globals.mc.player.ridingEntity?.let {
            it.stepHeight = if (entityStep.value) height.value else 1.0f
        }
    }

    @Listener
    private fun onStep(event: StepEvent) {
        if (fullNullCheck() || !canStep()) return

        if (mode.value == Mode.New) {
            if (event.stage == EventStageable.EventStage.PRE) {
                if (fix == 0) {
                    oldY = Globals.mc.player.posY
                    event.height = height.value
                }
            } else if (event.stage == EventStageable.EventStage.POST) {
                val offset = Globals.mc.player.entityBoundingBox.minY - oldY

                if (offset > 0.6 && fix == 0 && canStep() && newTimer.passed(65.0)) {
                    fakeJump()
                    if (disable.value) {
                        disable()
                        return
                    }

                    timerSpeed = if (offset > 1) 0.15F else 0.35F

                    if (height.value >= 4.0 && offset == 4.0) {
                        for (yPos in fourStep) Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + yPos, Globals.mc.player.posZ, Globals.mc.player.onGround))
                        Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + 4.0, Globals.mc.player.posZ)
                        if (timer.value) ++ticksCount
                   }

                    if (height.value >= 3.0 && offset == 3.0) {
                        for (yPos in threeStep) Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + yPos, Globals.mc.player.posZ, Globals.mc.player.onGround))
                        Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + 3.0, Globals.mc.player.posZ)
                        if (timer.value) ++ticksCount
                    }

                    if (height.value >= 2.5 && offset == 2.5) {
                        for (yPos in twoHalfStep) Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + yPos, Globals.mc.player.posZ, Globals.mc.player.onGround))
                        Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + 2.5, Globals.mc.player.posZ)
                        if (timer.value) ++ticksCount
                    }

                    if (height.value >= 2.0 && offset == 2.0) {
                        for (yPos in twoStep) Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + yPos, Globals.mc.player.posZ, Globals.mc.player.onGround))
                        Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + 2.0, Globals.mc.player.posZ)
                        if (timer.value) ++ticksCount
                    }

                    if (height.value >= 1.5 && offset == 1.5) {
                        for (yPos in oneHalfStep) Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + yPos, Globals.mc.player.posZ, Globals.mc.player.onGround))
                        Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + 1.5, Globals.mc.player.posZ)
                        if (timer.value) ++ticksCount
                    }

                    if (height.value >= 1.0 && offset == 1.0) {
                        for (yPos in oneStep) Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + yPos, Globals.mc.player.posZ, Globals.mc.player.onGround))
                        Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + 1.0, Globals.mc.player.posZ)
                        if (timer.value) ++ticksCount
                    }

                    if (height.value >= 0.5 && offset == 0.6) {
                        for (yPos in halfStep) Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + yPos, Globals.mc.player.posZ, Globals.mc.player.onGround))
                        Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + 0.6, Globals.mc.player.posZ)
                        if (timer.value) ++ticksCount
                    }

                    if (timer.value) {
                        ++ticksCount
                        if (ticksCount >= 2) {
                            TimerManager.setTimer(timerSpeed)
                            ticksCount = 0
                        }
                    }

                    fix = 2
                }
            }

        } else if (mode.value == Mode.Vanilla) {
            if (event.stage == EventStageable.EventStage.PRE) {
                event.height = height.value
            }
        }
    }

    private fun canStep(): Boolean {
        if (mode.value != Mode.Vanilla && SpeedModule.isEnabled) return false

        return MovementUtils.isMoving() &&
            Globals.mc.player.onGround ||
            !Globals.mc.player.isOnLadder ||
            !Globals.mc.player.isInWater ||
            !Globals.mc.player.isInLava &&
            !EntityUtil.isInWater(Globals.mc.player) &&
            !EntityUtil.isAboveWater(Globals.mc.player) &&
            Globals.mc.player.collidedHorizontally &&
            Globals.mc.player.collidedVertically &&
            !Globals.mc.player.movementInput.jump &&
            Globals.mc.player.fallDistance < 0.1
    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (fullNullCheck()) return

        if (event.stage == EventStageable.EventStage.PRE) {

            if (entityStep.value) {
                Globals.mc.player.ridingEntity?.let {
                    it.stepHeight = height.value
                }
            }

            // if (MovementUtils.isMoving() && PlayerUtil.isPlayerInHole(Globals.mc.player) && !Globals.mc.world.getEntitiesWithinAABBExcludingEntity(Globals.mc.player, Globals.mc.player.getEntityBoundingBox()).isEmpty()) {
            //     disable();
            //     return;
            ///   }
        }

        if (mode.value == Mode.Old) {
            if (event.stage == EventStageable.EventStage.PRE) {
                timerSpeed = 1.0F
                val dir = MovementUtils.getDirectionSpeed(0.1)

                var four = false
                var three = false
                var twoFive = false
                var two = false
                var oneFive = false
                var one = false

                if (Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(dir[0], 4.1, dir[1])).isEmpty() && Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(dir[0], 2.4, dir[1])).isNotEmpty()) {
                    four = true
                }

                if (Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(dir[0], 3.1, dir[1])).isEmpty() && Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(dir[0], 2.4, dir[1])).isNotEmpty()) {
                    three = true
                }

                if (Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(dir[0], 2.6, dir[1])).isEmpty() && Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(dir[0], 2.4, dir[1])).isNotEmpty()) {
                    twoFive = true
                }
                if (Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(dir[0], 2.1, dir[1])).isEmpty() && Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(dir[0], 1.9, dir[1])).isNotEmpty()) {
                    two = true
                }

                if (Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(dir[0], 1.6, dir[1])).isEmpty() && Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(dir[0], 1.4, dir[1])).isNotEmpty()) {
                    oneFive = true
                }

                if (Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(dir[0], 1.0, dir[1])).isEmpty() && Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(dir[0], 0.6, dir[1])).isNotEmpty()) {
                    one = true
                }

                if (!canStep()) return

                if (one && height.value >= 1.0) {
                    for (v in oneStep) Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + v, Globals.mc.player.posZ, Globals.mc.player.onGround))
                    Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + 1.0, Globals.mc.player.posZ)
                    timerSpeed = 0.35F
                    if (timer.value) ++ticksCount
                }

                if (oneFive && height.value >= 1.5) {
                    for (v in oneHalfStep) Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + v, Globals.mc.player.posZ, Globals.mc.player.onGround))
                    Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + 1.5, Globals.mc.player.posZ)
                    timerSpeed = 0.15F
                    if (timer.value) ++ticksCount
                }

                if (two && height.value >= 2.0) {
                    for (v in twoStep) Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + v, Globals.mc.player.posZ, Globals.mc.player.onGround))
                    Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + 2.0, Globals.mc.player.posZ)
                    timerSpeed = 0.15F
                    if (timer.value) ++ticksCount
                }

                if (twoFive && height.value >= 2.5) {
                    for (v in twoHalfStep) Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + v, Globals.mc.player.posZ, Globals.mc.player.onGround))
                    Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + 2.5, Globals.mc.player.posZ)
                    timerSpeed = 0.15F
                    if (timer.value) ++ticksCount
                }

                if (three && height.value >= 3.0) {
                    for (yPos in threeStep) Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + yPos, Globals.mc.player.posZ, Globals.mc.player.onGround))
                    Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + 3.0, Globals.mc.player.posZ)
                    timerSpeed = 0.15F
                    if (timer.value) ++ticksCount
                }

                if (four && height.value >= 4.0) {
                    for (yPos in fourStep) Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + yPos, Globals.mc.player.posZ, Globals.mc.player.onGround))
                    Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + 4.0, Globals.mc.player.posZ)
                    timerSpeed = 0.15F
                    if (timer.value) ++ticksCount
                }

                if (timer.value) {
                    ++ticksCount
                    if (ticksCount >= 2) {
                        TimerManager.setTimer(timerSpeed)
                        ticksCount = 0
                    }
                }
            }
        } else if (mode.value == Mode.Vanilla) {
            Globals.mc.player.stepHeight = if (canStep()) height.value else 0.6F
        }
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (mode.value == Mode.New) {
            if (event.stage == EventStageable.EventStage.PRE) {
                if (event.location.y - event.location.oldY >= 0.75 && Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.expand(0.0, -0.1, 0.0)).isNotEmpty())
                    newTimer.reset()
                if (fix > 0) {
                 //   Globals.mc.player.isSprinting = false;
                    fix--
                }
            }
        }
    }

    // There could be some anti cheats which tries to detect step by checking for achievements and stuff
    private fun fakeJump() {
        Globals.mc.player.isAirBorne = true
        Globals.mc.player.addStat(StatList.JUMP, 1)
    }


}