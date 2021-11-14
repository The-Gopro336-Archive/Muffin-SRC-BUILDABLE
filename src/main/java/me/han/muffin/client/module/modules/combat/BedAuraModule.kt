package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.ClientTickEvent
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.manager.managers.LocalMotionManager
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.other.RenderModeModule
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.utils.combat.CrystalUtils
import me.han.muffin.client.utils.combat.DamageData
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.extensions.kotlin.floorToInt
import me.han.muffin.client.utils.extensions.mc.block.*
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.entity.realHealth
import me.han.muffin.client.utils.extensions.mc.item.*
import me.han.muffin.client.utils.extensions.mc.world.closestVisibleSide
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.math.VectorUtils
import me.han.muffin.client.utils.math.VectorUtils.distanceTo
import me.han.muffin.client.utils.math.VectorUtils.toVec3d
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.math.rotation.Vec2f
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.utils.timer.TickTimer
import me.han.muffin.client.utils.timer.TimeUnit
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBed
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.tileentity.TileEntityBed
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.max

internal object BedAuraModule : Module("BedAura", Category.COMBAT, "Place bed and kills enemies") {
    private val page = EnumValue(Pages.General, "Page")

    private val timing = EnumValue({ page.value == Pages.General }, Timing.Adaptive, "Timing")
    private val autoSwitch = Value({ page.value == Pages.General },true, "AutoSwitch")
    private val rotate = Value({ page.value == Pages.General },true, "Rotate")
    private val onePointThirteen = Value({ page.value == Pages.General },false, ">1.13")
    private val strictDirection = Value({ page.value == Pages.General}, false, "StrictDirection")

    private val antiSuicideHp = NumberValue({ page.value == Pages.General },0.5, 0.0, 10.0, 0.1, "AntiSuicide")
    private val refillDelay = NumberValue({ page.value == Pages.General },2, 1, 5, 1, "RefillDelay")

    private val breakDelay = NumberValue({ page.value == Pages.Break },50, 0, 1000, 2, "BreakDelay")
    private val breakRange = NumberValue({ page.value == Pages.Break },5.0, 1.0, 10.0, 0.2, "BreakRange")
    private val breakWallRange = NumberValue({ page.value == Pages.Break },3.0F, 0.0F, 5F, 0.2F, "BreakWallRange")
    private val minBreakDamage = NumberValue({ page.value == Pages.Break },5F, 1F, 20F, 1F, "MinBreakDamage")
    private val maxSelfBreakDamage = NumberValue({ page.value == Pages.Break },6F, 1F, 10F, 0.1F, "MaxSelfBreakDamage")

    private val placeDelay = NumberValue({ page.value == Pages.Place },50, 0, 1000, 2, "PlaceDelay")
    private val placeRange = NumberValue({ page.value == Pages.Place },5.0, 1.0, 10.0, 0.2, "PlaceRange")
    private val placeWallRange = NumberValue({ page.value == Pages.Place },3.0F, 0.0F, 6F, 0.2F, "PlaceWallRange")
    private val minPlaceDamage = NumberValue({ page.value == Pages.Place },10F, 1F, 20F, 1F, "MinPlaceDamage")
    private val maxSelfPlaceDamage = NumberValue({ page.value == Pages.Place },4F, 1F, 10F, 0.1F, "MaxSelfPlaceDamage")

    private val swingArm = Value({ page.value == Pages.Render }, true, "SwingArm")
    private val renderMode = EnumValue({ page.value == Pages.Render }, RenderModeModule.RenderMode.Full, "RenderMode")

    private val renderPlace = Value({ page.value == Pages.Render }, true, "RenderPlace")
    private val renderPlaceAlpha = NumberValue({ page.value == Pages.Render && renderPlace.value },30, 0, 255, 1, "PlaceAlpha")
    private val renderPlaceLineWidth = NumberValue({ page.value == Pages.Render && renderBreak.value },1.5F, 0.1F, 5.0F, 0.1F, "PlaceLineWidth")

    private val renderBreak = Value({ page.value == Pages.Render }, true, "RenderBreak")
    private val renderBreakAlpha = NumberValue({ page.value == Pages.Render && renderBreak.value },45, 0, 255, 1, "BreakAlpha")
    private val renderBreakLineWidth = NumberValue({ page.value == Pages.Render && renderBreak.value },1.5F, 0.1F, 5.0F, 0.1F, "BreakLineWidth")

    private val cacheThreadPool = Executors.newCachedThreadPool()

    private var placeMap = emptyMap<BlockPos, DamageData>() // <BlockPos, <TargetDamage, SelfDamage, SquaredDistance>>
    private var bedMap = emptyMap<BlockPos, DamageData>() // <BlockPos, <TargetDamage, SelfDamage, SquaredDistance>>

    private val refillTimer = TickTimer(TimeUnit.TICKS)
    private var state = State.None
    private val clickPos = BlockPos.MutableBlockPos(0, -69, 0)
    private var lastRotation = Vec2f.ZERO

    private var inactiveTicks = 0

    private var target: EntityPlayer? = null

    private val breakTimer = Timer()
    private val placeTimer = Timer()

    private val maxRange get() = max(placeRange.value, breakRange.value)

    private var placeColour = Colour()
    private var breakColour = Colour()

    private val renderBedPlace = BlockPos.MutableBlockPos(0, -69, 0)
    private val renderBedBreak = BlockPos.MutableBlockPos(0, -69, 0)

    private val threadMap = hashMapOf<Thread, Future<*>?>(
        Thread { updateTarget() } to null,
        Thread { updatePlaceMap() } to null,
        Thread { updateBedMap() } to null,
        Thread { updateColour() } to null
    )

    private enum class Pages {
        General, Break, Place, Render
    }

    private enum class Timing {
        Vanilla, Adaptive
    }

    private enum class State {
        None, Place, Explode
    }

    init {
        addSettings(
            page,
            timing, autoSwitch, rotate, onePointThirteen, strictDirection, antiSuicideHp, refillDelay,
            breakDelay, breakRange, breakWallRange, minBreakDamage, maxSelfBreakDamage,
            placeDelay, placeRange, placeWallRange, minPlaceDamage, maxSelfPlaceDamage,
            swingArm, renderPlace, renderPlaceAlpha, renderPlaceLineWidth, renderBreak, renderBreakAlpha, renderBreakLineWidth
        )
    }

    override fun getHudInfo(): String? {
        return target?.name
    }

    override fun onDisable() {
        state = State.None
        inactiveTicks = 6

        renderBedPlace.setNull()
        renderBedBreak.setNull()
    }

    private fun updateBedMap() {
        val cacheList = ArrayList<Pair<BlockPos, DamageData>>()
        val currentTarget = target

        for (tileEntity in Globals.mc.world.loadedTileEntityList) {
            if (tileEntity !is TileEntityBed || !tileEntity.isHeadPiece) continue

            val distanceTo = Globals.mc.player.distanceTo(tileEntity.pos)
            if (distanceTo > breakRange.value) continue
            if (breakWallRange.value > 0.0 && !tileEntity.pos.isVisible() && distanceTo > breakWallRange.value) continue

            val topSideVec = tileEntity.pos.toVec3dCenter(0.0, 0.5, 0.0)
            val facing = EnumFacing.fromAngle(RotationUtils.getRotationTo(topSideVec).x.toDouble())

            val targetDamage = currentTarget?.let { CrystalUtils.calculateBedDamage(tileEntity.pos.offset(facing), it) } ?: 0.0F
            if (targetDamage < minBreakDamage.value) continue

            val selfDamage = CrystalUtils.calculateBedDamage(tileEntity.pos.offset(facing), Globals.mc.player)
            if (!doSuicideCheck(selfDamage) || selfDamage > maxSelfBreakDamage.value) continue

            cacheList.add(tileEntity.pos to DamageData(targetDamage, selfDamage, distanceTo))
        }

        bedMap = LinkedHashMap<BlockPos, DamageData>(cacheList.size).apply {
            putAll(cacheList.sortedByDescending { it.second.targetDamage })
        }

    }

    private fun updatePlaceMap() {
        val currentTarget = target
        val cacheList = ArrayList<Pair<BlockPos, DamageData>>()

        val posList = VectorUtils.getBlockPosInSphere(Globals.mc.player.eyePosition, placeRange.value.toFloat())

        for (pos in posList) {
            val distanceTo = Globals.mc.player.distanceTo(pos)

            if (placeWallRange.value > 0.0 && !pos.isVisible() && distanceTo > placeWallRange.value) continue

            val topSideVec = pos.toVec3dCenter(0.0, 0.5, 0.0)
            val facing = EnumFacing.fromAngle(RotationUtils.getRotationTo(topSideVec).x.toDouble())

            if (!canPlaceBed(pos)) continue

            val targetDamage = currentTarget?.let { CrystalUtils.calculateBedDamage(pos.offset(facing), it) } ?: 0.0F
            if (targetDamage < minPlaceDamage.value) continue

            val selfDamage = CrystalUtils.calculateBedDamage(pos.offset(facing), Globals.mc.player)
            if (!doSuicideCheck(selfDamage) || selfDamage > maxSelfPlaceDamage.value) continue

            cacheList.add(pos to DamageData(targetDamage, selfDamage, distanceTo))
        }

        placeMap = LinkedHashMap<BlockPos, DamageData>(cacheList.size).apply {
            putAll(cacheList.sortedByDescending { it.second.targetDamage })
        }

    }

    private fun updateTarget() {
        target = EntityUtil.findClosestTarget(maxRange)
    }

    private fun updateColour() {
        placeColour = Colour().clientColour(renderPlaceAlpha.value)
        breakColour = Colour().clientColour(renderBreakAlpha.value)
    }

    private fun doSuicideCheck(selfDamage: Float): Boolean {
        return antiSuicideHp.value == 0.0 ||
            (selfDamage < Globals.mc.player.realHealth - antiSuicideHp.value &&
                    Globals.mc.player.realHealth - selfDamage > antiSuicideHp.value)
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (fullNullCheck() || event.stage != EventStageable.EventStage.POST) return

        if (event.packet !is CPacketPlayer || state == State.None) return

        val hand = (getBedHand() ?: if (state == State.Place) null else EnumHand.MAIN_HAND) ?: return
        val facing = if (state == State.Place) EnumFacing.UP else if (strictDirection.value) clickPos.closestVisibleSide ?: EnumFacing.UP else EnumFacing.UP

        val hitVecOffset = facing.hitVecOffset
        val packet = CPacketPlayerTryUseItemOnBlock(clickPos, facing, hand, hitVecOffset.x.toFloat(), hitVecOffset.y.toFloat(), hitVecOffset.z.toFloat())
        Globals.mc.connection?.sendPacket(packet)
        if (swingArm.value) Globals.mc.player.swingArm(hand) else Globals.mc.connection?.sendPacket(CPacketAnimation(hand))

        state = State.None
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (fullNullCheck()) return

        if (Globals.mc.player.dimension == 0) {
            state = State.None
            resetRotation()
            inactiveTicks = 6
            return
        }

        inactiveTicks++
        renderBedBreak.setNull()
        renderBedPlace.setNull()

        if (canRefill() && refillTimer.tick(refillDelay.value.toLong())) {
            Globals.mc.player.storageSlots.firstItem<ItemBed, Slot>()?.let {
                quickMoveSlot(it)
            }
        }

        if (timing.value != Timing.Vanilla) return

        updateBedAura()
    }

    @Listener
    private fun onClientTicking(event: ClientTickEvent) {
        if (fullNullCheck()) return

        if (timing.value != Timing.Adaptive) return
        updateBedAura()
    }

    private fun updateBedAura() {
        for ((thread, future) in threadMap) {
            if (future?.isDone == false) continue // Skip if the previous thread isn't done
            threadMap[thread] = cacheThreadPool.submit(thread)
        }

        if (breakTimer.passed(breakDelay.value.toDouble())) {
            getExplodePos()?.let { preExplode(it) }
            breakTimer.reset()
        }

        if (placeTimer.passed(placeDelay.value.toDouble())) {
            getPlacePos()?.let { prePlace(it) }
            placeTimer.reset()
        }

        if (inactiveTicks <= 5) sendRotation() else resetRotation()
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        val playerView = Globals.mc.renderViewEntity ?: Globals.mc.player ?: return
        val interpView = MathUtils.interpolateEntity(playerView, RenderUtils.renderPartialTicks)

        if (renderPlace.value && renderBedPlace.isNotNull && getBedHand() != null) {
            val placeRed = placeColour.r
            val placeGreen = placeColour.g
            val placeBlue = placeColour.b
            val placeAlpha = placeColour.a

            val placeLineWidth = renderPlaceLineWidth.value

            val renderBB = renderBedPlace.state.getSelectedBoundingBox(Globals.mc.world, renderBedPlace)
                .grow(RenderUtils.BBGrow)
                .offset(-interpView.x, -interpView.y, -interpView.z)

            when (renderMode.value) {
                RenderModeModule.RenderMode.Solid -> RenderUtils.drawBoxESP(renderBB, placeRed, placeGreen, placeBlue, placeAlpha)
                RenderModeModule.RenderMode.Outline -> RenderUtils.drawBoxOutlineESP(renderBB, placeRed, placeGreen, placeBlue, placeAlpha, placeLineWidth)
                RenderModeModule.RenderMode.Full -> RenderUtils.drawBoxFullESP(renderBB, placeRed, placeGreen, placeBlue, placeAlpha, placeLineWidth)
            }

        }

        if (renderBreak.value && renderBedBreak.isNotNull) {
            val breakRed = breakColour.r
            val breakGreen = breakColour.g
            val breakBlue = breakColour.b
            val breakAlpha = breakColour.a

            val breakLineWidth = renderBreakLineWidth.value

            val renderBB = renderBedBreak.state.getSelectedBoundingBox(Globals.mc.world, renderBedBreak)
                .grow(RenderUtils.BBGrow)
                .offset(-interpView.x, -interpView.y, -interpView.z)

            when (renderMode.value) {
                RenderModeModule.RenderMode.Solid -> RenderUtils.drawBoxESP(renderBB, breakRed, breakGreen, breakBlue, breakAlpha)
                RenderModeModule.RenderMode.Outline -> RenderUtils.drawBoxOutlineESP(renderBB, breakRed, breakGreen, breakBlue, breakAlpha, breakLineWidth)
                RenderModeModule.RenderMode.Full -> RenderUtils.drawBoxFullESP(renderBB, breakRed, breakGreen, breakBlue, breakAlpha, breakLineWidth)
            }

        }

    }

    private fun canRefill(): Boolean {
        return Globals.mc.player.hotbarSlots.firstEmpty() != null
            && Globals.mc.player.storageSlots.firstItem<ItemBed, Slot>() != null
    }

    private fun canPlaceBed(posIn: BlockPos): Boolean {
        val originalState = posIn.state
        val canOriginalReplaceable = originalState.block.isReplaceable(Globals.mc.world, posIn)
        val pos = if (canOriginalReplaceable) posIn else posIn.up()

        val playerDirection = ((LocalMotionManager.serverSideRotation.x * 4.0f / 360.0f) + 0.5).floorToInt() and 3
        val facing = EnumFacing.byHorizontalIndex(playerDirection)
        val offsetPos = pos.offset(facing)

        val offsetState = offsetPos.state
        val isOffsetPosPlaceable = offsetPos.isAir || offsetState.block.isReplaceable(Globals.mc.world, offsetPos)
        val isOriginalPosPlaceable = canOriginalReplaceable || pos.isAir

        val downPos = pos.down()
        val offsetDownPos = offsetPos.down()

        return isOffsetPosPlaceable &&
                isOriginalPosPlaceable &&
                downPos.state.isSideSolid(Globals.mc.world, downPos, EnumFacing.UP) &&
                offsetDownPos.state.isSideSolid(Globals.mc.world, offsetDownPos, EnumFacing.UP)
    }

    private fun isFire(pos: BlockPos): Boolean {
        return pos.block == Blocks.FIRE
    }

    private fun getExplodePos() = bedMap.keys.firstOrNull()

    private fun getPlacePos() = placeMap.keys.firstOrNull()

    private fun prePlace(pos: BlockPos) {
        if (getExplodePos() != null) return

        val hand = getBedHand()
        if (autoSwitch.value && hand == null) Globals.mc.player.hotbarSlots.firstItem(Items.BED)?.let { swapToSlot(it) }

        renderBedPlace.setPos(pos)
        preClick(pos, Vec3d(0.5, 1.0, 0.5))
        state = State.Place
    }

    private fun preExplode(pos: BlockPos) {
        renderBedBreak.setPos(pos)
        preClick(pos, Vec3d(0.5, 0.0, 0.5))
        state = State.Explode
    }

    private fun preClick(pos: BlockPos, hitOffset: Vec3d) {
        inactiveTicks = 0
        clickPos.setPos(pos)
        lastRotation = RotationUtils.getRotationTo(pos.toVec3d(hitOffset))
    }

    private fun getSecondBedPos(pos: BlockPos): BlockPos {
        val rotation = RotationUtils.getRotationTo(pos.toVec3d(0.5, 0.0, 0.5))
        val facing = EnumFacing.fromAngle(rotation.x.toDouble())
        return pos.offset(facing)
    }

    private fun getBedHand(): EnumHand? {
        return when (Items.BED) {
            Globals.mc.player.heldItemMainhand.item -> EnumHand.MAIN_HAND
            Globals.mc.player.heldItemMainhand.item -> EnumHand.OFF_HAND
            else -> null
        }
    }

    private fun sendRotation() {
        if (rotate.value) addMotion { rotate(lastRotation) }
    }

    private fun resetRotation() {
        lastRotation = RotationUtils.getPlayerRotation(1F)
    }

}