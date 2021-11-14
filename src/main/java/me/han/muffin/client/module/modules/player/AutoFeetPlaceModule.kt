package me.han.muffin.client.module.modules.player

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.HoleManager.isInHole
import me.han.muffin.client.manager.managers.HoleManager.isInHoleOne
import me.han.muffin.client.manager.managers.LocalHotbarManager
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.movement.SpeedModule
import me.han.muffin.client.module.modules.movement.StepModule
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.RotateMode
import me.han.muffin.client.utils.block.BlockUtil
import me.han.muffin.client.utils.block.HoleUtils.centerPlayer
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.entity.MovementUtils.speed
import me.han.muffin.client.utils.extensions.kotlin.synchronized
import me.han.muffin.client.utils.extensions.mc.block.*
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.item.firstBlock
import me.han.muffin.client.utils.extensions.mc.item.firstByStack
import me.han.muffin.client.utils.extensions.mc.item.hotbarSlots
import me.han.muffin.client.utils.extensions.mc.world.getVisibleSides
import me.han.muffin.client.utils.extensions.mc.world.placeBlock
import me.han.muffin.client.utils.extensions.mc.world.searchForNeighbour
import me.han.muffin.client.utils.math.PlaceInfo
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.utils.timer.StopTimer
import me.han.muffin.client.utils.timer.TimeUnit
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.concurrent.Executors
import java.util.function.Predicate
import kotlin.concurrent.thread
import kotlin.math.abs

internal object AutoFeetPlaceModule: Module("AutoFeetPlaceRework", Category.PLAYER, "Surrounds you with obsidian to take less damage from crystal.") {
    private val page = EnumValue(Pages.General, "Page")

    private val timing = EnumValue({ page.value == Pages.General }, Timings.Sequential, "Timing")
    private val rotateMode = EnumValue({ page.value == Pages.General }, RotateMode.Tick, "Rotate")

    private val centerMode = EnumValue({ page.value == Pages.General }, CenterMode.Motion, "Center")
    private val centerDelay = NumberValue({ page.value == Pages.General && centerMode.value != CenterMode.Off },2, 0, 10, 1, "CenterDelay")
    private val noGhost = Value({ page.value == Pages.General },false, "NoGhost")
    private val swingArm = Value({ page.value == Pages.General },false, "SwingArm")

    private val strictDirection = Value({ page.value == Pages.General },false, "StrictDirection")
    private val blocksPerTick = NumberValue({ page.value == Pages.General },3, 1, 10, 1, "BlocksPerTick")
    private val placeDelay = NumberValue({ page.value == Pages.General },1, 0, 10, 1, "PlaceDelay")

    private val disableMoving = Value({ page.value == Pages.Disabling }, false, "DisableMoving")
    private val disableJumping = Value({ page.value == Pages.Disabling },false, "DisableJumping")
    private val disableOffGround = Value({ page.value == Pages.Disabling }, false, "DisableOffGround")
    private val disableWhileStrafe = Value({ page.value == Pages.Disabling },false, "DisableWhileStrafe")
    private val disableWhileStep = Value({ page.value == Pages.Disabling },false, "DisableWhileStep")
    private val disableOutOfHole = NumberValue({ page.value == Pages.Disabling },10, 0, 40, 1, "DisableOutOfHole")

    private val renderMode = EnumValue({ page.value == Pages.Rendering }, RenderMode.Full, "RenderMode")

    private val renderRed = NumberValue({ page.value == Pages.Rendering && renderMode.value != RenderMode.None },15, 0, 255, 5, "Red")
    private val renderGreen = NumberValue({ page.value == Pages.Rendering && renderMode.value != RenderMode.None },50, 0, 255, 5, "Green")
    private val renderBlue = NumberValue({ page.value == Pages.Rendering && renderMode.value != RenderMode.None },165, 0, 255, 5, "Blue")
    private val renderAlpha = NumberValue({ page.value == Pages.Rendering && renderMode.value != RenderMode.None },25, 0, 255, 5, "Alpha")

    private val lineWidth = NumberValue({ page.value == Pages.Rendering && renderMode.value != RenderMode.None && renderMode.value != RenderMode.Solid }, 0.5F, 0.1F, 5.0F, 0.1F, "LineWidth")

    private var placeTicks = 0
    private var obsidianSlot = -1

    private var centerPos = Vec3d.ZERO

    private val holePos = BlockPos.MutableBlockPos(0, -69, 0)
    private val toggleTimer = StopTimer(TimeUnit.TICKS)

    private val renderBlock = BlockPos.MutableBlockPos(0, -69, 0)
    private val offsets = arrayListOf<BlockPos>().synchronized()

    private var offsetThread: Thread? = null
    private val offsetExecutor = Executors.newSingleThreadExecutor()

    private val centerDelayTimer = Timer()
    private val delayTimer = Timer()

    private enum class Pages {
        General, Disabling, Rendering
    }

    private enum class Timings {
        Sequential, Vanilla
    }

    private enum class CenterMode {
        Off, Motion, Teleport
    }

    private enum class RenderMode {
        None, Solid, Outline, Full
    }

    init {
        addSettings(
            page,
            timing, rotateMode, centerMode, centerDelay, noGhost, swingArm, strictDirection, blocksPerTick, placeDelay,
            disableMoving, disableJumping, disableOffGround, disableWhileStrafe, disableWhileStep, disableOutOfHole,
            renderMode, renderRed, renderGreen, renderBlue, lineWidth
        )
    }

    override fun onEnable() {
        if (fullNullCheck()) return
        updateOffsets()

        centerPos = BlockUtil.getCenter(Globals.mc.player.positionVector)

        renderBlock.setNull()
        offsets.clear()
    }

    override fun onToggle() {
        if (fullNullCheck()) return

        toggleTimer.reset()
        delayTimer.reset()
        centerDelayTimer.reset()
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return
        updateOffsets()

        if (renderMode.value == RenderMode.None || renderBlock.isNull) return

        val red = renderRed.value
        val green = renderGreen.value
        val blue = renderBlue.value
        val alpha = renderAlpha.value

        val lineWidth = lineWidth.value

        when (renderMode.value) {
            RenderMode.Solid -> RenderUtils.drawBlockESP(renderBlock, red, green, blue, alpha)
            RenderMode.Outline -> RenderUtils.drawBlockOutlineESP(renderBlock, red, green, blue, alpha, lineWidth)
            RenderMode.Full -> RenderUtils.drawBlockFullESP(renderBlock, red, green, blue, alpha, lineWidth)
        }

    }

    @Listener
    private fun onMoving(event: MoveEvent) {
        if (fullNullCheck()) return
        if (disableMoving.value) disable()
    }

    inline fun <reified B : Block, T : Slot> Iterable<T>.firstBlock(predicate: Predicate<ItemStack>? = null) =
        firstByStack { itemStack ->
            itemStack.item.let { it is ItemBlock && it.block is B } && (predicate == null || predicate.test(itemStack))
        }

    fun <T : Slot> Iterable<T>.firstBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
        firstByStack { itemStack ->
            itemStack.item.let { it is ItemBlock && it.block == block } && (predicate == null || predicate.test(itemStack))
        }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (timing.value != Timings.Sequential || fullNullCheck()) return

        if (doDisablingChecks()) {
            disable()
            return
        }

        if (Globals.mc.player.isInHoleOne) {
            return
        }

        if (event.stage == EventStageable.EventStage.PRE) {
            placeTicks = 0
            renderBlock.setNull()

            obsidianSlot = Globals.mc.player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)?.hotbarSlot ?: -1

            if (obsidianSlot == -1) {
                ChatManager.sendMessage("${ChatFormatting.RED}Missing obsidian in hotbar.")
                disable()
                return
            }

            doMoveToCenter()
        } else if (event.stage == EventStageable.EventStage.POST && obsidianSlot != -1 && delayTimer.passedTicks(placeDelay.value) && offsets.isNotEmpty()) {
            val eyesPos = Globals.mc.player.eyePosition
            val placeOffsets = offsets.sortedBy { Globals.mc.player.getDistanceSq(it) }

            for (offset in placeOffsets) {
                val neighbour = searchForNeighbour(offset, if (strictDirection.value) 1 else 4, Globals.mc.playerController.blockReachDistance, true)
                if (neighbour == null) {
                    val alternateNeighbours = getAlternateNeighbour(offset)
                    for (alternate in alternateNeighbours) {
                        val alternateNeighbour = searchForNeighbour(alternate, 1, Globals.mc.playerController.blockReachDistance, true) ?: continue
                        placeBlockCount(alternateNeighbour)
                    }
                    continue
                }

                placeBlockCount(neighbour)
            }

            delayTimer.reset()
        }

    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (timing.value != Timings.Vanilla || fullNullCheck()) return

        if (doDisablingChecks()) {
            disable()
            return
        }

        if (Globals.mc.player.isInHoleOne) {
            return
        }

        if (event.stage == EventStageable.EventStage.PRE) {
            placeTicks = 0
            renderBlock.setNull()

            obsidianSlot = Globals.mc.player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)?.hotbarSlot ?: -1

            if (obsidianSlot == -1) {
                ChatManager.sendMessage("${ChatFormatting.RED}Missing obsidian in hotbar.")
                disable()
                return
            }

            doMoveToCenter()

            if (!delayTimer.passedTicks(placeDelay.value)) return

            if (offsets.isEmpty()) return

            val eyesPos = Globals.mc.player.eyePosition
            val placeOffsets = offsets.sortedBy { Globals.mc.player.getDistanceSq(it) }

            for (offset in placeOffsets) {
                val neighbour = searchForNeighbour(offset, if (strictDirection.value) 1 else 4, Globals.mc.playerController.blockReachDistance, true)
                if (neighbour == null) {
                    val alternateNeighbours = getAlternateNeighbour(offset)
                    for (alternate in alternateNeighbours) {
                        if (!alternate.hasNeighbour || !alternate.isPlaceable()) continue
                        val alternateNeighbour = searchForNeighbour(alternate, 1, Globals.mc.playerController.blockReachDistance, true) ?: continue
                        placeBlockCount(alternateNeighbour)
                    }
                    continue
                }

                placeBlockCount(neighbour)
            }

            delayTimer.reset()
        }
    }

    private fun updateOffsets() {
        if (offsetThread == null || !offsetThread!!.isAlive || offsetThread!!.isInterrupted) {
            offsetThread = thread(start = false) {
                val tempOffsets = getOffsets()
                synchronized(offsets) {
                    offsets.clear()
                    offsets.addAll(tempOffsets)
                }
            }
            offsetExecutor.execute(offsetThread!!)
        }
    }

    private fun getOffsets(): ArrayList<BlockPos> {
        val flooredPosition = Globals.mc.player.flooredPosition
        val downPos = flooredPosition.down()

        return arrayListOf<BlockPos>().apply {
            EnumFacing.HORIZONTALS.forEach {
                add(downPos.offset(it))
                add(flooredPosition.offset(it))
            }
            removeIf { !it.isAir || !it.isPlaceable() }
        }
    }

    private fun getAlternateNeighbour(source: BlockPos): List<BlockPos> {
        return arrayListOf<BlockPos>().apply {
            EnumFacing.values().forEach { add(source.offset(it)) }
            removeIf { offset -> offset.getVisibleSides().isEmpty() }
        }
    }

    private fun placeBlockCount(info: PlaceInfo) {
        if (placeTicks < blocksPerTick.value) {
            if (placeBlock(info)) if (renderMode.value != RenderMode.None) renderBlock.setPos(info.placedPos)
            ++placeTicks
        }
    }

    private fun placeBlock(info: PlaceInfo): Boolean {
        if (obsidianSlot == -1) return false

        val rotationMode = rotateMode.value
        val shouldFaceInstant = rotationMode == RotateMode.Speed || rotationMode == RotateMode.Both

        val lastSlot = Globals.mc.player.inventory.currentItem

        val shouldSwap = LocalHotbarManager.serverSideHotbar != obsidianSlot
        val shouldSneak = !Globals.mc.player.isSneaking && (rightClickableBlock.contains(info.pos.block) || info.pos.needTileSneak)
        val isSprinting = Globals.mc.player.isSprinting

        if (rotationMode == RotateMode.Tick || rotationMode == RotateMode.Both) addMotion { rotate(RotationUtils.getRotationTo(info.hitVec)) }

        if (shouldSwap) InventoryUtils.swapSlot(obsidianSlot)

        if (isSprinting) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SPRINTING))
        if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))

        placeBlock(info, EnumHand.MAIN_HAND, packet = noGhost.value, packetRotate = shouldFaceInstant, swingArm = swingArm.value, noGhost = true)

        if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
        if (isSprinting) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SPRINTING))

        if (shouldSwap) InventoryUtils.swapSlot(lastSlot)
        return true
    }

    private fun doMoveToCenter() {
        if (centerMode.value != CenterMode.Off && centerPos != Vec3d.ZERO && centerDelayTimer.passedTicks(centerDelay.value) && !Globals.mc.player.isInHole && obsidianSlot != -1) {
            val x = abs(centerPos.x - Globals.mc.player.posX)
            val z = abs(centerPos.z - Globals.mc.player.posZ)
            if (x <= 0.1 && z <= 0.1) {
                centerPos = Vec3d.ZERO
            } else {
                Globals.mc.player.centerPlayer(centerMode.value == CenterMode.Teleport)
            }
            centerDelayTimer.reset()
        }
    }

    private fun doDisablingChecks(): Boolean {
        if (disableWhileStrafe.value && SpeedModule.isEnabled && MovementUtils.isMovingSpeed && !Globals.mc.player.onGround) return true
        if (disableWhileStep.value && StepModule.isEnabled && MovementUtils.isMovingSpeed && Globals.mc.player.onGround) return true

        if (holePos.isNull || inHoleCheck()) holePos.setPos(Globals.mc.player.flooredPosition)
        if (Globals.mc.player.flooredPosition != holePos) doOutOfHoleCheck() else toggleTimer.reset()

        return false
    }

    private fun doOutOfHoleCheck() {
       if ((disableOutOfHole.value > 0 && toggleTimer.stop() > disableOutOfHole.value) || disableJumping.value && Globals.mc.player.movementInput.jump) {
           disable()
       }
    }

    private fun inHoleCheck(): Boolean {
        return Globals.mc.player.onGround && Globals.mc.player.speed < 0.15 && Globals.mc.player.isInHole
    }

}