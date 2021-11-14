package me.han.muffin.client.module.modules.player

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.ClientTickEvent
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.manager.managers.HoleManager.isInHole
import me.han.muffin.client.manager.managers.LocalHotbarManager
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.exploits.NoMineAnimationModule
import me.han.muffin.client.module.modules.movement.SpeedModule
import me.han.muffin.client.module.modules.movement.StepModule
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.block.BlockUtil
import me.han.muffin.client.utils.block.BlockUtil.ValidResult
import me.han.muffin.client.utils.block.HoleUtils
import me.han.muffin.client.utils.block.HoleUtils.centerPlayer
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.extensions.mc.block.*
import me.han.muffin.client.utils.extensions.mixin.misc.rightClickDelayTimer
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.math.rotation.Vec2f
import me.han.muffin.client.utils.timer.StopTimer
import me.han.muffin.client.utils.timer.TimeUnit
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameType
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.abs

object AutoFeetPlace: Module("AutoFeetPlace", Category.PLAYER, "Surrounds you with obsidian to take less damage from crystal.", 250) {

    private val checks = EnumValue(Check.Normal, "Checks")

    private val swingArm = Value(true, "SwingArm")
    private val full = Value(true, "Full")
    private val center = Value(true, "Center")
    private val disableStrafe = Value(true, "DisableStrafe")
    private val disableOffGround = Value(true, "DisableOffGround")
    private val disableWhileStrafe = Value(true, "DisableWhileStrafe")
    private val disableWhileStep = Value(true, "DisableWhileStep")
    private val jumpDisable = Value(true, "JumpDisable")
    private val outOfHolesTimeout = NumberValue(25, 0, 40, 1, "OutOfHolesTimeout")
    private val blocksPerTick = NumberValue(3, 1, 10, 1, "BlocksPerTick")
    private val delay = NumberValue(3, 0, 10, 1, "Delay")
    private val teleportDelay = NumberValue(1.2f, 0.0f, 3.0f, 0.1f, "TeleportDelay")
    private val render = Value(true, "Render")
    private val rotateConfirm = Value(false, "RotateConfirm")

    private val renderBlock = BlockPos.MutableBlockPos(0, -69, 0)

    private val centerTimer = Timer()
    private val timer = Timer()

    private var obsidianSlot = -1
    private var inactiveTicks = 0

    private var rotationTo = Vec2f.ZERO
    private var placements = 0

    private var didPlace = false

    private val toggleTimer = StopTimer(TimeUnit.TICKS)

    private val holePos = BlockPos.MutableBlockPos(0, -69, 0)
    private var blockData: Pair<BlockPos, EnumFacing>? = null
    private var centerVector = Vec3d.ZERO

    private enum class Check {
        Normal, Full, Fast
    }

    init {
        addSettings(
            checks,
            center, jumpDisable, outOfHolesTimeout,
            swingArm, full, disableStrafe,
            disableOffGround, disableWhileStrafe, disableWhileStep,
            blocksPerTick, delay, teleportDelay,
            render, rotateConfirm
        )
    }

    override fun onEnable() {
        if (fullNullCheck()) return

        toggleTimer.reset(0L)
        centerVector = BlockUtil.getCenter(Globals.mc.player.positionVector)

        if (center.value && !Globals.mc.player.isInHole && centerVector != Vec3d.ZERO && getObsidianFromHotbar() != -1) {
            centerPlayer()
        }

    }

    override fun onDisable() {
        if (fullNullCheck()) return

        blockData = null
        obsidianSlot = -1
        toggleTimer.reset(0L)

        holePos.setNull()
        inactiveTicks = 0
    }

    private fun getPlaceableBlock(full: Boolean): Pair<BlockPos, EnumFacing>? {
        val playerPos = Globals.mc.player.flooredPosition

        for (facing in EnumFacing.Plane.HORIZONTAL.facings()) {

            val neighbour = playerPos.offset(facing)
            val downBlock = neighbour.down()
            val opposite = facing.opposite

            val downBlockNeighbour = downBlock.offset(opposite)
            if (!Globals.mc.world.mayPlace(Blocks.OBSIDIAN, downBlock, false, EnumFacing.DOWN, null)) {
                if (Globals.mc.world.mayPlace(Blocks.OBSIDIAN, neighbour, false, EnumFacing.DOWN, null)) {
                    return Pair(downBlock, EnumFacing.UP)
                }
            } else if (full && !Globals.mc.world.mayPlace(Blocks.OBSIDIAN, downBlockNeighbour, false, opposite, null) && Globals.mc.world.mayPlace(Blocks.OBSIDIAN, downBlock, false, opposite, null)) {
                return Pair(downBlockNeighbour, facing)
            }
        }

        return null
    }

    private fun getObsidianFromHotbar(): Int {
        val item = Globals.mc.player.inventory.getCurrentItem().item

        if (item is ItemBlock && item.block == Blocks.OBSIDIAN) return Globals.mc.player.inventory.currentItem

        for (i in 0 until 9) {
            val slot = Globals.mc.player.inventory.getStackInSlot(i).item
            if (slot is ItemBlock && slot.block === Blocks.OBSIDIAN) {
                return i
            }
        }

        return -1
    }

    @Listener
    private fun onMoving(event: MoveEvent) {
        if (fullNullCheck()) return

        if (disableStrafe.value) disable()

        if (center.value && !Globals.mc.player.isInHole && centerVector != Vec3d.ZERO && obsidianSlot != -1 && centerTimer.passed((teleportDelay.value * 1000f).toDouble())) {
            centerPlayer()
            centerTimer.reset()
        }

    }

    private fun centerPlayer() {
        val x = abs(centerVector.x - Globals.mc.player.posX)
        val z = abs(centerVector.z - Globals.mc.player.posZ)
        if (x <= 0.1 && z <= 0.1) {
            centerVector = Vec3d.ZERO
        } else {
            Globals.mc.player.centerPlayer(false)
        }
    }

    private fun inHoleCheck(): Boolean {
        return Globals.mc.player.onGround && MovementUtils.motionSpeed < 0.15 && Globals.mc.player.isInHole
    }

    private fun outOfHoleCheck() {
        if (outOfHolesTimeout.value > 0 && toggleTimer.stop() > outOfHolesTimeout.value || jumpDisable.value && Globals.mc.player.movementInput.jump) {
            disable()
        }
    }

    private fun isPlaceable(): Boolean {
        val playerPos = Globals.mc.player.flooredPosition
        return HoleUtils.surroundOffset.any {
            val pos = playerPos.add(it)
            pos.isPlaceable(true)
        }
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (fullNullCheck()) return

        if (event.stage == EventStageable.EventStage.PRE) addMotion { rotate(rotationTo) }

        if (checks.value != Check.Normal) return

        didPlace = false
        placements = 0
        inactiveTicks++
        renderBlock.setNull()

        if (inactiveTicks > 5) rotationTo = RotationUtils.getPlayerRotation(1.0F)

        if (holePos.isNull || inHoleCheck()) {
            holePos.setPos(Globals.mc.player.flooredPosition)
        }

        if (Globals.mc.player.flooredPosition != holePos) {
            outOfHoleCheck()
        } else {
            toggleTimer.reset(0L)
        }

        if (event.stage == EventStageable.EventStage.PRE) {
            val obbySlot = getObsidianFromHotbar()
            obsidianSlot = -1

            var currentBlockData = getPlaceableBlock(false)

            if (full.value && obbySlot != -1 && currentBlockData == null) {
                currentBlockData = getPlaceableBlock(true)
            }

            blockData = currentBlockData

            if (blockData != null) {
                obsidianSlot = obbySlot
                rotationTo = RotationUtils.getRotationTo(Vec3d(blockData!!.first))
            }

        } else if (event.stage == EventStageable.EventStage.POST && timer.passedTicks(delay.value) && blockData != null && obsidianSlot != -1) {
            doNormalCheckPlacements(blockData!!.first, blockData!!.second, obsidianSlot)
            if (didPlace) timer.reset()
        }


    }

    private fun doNormalCheckPlacements(pos: BlockPos, facing: EnumFacing, slot: Int) {
        if (placements < blocksPerTick.value) {

            if (render.value) renderBlock.setPos(pos)

            val hitVec = pos.getHitVec(facing)
            inactiveTicks = 0

            val lastSlot = Globals.mc.player.inventory.currentItem
            val shouldSwap = LocalHotbarManager.serverSideHotbar != slot
            val shouldSneak = !Globals.mc.player.isSneaking && (rightClickableBlock.contains(pos.block) || pos.needTileSneak)

            if (shouldSwap) InventoryUtils.swapSlot(slot)
            if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))

            Globals.mc.playerController.processRightClickBlock(Globals.mc.player, Globals.mc.world, pos, facing, hitVec, EnumHand.MAIN_HAND)
            Globals.mc.player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))

            if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
            if (shouldSwap) InventoryUtils.swapSlot(lastSlot)

            didPlace = true
            ++placements
        }
    }

    @Listener
    private fun onTicking(event: ClientTickEvent) {
        if (fullNullCheck()) return

        if (disableWhileStep.value && StepModule.isEnabled && MovementUtils.isMovingSpeed && Globals.mc.player.onGround) {
            disable()
            return
        }

        if (disableWhileStrafe.value && SpeedModule.isEnabled && MovementUtils.isMovingSpeed && !Globals.mc.player.onGround) {
            disable()
            return
        }

        if (disableWhileStrafe.value && !Globals.mc.player.onGround) {
            disable()
            return
        }

        if (checks.value == Check.Normal) return
        didPlace = false
        placements = 0
        inactiveTicks++
        renderBlock.setNull()

        if (inactiveTicks > 5) rotationTo = RotationUtils.getPlayerRotation(1f)

        obsidianSlot = getObsidianFromHotbar()

        if (obsidianSlot == -1) return

        if (!timer.passedTicks(delay.value)) return

        // Update hole pos
        if (holePos.isNull || inHoleCheck()) {
            holePos.setPos(Globals.mc.player.flooredPosition)
        }

        // Out of hole check
        if (Globals.mc.player.flooredPosition != holePos) {
            outOfHoleCheck()
        } else {
            toggleTimer.reset(0L)
        }

        val flooredPosition = Globals.mc.player.flooredPosition

        if (checks.value == Check.Fast) {
            val placeTargets = arrayListOf<Vec3d>().apply {
                addAll(HoleUtils.surroundTargets)
            }

            for (vector in placeTargets) {
                val position = BlockPos(vector)
                val pos = flooredPosition.down().add(position)

                val validResult = BlockUtil.valid(pos)

                if (validResult == ValidResult.NoEntityCollision) {
                    placeAtPos(pos)
                    continue
                }

                if (validResult != ValidResult.Ok || MathUtils.areVec3dsAligned(Globals.mc.player.positionVector, vector)) {
                    continue
                }

                placeAtPos(pos)
            }

        } else {


            val surroundBlocks = arrayListOf<BlockPos>().apply {
                EnumFacing.HORIZONTALS.forEach { add(flooredPosition.offset(it)) }

                if (full.value) {
                    val box = Globals.mc.player.entityBoundingBox
                    val sets = hashSetOf(
                        BlockPos(box.minX - 1, box.minY - 1, box.minZ), BlockPos(box.minX, box.minY - 1, box.minZ - 1),
                        BlockPos(box.maxX + 1, box.minY - 1, box.minZ), BlockPos(box.maxX, box.minY - 1, box.minZ - 1),
                        BlockPos(box.minX - 1, box.minY - 1, box.maxZ), BlockPos(box.minX, box.minY - 1, box.maxZ + 1),
                        BlockPos(box.maxX + 1, box.minY - 1, box.maxZ), BlockPos(box.maxX, box.minY - 1, box.maxZ + 1),
                        BlockPos(box.minX - 1, box.minY, box.minZ), BlockPos(box.minX, box.minY, box.minZ - 1),
                        BlockPos(box.maxX + 1, box.minY, box.minZ), BlockPos(box.maxX, box.minY, box.minZ - 1),
                        BlockPos(box.minX - 1, box.minY, box.maxZ), BlockPos(box.minX, box.minY, box.maxZ + 1),
                        BlockPos(box.maxX + 1, box.minY, box.maxZ), BlockPos(box.maxX, box.minY, box.maxZ + 1)
                    )
                    addAll(sets)
                }
            }

            for (pos in surroundBlocks) {
                val result = BlockUtil.valid(pos)

                if (result == ValidResult.AlreadyBlockThere && !pos.state.isReplaceable) continue
                if (result ==  ValidResult.NoEntityCollision) continue

                if (result == ValidResult.NoNeighbours) {

                    val northPos = pos.north()
                    val southPos = pos.south()
                    val eastPos = pos.east()
                    val westPos = pos.west()

                    val downPos = pos.down()

                    val alternatePos = arrayOf(
                        northPos, southPos, eastPos, westPos,
                        downPos, downPos.north(), downPos.south(), downPos.east(), downPos.west(),
                        pos.up(), northPos.up(), southPos.up(), eastPos.up(), westPos.up(),
                        pos.up().up(), downPos.down()
                    )

                    for (alternate in alternatePos) {
                        val alternateResult = BlockUtil.valid(alternate)
                        if (alternateResult == ValidResult.NoEntityCollision || alternateResult == ValidResult.NoNeighbours) continue
                        placeAtPos(alternate)
                        break
                    }

                    continue
                }

                placeAtPos(pos)
            }
        }

        if (didPlace) {
            timer.reset()
        }
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        if (render.value && renderBlock.isNotNull) {
            Muffin.getInstance().blockRenderer.drawFull(renderBlock, 40)
        }
    }

    private fun placeAtPos(pos: BlockPos) {
        if (placements < blocksPerTick.value) {
            if (render.value) renderBlock.setPos(pos)

            placeBlockInRange(pos)
            didPlace = true
            ++placements
        }
    }

    private fun placeBlockInRange(pos: BlockPos): Boolean {
        if (!pos.isPlaceable() || !pos.canPlaceNoCollide) return false

        val side = pos.firstSide ?: return false

        val neighbour = pos.offset(side)
        val opposite = side.opposite

        if (!neighbour.canBeClicked) return false

        val hitVec = neighbour.getHitVec(opposite)

        inactiveTicks = 0
        rotationTo = RotationUtils.getRotationTo(hitVec)

        val lastSlot = Globals.mc.player.inventory.currentItem
        val shouldSwap = LocalHotbarManager.serverSideHotbar != obsidianSlot
        val shouldSneak = !Globals.mc.player.isSneaking && (rightClickableBlock.contains(neighbour.block) || neighbour.needTileSneak)

        if (shouldSwap) InventoryUtils.swapSlot(obsidianSlot)
        if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))

        if (rotateConfirm.value) RotationUtils.faceVectorWithPositionPacket(hitVec)

        Globals.mc.playerController.processRightClickBlock(Globals.mc.player, Globals.mc.world, neighbour, opposite, hitVec, EnumHand.MAIN_HAND)
        if (swingArm.value) Globals.mc.player.swingArm(EnumHand.MAIN_HAND) else Globals.mc.player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))

        Globals.mc.rightClickDelayTimer = 4

        if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
        if (shouldSwap) InventoryUtils.swapSlot(lastSlot)

        if (Globals.mc.playerController.currentGameType != GameType.CREATIVE)
            Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, neighbour, opposite))

        if (NoMineAnimationModule.isEnabled) NoMineAnimationModule.resetMining()

        return true
    }

}