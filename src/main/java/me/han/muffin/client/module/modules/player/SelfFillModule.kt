package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.JumpEvent
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.manager.managers.LocalHotbarManager
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.manager.managers.SpeedManager.speedKmh
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.RotateMode
import me.han.muffin.client.utils.block.BlockUtil
import me.han.muffin.client.utils.block.HoleUtils
import me.han.muffin.client.utils.block.HoleUtils.centerPlayer
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.extensions.kotlin.floorToInt
import me.han.muffin.client.utils.extensions.mc.block.*
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mixin.entity.isInWeb
import me.han.muffin.client.utils.math.RayTraceUtils
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.math.rotation.Vec2f
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.abs

/**
 * @author han
 */
internal object SelfFillModule: Module("SelfFill", Category.PLAYER, "Attempt to place a block and rubberband you into it.", 285) {
    private val mode = EnumValue(Mode.Instant, "Mode")
    private val onlyEChest = Value(false, "OnlyEChest")

    private val autoJump = EnumValue({ mode.value == Mode.Normal }, JumpMode.Motion, "AutoJump")
    private val centerMode = EnumValue({ mode.value == Mode.Normal && autoJump.value != JumpMode.Off }, CenterMode.Teleport, "CenterMode")

    private val stable = Value(true, "Stable")

    private val fastReact = Value(false, "FastReact")

    private val least = Value({ mode.value == Mode.Instant }, false, "Least")

    private val rotate = EnumValue(RotateMode.Both, "Rotate")
    private val swingArm = Value(false, "SwingArm")

    private val dynamicOffset = Value(true, "DynamicOffset")
    private val offset = NumberValue({ !dynamicOffset.value }, 5.0, -10.0, 10.0, 0.2, "Offset")
    private val delay = NumberValue({ mode.value == Mode.Normal },3, 0, 10, 1, "Delay")
    private val maxSpeed = NumberValue(25.0, 0.0, 35.0, 0.2, "MaxSpeed")
    private val stopWhileMove = Value(false, "StopWhileMove")

    private var jumped = false
    private var jumpTicksCount = 0
    private var itemSlot = -1
    private var targetPos: BlockPos? = null
    //private val targetPos = BlockPos.MutableBlockPos(0, -69, 0)

    private var currentState = State.Placing
    private val toggleTimer = Timer()

    private var centerVector = Vec3d.ZERO
    private var teleportIds = 0

    private var groundTicks = 0

    private var lastRotation = Vec2f.ZERO

    private var nowPlaceable = false

    private var hasStopped = false
    private var didInstant = false

    private enum class Mode {
        Normal, Instant
    }

    private enum class JumpMode {
        Off, Normal, Motion
    }

    private enum class CenterMode {
        Off, Teleport, Motion
    }

    private enum class State {
        Placing, Done
    }

    init {
        addSettings(mode, onlyEChest, autoJump, centerMode, stable, fastReact, least, rotate, swingArm, dynamicOffset, offset, delay, maxSpeed, stopWhileMove)
    }

    private fun getDynamicYPos(): Double {
        if (dynamicOffset.value) {
            val currentOffset = offset.value
            return if (Globals.mc.player.posY < 127.5) offset.value else offset.value
        }
        return offset.value
    }

    private fun getJumpOffsets(yOffset: Double): Pair<Boolean, DoubleArray> {
        val localOffset = yOffset.floorToInt()
        val defaultOffsets = doubleArrayOf(0.419997086886978, 0.7500029, 0.9999942, 1.170005801788139, 1.170005801788139)

        var isFourOnTop = true

        for (i in 0..4) {
            val yAddon = localOffset + i
            val playerOffset = BlockPos(Globals.mc.player.posX.floorToInt(), yAddon, Globals.mc.player.posZ.floorToInt())
            if (!playerOffset.isAir) isFourOnTop = false
        }

        return isFourOnTop to if (isFourOnTop) doubleArrayOf(0.42, 0.75, 1.01, 1.16) else defaultOffsets
    }

    private fun teleportToCenter() {
        val x = abs(centerVector.x - Globals.mc.player.posX)
        val z = abs(centerVector.z - Globals.mc.player.posZ)
        if (x <= 0.1 && z <= 0.1) {
            centerVector = Vec3d.ZERO
        } else {
            Globals.mc.player.centerPlayer(centerMode.value == CenterMode.Teleport)
        }
    }


    override fun onEnable() {
        if (fullNullCheck()) return

        lastRotation = Vec2f.ZERO
        nowPlaceable = false

        centerVector = BlockUtil.getCenter(Globals.mc.player.positionVector)

        if (mode.value == Mode.Normal && autoJump.value != JumpMode.Off && centerMode.value != CenterMode.Off && !HoleUtils.isBurrowed(Globals.mc.player) && centerVector != Vec3d.ZERO && findBlockInHotbar() != -1) {
            teleportToCenter()
        }

        if (mode.value == Mode.Normal && autoJump.value != JumpMode.Off && !HoleUtils.isBurrowed(Globals.mc.player)) {
            when (autoJump.value) {
                JumpMode.Normal -> Globals.mc.player.jump()
                JumpMode.Motion -> Globals.mc.player.motionY = 0.41999998688697815
            }
        }
    }

    override fun onDisable() {
        didInstant = false
        hasStopped = false
        itemSlot = -1
        jumped = false
        jumpTicksCount = 0
        groundTicks = 0
        teleportIds = 0
    }

    override fun onToggle() {
        currentState = State.Placing
        targetPos = null
        toggleTimer.reset()
    }

    private fun findBlockInHotbar(): Int {
        if (onlyEChest.value) return InventoryUtils.findBlock(Blocks.ENDER_CHEST)

        val stack = Globals.mc.player.inventory.getCurrentItem()
        if (stack.count != 0 && stack.item is ItemBlock) {
            return Globals.mc.player.inventory.currentItem
        }

        val obsidianSlot = InventoryUtils.findBlock(Blocks.OBSIDIAN)
        if (obsidianSlot != -1) return obsidianSlot

        return InventoryUtils.findGenericBlock()
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (fullNullCheck()) return

        if (Globals.mc.player.isInWeb) {
            disable()
            return
        }

        if (event.stage == EventStageable.EventStage.PRE) {
            if (maxSpeed.value > 0.0 && Globals.mc.player.speedKmh > maxSpeed.value) {
                disable()
                return
            }

            if (stopWhileMove.value && MovementUtils.isMoving() && MovementUtils.hasMotion() && !hasStopped) {
                Globals.mc.player.motionX = 0.0
                Globals.mc.player.motionZ = 0.0
                hasStopped = true
            }

            val slots = findBlockInHotbar()
            itemSlot = -1

            if (slots == -1) return

            if (mode.value == Mode.Normal && autoJump.value != JumpMode.Off && centerMode.value != CenterMode.Off && !HoleUtils.isBurrowed(Globals.mc.player) && centerVector != Vec3d.ZERO) {
                teleportToCenter()
            }

            if (mode.value == Mode.Instant || jumped || autoJump.value != JumpMode.Off) {
                val fallBlock = Globals.mc.player.flooredPosition.down() // Globals.mc.player.positionVector.subtract(0.0, 1.0, 0.0).toBlockPos()
                if (fallBlock.up(3).isAir) {
                    targetPos = fallBlock
                    if (targetPos != null) {
                        jumpTicksCount++
                        itemSlot = slots
                        val rotationTo = RotationUtils.getRotationTo(targetPos!!.toVec3dCenter())
                        lastRotation = rotationTo
                        if (rotate.value == RotateMode.Tick || rotate.value == RotateMode.Both) addMotion { rotate(rotationTo) }
                    }
                }
            }

        } else if (event.stage == EventStageable.EventStage.POST && itemSlot != -1 && targetPos != null && (mode.value == Mode.Instant || jumped || autoJump.value != JumpMode.Off)) {
            if (mode.value == Mode.Normal && jumpTicksCount >= 2 + delay.value) {
                placeBlock(targetPos!!)
                jumpTicksCount = 0
            } else if (mode.value == Mode.Instant) {
//                if (placeInstantBlock(targetPos)) {
//                    ChatManager.sendMessage("success")
//                } else {
//                    ChatManager.sendMessage("fail")
//                }
                placeInstantBlock(targetPos!!)

                /*
                val upperPos = targetPos!!.up()
                val hitVec = BlockUtil.getHitVec(targetPos!!, EnumFacing.UP)

                val currentY = Globals.mc.player.posY
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 0.42, Globals.mc.player.posZ, false))
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 0.75, Globals.mc.player.posZ, false))
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 1.0, Globals.mc.player.posZ, false))
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 1.16, Globals.mc.player.posZ, false))
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 1.23, Globals.mc.player.posZ, false))
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 1.2, Globals.mc.player.posZ, false))

                val hitVecX = (hitVec.x - upperPos.x).toFloat()
                val hitVecY = (hitVec.y - upperPos.y).toFloat()
                val hitVecZ = (hitVec.z - upperPos.z).toFloat()

                val isEqualToSlot = Globals.mc.player.inventory.currentItem != itemSlot
                val lastSlot = Globals.mc.player.inventory.currentItem

                if (isEqualToSlot) InventoryUtils.swapSlot(itemSlot)

                if (rotateConfirm.value) BlockUtil.faceVectorPacketInstant(hitVec)

                Globals.mc.player.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(targetPos!!, EnumFacing.UP, EnumHand.MAIN_HAND, hitVecX, hitVecY, hitVecZ))
                if (swingArm.value) Globals.mc.player.swingArm(EnumHand.MAIN_HAND) else Globals.mc.player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))

                if (isEqualToSlot) InventoryUtils.swapSlot(lastSlot)

                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + offset.value, Globals.mc.player.posZ, Globals.mc.player.onGround))
                if (stable.value) Globals.mc.player.connection.sendPacket(CPacketConfirmTeleport(++teleportIds))

                 */
            }

        }

        if (mode.value == Mode.Instant || jumped || autoJump.value != JumpMode.Off) {
            if (Globals.mc.player.onGround) ++groundTicks
        }

        if (HoleUtils.isBurrowed(Globals.mc.player)) {
            disable()
            return
        }

        if (groundTicks >= 2) {
            disable()
        }

        if (currentState == State.Done && toggleTimer.passed(450.0)) {
            disable()
            toggleTimer.reset()
        }

    }

    @Listener
    private fun onJump(event: JumpEvent) {
        jumped = true
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (event.packet is SPacketPlayerPosLook) {
            var x = event.packet.x
            var y = event.packet.y
            var z = event.packet.z
            var yaw = event.packet.yaw
            var pitch = event.packet.pitch

            if (event.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.X)) {
                x += Globals.mc.player.posX
            } else {
                Globals.mc.player.motionX = 0.0
            }

            if (event.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.Y)) {
                y += Globals.mc.player.posY
            } else {
                Globals.mc.player.motionY = 0.0
            }

            if (event.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.Z)) {
                z += Globals.mc.player.posZ
            } else {
                Globals.mc.player.motionZ = 0.0
            }

            if (event.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.X_ROT)) {
                pitch += Globals.mc.player.rotationPitch
            }

            if (event.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.Y_ROT)) {
                yaw += Globals.mc.player.rotationYaw
            }

            teleportIds = event.packet.teleportId

            if (stable.value) {
                Globals.mc.player.setPositionAndRotation(x, y, z, yaw, pitch)
                Globals.mc.player.connection.sendPacket(CPacketConfirmTeleport(event.packet.teleportId))
                Globals.mc.player.connection.sendPacket(CPacketPlayer.PositionRotation(Globals.mc.player.posX, Globals.mc.player.entityBoundingBox.minY, Globals.mc.player.posZ, Globals.mc.player.rotationYaw, Globals.mc.player.rotationPitch, true))
            }

        }

    }

    private fun placeInstantBlock(pos: BlockPos): Boolean {
        if (itemSlot == -1 || !pos.canPlaceNoCollide) {
            return false
        }

        val upperPos = pos.up()
        val currentY = Globals.mc.player.posY

        val result = RayTraceUtils.getRayTraceResult(lastRotation)
        val hitVec = result.hitVec

        val hitVecX = (hitVec.x - upperPos.x).toFloat()
        val hitVecY = (hitVec.y - upperPos.y).toFloat()
        val hitVecZ = (hitVec.z - upperPos.z).toFloat()

        val lastSlot = Globals.mc.player.inventory.currentItem
        val shouldSwap = LocalHotbarManager.serverSideHotbar != itemSlot

        val shouldSneak = !Globals.mc.player.isSneaking && (pos.block.onBlockActivated(Globals.mc.world, pos, pos.state, Globals.mc.player, EnumHand.MAIN_HAND, result.sideHit, 0.0F, 0.0F, 0.0F) || rightClickableBlock.contains(pos.block) || pos.needTileSneak)
        val isSprinting = Globals.mc.player.isSprinting

        if (rotate.value == RotateMode.Speed || rotate.value == RotateMode.Both) RotationUtils.faceVectorWithPositionPacket(pos.toVec3dCenter())
        if (shouldSwap) InventoryUtils.swapSlot(itemSlot)

        val (isFour, offsets) = getJumpOffsets(currentY)
        val onGround = if (isFour) Globals.mc.player.onGround else false

        if (least.value) {
            for (offset in offsets) {
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + offset, Globals.mc.player.posZ, onGround))
            }
        } else {
            Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 0.419997086886978, Globals.mc.player.posZ, false))
            Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 0.7500029, Globals.mc.player.posZ, false))
            Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 0.9999942, Globals.mc.player.posZ, false))
            Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 1.170005801788139, Globals.mc.player.posZ, false))
            Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 1.170005801788139, Globals.mc.player.posZ, false))
        }

        if (isSprinting) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SPRINTING))
        if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))

        Globals.mc.player.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(pos, EnumFacing.UP, EnumHand.MAIN_HAND, hitVecX, hitVecY, hitVecZ))
        if (swingArm.value) Globals.mc.player.swingArm(EnumHand.MAIN_HAND) else Globals.mc.player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))

        if (dynamicOffset.value) {
            if (least.value) {
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 1.242605801394748, Globals.mc.player.posZ, false))
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 1.810026103576277, Globals.mc.player.posZ, false))
            } else {
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 1.242605801394748, Globals.mc.player.posZ, false))
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 2.340026103576277, Globals.mc.player.posZ, false))
            }
        } else {
            Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + this.offset.value, Globals.mc.player.posZ, Globals.mc.player.onGround))
        }

        if (fastReact.value) Globals.mc.player.connection.sendPacket(CPacketConfirmTeleport(++teleportIds))

        if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
        if (isSprinting) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SPRINTING))
        if (shouldSwap) InventoryUtils.swapSlot(lastSlot)

        currentState = State.Done
        return true
    }

    private fun placeBlock(pos: BlockPos): Boolean {
        if (!pos.canPlaceNoCollide) return false

        val facing = pos.firstSide ?: return false

        val offset = pos.offset(facing)
        val opposite = facing.opposite

        val eyesPos = Globals.mc.player.eyePosition
        val hitVec = offset.getHitVec(opposite)

        if (!offset.canBeClicked || eyesPos.distanceTo(hitVec) > 5.0) return false

        val isNotEqualsToBlock = LocalHotbarManager.serverSideHotbar != itemSlot
        val isSprinting = Globals.mc.player.isSprinting
        val lastSlot = Globals.mc.player.inventory.currentItem

        val neighbourPos = offset.block
        val shouldSneak = rightClickableBlock.contains(neighbourPos)

        if (rotate.value == RotateMode.Speed || rotate.value == RotateMode.Both) RotationUtils.faceVectorWithPositionPacket(pos.toVec3dCenter())

        if (isNotEqualsToBlock) InventoryUtils.swapSlot(itemSlot)
        if (isSprinting) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SPRINTING))
        if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))

        Globals.mc.playerController.processRightClickBlock(Globals.mc.player, Globals.mc.world, offset, opposite, hitVec, EnumHand.MAIN_HAND)
        if (swingArm.value) Globals.mc.player.swingArm(EnumHand.MAIN_HAND) else Globals.mc.player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))

        Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + this.offset.value, Globals.mc.player.posZ, Globals.mc.player.onGround))
        // Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, startPos.y, Globals.mc.player.posZ, Globals.mc.player.onGround))
        // Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY - 1,  Globals.mc.player.posZ)
        if (fastReact.value) Globals.mc.player.connection.sendPacket(CPacketConfirmTeleport(++teleportIds))

        if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
        if (isSprinting) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SPRINTING))

        if (isNotEqualsToBlock) InventoryUtils.swapSlot(lastSlot)
        return true
    }


}