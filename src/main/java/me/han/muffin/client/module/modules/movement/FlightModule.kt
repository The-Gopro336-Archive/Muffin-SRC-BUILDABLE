package me.han.muffin.client.module.modules.movement

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.manager.managers.TimerManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.entity.PlayerUtil
import me.han.muffin.client.utils.math.RandomUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.hypot
import kotlin.math.max

internal object FlightModule: Module("Flight", Category.MOVEMENT, "Allows you to fly in the air.") {

    private val mode = EnumValue(Modes.Vanilla, "Mode")
    private val speed = NumberValue(1.0, 0.0, 20.0, 0.1, "Speed")
    private val glide = Value(false, "Glide")
    private val damageTesting = Value({ mode.value == Modes.Testing }, true, "DamageTesting")
    private val glideSpeed = NumberValue(0.0, 0.0, 10.0, 0.1, "GlideSpeed")
    private val elytraOnly = Value(false, "Elytra")
    private val antiKick = Value(true, "AntiKick")
    private val betterAntiKick = Value(true, "BetterAntiKick")

    private val groundTimer = Timer()
    private var counter = 0
    private var flySpeed = 0.0
    private val timer = Timer()
    private var allowed = false
    private var damageFly = false
    private var lastDist = 0.0
    private var randomValue = 0.0

    var reset = false
    var timerSpeed = 0f
    var lastDisable: Long = 0
    private val spartanTimer = Timer()

    private var lastFlag = 0L

    enum class Modes {
        Vanilla, Creative, Testing, Zone, Zone2, Zone3
    }

    init {
        addSettings(mode, damageTesting, speed, glide, glideSpeed, elytraOnly, antiKick, betterAntiKick)
    }

    override fun getHudInfo(): String {
        return mode.fixedValue
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return
        if (event.packet is SPacketPlayerPosLook) lastFlag = System.currentTimeMillis()
    }

    override fun onEnable() {
        if (fullNullCheck()) return

        if (mode.value == Modes.Testing) {
            if (MovementUtils.isMoving() && System.currentTimeMillis() - lastFlag >= 5000 && !Globals.mc.gameSettings.keyBindSprint.isKeyDown && System.currentTimeMillis() - lastDisable > 1500L) {
                damageFly = true
                allowed = !allowed
            } else {
                damageFly = false
            }
            counter = 0
            flySpeed = 0.0
            timer.reset()
            randomValue = 0.001111111111111
        }

        if (mode.value == Modes.Zone3) {
            val x = Globals.mc.player.posX
            val y = Globals.mc.player.posY
            val z = Globals.mc.player.posZ
            for (i in 0..64) {
                Globals.mc.connection?.sendPacket(CPacketPlayer.Position(x, y + 0.049, z, false))
                Globals.mc.connection?.sendPacket(CPacketPlayer.Position(x, y, z, false))
            }
            Globals.mc.connection?.sendPacket(CPacketPlayer.Position(x, y + 0.1, z, true))
            Globals.mc.player.motionX *= 0.1
            Globals.mc.player.motionZ *= 0.1
            Globals.mc.player.swingArm(EnumHand.MAIN_HAND)
        }
    }

    override fun onDisable() {
        if (fullNullCheck()) return
        TimerManager.resetTimer()

        if (mode.value == Modes.Testing && reset) Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY - randomValue, Globals.mc.player.posZ);
    }

    @Listener
    private fun onMove(event: MoveEvent) {

        if (betterAntiKick.value && Globals.mc.player.ticksExisted % 78 == 0) { //Vanilla will kick you after 80 ticks, so use 78 to be safe as if a player is lagging 79 might still kick
            MovementUtils.fallPacket()
            MovementUtils.ascendPacket()
        }

        if (elytraOnly.value && !Globals.mc.player.isElytraFlying) return

        if (mode.value == Modes.Testing && damageTesting.value) {
            Globals.mc.player.onGround = true
            if (Globals.mc.player.ticksExisted % 10 == 0 && MovementUtils.isMoving()) Globals.mc.player.cameraYaw = 0.16f

            when (counter) {
                0 -> if (timer.passed(if (allowed) 250.0 else 150.0)) {
                    PlayerUtil.damageHypixel()
                    flySpeed = MovementUtils.getBaseMoveSpeed() * if (allowed) 1.25 else 1.25
                    timer.reset()
                    counter = 1
                } else {
                    flySpeed = 0.0
                    event.x = (0.0.also { Globals.mc.player.motionX = it })
                    event.y = (0.0.also { Globals.mc.player.motionY = it })
                    event.z = (0.0.also { Globals.mc.player.motionZ = it })
                }
                1 -> {
                    flySpeed *= 2.14999
                    event.y = (0.41999998688697815.also { Globals.mc.player.motionY = it })
                    counter = 2
                }
                2 -> {
                    flySpeed = if (allowed) 1.37 else 1.42
                    counter = 3
                }
                else -> {
                    if (counter > 10) {
                        if (timerSpeed > 1.0) {
                            timerSpeed -= 0.055f
                            TimerManager.setTimer(timerSpeed)
                        } else {
                            TimerManager.resetTimer()
                        }
                    } else if (counter == 9) {
                        // timerSpeed = 2.6f;
                        timerSpeed = 1.4f
                    }
                    if (Globals.mc.player.collidedHorizontally) {
                        TimerManager.resetTimer()
                        flySpeed *= .5
                    }
                    flySpeed -= flySpeed / 159
                    counter++
                }
            }

            MovementUtils.setMoveMotion(event, if (flySpeed == 0.0) 0.0 else max(flySpeed, MovementUtils.getBaseMoveSpeed()))
        }

        if (mode.value == Modes.Creative) {
            val modifiedSpeed = speed.value / 10.0
            if (Globals.mc.player.movementInput.jump) {
                Globals.mc.player.motionY = modifiedSpeed
                event.y = Globals.mc.player.motionY
            } else {
                if (Globals.mc.player.movementInput.sneak) {
                    Globals.mc.player.motionY = -modifiedSpeed
                    event.y = Globals.mc.player.motionY
                } else {
                    Globals.mc.player.motionY = 0.0
                    event.y = 0.0
                    if (!Globals.mc.player.collidedVertically && glide.value) {
                        Globals.mc.player.motionY -= glideSpeed.value
                        event.y = Globals.mc.player.motionY
                    }
                }
            }
            MovementUtils.setMoveMotion(event, modifiedSpeed)
        }
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return
        if (elytraOnly.value && !Globals.mc.player.isElytraFlying) return

        if (mode.value == Modes.Zone) {
            Globals.mc.player.motionY = 0.0
            if (spartanTimer.passedTicks(12)) {
                Globals.mc.connection?.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + 8, Globals.mc.player.posZ, true))
                Globals.mc.connection?.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY - 8, Globals.mc.player.posZ, true))
                spartanTimer.reset()
            }
        }

        if (mode.value == Modes.Zone2) {
            MovementUtils.strafe(0.264)
            if (Globals.mc.player.ticksExisted % 8 == 0) Globals.mc.connection?.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + 10, Globals.mc.player.posZ, true))
        }

        if (mode.value == Modes.Zone3) {
            Globals.mc.player.capabilities.isFlying = false
            Globals.mc.player.setVelocity(0.0, 0.0, 0.0)

            if (Globals.mc.gameSettings.keyBindJump.isKeyDown) Globals.mc.player.motionY += speed.value
            if (Globals.mc.gameSettings.keyBindSneak.isKeyDown) Globals.mc.player.motionY -= speed.value
            MovementUtils.strafe(speed.value.toDouble())
        }

        if (mode.value == Modes.Testing) {
            Globals.mc.player.onGround = true
            val xDist = Globals.mc.player.posX - Globals.mc.player.prevPosX
            val zDist = Globals.mc.player.posZ - Globals.mc.player.prevPosZ
            lastDist = hypot(xDist, zDist)

            if (counter > 1 || !damageFly) {
                Globals.mc.player.motionY = 0.0
                if (Globals.mc.player.ticksExisted % 2 == 0) {
                    reset = true
                    Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + randomValue, Globals.mc.player.posZ)
                } else {
                    reset = false
                }
                if (!MovementUtils.isMoving()) {
                    val speed = 0.1
                    MovementUtils.strafe(speed)
                }
                if (Globals.mc.player.ticksExisted % 5 == 0) {
                    randomValue += RandomUtils.nextDouble(-0.000009, 0.000009)
                }
            }
        }

        if (mode.value == Modes.Vanilla) {
            Globals.mc.player.capabilities.isFlying = false
            Globals.mc.player.setVelocity(0.0, 0.0, 0.0)
            if (Globals.mc.gameSettings.keyBindJump.isKeyDown) Globals.mc.player.motionY += speed.value
            if (Globals.mc.gameSettings.keyBindSneak.isKeyDown) Globals.mc.player.motionY -= speed.value
            if (!Globals.mc.player.collidedVertically && glide.value) Globals.mc.player.motionY -= glideSpeed.value
            MovementUtils.strafe(speed.value.toDouble())
            handleVanillaKickBypass()
        }

        if (mode.value != Modes.Vanilla && antiKick.value && Globals.mc.player.ticksExisted % 4 == 0) Globals.mc.player.motionY -= 0.04
    }

    private fun calculateGround(): Double {
        val playerBoundingBox = Globals.mc.player.entityBoundingBox
        var blockHeight = 1.0

        var ground = Globals.mc.player.posY
        while (ground > 0.0) {
            val customBox = AxisAlignedBB(playerBoundingBox.maxX, ground + blockHeight, playerBoundingBox.maxZ, playerBoundingBox.minX, ground, playerBoundingBox.minZ)
            if (Globals.mc.world.checkBlockCollision(customBox)) {
                if (blockHeight <= 0.05) return ground + blockHeight
                ground += blockHeight
                blockHeight = 0.05
            }
            ground -= blockHeight
        }

        return 0.0
    }

    private fun handleVanillaKickBypass() {
        if (!antiKick.value || !groundTimer.passed(1000.0)) return
        val ground = calculateGround()

        while (Globals.mc.player.posY > ground) {
            Globals.mc.connection?.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ, true))
            if (Globals.mc.player.posY - 8.0 < ground) break // Prevent next step
            Globals.mc.player.posY -= 8.0
        }
        Globals.mc.connection?.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, ground, Globals.mc.player.posZ, true))

        var posY = ground
        while (posY < Globals.mc.player.posY) {
            Globals.mc.connection?.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, posY, Globals.mc.player.posZ, true))
            if (posY + 8.0 > Globals.mc.player.posY) break // Prevent next step
            posY += 8.0
        }

        Globals.mc.connection?.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ, true))
        groundTimer.reset()
    }

}