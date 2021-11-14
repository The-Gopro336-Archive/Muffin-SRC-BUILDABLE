package me.han.muffin.client.module.modules.movement

import io.netty.util.internal.ConcurrentSet
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.client.TravelEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.event.events.world.WorldEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.exploits.PhaseModule
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.extensions.mixin.netty.*
import me.han.muffin.client.utils.math.RandomUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityBoat
import net.minecraft.network.Packet
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.SPacketEntity
import net.minecraft.network.play.server.SPacketEntityTeleport
import net.minecraft.network.play.server.SPacketMoveVehicle
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.chunk.EmptyChunk
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object BoatFlyModule: Module("BoatFly", Category.MOVEMENT, "Allow you to fly around with boat.") {
    private val page = EnumValue(Pages.Speed, "Page")
    // speed //
    val horizontalSpeed = NumberValue({ page.value == Pages.Speed }, 1F, 0.1F, 20.0F, 0.1F, "HorizontalSpeed")
    private val verticalSpeed = NumberValue({ page.value == Pages.Speed }, 1.0F, 0.0F, 10.0F, 0.1F, "VerticalSpeed")
    private val descendSpeed = NumberValue({ page.value == Pages.Speed }, 1.0F, 0.0F, 10.0F, 0.1F, "DescendSpeed")
    val glideSpeed = NumberValue({ page.value == Pages.Speed && !antiKick.value }, 0.1F, 0.0F, 5.0F, 0.1F, "GlideSpeed")
    // speed //

    // misc //
    private val exploit = EnumValue({ page.value == Pages.Misc }, Exploits.Strict, "Exploits")
    private val duck = Value({ page.value == Pages.Misc }, true, "Duck")
    val antiKick = Value({ page.value == Pages.Misc }, true, "AntiKick")
    private val fixYaw = Value({ page.value == Pages.Misc }, true, "FixYaw")

    private val frequency = NumberValue({ page.value == Pages.Misc }, 0, 0, 20, 1, "Frequency")
    private val ticksToPause = NumberValue({ page.value == Pages.Misc }, 5, 0, 20, 1, "TicksToPause")
    private val distanceToPause = NumberValue({ page.value == Pages.Misc }, 2, 0, 20, 1, "DistanceToPause")
    private val frequencyLagBack = NumberValue({ page.value == Pages.Misc }, 5, 0, 50, 1, "FrequencyLagBack")
    private val consecutiveLagBackTicks = NumberValue({ page.value == Pages.Misc }, 1, 0, 10, 1, "ConsecutiveLagBack")

    private val forceMoving = Value({ page.value == Pages.Misc }, false, "ForceMoving")

    private val moveConfirm = EnumValue({ page.value == Pages.Misc }, Confirm.New, "MoveConfirm")
  //  private val legitConfirm = Value({ page.value == Pages.Misc && moveConfirm.value == Confirm.New }, false, "LegitConfirm")
    private val serverSideCheck = Value({ page.value == Pages.Misc }, false, "ServerCheck")
    // misc //

    val boatScale = NumberValue({ page.value == Pages.Rendering }, 1.0, 0.0, 2.0, 0.1, "BoatScale")
    val boatOpacity = NumberValue({ page.value == Pages.Rendering }, 255, 0, 255, 1, "BoatOpacity")

    private val playerPackets = ConcurrentSet<CPacketPlayer>()
    private var flyingTicks = 0
    private var teleportId = 0
    private val teleportMap = ConcurrentHashMap<Int, IDTime>()
    private var lastWorkingPos = Vec3d.ZERO
    private var notWorkingPos = Vec3d.ZERO
    private val distanceTimer = Timer()
    private val tickTimer = Timer()
    private val bypassTimer = Timer()

    private var ridingBoat: EntityBoat? = null
    private var lagBackTicks = 0

    private var receivedTeleportPacket = false
    private var shouldSendConsecutiveInteractPacket = false

    private var countLagTicks = 0

    private var frequencies = 0

    private val vehiclePackets = LinkedBlockingQueue<Packet<*>>()
    private var disableLogger = false

    private enum class Pages {
        Speed, Misc, Rendering
    }

    private enum class Confirm {
        Off, Old, New
    }

    private enum class Exploits {
        Off, Strong, Strict, Test//, Test, TestStrong, Test2, Test2Strong, Test3, Test3Strong
    }

    init {
        addSettings(
            page,
            // speed //
            horizontalSpeed, verticalSpeed, descendSpeed, glideSpeed,
            // speed //
            // misc //
            duck, antiKick, exploit, fixYaw, frequency, ticksToPause, distanceToPause, frequencyLagBack, consecutiveLagBackTicks, forceMoving, moveConfirm, serverSideCheck,
            // misc //
            // rendering //
            boatScale, boatOpacity
            // rendering //
        )
    }

    override fun getHudInfo(): String? = if (exploit.value == Exploits.Strict || exploit.value == Exploits.Strong) exploit.fixedValue else "Normal"

    private fun resetTicks(ticks: Int): Boolean {
        if (flyingTicks++ >= ticks) {
            flyingTicks = 0
            return true
        }
        return false
    }

    private fun clearAll() {
        ridingBoat = null
        shouldSendConsecutiveInteractPacket = false
        distanceTimer.reset()
        lastWorkingPos = Vec3d.ZERO
        notWorkingPos = Vec3d.ZERO
        lastLoadedChunkPos = Vec3d.ZERO
        flyingTicks = 0
        countLagTicks = 0
        lagBackTicks = 0
        frequencies = 0
        teleportId = 0
        teleportMap.clear()
        playerPackets.clear()
    }

    override fun onEnable() {
        clearAll()
    }

    override fun onDisable() {
/*
        if (Globals.mc.player?.ridingEntity is EntityBoat) {
            val ridingEntity = Globals.mc.player?.ridingEntity!! as EntityBoat
            if (MovementUtils.isMoving() && !isBorderingChunk(ridingEntity, ridingEntity.motionX, ridingEntity.motionZ)) {
                sendBypassPackets(ridingEntity, ridingEntity.motionX, ridingEntity.motionY, ridingEntity.motionZ, true, true, true, true)
            } else if (isBorderingChunk(ridingEntity, ridingEntity.motionX, ridingEntity.motionZ)) {
                sendBypassPackets(ridingEntity, ridingEntity.motionX, ridingEntity.motionY, ridingEntity.motionZ, false, false, false, false)
            }  else {
                sendBypassPackets(ridingEntity, ridingEntity.motionX, ridingEntity.motionY, ridingEntity.motionZ, true, true, true, true)
            }
        }
 */

        clearAll()
    }


    @Listener
    private fun onWorldUnload(event: WorldEvent.Unload) {
        clearAll()
        disable()
    }


    @Listener
    private fun onServerDisconnect(event: ServerEvent.Disconnect) {
        clearAll()
        disable()
    }

    @Listener
    private fun onPlayerDisconnect(event: ServerEvent.Disconnect) {
        if (event.state == EventStageable.EventStage.PRE) vehiclePackets.clear()
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {

        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (Globals.mc.player?.ridingEntity !is EntityBoat) return

        if (exploit.value == Exploits.Test && !disableLogger) {
            if (event.packet is CPacketPlayer.Position || event.packet is CPacketPlayer.Rotation || event.packet is CPacketPlayer.PositionRotation || event.packet is CPacketConfirmTeleport || event.packet is CPacketInput || event.packet is CPacketUseEntity || event.packet is CPacketSteerBoat || event.packet is CPacketVehicleMove) {
                vehiclePackets.add(event.packet)
                event.cancel()
            }
        }

        /*
        if (event.packet is CPacketVehicleMove) {
            Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(event.packet.x, event.packet.y, event.packet.z, false))
            Globals.mc.player.setPosition(event.packet.x, event.packet.y, event.packet.z)

            if (frequency.value > 0 && MovementUtils.isKeyDown() && Globals.mc.player.ticksExisted % frequency.value == 0) {
                Globals.mc.player.connection.sendPacket(CPacketSteerBoat(false, false))
                if (legit.value) {
                    Globals.mc.player.connection.sendPacket(CPacketUseEntity(Globals.mc.player.ridingEntity, EnumHand.OFF_HAND))
                } else {
                    for (i in 0..3) Globals.mc.player.connection.sendPacket(CPacketUseEntity(Globals.mc.player.ridingEntity, EnumHand.OFF_HAND))
                }
            }
        }
         */

        //     SPacketUnloadChunk
        // SPacketChunkData
        /*
        if (strict.value && event.packet is CPacketVehicleMove) {
            if (frequency.value == 0) {
                Globals.mc.playerController.interactWithEntity(Globals.mc.player, Globals.mc.player.ridingEntity, EnumHand.OFF_HAND)
            } else if (Globals.mc.player.ticksExisted % frequency.value == 0) {
                    Globals.mc.playerController.interactWithEntity(Globals.mc.player, Globals.mc.player.ridingEntity, EnumHand.OFF_HAND)
              //      Globals.mc.player.connection.sendPacket(CPacketUseEntity(Globals.mc.player.ridingEntity, EnumHand.OFF_HAND))
            }


     //       val boat = Globals.mc.player.ridingEntity as EntityBoat
     //       if (lastLoadedChunkPos != Vec3d.ZERO && Globals.mc.world.getChunk((boat.posX).toInt() shr 4, (boat.posZ).toInt() shr 4) is EmptyChunk) {
     //           boat.setPosition(lastLoadedChunkPos.x, lastLoadedChunkPos.y, lastLoadedChunkPos.z)
     //           event.cancel()
     //       }

        }
         */


        // if (event.packet is CPacketPlayer.Position || event.packet is CPacketPlayer.PositionRotation) {
        //     event.cancel()
        // }

        if (event.packet is CPacketPlayer && event.packet !is CPacketPlayer.Rotation) {
            event.cancel()
        }

        if (event.packet is CPacketPlayer.Rotation) event.cancel()

        //    if (event.packet is CPacketSteerBoat) event.cancel()

        if (duck.value && event.packet is CPacketInput) {
            event.cancel()
        }

        /*
                if (event.packet is CPacketPlayer) {
                    if (playerPackets.contains(event.packet)) {
                        playerPackets.remove(event.packet)
                        return
                    }
                    event.cancel()
                }
         */

    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (Globals.mc.player?.ridingEntity !is EntityBoat) return

        if (event.packet is SPacketMoveVehicle) {
            frequencies++
            if (frequencyLagBack.value == 0) {
                frequencies = 0
                event.cancel()
            } else if (frequencyLagBack.value > 0 && frequencies <= frequencyLagBack.value) {
                event.cancel()
            }

       //     if (frequencyLagBack.value in 1 until frequencies) {
       //         return
       //     }
       //     event.cancel()
        }

        if (serverSideCheck.value && event.packet is SPacketEntityTeleport) {
            Globals.mc.player?.ridingEntity?.let {
                if (it is EntityBoat) {
                    if (event.packet.entityId == it.entityId || event.packet.entityId == Globals.mc.player.entityId) {
                        receivedTeleportPacket = true
                    }
                }
            }
        }

        if (!forceMoving.value) {
            if (event.packet is SPacketEntity.S15PacketEntityRelMove || event.packet is SPacketEntity.S17PacketEntityLookMove) {
                if (serverSideCheck.value) receivedTeleportPacket = true
                event.cancel()
            }
        }

        if (event.packet is SPacketPlayerPosLook) {
            event.packet.rotationYaw = Globals.mc.player.rotationYaw
            event.packet.rotationPitch = Globals.mc.player.rotationPitch

            teleportId = event.packet.teleportId

            when (moveConfirm.value) {
                Confirm.Old -> {
                    event.packet.posX = Globals.mc.player.posX
                    event.packet.posY = Globals.mc.player.posY
                    event.packet.posZ = Globals.mc.player.posZ
                }

                Confirm.New -> {
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
                  //  if (legitConfirm.value) {
                  //      Globals.mc.player.setPositionAndRotation(x, y, z, yaw, pitch)
                  //      Globals.mc.connection?.sendPacket(CPacketPlayer.PositionRotation(Globals.mc.player.posX, Globals.mc.player.entityBoundingBox.minY, Globals.mc.player.posZ, Globals.mc.player.rotationYaw, Globals.mc.player.rotationPitch, false))
                  //  } else {
                        Globals.mc.connection?.sendPacket(CPacketPlayer.PositionRotation(x, Globals.mc.player.entityBoundingBox.minY, z, yaw, pitch, false))
                  //  }
                }
            }

            event.cancel()
        }

    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        teleportMap.entries.removeIf { it.value.timer.passedSeconds(30) }

        if (exploit.value == Exploits.Test && vehiclePackets.size > 3) {
            blink()
        }

        if (consecutiveLagBackTicks.value > 0 && ridingBoat != null) {
            ridingBoat!!.isDead = false
            if (!Globals.mc.player.isRiding) {
                Globals.mc.player.startRiding(ridingBoat!!, true)
            }

            if (Globals.mc.player.ridingEntity == ridingBoat) shouldSendConsecutiveInteractPacket = true

            ridingBoat = null
        }

        if (isTerrainLoaded()) {
            shouldSendConsecutiveInteractPacket = false
        }

    }


    @Listener
    private fun onTravel(event: TravelEvent) {
        Globals.mc.player?.ridingEntity?.let {
            if (it is EntityBoat && it.controllingPassenger == Globals.mc.player) {
                moveEntity(it)
         //       event.cancel()
            }
        }
    }

    @Listener
    fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (Globals.mc.player.isDead || Globals.mc.player.health <= 0.0) {
            disable()
            return
        }
    }


    private fun sendPlayerPackets(packet: CPacketPlayer) {
        playerPackets.add(packet)
        Globals.mc.player.connection.sendPacket(packet)
    }

    private fun outOfBoundsVector(position: Vec3d): Vec3d {
        var spoofX = position.x
        var spoofY = position.y
        var spoofZ = position.z
        //     if (ncpStrict.value) {
        spoofX += RandomUtils.random.nextInt(100000)
        spoofZ += RandomUtils.random.nextInt(100000)
        //  } else {
        spoofY -= 1339.69
        //    }
        return Vec3d(spoofX, spoofY, spoofZ)
    }

    private fun sendBypassPackets(entity: EntityBoat, x: Double, y: Double, z: Double, canVehicleMove: Boolean, canInteract: Boolean, canSteerBoat: Boolean, canTeleport: Boolean) {
        val vector = Vec3d(x, y, z)
        val position = entity.positionVector.add(vector)

        //  sendPlayerPackets(CPacketPlayer.Position(position.x, position.y, position.z, true))
        //  sendPlayerPackets(CPacketPlayer.Position(outOfBoundsPacket.x, outOfBoundsPacket.y, outOfBoundsPacket.z, true))
        //    ++buffer

        //    if (MovementUtils.isMoving())
        if (canSteerBoat)
            Globals.mc.player.connection.sendPacket(CPacketSteerBoat(entity.getPaddleState(0), entity.getPaddleState(1)))

        // else
        // /    Globals.mc.player.connection.sendPacket(CPacketSteerBoat(true, true))
        //   Globals.mc.player.connection.sendPacket(CPacketSteerBoat(entity.getPaddleState(0), entity.getPaddleState(1)))
        //  else
        //      Globals.mc.player.connection.sendPacket(CPacketSteerBoat(true, true))

        if (canVehicleMove)
            Globals.mc.player.connection.sendPacket(CPacketVehicleMove(entity))

        if (canInteract) {
            if (exploit.value == Exploits.Strict || exploit.value == Exploits.Test) {
                if (frequency.value == 0) {
                    Globals.mc.player.connection.sendPacket(CPacketUseEntity(entity, EnumHand.OFF_HAND))
            //        Globals.mc.playerController.interactWithEntity(Globals.mc.player, entity, EnumHand.OFF_HAND)
                } else if (Globals.mc.player.ticksExisted % frequency.value == 0) {
                    Globals.mc.player.connection.sendPacket(CPacketUseEntity(entity, EnumHand.OFF_HAND))
            //        Globals.mc.playerController.interactWithEntity(Globals.mc.player, entity, EnumHand.OFF_HAND)
                }
            } else if (exploit.value == Exploits.Strong) {
                for (i in 0 until 3) {
                    if (frequency.value == 0) {
                        Globals.mc.player.connection.sendPacket(CPacketUseEntity(entity, EnumHand.OFF_HAND))
                    } else if (Globals.mc.player.ticksExisted % frequency.value == 0) {
                        Globals.mc.player.connection.sendPacket(CPacketUseEntity(entity, EnumHand.OFF_HAND))
                    }
                }
            } /*else if (exploit.value == Exploits.Test) {
                if (frequency.value == 0) {
                    Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))
                    Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
                } else if (Globals.mc.player.ticksExisted % frequency.value == 0) {
                    Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))
                    Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
                }
            } else if (exploit.value == Exploits.TestStrong) {
                for (i in 0..3) {
                    if (frequency.value == 0) {
                        Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))
                        Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
                    } else if (Globals.mc.player.ticksExisted % frequency.value == 0) {
                        Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))
                        Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
                    }
                }
            } else if (exploit.value == Exploits.Test2) {
                if (frequency.value == 0) {
                    Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_RIDING_JUMP))
                } else if (Globals.mc.player.ticksExisted % frequency.value == 0) {
                    Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_RIDING_JUMP))
                }
            } else if (exploit.value == Exploits.Test2Strong) {
                for (i in 0..3) {
                    if (frequency.value == 0) {
                        Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_RIDING_JUMP))
                    } else if (Globals.mc.player.ticksExisted % frequency.value == 0) {
                        Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_RIDING_JUMP))
                    }
                }
            } else if (exploit.value == Exploits.Test3) {
                if (frequency.value == 0) {
                    Globals.mc.player.connection.sendPacket(CPacketSteerBoat(entity.getPaddleState(0), entity.getPaddleState(1)))
                } else if (Globals.mc.player.ticksExisted % frequency.value == 0) {
                    Globals.mc.player.connection.sendPacket(CPacketSteerBoat(entity.getPaddleState(0), entity.getPaddleState(1)))
                }
            } else if (exploit.value == Exploits.Test3Strong) {
                for (i in 0..3) {
                    if (frequency.value == 0) {
                        Globals.mc.player.connection.sendPacket(CPacket(entity.getPaddleState(0), entity.getPaddleState(1)))
                    } else if (Globals.mc.player.ticksExisted % frequency.value == 0) {
                        Globals.mc.player.connection.sendPacket(CPacketSteerBoat(entity.getPaddleState(0), entity.getPaddleState(1)))
                    }
                }
            }
            */
        }

        if (canTeleport) {
            Globals.mc.player.connection.sendPacket(CPacketConfirmTeleport(++teleportId))
            teleportMap[teleportId] = IDTime(position, Timer())
        }
    }


    private var lastLoadedChunkPos = Vec3d.ZERO
    private val pauseTimer = Timer()

    private fun blink() {
        try {
            disableLogger = true
            while (vehiclePackets.isNotEmpty()) {
                Globals.mc.player.connection.sendPacket(vehiclePackets.take())
            }
            disableLogger = false
        } catch (e: Exception) {
            disableLogger = false
        }
    }

    private fun moveEntity(entity: EntityBoat) {
        val yawRad = MovementUtils.calcMoveYaw()
        val motionX = -sin(yawRad) * horizontalSpeed.value
        val motionZ = cos(yawRad) * horizontalSpeed.value

        if (fixYaw.value) entity.rotationYaw = Globals.mc.player.rotationYaw
        entity.updateInputs(false, false, false, false)

        val speed = if (Globals.mc.player.movementInput.jump) {
            if (antiKick.value) if (resetTicks(20)) -0.032 else verticalSpeed.value.toDouble() else verticalSpeed.value.toDouble()
        } else if (Globals.mc.player.movementInput.sneak) {
            -descendSpeed.value.toDouble()
        } else if (!entity.isInWater) {
            if (antiKick.value) {
                if (resetTicks(1)) -0.1 else 0.1
            } else {
                -glideSpeed.value.toDouble()
            }
        } else {
            0.0
        }

        if (consecutiveLagBackTicks.value in 1..lagBackTicks && !shouldSendConsecutiveInteractPacket) {
            ridingBoat = entity
            Globals.mc.player.dismountRidingEntity()
            return
        }

        if (shouldSendConsecutiveInteractPacket) {
            sendBypassPackets(entity, entity.motionX, entity.motionY, entity.motionZ, canVehicleMove = true, canInteract = true, canSteerBoat = true, canTeleport = true)
            shouldSendConsecutiveInteractPacket = false
            return
        }

        for (i in 1..1) {

            if (frequencyLagBack.value in 1 until frequencies) {
                entity.motionX = 0.0
                entity.motionZ = 0.0
                if (speed != 0.0) entity.motionY = speed * i
                sendBypassPackets(entity, entity.motionX, entity.motionY, entity.motionZ, canVehicleMove = true, canInteract = true, canSteerBoat = true, canTeleport = true)
                frequencies = 0
                return
            }

            if (serverSideCheck.value && receivedTeleportPacket) {
                entity.motionX = 0.0
                entity.motionZ = 0.0
                if (speed != 0.0) entity.motionY = speed * i
                sendBypassPackets(entity, entity.motionX, entity.motionY, entity.motionZ, canVehicleMove = true, canInteract = true, canSteerBoat = true, canTeleport = true)
                receivedTeleportPacket = false
                return
            }

            if (distanceToPause.value > 0) {
                if (lastWorkingPos != Vec3d.ZERO && notWorkingPos != Vec3d.ZERO && (!isTerrainLoaded() || !isBlockInRenderDistance(BlockPos(entity.posX, entity.posY, entity.posZ)))) {
                    if (abs(lastWorkingPos.x - notWorkingPos.x) > distanceToPause.value || abs(lastWorkingPos.y - notWorkingPos.y) > distanceToPause.value || abs(lastWorkingPos.z - notWorkingPos.z) > distanceToPause.value) {
                        entity.motionX = 0.0
                        entity.motionZ = 0.0
                        if (speed != 0.0) entity.motionY = speed * i
                        sendBypassPackets(entity, entity.motionX, entity.motionY, entity.motionZ, canVehicleMove = true, canInteract = true, canSteerBoat = true, canTeleport = true)
                        return
                    }
                }
            }

            if (MovementUtils.isMoving() && !isBorderingChunk(entity, motionX, motionZ)) {
                entity.motionX = motionX * i
                entity.motionZ = motionZ * i
                if (speed != 0.0) entity.motionY = speed * i
                lastWorkingPos = entity.positionVector
                lagBackTicks = 0
                countLagTicks = 0
                sendBypassPackets(entity, entity.motionX, entity.motionY, entity.motionZ, canVehicleMove = true, canInteract = true, canSteerBoat = true, canTeleport = true)
            } else if (isBorderingChunk(entity, motionX, motionZ)) {
                lagBackTicks++
                countLagTicks++
                notWorkingPos = entity.positionVector
                entity.motionX = 0.0
                entity.motionZ = 0.0
                if (speed != 0.0) entity.motionY = speed * i
                if (ticksToPause.value > 0 && countLagTicks < ticksToPause.value) {
                    sendBypassPackets(entity, entity.motionX, entity.motionY, entity.motionZ, canVehicleMove = true, canInteract = true, canSteerBoat = true, canTeleport = true)
                    countLagTicks = 0
                } else {
                    sendBypassPackets(entity, entity.motionX, entity.motionY, entity.motionZ, canVehicleMove = true, canInteract = true, canSteerBoat = true, canTeleport = true)
                }
            }  else {
                entity.motionX = 0.0
                entity.motionZ = 0.0
                if (speed != 0.0) entity.motionY = speed * i
                sendBypassPackets(entity, entity.motionX, entity.motionY, entity.motionZ, canVehicleMove = true, canInteract = true, canSteerBoat = true, canTeleport = true)
            }

            if (PhaseModule.isEnabled && PhaseModule.mode.value == PhaseModule.Mode.Boat) {
                entity.entityBoundingBox = entity.entityBoundingBox.offset(entity.motionX * i, speed * i, entity.motionZ * i)
            }

        }

    }

    private fun isBorderingChunk(entity: Entity, motionX: Double, motionZ: Double): Boolean {
        val boatPos = BlockPos(entity.posX + motionX, entity.posY, entity.posZ + motionZ)
        return !isTerrainLoaded() || !isBlockInRenderDistance(boatPos) || Globals.mc.world.getChunk((entity.posX + motionX).toInt() shr 4, (entity.posZ + motionZ).toInt() shr 4) is EmptyChunk
    }

    private fun isBlockInRenderDistance(pos: BlockPos) = Globals.mc.world.getChunk(pos).isLoaded

    private fun isTerrainLoaded() = Globals.mc.player.connection.doneLoadingTerrain

    /*
    fun doVanillaPosLook(event: PacketEvent.Receive) {
        if (event.packet is SPacketPlayerPosLook && (isTerrainLoaded() || isBlockInRenderDistance(PlayerUtil.getPlayerPosFloored(Globals.mc.player)))) {
            event.cancel()
            var d0 = event.packet.x
            var d1 = event.packet.y
            var d2 = event.packet.z
            if (event.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.X)) {
                d0 += Globals.mc.player.posX
            }
            if (event.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.Y)) {
                d1 += Globals.mc.player.posY
            }
            if (event.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.Z)) {
                d2 += Globals.mc.player.posZ
            }
            Globals.mc.connection?.sendPacket(CPacketConfirmTeleport(event.packet.teleportId))
            Globals.mc.connection?.sendPacket(CPacketPlayer.PositionRotation(d0, Globals.mc.player.entityBoundingBox.minY, d2, event.packet.yaw, event.packet.pitch, false))
        }
    }
     */

    class IDTime(val vector: Vec3d, val timer: Timer = Timer()) {
        init {
            timer.reset()
        }
    }

}