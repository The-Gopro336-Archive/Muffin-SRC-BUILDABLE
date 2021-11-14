package me.han.muffin.client.module.modules.movement

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.event.events.client.TravelEvent
import me.han.muffin.client.event.events.movement.SmoothElytraEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.LocalMotionManager
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.manager.managers.SpeedManager.speedKmh
import me.han.muffin.client.manager.managers.TimerManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.entity.MovementUtils.speed
import me.han.muffin.client.utils.extensions.kotlin.toDegree
import me.han.muffin.client.utils.extensions.kotlin.toRadian
import me.han.muffin.client.utils.extensions.mc.entity.groundPos
import me.han.muffin.client.utils.extensions.mc.world.isLiquidBelow
import me.han.muffin.client.utils.extensions.mixin.netty.rotationPitch
import me.han.muffin.client.utils.math.rotation.Vec2f
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.init.Items
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.ItemElytra
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.server.SPacketEntityMetadata
import net.minecraft.network.play.server.SPacketPlayerPosLook
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.*

internal object ElytraFlyModule: Module("ElytraFly", Category.MOVEMENT, "Allows infinite and way easier Elytra flying.") {

    private val mode = EnumValue(ElytraFlightMode.CONTROL, "Mode")
    private val durabilityWarning = Value(true, "DurabilityWarning")
    private val smoothElytra = Value(true, "SmoothElytra")

    private val threshold = NumberValue(5, 1, 50, 1, "Broken%")
    private val autoLanding = Value(false, "AutoLanding")

    private val takeOffMode = EnumValue(TakeOffMode.Timer, "TakeOffMode")
    private val highPingOptimize = Value({ takeOffMode.value != TakeOffMode.Off }, true, "HighPingOptimize")
    private val minTakeoffHeight = NumberValue({ takeOffMode.value != TakeOffMode.Off }, 0.5F, 0.0F, 1.5F, 0.1F, "MinTakeOffHeight")

    private val accelerateStartSpeed = NumberValue({ mode.value != ElytraFlightMode.BOOST }, 100, 0, 100, 1, "StartSpeed")
    private val accelerateTime = NumberValue({ mode.value != ElytraFlightMode.BOOST }, 0.0F, 0.0F, 10.0F, 0.1F, "AccelerateTime")

    private val boostPitch = NumberValue(20, 0, 90, 1, "BaseBoostPitch")
    private val ncpStrict = Value(true, "NCPStrict")
    private val legacyLookBoost = Value(false, "LookBoost")
    private val altitudeHold = Value(false, "AltitudeHold")
    private val dynamicDownSpeed = Value(false, "DynamicDescendSpeed")
    private val horizontalSpeed = NumberValue(1.81F, 0.1F, 20.0F, 0.01F, "HorizontalSpeed")
    private val verticalSpeed = NumberValue({ mode.value == ElytraFlightMode.BOOST || mode.value == ElytraFlightMode.CREATIVE },1.0F, 0.1F, 5.0F, 0.01F, "VerticalSpeed")
    private val fallSpeed = NumberValue(0.00000000000003F, 0.0F, 0.3F, 0.00000000000001F, "FallSpeed")
    private val descendSpeed = NumberValue(1.0F, 0.0F, 5.0F, 0.1F, "MinDescendSpeed")
    private val fastDescendSpeed = NumberValue(2.0F, 0.0F, 5.0F, 0.1F, "MaxDescendSpeed")

    private enum class ElytraFlightMode {
        BOOST, CONTROL, CREATIVE, PACKET, Factorize
    }

    init {
        addSettings(
            mode, smoothElytra, durabilityWarning, threshold,
            takeOffMode, highPingOptimize, minTakeoffHeight,
            accelerateStartSpeed, accelerateTime,
            boostPitch, ncpStrict, legacyLookBoost, altitudeHold, dynamicDownSpeed, horizontalSpeed, verticalSpeed,
            fallSpeed, descendSpeed, fastDescendSpeed
        )
    }

    override fun getHudInfo(): String {
        return mode.fixedValue
    }

    @Listener
    private fun onSmoothElytra(event: SmoothElytraEvent) {
        if (smoothElytra.value) event.isWorldRemote = false
    }

    /* Generic states */
    private var elytraIsEquipped = false
    private var elytraDurability = 0
    private var outOfDurability = false
    private var wasInLiquid = false
    var isFlying = false
    private var isPacketFlying = false
    private var isStandingStillH = false
    private var isStandingStill = false
    private var speedPercentage = 0.0f

    /* Control mode states */
    private var hoverTarget = -1.0
    private var packetYaw = 0.0f
    private var packetPitch = 0.0f
    private var hoverState = false

    private var boostingTick = 0

    private enum class TakeOffMode {
        Off, Normal, Timer
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (Globals.mc.player == null || Globals.mc.player.isSpectator || !elytraIsEquipped || elytraDurability <= 1 || !isFlying || mode.value == ElytraFlightMode.BOOST)
            return

        if (event.stage != EventStageable.EventStage.PRE) return

        if (event.packet is SPacketPlayerPosLook && mode.value != ElytraFlightMode.PACKET) {
            event.packet.rotationPitch = Globals.mc.player.rotationPitch
        }
        if (event.packet is SPacketEntityMetadata && isPacketFlying) {
            if (event.packet.entityId == Globals.mc.player.entityId) event.cancel()
        }

    }

    @Listener
    private fun onTravel(event: TravelEvent) {
        if (fullNullCheck()) return

        stateUpdate(event)

        if (elytraIsEquipped && elytraDurability > 1) {
            if (autoLanding.value) {
                landing(event)
                return
            }
            if (!isFlying && !isPacketFlying) {
                takeoff(event)
            } else {
                TimerManager.resetTimer()
                Globals.mc.player.isSprinting = false
                when (mode.value) {
                    ElytraFlightMode.BOOST -> boostMode()
                    ElytraFlightMode.CONTROL -> controlMode(event)
                    ElytraFlightMode.CREATIVE -> creativeMode()
                    ElytraFlightMode.PACKET -> packetMode(event)
                }
            }
            spoofRotation()
        } else if (!outOfDurability) {
            reset(true)
        }
    }

    @Listener
    private fun onMoving(event: MoveEvent) {
        if (fullNullCheck() || mode.value != ElytraFlightMode.Factorize || Globals.mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).item != Items.ELYTRA) return

        if (elytraIsEquipped && elytraDurability > 1) {
            if (!Globals.mc.player.isElytraFlying || Globals.mc.player.movementInput.jump) {
                return
            }

            spoofRotation()
            if (Globals.mc.player.speedKmh > 180.0) return

            setSpeed(getYaw(), false)
        }  else if (!outOfDurability) {
            reset(true)
        }
    }


    private fun stateUpdate(event: TravelEvent) {
        val armorSlot = Globals.mc.player.inventory.armorInventory[2]
        elytraIsEquipped = armorSlot.item == Items.ELYTRA

        if (elytraIsEquipped) {
            val oldDurability = elytraDurability
            elytraDurability = armorSlot.maxDamage - armorSlot.itemDamage

            if (!Globals.mc.player.onGround && oldDurability != elytraDurability) {
                if (durabilityWarning.value && elytraDurability > 1 && elytraDurability < threshold.value * armorSlot.maxDamage / 100) {
                    Globals.mc.soundHandler.playSound(PositionedSoundRecord.getRecord(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f))
                    ChatManager.sendMessage("$chatName Warning: Elytra has " + (elytraDurability - 1) + " durability remaining")
                } else if (elytraDurability <= 1 && !outOfDurability) {
                    outOfDurability = true
                    if (durabilityWarning.value) {
                        Globals.mc.soundHandler.playSound(PositionedSoundRecord.getRecord(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f))
                        ChatManager.sendMessage("$chatName Elytra is out of durability, holding player in the air")
                    }
                }
            }
        } else elytraDurability = 0

        if (!Globals.mc.player.onGround && elytraDurability <= 1 && outOfDurability) {
            holdPlayer(event)
        } else if (outOfDurability) outOfDurability = false /* Reset if players is on ground or replace with a new elytra */

        if (Globals.mc.player.isInWater || Globals.mc.player.isInLava) {
            wasInLiquid = true
        } else if (Globals.mc.player.onGround || isFlying || isPacketFlying) {
            wasInLiquid = false
        }

        isFlying = Globals.mc.player.isElytraFlying || (Globals.mc.player.capabilities.isFlying && mode.value == ElytraFlightMode.CREATIVE)

        isStandingStillH = Globals.mc.player.movementInput.moveForward == 0f && Globals.mc.player.movementInput.moveStrafe == 0f
        isStandingStill = isStandingStillH && !Globals.mc.player.movementInput.jump && !Globals.mc.player.movementInput.sneak

        if (!isFlying || isStandingStill) speedPercentage = accelerateStartSpeed.value.toFloat()
    }

    private fun reset(cancelFlying: Boolean) {
        wasInLiquid = false
        isFlying = false
        isPacketFlying = false
        TimerManager.resetTimer()
        Globals.mc.player.capabilities.flySpeed = 0.05f
        if (cancelFlying) Globals.mc.player.capabilities.isFlying = false
    }

    private fun holdPlayer(event: TravelEvent) {
        event.cancel()
        TimerManager.resetTimer()
        Globals.mc.player.setVelocity(0.0, -0.01, 0.0)
    }


    private fun landing(event: TravelEvent) {
        when {
            Globals.mc.player.onGround -> {
                ChatManager.sendMessage("$chatName Landed!")
                autoLanding.value = false
                return
            }
            Globals.mc.world.isLiquidBelow(Globals.mc.player) -> {
                ChatManager.sendMessage("$chatName Liquid below, disabling.")
                autoLanding.value = false
            }
            Globals.mc.player.capabilities.isFlying || !Globals.mc.player.isElytraFlying || isPacketFlying -> {
                reset(true)
                takeoff(event)
                return
            }
            else -> {
                when {
                    Globals.mc.player.posY > Globals.mc.player.groundPos.y + 1.0f -> {
                        TimerManager.resetTimer()
                        Globals.mc.player.motionY = max(min(-(Globals.mc.player.posY - Globals.mc.player.groundPos.y) / 20.0, -0.5), -5.0)
                    }
                    Globals.mc.player.motionY != 0.0 -> {
                        if (!Globals.mc.isIntegratedServerRunning)
                            TimerManager.setTimerRaw(200.0F)
                        Globals.mc.player.motionY = 0.0
                    }
                    else -> {
                        Globals.mc.player.motionY = -0.2
                    }
                }
            }
        }
        Globals.mc.player.setVelocity(0.0, Globals.mc.player.motionY, 0.0) /* Kills horizontal motion */
        event.cancel()
    }

    private fun takeoff(event: TravelEvent) {

        val timerSpeed = if (highPingOptimize.value) 400.0f else 200.0f
        val height = if (highPingOptimize.value) 0.0f else minTakeoffHeight.value
        val closeToGround = Globals.mc.player.posY <= Globals.mc.player.groundPos.y + height && !wasInLiquid && !Globals.mc.isIntegratedServerRunning

        if (takeOffMode.value == TakeOffMode.Off || Globals.mc.player.onGround) {
            reset(Globals.mc.player.onGround)
            return
        }

        if (Globals.mc.player.motionY < 0 && !highPingOptimize.value || Globals.mc.player.motionY < -0.02) {
            if (closeToGround) {
                TimerManager.setTimerRaw(25.0f)
                return
            }
            if (!highPingOptimize.value && !wasInLiquid && !Globals.mc.isIntegratedServerRunning) { /* Cringe moment when you use elytra flight in single player world */
                event.cancel()
                Globals.mc.player.setVelocity(0.0, -0.02, 0.0)
            }

            if (takeOffMode.value == TakeOffMode.Timer && !Globals.mc.isIntegratedServerRunning) TimerManager.setTimerRaw(timerSpeed * 2.0f)
            if (highPingOptimize.value) Globals.mc.connection?.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_FALL_FLYING))
            else {
                if (Globals.mc.player.ticksExisted % 5 == 0) Globals.mc.connection?.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_FALL_FLYING))
            }
            hoverTarget = Globals.mc.player.posY + 0.2
        } else if (highPingOptimize.value && !closeToGround) {
            TimerManager.setTimerRaw(timerSpeed)
        }
    }


    /**
     *  Calculate yaw for control and packet mode
     *
     *  @return Yaw in radians based on player rotation yaw and movement input
     */
    private fun getYaw(): Double {
        val yawRad = MovementUtils.calcMoveYaw()
        packetYaw = yawRad.toDegree().toFloat()
        return yawRad
    }

    /**
     * Calculate a speed with a non linear acceleration over time
     *
     * @return boostingSpeed if [boosting] is true, else return a accelerated speed.
     */
    private fun getSpeed(boosting: Boolean): Double {
        return when {
            boosting -> (if (ncpStrict.value) min(horizontalSpeed.value, 2.0f) else horizontalSpeed.value).toDouble()

            accelerateTime.value != 0.0f && accelerateStartSpeed.value != 100 -> {
                speedPercentage = min(speedPercentage + (100.0f - accelerateStartSpeed.value) / (accelerateTime.value * 20.0f), 100.0f)
                val speedMultiplier = speedPercentage / 100.0
                horizontalSpeed.value * speedMultiplier * (cos(speedMultiplier * PI) * -0.5 + 0.5)
            }

            else -> horizontalSpeed.value.toDouble()
        }
    }


    private fun setSpeed(yaw: Double, boosting: Boolean) {
        val acceleratedSpeed = getSpeed(boosting)
        Globals.mc.player.setVelocity(sin(-yaw) * acceleratedSpeed, Globals.mc.player.motionY, cos(yaw) * acceleratedSpeed)
    }

    private fun boostMode() {
        val yaw = Globals.mc.player.rotationYaw.toRadian()
        Globals.mc.player.motionX -= Globals.mc.player.movementInput.moveForward * sin(yaw) * horizontalSpeed.value / 20
        if (Globals.mc.player.movementInput.jump) Globals.mc.player.motionY += verticalSpeed.value / 15 else if (Globals.mc.player.movementInput.sneak) Globals.mc.player.motionY -= descendSpeed.value / 15
        Globals.mc.player.motionZ += Globals.mc.player.movementInput.moveForward * cos(yaw) * horizontalSpeed.value / 20
    }

    /* Control Mode */
    private fun controlMode(event: TravelEvent) {
        val currentSpeed = Globals.mc.player.speed
        val moveUp = if (!legacyLookBoost.value) Globals.mc.player.movementInput.jump else Globals.mc.player.rotationPitch < -10.0f && !isStandingStillH

        val moveDown = if (NoSlowModule.isEnabled && NoSlowModule.inventoryWalk.value && NoSlowModule.noSneak.value && Globals.mc.currentScreen != null || moveUp) false
        else Globals.mc.player.movementInput.sneak

        /* Dynamic down speed */
        val calcDownSpeed = if (dynamicDownSpeed.value) {
            val minDownSpeed = min(descendSpeed.value, fastDescendSpeed.value).toDouble()
            val maxDownSpeed = max(descendSpeed.value, fastDescendSpeed.value).toDouble()
            if (Globals.mc.player.rotationPitch > 0) Globals.mc.player.rotationPitch / 90.0 * (maxDownSpeed - minDownSpeed) + minDownSpeed else minDownSpeed
        } else descendSpeed.value.toDouble()

        if (hoverTarget < 0.0 || moveUp)
            hoverTarget = Globals.mc.player.posY
        else if (moveDown)
            hoverTarget = Globals.mc.player.posY - calcDownSpeed

        hoverState = (if (hoverState) Globals.mc.player.posY < hoverTarget else Globals.mc.player.posY < hoverTarget - 0.1) && altitudeHold.value


        if (!isStandingStillH || moveUp) {
            if ((moveUp || hoverState) && (currentSpeed >= 0.8 || Globals.mc.player.motionY > 1.0)) {
                upwardFlight(currentSpeed, getYaw())
            } else if (!isStandingStillH || moveUp) {
                packetPitch = 10F
                Globals.mc.player.motionY = -fallSpeed.value.toDouble()
                setSpeed(getYaw(), moveUp)
                boostingTick = 0
            }
        } else Globals.mc.player.setVelocity(0.0, 0.0, 0.0)

        if (moveDown) Globals.mc.player.motionY = -calcDownSpeed
        event.cancel()
    }

    private fun upwardFlight(currentSpeed: Double, yaw: Double) {
        val multipliedSpeed = 0.128 * min(horizontalSpeed.value, 2.0f)
        val strictPitch = asin((multipliedSpeed - sqrt(multipliedSpeed * multipliedSpeed - 0.0348)) / 0.12).toDegree().toFloat()
        val basePitch = if (ncpStrict.value && strictPitch < boostPitch.value && !strictPitch.isNaN()) -strictPitch else -boostPitch.value.toFloat()

        val targetPitch = if (Globals.mc.player.rotationPitch < 0.0f) { max(Globals.mc.player.rotationPitch * (90.0f - boostPitch.value.toFloat()) / 90.0f - boostPitch.value.toFloat(), -90.0f) } else -boostPitch.value.toFloat()

        packetPitch = if (packetPitch <= basePitch && boostingTick > 2) {
            if (packetPitch < targetPitch) packetPitch += 17.0f
            if (packetPitch > targetPitch) packetPitch -= 17.0f
            max(packetPitch, targetPitch)
        } else basePitch
        boostingTick++


        val pitch = packetPitch.toRadian().toDouble()
        val targetMotionX = sin(-yaw) * sin(-pitch)
        val targetMotionZ = cos(yaw) * sin(-pitch)
        val targetSpeed = hypot(targetMotionX, targetMotionZ)
        val upSpeed = currentSpeed * sin(-pitch) * 0.04
        val fallSpeed = cos(pitch) * cos(pitch) * 0.06 - 0.08

        Globals.mc.player.motionX -= upSpeed * targetMotionX / targetSpeed - (targetMotionX / targetSpeed * currentSpeed - Globals.mc.player.motionX) * 0.1
        Globals.mc.player.motionY += upSpeed * 3.2 + fallSpeed
        Globals.mc.player.motionZ -= upSpeed * targetMotionZ / targetSpeed - (targetMotionZ / targetSpeed * currentSpeed - Globals.mc.player.motionZ) * 0.1

        Globals.mc.player.motionX *= 0.99
        Globals.mc.player.motionY *= 0.98
        Globals.mc.player.motionZ *= 0.99
    }


    private fun creativeMode() {
        if (Globals.mc.player.onGround) {
            reset(true)
            return
        }

        packetPitch = 0F
        Globals.mc.player.capabilities.isFlying = true
        Globals.mc.player.capabilities.flySpeed = getSpeed(false).toFloat()

        val motionY = when {
            isStandingStill -> 0.0
            Globals.mc.player.movementInput.jump -> verticalSpeed.value.toDouble()
            Globals.mc.player.movementInput.sneak -> -descendSpeed.value.toDouble()
            else -> -fallSpeed.value.toDouble()
        }

        Globals.mc.player.setVelocity(0.0, motionY, 0.0)
    }

    private fun packetMode(event: TravelEvent) {
        isPacketFlying = !Globals.mc.player.onGround
        Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_FALL_FLYING))

        if (!isStandingStillH) {
            setSpeed(getYaw(), false)
        } else Globals.mc.player.setVelocity(0.0, 0.0, 0.0)
        Globals.mc.player.motionY = (if (Globals.mc.player.movementInput.sneak) -descendSpeed.value else -fallSpeed.value).toDouble()

        event.cancel()
    }

    private fun spoofRotation() {
        if (Globals.mc.player.isSpectator || !elytraIsEquipped || elytraDurability <= 1 || !isFlying) return
        val packet = LocalMotionManager.Motion()
        var rotation = Vec2f(Globals.mc.player)

        if (autoLanding.value) {
            rotation = Vec2f(rotation.x, -20f)
        } else if (mode.value != ElytraFlightMode.BOOST) {
            if (!isStandingStill && mode.value != ElytraFlightMode.CREATIVE) rotation = Vec2f(packetYaw, rotation.y)
            if (!isStandingStill) rotation = Vec2f(rotation.x, packetPitch) else packet.rotation = null
        }
        packet.rotation = rotation

        addMotion(packet)
    }

    fun shouldSwing(): Boolean {
        return isEnabled && isFlying && !autoLanding.value && (mode.value == ElytraFlightMode.CONTROL || mode.value == ElytraFlightMode.PACKET) && Globals.mc.player.inventoryContainer.getSlot(6).stack.item is ItemElytra
    }

    override fun onEnable() {
        if (fullNullCheck()) return

        autoLanding.value = false
        speedPercentage = accelerateStartSpeed.value.toFloat() /* For acceleration */
        hoverTarget = -1.0 /* For control mode */
    }

    override fun onDisable() {
        reset(true)
    }


}