package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.event.events.entity.player.PlayerPushEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.manager.managers.LocalHotbarManager
import me.han.muffin.client.manager.managers.LocalMotionManager
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.color.ColourUtils
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.entity.EntityUtil.prevPosVector
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.extensions.kotlin.toDegree
import me.han.muffin.client.utils.extensions.kotlin.toRadian
import me.han.muffin.client.utils.extensions.mc.block.*
import me.han.muffin.client.utils.extensions.mc.world.searchForNeighbour
import me.han.muffin.client.utils.extensions.mixin.misc.rightClickDelayTimer
import me.han.muffin.client.utils.math.PlaceInfo
import me.han.muffin.client.utils.math.RayTraceUtils
import me.han.muffin.client.utils.math.VectorUtils.toBlockPos
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.math.rotation.Vec2f
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.utils.timer.TickTimer
import me.han.muffin.client.utils.timer.TimeUnit
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.init.Blocks
import net.minecraft.item.ItemAir
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemSlab
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketHeldItemChange
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.stats.StatList
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.*

internal object ScaffoldModule: Module("Scaffold", Category.PLAYER, "Places blocks under you.") {
    private val mode = EnumValue(Mode.Normal, "Mode")

    private val towerMode = EnumValue(TowerMode.Motion, "TowerMode")

    private val keepSprint = Value(false, "KeepSprint")
    private val keepRotations = Value(false, "KeepRotations")
    private val strictDirection = Value(true, "StrictDirection")

    private val delay = NumberValue(50F, 1F, 500F, 1F, "Delay")
    private val yawStep = NumberValue(160F, 10F, 180F, 2F, "YawStep")
    private val maxRange = NumberValue(1, 0, 3, 1, "MaxRange")

    private val swingArm = Value(true, "SwingArm")

    private val stopMotion = Value(false, "StopMotion")
    private val preferObsidian = Value(true, "PreferObsidian")
    private val render = Value(true, "Render")

    private val renderPos = BlockPos.MutableBlockPos(0, -69, 0)
    private var blockData: PlaceInfo? = null
    // private var targetPlace: PlaceInfo? = null

    private val timer = Timer()
    private val stopMotionTimer = Timer()
    private var cachedSlot = -1
    private var canScaffold = false
    private val rubberBandTimer = TickTimer(TimeUnit.TICKS)

    private var limitedRotation = Vec2f.ZERO
    private var lastRotation = Vec2f.ZERO

    private var facesBlock = false
    private var jumpGround = 0.0

    init {
        addSettings(mode, towerMode, keepSprint, keepRotations, strictDirection, delay, yawStep, maxRange, swingArm, stopMotion, preferObsidian, render)
    }

    private enum class Mode {
        Normal, Strict
    }

    private enum class TowerMode {
        None, Motion, Constant, Bypass
    }

    override fun onEnable() {
        cachedSlot = -1
        blockData = null
        // targetPlace = null
        renderPos.setNull()
    }

    override fun onDisable() {
        facesBlock = false
        lastRotation = Vec2f.ZERO
        limitedRotation = Vec2f.ZERO
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (stopMotion.value && stopMotionTimer.passed(150.0) && Globals.mc.player.onGround) {
            Globals.mc.player.motionZ = 0.0
            Globals.mc.player.motionX = 0.0
            stopMotionTimer.reset()
        }

        if (keepRotations.value && lastRotation != Vec2f.ZERO) {
            addMotion { rotate(lastRotation) }
        }

        updateIsFaced()

        if (event.stage == EventStageable.EventStage.PRE) {
            val slot = findBlockInHotbar()

            renderPos.setNull()
            blockData = null
            cachedSlot = -1

            if (Globals.mc.player.isSneaking || slot == -1) return

            val cos = cos((Globals.mc.player.rotationYaw + 90.0F).toRadian())
            val sin = sin((Globals.mc.player.rotationYaw + 90.0F).toRadian())
            val xOffset = Globals.mc.player.movementInput.moveForward * 0.4 * cos + Globals.mc.player.movementInput.moveStrafe * 0.4 * sin
            val zOffset = Globals.mc.player.movementInput.moveForward * 0.4 * sin - Globals.mc.player.movementInput.moveStrafe * 0.4 * cos

            val currentEntity = (if (Globals.mc.player.isRiding) Globals.mc.player.ridingEntity else Globals.mc.player) ?: Globals.mc.player
            canScaffold = mode.value != Mode.Strict

            var modifiedPosY = 1.0
            if (Globals.mc.player.inventory.getStackInSlot(slot).item is ItemSlab) {
                modifiedPosY = 0.5
            }

            val pos =
                if (Globals.mc.player.posY == Globals.mc.player.posY.toInt() + 0.5)
                    Globals.mc.player.flooredPosition
                else BlockPos(
                    currentEntity.posX + if (canScaffold) xOffset else 0.0,
                    currentEntity.posY - modifiedPosY,
                    currentEntity.posZ + if (canScaffold) zOffset else 0.0
                )

            if (!pos.isFullBox && pos.isPlaceable()) {
                renderPos.setPos(pos)

                blockData = calcNextPos()?.let {
                    searchForNeighbour(it, 1, visibleSideCheck = strictDirection.value, sides = arrayOf(EnumFacing.DOWN))
                        ?: searchForNeighbour(it, 3, visibleSideCheck = strictDirection.value, sides = EnumFacing.HORIZONTALS)
                }

                cachedSlot = slot
                if (blockData != null) {
                    val rotation = getRotations(blockData!!.pos, blockData!!.side)

                    val maxTurnSpeed = yawStep.value
                    val minTurnSpeed = maxTurnSpeed.div(2.55)

                    val limitedRotation =
                        if (yawStep.value == 0F || yawStep.value == 180F) rotation
                        else RotationUtils.limitAngleChange(LocalMotionManager.serverSideRotation, rotation, ((Math.random() * (maxTurnSpeed - minTurnSpeed) + minTurnSpeed).toFloat()))

                    lastRotation = limitedRotation
                    addMotion { rotate(limitedRotation) }
                    return
                }
            } else {
                if (!timer.passed(delay.value * 5.0)) {
                    addMotion { rotate(lastRotation) }
                    return
                }
            }

//            if (BlockUtil.isPlaceable(pos) && search(pos, true)) {
//                renderPos = pos
//                cachedSlot = slot
//            }

        } else if (event.stage == EventStageable.EventStage.POST && blockData != null && timer.passed(delay.value.toDouble()) && cachedSlot != -1 && facesBlock) {
            val result = RayTraceUtils.getRayTraceResult(lastRotation)

                if (towerMode.value != TowerMode.None) {
                    Globals.mc.rightClickDelayTimer = 3
                    if (canTower && Globals.mc.player.movementInput.jump && !MovementUtils.isMoving()) {
                        Globals.mc.rightClickDelayTimer = 0

                        when (towerMode.value) {
                            TowerMode.Motion -> {
                                Globals.mc.player.jump()
                                if (Globals.mc.player.motionY < 0.1) {
                                    Globals.mc.player.motionY = -0.3
                                }
                            }
                            TowerMode.Constant -> {
                                if (Globals.mc.player.onGround) {
                                    fakeJump()
                                    jumpGround = Globals.mc.player.posY
                                    Globals.mc.player.motionY = 0.42
                                }
                                if (Globals.mc.player.posY > jumpGround + 0.79) {
                                    fakeJump()
                                    Globals.mc.player.setPosition(Globals.mc.player.posX, truncate(Globals.mc.player.posY), Globals.mc.player.posZ)
                                    Globals.mc.player.motionY = 0.42
                                    jumpGround = Globals.mc.player.posY
                                }
                            }
                            TowerMode.Bypass -> {
                                if (Globals.mc.player.ticksExisted % 4 == 1) {
                                    Globals.mc.player.motionY = 0.4195464
                                    Globals.mc.player.setPosition(Globals.mc.player.posX - 0.035, Globals.mc.player.posY, Globals.mc.player.posZ)
                                } else if (Globals.mc.player.ticksExisted % 4 == 0) {
                                    Globals.mc.player.motionY = -0.5
                                    Globals.mc.player.setPosition(Globals.mc.player.posX + 0.035, Globals.mc.player.posY, Globals.mc.player.posZ)
                                }
                            }
                        }

                       // if (rubberBandTimer.tick(10, false)) {
                       //     Globals.mc.player.motionY = 0.41999998688697815
                       // } else if (Globals.mc.player.fallDistance <= 2.0f) {
                       //     Globals.mc.player.motionY = -0.169
                       // }
                       // event.location.y = 0.0
                    }
                }

                val lastSlot = Globals.mc.player.inventory.currentItem
                val isNotEqualsToBlock = LocalHotbarManager.serverSideHotbar != cachedSlot
                val isSprinting = !keepSprint.value && Globals.mc.player.isSprinting


                val neighbourBlock = blockData!!.pos.block

                val isActivated =
                    blockData!!.pos.needTileSneak ||
                    //neighbourBlock.onBlockActivated(Globals.mc.world, blockData!!.position, blockData!!.position.state, Globals.mc.player, EnumHand.MAIN_HAND, blockData!!.face, result.hitVec.x.toFloat(), result.hitVec.y.toFloat(), result.hitVec.z.toFloat()) ||
                    rightClickableBlock.contains(neighbourBlock)

                if (isNotEqualsToBlock) {
                    InventoryUtils.swapSlot(cachedSlot)
                }

                if (isSprinting) {
                    Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SPRINTING))
                }

                if (isActivated) {
                    Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))
                }

                Globals.mc.playerController.processRightClickBlock(Globals.mc.player, Globals.mc.world, blockData!!.pos, blockData!!.side, result.hitVec, EnumHand.MAIN_HAND)
                if (swingArm.value) Globals.mc.player.swingArm(EnumHand.MAIN_HAND) else Globals.mc.player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))

                if (isActivated) {
                    Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
                }

                if (isSprinting) {
                    Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SPRINTING))
                }

                if (isNotEqualsToBlock) {
                    InventoryUtils.swapSlot(lastSlot)
                }

            blockData = null
            timer.reset()
        }
    }



   // @Listener
   // private fun onJump(event: JumpEvent) {
       // event.cancel()
   // }

    var prevSlot = -1

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE) return
        if (event.packet is CPacketHeldItemChange) if (event.packet.slotId == prevSlot) event.cancel() else prevSlot = event.packet.slotId
    }

    @Listener
    private fun onMove(event: MoveEvent) {
        if (!stopMotion.value) return

        var x = event.x
        val y = event.y
        var z = event.z

        if (Globals.mc.player.onGround && !Globals.mc.player.noClip) {
            val increment = 0.05
            while (x != 0.0 && isOffsetBBEmpty(x, -1.0, 0.0)) {
                if (x < increment && x >= -increment) {
                    x = 0.0
                } else if (x > 0.0) {
                    x -= increment
                } else {
                    x += increment
                }
            }
            while (z != 0.0 && isOffsetBBEmpty(0.0, -1.0, z)) {
                if (z < increment && z >= -increment) {
                    z = 0.0
                } else if (z > 0.0) {
                    z -= increment
                } else {
                    z += increment
                }
            }
            while (x != 0.0 && z != 0.0 && isOffsetBBEmpty(x, -1.0, z)) {
                if (x < increment && x >= -increment) {
                    x = 0.0
                } else if (x > 0.0) {
                    x -= increment
                } else {
                    x += increment
                }
                if (z < increment && z >= -increment) {
                    z = 0.0
                } else if (z > 0.0) {
                    z -= increment
                } else {
                    z += increment
                }
            }
        }

        event.x = x
        event.y = y
        event.z = z
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (event.packet is SPacketPlayerPosLook) {
            rubberBandTimer.reset()
        }
    }


  //  private fun onTravel(event: TravelEvent) {
  //      if (Globals.mc.player == null || !tower.value || !Globals.mc.gameSettings.keyBindJump.isKeyDown || !isHoldingBlock) return
  //  }

    @Listener
    private fun onPlayerPushed(event: PlayerPushEvent) {
        if (towerMode.value != TowerMode.None && event.type == PlayerPushEvent.Type.BLOCK) {
            event.cancel()
        }
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        if (renderPos.isNull) return

        if (render.value && blockData != null) {
            val x1 = renderPos.x - RenderUtils.renderPosX
            val x2 = renderPos.x - RenderUtils.renderPosX + 1
            val y1 = renderPos.y - RenderUtils.renderPosY
            val y2 = renderPos.y - RenderUtils.renderPosY + 1
            val z1 = renderPos.z - RenderUtils.renderPosZ
            val z2 = renderPos.z - RenderUtils.renderPosZ + 1
            RenderUtils.drawBoxFullESP(AxisAlignedBB(x1, y1, z1, x2, y2, z2), ColourUtils.toRGBAClient(45), 2.0f)
        }
    }

    //Send jump packets, bypasses Hypixel.
    private fun fakeJump() {
        Globals.mc.player.isAirBorne = true
        Globals.mc.player.addStat(StatList.JUMP, 1)
    }

    private fun hasItemCheck(stack: ItemStack?) = stack == null || stack.isEmpty || stack.item is ItemAir

    private fun isOffsetBBEmpty(x: Double, y: Double, z: Double): Boolean {
        return Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(x, y, z)).isEmpty()
    }

    fun getRotations(pos: BlockPos, face: EnumFacing): Vec2f {
        val eyesPos = Vec3d(Globals.mc.player.posX, Globals.mc.player.entityBoundingBox.minY + Globals.mc.player.eyeHeight, Globals.mc.player.posZ)
        val hitVec = pos.getHitVec(face)

        val vecDiff = hitVec.subtract(eyesPos)
        val distance = hypot(vecDiff.x, vecDiff.z)

        val yaw = RotationUtils.normalizeAngle((atan2(vecDiff.z, vecDiff.x).toDegree()) - 90.0)
        val pitch = (-(atan2(vecDiff.y, distance)).toDegree())

        return Vec2f(yaw, pitch)
    }


    private fun findBlockInHotbar(): Int {
        val stack = Globals.mc.player.inventory.getCurrentItem()
        if (stack.count != 0 && stack.item is ItemBlock) {
            return Globals.mc.player.inventory.currentItem
        }

        val obsidianSlot = InventoryUtils.findBlock(Blocks.OBSIDIAN)
        if (preferObsidian.value && obsidianSlot != -1) return obsidianSlot

        return InventoryUtils.findGenericBlock()
    }

    private val canTower: Boolean
        get() = !Globals.mc.player.onGround
                && Globals.mc.player.posY - floor(Globals.mc.player.posY) <= 0.1

    private fun updateIsFaced() {
        if (blockData == null || yawStep.value == 180F || yawStep.value == 0F) {
            facesBlock = true
            return
        }

        facesBlock = abs(RotationUtils.getAngleDifference(getRotations(blockData!!.pos, blockData!!.side).x, LocalMotionManager.serverSideRotation.x)) <= 20
    }

//    private fun search(blockPosition: BlockPos, checks: Boolean): Boolean {
//        if (!CrystalUtils.isValidMaterial(blockPosition.material)) return false
//
//        // Search Ranges
//        val xzRV = 0.8
//        val xzSSV = BlockUtil.calcStepSize(xzRV, 8.0)
//        val yRV = 0.8
//        val ySSV = BlockUtil.calcStepSize(yRV, 0.8)
//
//        var xSearchFace = 0.0
//        var ySearchFace = 0.0
//        var zSearchFace = 0.0
//
//        val eyesPos = Vec3d(Globals.mc.player.posX, Globals.mc.player.entityBoundingBox.minY + Globals.mc.player.eyeHeight, Globals.mc.player.posZ)
//
//        var placeRotation: Pair<PlaceInfo, Vec2f>? = null
//
//        for (side in EnumFacing.values()) {
//            val neighbor = blockPosition.offset(side)
//            if (!BlockUtil.canBeClicked(neighbor)) continue
//            val dirVec = Vec3d(side.directionVec)
//
//            var xSearch = 0.5 - xzRV / 2
//            while (xSearch <= 0.5 + xzRV / 2) {
//                var ySearch = 0.5 - yRV / 2
//                while (ySearch <= 0.5 + yRV / 2) {
//                    var zSearch = 0.5 - xzRV / 2
//                    while (zSearch <= 0.5 + xzRV / 2) {
//
//                        val posVec = blockPosition.toVec3d(xSearch, ySearch, zSearch)
//                        val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
//
//                        val hitVec = posVec.add(Vec3d(dirVec.x * 0.5, dirVec.y * 0.5, dirVec.z * 0.5))
//                        if (checks && (eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) || Globals.mc.world.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null)) {
//                            zSearch += xzSSV
//                            continue
//                        }
//
//                        // Face block
//                        for (i in 0 until 1) {
//                            val vecDiff = hitVec.subtract(eyesPos)
//                            val diffXZ = hypot(vecDiff.x, vecDiff.z)
//
//                            val pitch = RotationUtils.normalizeAngle((-atan2(vecDiff.y, diffXZ).toDegree()))
//                            val rotation = Vec2f(
//                                RotationUtils.normalizeAngle((atan2(vecDiff.z, vecDiff.x).toDegree()) - 90.0),
//                                pitch
//                            )
//
//                            val rotationVector = RotationUtils.getVectorForRotation(rotation)
//                            val vector = eyesPos.add(rotationVector.x * 4, rotationVector.y * 4, rotationVector.z * 4)
//
//                            val result = Globals.mc.world.rayTraceBlocks(eyesPos, vector, false, false, true)
//                            if (result == null || result.typeOfHit != RayTraceResult.Type.BLOCK || result.blockPos != neighbor) continue
//
//                            if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(placeRotation.second)) {
//                                placeRotation = Pair(PlaceInfo(neighbor, side.opposite, hitVec), rotation)
//                            }
//
//                            xSearchFace = xSearch
//                            ySearchFace = ySearch
//                            zSearchFace = zSearch
//                        }
//
//                        zSearch += xzSSV
//                    }
//                    ySearch += ySSV
//                }
//                xSearch += xzSSV
//            }
//        }
//
//        if (placeRotation == null) return false
//
//
//        if (yawStep.value < 180) {
//            limitedRotation = RotationUtils.limitAngleChange(LocalMotionManager.serverSideRotation, placeRotation.second, ((Math.random() * (138.69 - 54.11) + 54.11).toFloat()))
//            addMotion { rotate(limitedRotation) }
//
//            lastRotation = limitedRotation
//            facesBlock = false
//
//            for (side in EnumFacing.values()) {
//                val neighbor = blockPosition.offset(side)
//                if (!BlockUtil.canBeClicked(neighbor)) continue
//
//                val dirVec = Vec3d(side.directionVec)
//                val posVec = blockPosition.toVec3d(xSearchFace, ySearchFace, zSearchFace)
//
//                val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
//                val hitVec = posVec.add(Vec3d(dirVec.x * 0.5, dirVec.y * 0.5, dirVec.z * 0.5))
//
//                if (checks && (eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) || Globals.mc.world.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null))
//                    continue
//
//                val rotationVector = RotationUtils.getVectorForRotation(limitedRotation)
//                val vector = eyesPos.add(rotationVector.x * 4, rotationVector.y * 4, rotationVector.z * 4)
//
//                val result = Globals.mc.world.rayTraceBlocks(eyesPos, vector, false, false, true)
//                if (result == null || result.typeOfHit != RayTraceResult.Type.BLOCK || result.blockPos != neighbor) continue
//
//                facesBlock = true
//                break
//            }
//        } else {
//            addMotion { rotate(placeRotation.second) }
//            lastRotation = placeRotation.second
//            facesBlock = true
//        }
//
//        targetPlace = placeRotation.first
//        return true
//    }

    private fun calcNextPos(): BlockPos? {
        val posVec = Globals.mc.player.positionVector
        val blockPos = posVec.toBlockPos()
        return checkPos(blockPos)
            ?: run {
                val realMotion = posVec.subtract(Globals.mc.player.prevPosVector)
                val nextPos = blockPos.add(roundToRange(realMotion.x), 0, roundToRange(realMotion.z))
                checkPos(nextPos)
            }
    }

    private fun checkPos(blockPos: BlockPos): BlockPos? {
        val center = Vec3d(blockPos.x + 0.5, blockPos.y.toDouble(), blockPos.z + 0.5)
        val rayTraceResult = Globals.mc.world.rayTraceBlocks(
            center,
            center.subtract(0.0, 0.5, 0.0),
            false,
            true,
            false
        )
        return blockPos.down().takeIf { rayTraceResult?.typeOfHit != RayTraceResult.Type.BLOCK }
    }

    private fun roundToRange(value: Double) =
        (value * 2.5 * maxRange.value).roundToInt().coerceAtMost(maxRange.value)

    private fun runTowerMode() {

    }

}