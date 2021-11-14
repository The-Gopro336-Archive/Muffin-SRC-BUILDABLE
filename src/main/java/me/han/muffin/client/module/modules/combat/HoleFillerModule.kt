package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.HoleManager
import me.han.muffin.client.manager.managers.HoleManager.holeInfo
import me.han.muffin.client.manager.managers.HoleManager.isInHole
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.other.RenderModeModule
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.block.BlockUtil
import me.han.muffin.client.utils.block.HoleType
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.extensions.kotlin.square
import me.han.muffin.client.utils.extensions.kotlin.synchronized
import me.han.muffin.client.utils.extensions.mc.block.isVisible
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.item.firstBlock
import me.han.muffin.client.utils.extensions.mc.item.firstByStack
import me.han.muffin.client.utils.extensions.mc.item.hotbarSlots
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
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
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.function.Predicate

/**
 * @author han
 */
internal object HoleFillerModule: Module("HoleFiller", Category.COMBAT, "Fill the holes to trick enemies.", 260) {

    private val item = EnumValue(Items.Obsidian, "Item")
    private val toggles = Value(true, "AutoDisable")
    private val toggleOnFilled = Value({ toggles.value && smart.value },true, "ToggleOnFilled")

    private val holeOnly = Value(true, "HoleOnly")
    private val frustumCheck = Value(false, "FrustumCheck")
    private val rotate = Value(true, "Rotate")
    private val rotateConfirm = Value(false, "RotateConfirm")
    private val swingArm = Value(true, "SwingArm")

    private val avoidSelf = Value(false, "AvoidSelf")
    private val fillExtend = Value(true, "FillExtend")

    private val maxHoles = NumberValue(5, 1, 20, 1, "MaxHoles")
    private val radius = NumberValue(5.0, 1.0, 8.0, 0.2, "Radius")
    private val wallRange = NumberValue(3.0, 0.0, 8.0, 0.2, "WallRange")
    private val delay = NumberValue(1, 0, 10, 1, "Delay")

    private val smart = Value(false, "Smart")
    private val enemyRange = NumberValue({ smart.value },3.0, 0.0, 10.0, 0.2, "EnemyRange")
    private val onlyFillBelow = Value({ smart.value }, false, "OnlyFillBelowEnemy")
    private val fillEnemyHole = Value({ smart.value && item.value == Items.Web },false, "FillEnemyHole")
    private val fillSelfHole = Value({ item.value == Items.Web }, false, "FillSelfHole")

    private val render = Value(false, "Render")

    private val timer = Timer()
    private val holesToFill = arrayListOf<BlockPos>().synchronized()
    private var currentTarget: EntityPlayer? = null
    private var holesFilled = 0

    private var currentSlot = -1

    val isProcessing: Boolean get() = isEnabled && holesToFill.isNotEmpty()

    init {
        addSettings(
            item,
            toggles, toggleOnFilled,
            holeOnly, frustumCheck,
            rotate, rotateConfirm,
            swingArm, avoidSelf, fillExtend,
            radius, wallRange, maxHoles, delay, render,
            smart, enemyRange, onlyFillBelow,
            fillEnemyHole, fillSelfHole
        )
    }

    private enum class Timing {
        Vanilla, Sequential
    }

    private enum class Items {
        Obsidian, Web
    }

    override fun onEnable() {
        if (fullNullCheck()) return
        currentSlot = -1

        if (holeOnly.value && !Globals.mc.player.isInHole) disable() else findNewHoles()
    }

    override fun onDisable() {
        if (fullNullCheck()) return
        currentTarget = null
        holesFilled = 0
    }

    private fun updateHoles() {

    }

    private fun getRequiredBlock(): Block {
        return if (item.value == Items.Obsidian) Blocks.OBSIDIAN else Blocks.WEB
    }

    fun <T : Slot> Iterable<T>.firstBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
        firstByStack { itemStack ->
            itemStack.item.let { it is ItemBlock && it.block == block } && (predicate == null || predicate.test(itemStack))
        }

    private fun getHotbarSlot(): Int {
        return Globals.mc.player.hotbarSlots.firstBlock(getRequiredBlock())?.hotbarSlot ?: InventoryUtils.findGenericBlock() ?: -1
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (fullNullCheck()) return

        if (holesToFill.isEmpty()) {
            if (smart.value) {
                if (toggles.value && currentTarget != null && (!toggleOnFilled.value || holesFilled > 0)) {
                    disable()
                    return
                } else {
                    findNewHoles()
                }
            } else {
                if (toggles.value) {
                    disable()
                    return
                } else {
                    findNewHoles()
                }
            }
        }

        if (!timer.passedTicks(delay.value)) return

        synchronized(holesToFill) {
            holesToFill.removeIf { BlockUtil.valid(it) != BlockUtil.ValidResult.Ok }
        }

        currentSlot = -1
        val slot = getHotbarSlot()

        if (slot == -1) {
            ChatManager.sendMessage("Missing required item.")
            disable()
            return
        }

        val posToFill = synchronized(holesToFill) { holesToFill.firstOrNull() } ?: return

        currentSlot = slot

        val lastSlot = Globals.mc.player.inventory.currentItem
        InventoryUtils.swapSlot(slot)

        if (rotate.value) addMotion { rotate(RotationUtils.getRotationTo(posToFill.toVec3dCenter())) }

        if (BlockUtil.place(posToFill, radius.value, rotate = rotateConfirm.value, slab = false, swingArm = swingArm.value) == BlockUtil.PlaceResult.Placed) {
            holesToFill.remove(posToFill)
            holesFilled++
        }

        InventoryUtils.swapSlot(lastSlot)

        timer.reset()
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (!render.value) return

        val red = Muffin.getInstance().fontManager.publicRed
        val green = Muffin.getInstance().fontManager.publicGreen
        val blue = Muffin.getInstance().fontManager.publicBlue

        val width = RenderModeModule.lineWidth.value

        synchronized(holesToFill) {
            holesToFill.forEach {
                when (RenderModeModule.holeFiller.value) {
                    RenderModeModule.RenderMode.Solid -> {
                        Muffin.getInstance().blockRenderer.drawSolid(it, red, green, blue, 70)
                    }
                    RenderModeModule.RenderMode.Outline -> {
                        RenderUtils.drawBlockOutlineESP(it, red, green, blue, 70, width)
                    }
                    RenderModeModule.RenderMode.Full -> {
                        Muffin.getInstance().blockRenderer.drawFull(it, red, green, blue, 70, 1.5F)
                    }
                }
            }
        }

    }

    private fun findNewHoles() {
        val tempFillBlocks = arrayListOf<BlockPos>()
        var holes = 0

        if (smart.value) currentTarget = EntityUtil.findClosestTarget(radius.value + enemyRange.value) ?: return

        val flooredPosition = Globals.mc.player.positionVector
        val eyesPos = Globals.mc.player.eyePosition

        for (holeInfo in HoleManager.holeInfosNearFiltered) {
            val pos = holeInfo.origin

            if (tempFillBlocks.contains(pos)) continue

            if (smart.value && currentTarget != null && currentTarget!!.getDistanceSq(pos) >= enemyRange.value.square) continue
            if (smart.value && onlyFillBelow.value && currentTarget != null && pos.y >= currentTarget!!.posY) continue
            if (frustumCheck.value && !RenderUtils.isInViewFrustum(pos)) continue

            if (wallRange.value > 0.0 && !pos.isVisible() && eyesPos.squareDistanceTo(pos.toVec3dCenter()) > wallRange.value.square) continue

            when (pos.holeInfo.type) {
                HoleType.None -> {
                    if (item.value == Items.Web && fillEnemyHole.value && smart.value && currentTarget != null && pos == currentTarget!!.flooredPosition) {
                        if (++holes < maxHoles.value) {
                            tempFillBlocks.add(pos)
                        }
                    }
                }
                HoleType.Bedrock, HoleType.Obsidian -> {
                    if (++holes < maxHoles.value) tempFillBlocks.add(pos)
                }
                HoleType.Two, HoleType.Four -> {
                    if (fillExtend.value) if (++holes < maxHoles.value) tempFillBlocks.addAll(holeInfo.holePos)
                }
            }
        }

        tempFillBlocks.removeIf { BlockUtil.valid(it) != BlockUtil.ValidResult.Ok }

        holesToFill.clear()
        holesToFill.addAll(tempFillBlocks)

        holesToFill.removeIf { BlockUtil.valid(it) != BlockUtil.ValidResult.Ok }

//        if (avoidSelf.value) {
//            holesToFill.removeIf {
//                val yawTo = RotationUtils.getRotationTo(flooredPosition, it.toVec3dCenter()).x
//                MovementUtils.isMoving() && Globals.mc.player.speedKmh > 15.0F && yawTo < 22.5F
//            }
//        }

    }

}