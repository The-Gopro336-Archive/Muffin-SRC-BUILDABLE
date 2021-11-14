package me.han.muffin.client.module.modules.player

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.KeyPressedEvent
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.MouseEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.HoleManager.isInHole
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.combat.HoleFillerModule
import me.han.muffin.client.utils.ChatIDs
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.RotateMode
import me.han.muffin.client.utils.block.BlockUtil
import me.han.muffin.client.utils.block.HoleUtils
import me.han.muffin.client.utils.client.BindUtils
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.extensions.kotlin.next
import me.han.muffin.client.utils.extensions.mc.block.*
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.BindValue
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.block.Block
import net.minecraft.block.BlockWeb
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.util.math.BlockPos
import org.lwjgl.input.Keyboard
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object WebModule: Module("AutoWeb", Category.PLAYER, "Automatically place webs on you or enemy.", 240) {

    private val items = EnumValue(Item.Web, "Items")
    private val mode = EnumValue(Mode.Target, "Mode")
    private val disable = Value(true, "AutoDisable")
    private val rotateMode = EnumValue(RotateMode.Tick, "Rotate")
    private val swingArm = Value(true, "SwingArm")
    private val doubleWeb = Value(false, "DoubleWeb")
    private val filterWebbed = Value(true, "FilterWebbed")
    private val dynamicPosition = Value(true, "DynamicPosition")
    private val inHoleOnly = Value(false, "InHoleOnly")
    private val pauseWhileFill = Value(false, "PauseWhileFill")
    private val range = NumberValue(4.0, 0.0, 10.0, 1.0, "Range")
    private val blocksPerTick = NumberValue(2, 1, 10, 1, "BlocksPerTick")
    private val delay = NumberValue(2, 0, 10, 1, "Delay")
    private val targetSwitch = BindValue(Keyboard.KEY_NONE, "TargetSwitch")

    private val timer = Timer()
    val placingBlock = BlockPos.MutableBlockPos(0, -69, 0)
    private var placements = 0
    private var placedCounter = 0

    val isProcessing: Boolean get() = isEnabled && placingBlock.isNotNull

    init {
        addSettings(
            items,
            mode, disable,
            rotateMode, swingArm, doubleWeb,
            filterWebbed, dynamicPosition, inHoleOnly,
            pauseWhileFill,
            range, blocksPerTick, delay,
            targetSwitch
        )
    }

    private enum class Item {
        Web, Skull
    }

    private enum class Mode {
        Self, Target
    }

    override fun onEnable() {
        if (fullNullCheck()) return
    }

    override fun onDisable() {
        if (fullNullCheck()) return
        timer.reset()
    }

    override fun onToggle() {
        placedCounter = 0
        placingBlock.setNull()
    }

    private fun getItems(): Block = when (items.value) {
        Item.Web -> Blocks.WEB
        Item.Skull -> Blocks.SKULL
        else -> Blocks.WEB
    }

    @Listener
    private fun onMouse(event: MouseEvent) {
        if (fullNullCheck()) return
        if (BindUtils.checkIsClickedToggle(targetSwitch.value)) mode.value = mode.value.next()
    }

    @Listener
    private fun onKeyPress(event: KeyPressedEvent) {
        if (fullNullCheck()) return
        if (BindUtils.checkIsClickedToggle(targetSwitch.value)) mode.value = mode.value.next()
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        placingBlock.setNull()

        if (inHoleOnly.value && !Globals.mc.player.isInHole) {
            disable()
            return
        }

        if (pauseWhileFill.value && HoleFillerModule.isProcessing) {
            return
        }

        placements = 0
        val target = (if (mode.value == Mode.Target) findClosestTarget(range.value, filterWebbed.value) else Globals.mc.player) ?: return
        val interpolatedVec = target.flooredPosition

        val targetPos = ArrayList<BlockPos>()
        targetPos.add(interpolatedVec)
        if (doubleWeb.value || (dynamicPosition.value && (HoleUtils.isBurrowed(target) || interpolatedVec.isFullBox))) targetPos.add(interpolatedVec.up())

        if (!timer.passedTicks(delay.value)) return

        val itemSlot = if (items.value != Item.Skull) InventoryUtils.findBlock(getItems()) else InventoryUtils.findItem(Items.SKULL)

        if (itemSlot == -1) {
            ChatManager.sendDeleteMessage("${ChatFormatting.RED}Missing required item.", "Missing", ChatIDs.WEB_MODULE)
            disable()
            return
        }

        for (pos in targetPos) {
            val validResult = BlockUtil.valid(pos)

            if (validResult == BlockUtil.ValidResult.AlreadyBlockThere && !pos.isPlaceable(true)) continue
            if (pos.block == Blocks.WEB) continue

            if (validResult == BlockUtil.ValidResult.NoNeighbours) {
                val altPosList = arrayOf(pos.down(), pos.north(), pos.south(), pos.east(), pos.west(), pos.up(), pos.up().up())
                for (altPos in altPosList) {
                    val altValidResult = BlockUtil.valid(altPos)
                    if (altValidResult == BlockUtil.ValidResult.NoNeighbours) continue
                    placeAtPos(altPos, itemSlot)
                    placedCounter++
                }
                continue
            }

            placeAtPos(pos, itemSlot)
        }

        targetPos.clear()
        timer.reset()

        if (disable.value && interpolatedVec.block is BlockWeb && (!doubleWeb.value || interpolatedVec.up().block is BlockWeb)) {
            disable()
        }

        if (disable.value && items.value == Item.Skull && (placedCounter > 0 || interpolatedVec.block == Blocks.SKULL)) {
            disable()
        }

    }

    private fun placeAtPos(pos: BlockPos, itemSlot: Int) {
        val shouldInstantRotate = rotateMode.value == RotateMode.Speed || rotateMode.value == RotateMode.Both
        placingBlock.setPos(pos)

        val lastSlot = Globals.mc.player.inventory.currentItem
        InventoryUtils.swapSlot(itemSlot)

        if (placements < blocksPerTick.value) {
            if (rotateMode.value == RotateMode.Tick || rotateMode.value == RotateMode.Both) addMotion { rotate(RotationUtils.getRotationTo(pos.toVec3dCenter())) }

            val result = BlockUtil.place(pos, range.value, rotate = shouldInstantRotate, slab = false, swingArm = swingArm.value, ignoreSelfNeighbour = true)
            if (result == BlockUtil.PlaceResult.Placed) ++placements
        }

        InventoryUtils.swapSlot(lastSlot)
    }

    private fun findClosestTarget(range: Double, shouldFilterWebbed: Boolean): EntityPlayer? {
        var closestTarget: EntityPlayer? = null

        for (target in Globals.mc.world.playerEntities) {
            if (EntityUtil.isntValid(target, range)) continue
            if (!shouldFilterWebbed || target.flooredPosition.block is BlockWeb) continue

            if (closestTarget == null) {
                closestTarget = target
                continue
            }

            if (Globals.mc.player.getDistanceSq(target) >= Globals.mc.player.getDistanceSq(closestTarget)) continue

            closestTarget = target
        }

        return closestTarget
    }

}