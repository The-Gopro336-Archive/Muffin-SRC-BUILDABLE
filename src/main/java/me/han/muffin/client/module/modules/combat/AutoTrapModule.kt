package me.han.muffin.client.module.modules.combat

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.LocalHotbarManager
import me.han.muffin.client.manager.managers.LocalMotionManager
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.RotateMode
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.extensions.kotlin.synchronized
import me.han.muffin.client.utils.extensions.mc.block.*
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.item.firstBlock
import me.han.muffin.client.utils.extensions.mc.item.firstByStack
import me.han.muffin.client.utils.extensions.mc.item.hotbarSlots
import me.han.muffin.client.utils.extensions.mc.world.placeBlock
import me.han.muffin.client.utils.extensions.mc.world.searchForNeighbour
import me.han.muffin.client.utils.extensions.mixin.misc.rightClickDelayTimer
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.math.PlaceInfo
import me.han.muffin.client.utils.math.VectorUtils.toBlockPos
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.concurrent.Executors
import java.util.function.Predicate
import kotlin.concurrent.thread

internal object AutoTrapModule: Module("AutoTrap", Category.COMBAT, "Automatically trap enemies with obsidian.") {
    private val page = EnumValue(Pages.General, "Page")

    private val timing = EnumValue({ page.value == Pages.General }, Timing.Sequential, "Timing")
    private val structureMode = EnumValue({ page.value == Pages.General}, StructureMode.Strict, "StructureMode")

    private val autoDisable = Value({ page.value == Pages.General },false, "AutoDisable")
    private val rotateMode = EnumValue({ page.value == Pages.General }, RotateMode.Tick, "Rotate")
    private val noGhost = Value({ page.value == Pages.General },true, "NoGhost")
    private val swingArm = Value({ page.value == Pages.General },false, "SwingArm")

    private val range = NumberValue({ page.value == Pages.General },5.5, 3.5, 8.0, 0.2, "Range")
    private val blocksPerTick = NumberValue({ page.value == Pages.General },2, 1, 15, 1, "BlocksPerTick")
    private val delay = NumberValue({ page.value == Pages.General },1, 0, 10, 1, "Delay")

    private val strictDirection = Value({ page.value == Pages.General },false, "StrictDirection")
    private val strictJump = Value({ page.value == Pages.General }, false, "StrictJump")
    private val avoidSelf = Value({ page.value == Pages.General },true, "AvoidSelf")
    private val ignoreTrapped = Value({ page.value == Pages.General}, false, "IgnoreTrapped")

    private val antiDrop = Value({ page.value == Pages.Style },false, "AntiDrop")
    private val antiStep = Value({ page.value == Pages.Style },false, "AntiStep")

    private val renderMode = EnumValue({ page.value == Pages.Rendering }, RenderMode.Full, "RenderMode")

    private val renderRed = NumberValue({ page.value == Pages.Rendering && renderMode.value != RenderMode.None },15, 0, 255, 5, "Red")
    private val renderGreen = NumberValue({ page.value == Pages.Rendering && renderMode.value != RenderMode.None },50, 0, 255, 5, "Green")
    private val renderBlue = NumberValue({ page.value == Pages.Rendering && renderMode.value != RenderMode.None },165, 0, 255, 5, "Blue")
    private val renderAlpha = NumberValue({ page.value == Pages.Rendering && renderMode.value != RenderMode.None },25, 0, 255, 5, "Alpha")

    private val lineWidth = NumberValue({ page.value == Pages.Rendering && renderMode.value != RenderMode.None && renderMode.value != RenderMode.Solid }, 0.5F, 0.1F, 5.0F, 0.1F, "LineWidth")

    private var currentTarget: EntityPlayer? = null

    private var placeTicks = 0
    private var obsidianSlot = -1

    private val delayTimer = Timer()

    private val renderBlock = BlockPos.MutableBlockPos(0, -69, 0)
    private val trapStructure = arrayListOf<BlockPos>().synchronized()

    private var shouldForcePacketPlace = false
    private var spoofedCount = 0

    private var structureThread: Thread? = null
    private val structureExecutor = Executors.newSingleThreadExecutor()

    init {
        addSettings(
            page,
            timing, structureMode,
            autoDisable, rotateMode, noGhost, swingArm, range, blocksPerTick, delay, strictDirection, strictJump, avoidSelf, ignoreTrapped,
            antiDrop, antiStep,
            renderMode, renderRed, renderGreen, renderBlue, lineWidth
        )
    }

    private enum class Timing {
        Vanilla, Sequential
    }

    private enum class StructureMode {
        Basic, Normal, Strict
    }

    private enum class Pages {
        General, Style, Rendering
    }

    private enum class RenderMode {
        None, Solid, Outline, Full
    }

    override fun onEnable() {
        shouldForcePacketPlace = false
        spoofedCount = 0
    }

    override fun onToggle() {
        delayTimer.reset()
    }

    private fun updateTarget() {
        currentTarget = EntityUtil.findClosestTargetFilter(range.value) {
            ignoreTrapped.value && !flooredPosition.up(2).isAir
        }
    }

    private fun getExtraBlockFromFacing(source: BlockPos, currentFacing: EnumFacing, excludeStrict: Boolean): List<BlockPos> {
        return arrayListOf<BlockPos>().apply {
            EnumFacing.HORIZONTALS
                .filter { !excludeStrict || it != currentFacing }
                .forEach { add(source.offset(it)) }
        }.sortedByDescending { Globals.mc.player.getDistanceSq(it) }
    }

    private fun updateStructure() {
        if (structureThread == null || !structureThread!!.isAlive || structureThread!!.isInterrupted) {
            structureThread = thread(start = false) {
                val eyesPos = Globals.mc.player.eyePosition
                val horizontalFacing = LocalMotionManager.horizontalFacing
                val targetBehindBlock = getNCPDirectionBlock()

                val tempPlaceTargets = arrayListOf<BlockPos>().apply {
                    if (antiDrop.value) add(BlockPos(0, -1, 0))

                    if (structureMode.value == StructureMode.Basic) {
                        addAll(TrapOffsets.TrapBase.offset)

                        if (strictDirection.value) add(BlockPos(0, 3, 0).add(targetBehindBlock))
                        else add(BlockPos(0, 3, -1))

                        add(BlockPos(0, 3, 0))
                    } else if (structureMode.value == StructureMode.Normal || structureMode.value == StructureMode.Strict) {
                        val isStrictMode = structureMode.value == StructureMode.Strict

                        val baseOffset = getExtraBlockFromFacing(BlockPos.ORIGIN, horizontalFacing, isStrictMode)
                        val feetOffset = getExtraBlockFromFacing(BlockPos(0, 1, 0), horizontalFacing, isStrictMode)
                        val bodyOffset = getExtraBlockFromFacing(BlockPos(0, 2, 0), horizontalFacing, isStrictMode)

                        if (structureMode.value == StructureMode.Normal) {
                            addAll(baseOffset)
                            addAll(feetOffset)
                            addAll(bodyOffset)

                            if (strictDirection.value) add(BlockPos(0, 3, 0).add(targetBehindBlock))
                            else add(BlockPos(0, 3, -1))

                            add(BlockPos(0, 3, 0))
                        } else if (isStrictMode) {
                            val basePos = BlockPos.ORIGIN

                            val baseBehind = basePos.offset(horizontalFacing)
                            val feetBehind = baseBehind.up()
                            val bodyBehind = feetBehind.up()
                            val headBehind = bodyBehind.up()

                            val headBlock = basePos.add(0, 3, 0)
                            val firstStructure = arrayOf(baseBehind, feetBehind, bodyBehind, headBehind, headBlock)

                            addAll(firstStructure)
                            addAll(baseOffset)
                            addAll(feetOffset)
                            addAll(bodyOffset)
                        }

                    }

                    if (antiStep.value) add(BlockPos(0, 4, 0))
                }

                synchronized(trapStructure) {
                    trapStructure.clear()
                    trapStructure.addAll(tempPlaceTargets)
                }
            }
            structureExecutor.execute(structureThread!!)
        }
    }

    fun <T : Slot> Iterable<T>.firstBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
        firstByStack { itemStack ->
            itemStack.item.let { it is ItemBlock && it.block == block } && (predicate == null || predicate.test(itemStack))
        }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (fullNullCheck() || timing.value != Timing.Sequential) return

        if (event.stage == EventStageable.EventStage.PRE) {
            placeTicks = 0
            renderBlock.setNull()

            obsidianSlot = Globals.mc.player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)?.hotbarSlot ?: -1

            if (obsidianSlot == -1) {
                ChatManager.sendMessage("${ChatFormatting.RED}Missing obsidian in hotbar.")
                disable()
                return
            }

            updateTarget()
        } else if (event.stage == EventStageable.EventStage.POST && currentTarget != null && delayTimer.passedTicks(delay.value) && trapStructure.isNotEmpty()) {
            val eyesPos = Globals.mc.player.eyePosition
            val targetVector = currentTarget!!.flooredPosition

            if (avoidSelf.value && MathUtils.areVec3dsAligned(Globals.mc.player.positionVector, currentTarget!!.positionVector)) return

            val placeStructure = trapStructure.sortedBy { it.y }

            for (structure in placeStructure) {
                val placeTarget = targetVector.down().add(structure)
                if (!placeTarget.hasNeighbour || !placeTarget.isPlaceable()) continue

                val placeNeighbour = searchForNeighbour(placeTarget, 2, range.value.toFloat(), false) ?: continue
                placeBlocks(placeNeighbour)
            }

            delayTimer.reset()
        }

        // ChatManager.sendMessage(placeTicks.toString())
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (fullNullCheck() || timing.value != Timing.Vanilla) return

        if (event.stage == EventStageable.EventStage.PRE) {
            placeTicks = 0
            renderBlock.setNull()

            obsidianSlot = Globals.mc.player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)?.hotbarSlot ?: -1

            if (obsidianSlot == -1) {
                ChatManager.sendMessage("${ChatFormatting.RED}Missing obsidian in hotbar.")
                disable()
                return
            }

            if (!delayTimer.passedTicks(delay.value)) return
            delayTimer.reset()

            updateTarget()

            val target = currentTarget ?: return

            if (trapStructure.isEmpty()) {
                return
            }

            val eyesPos = Globals.mc.player.eyePosition
            val targetVector = target.flooredPosition

            if (avoidSelf.value && MathUtils.areVec3dsAligned(Globals.mc.player.positionVector, target.positionVector)) return

            val placeStructure = trapStructure.sortedBy { it.y }

            for (structure in placeStructure) {
                val placeTarget = targetVector.down().add(structure)
                if (!placeTarget.hasNeighbour || !placeTarget.isPlaceable()) continue

                val placeNeighbour = searchForNeighbour(placeTarget, 2, range.value.toFloat(), false) ?: continue
                placeBlocks(placeNeighbour)
            }

        }
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        updateStructure()
        if (renderMode.value == RenderMode.None) return

        if (renderBlock.isNull) return

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

    private fun placeBlocks(info: PlaceInfo) {
        if (placeTicks < blocksPerTick.value) {
            shouldForcePacketPlace = strictJump.value && info.pos.y - 1.0 > Globals.mc.player.posY + Globals.mc.player.eyeHeight
            if (placeBlockInRange(info)) if (renderMode.value != RenderMode.None) renderBlock.setPos(info.placedPos)
            ++placeTicks
        }
    }

    private fun placeBlockInRange(info: PlaceInfo): Boolean {
        if (obsidianSlot == -1) return false

        val rotationMode = rotateMode.value
        val shouldFaceInstant = rotationMode == RotateMode.Speed || rotationMode == RotateMode.Both

        val clickPos = info.pos

        val lastSlot = Globals.mc.player.inventory.currentItem
        val shouldSwap = LocalHotbarManager.serverSideHotbar != obsidianSlot

        val neighbourBlock = clickPos.block
        val shouldSneak = !Globals.mc.player.isSneaking && (rightClickableBlock.contains(neighbourBlock) || clickPos.needTileSneak)
        val isSprinting = Globals.mc.player.isSprinting

        if (rotationMode == RotateMode.Tick || rotationMode == RotateMode.Both) addMotion { rotate(RotationUtils.getRotationTo(info.hitVec)) }

        if (shouldSwap) InventoryUtils.swapSlot(obsidianSlot)

        if (isSprinting) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SPRINTING))
        if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))

        if (strictJump.value && shouldForcePacketPlace) {
            if (spoofedCount == 0) {
                val currentY = Globals.mc.player.posY
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 0.419997086886978, Globals.mc.player.posZ, false))
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 0.7500029, Globals.mc.player.posZ, false))
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 0.9999942, Globals.mc.player.posZ, false))
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 1.170005801788139, Globals.mc.player.posZ, false))
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, currentY + 1.170005801788139, Globals.mc.player.posZ, false))
            }
            ChatManager.sendMessage("fucking yo suck my dick")
            placeBlock(info, EnumHand.MAIN_HAND, true, shouldFaceInstant, swingArm.value, true)
            if (spoofedCount == 0) {
                Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ, Globals.mc.player.onGround))
            }
            spoofedCount++
        } else {
            spoofedCount = 0
            shouldForcePacketPlace = false
            placeBlock(info, EnumHand.MAIN_HAND, noGhost.value, shouldFaceInstant, swingArm.value, true)
        }

        if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
        if (isSprinting) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SPRINTING))

        if (shouldSwap) InventoryUtils.swapSlot(lastSlot)  // Globals.mc.player.connection.sendPacket(CPacketHeldItemChange(lastSlot))

        Globals.mc.rightClickDelayTimer = 4

        return true
    }

    private fun getNCPDirectionBlock(): BlockPos {
        //var defaultFacing = EnumFacing.NORTH
        //for (hFacing in EnumFacing.HORIZONTALS) {
        //    if (hFacing != Globals.mc.player.horizontalFacing.opposite) continue
        //    defaultFacing = hFacing
        //}
        return Vec3d(Globals.mc.player.horizontalFacing.directionVec).toBlockPos()
    }

    private enum class TrapOffsets(val offset: Array<BlockPos>) {
        TrapBase(arrayOf(
            BlockPos(-1, 0, 0),
            BlockPos(1, 0, 0),
            BlockPos(0, 0, -1),
            BlockPos(0, 0, 1),
            BlockPos(1, 1, 0),
            BlockPos(-1, 1, 0),
            BlockPos(0, 1, 1),
            BlockPos(0, 1, -1),
            BlockPos(-1, 2, 0),
            BlockPos(1, 2, 0),
            BlockPos(0, 2, 1),
            BlockPos(0, 2, -1)
        )), Tower(
            arrayOf(

            )
        )
    }

}