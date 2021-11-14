package me.han.muffin.client.module.modules.combat

import com.google.common.util.concurrent.ThreadFactoryBuilder
import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.ClientTickEvent
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.entity.SyncCurrentPlayItemEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.render.Render2DEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.event.events.render.entity.RenderEntityModelEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.LocalHotbarManager
import me.han.muffin.client.manager.managers.LocalHotbarManager.resetHotbar
import me.han.muffin.client.manager.managers.LocalHotbarManager.serverSideItem
import me.han.muffin.client.manager.managers.LocalHotbarManager.spoofHotbar
import me.han.muffin.client.manager.managers.LocalMotionManager
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.manager.managers.SpeedManager.speedKmh
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.other.RenderModeModule
import me.han.muffin.client.module.modules.player.WebModule
import me.han.muffin.client.module.modules.render.ChamsRewriteModule
import me.han.muffin.client.utils.InfoUtils
import me.han.muffin.client.utils.PredictMode
import me.han.muffin.client.utils.block.HoleUtils
import me.han.muffin.client.utils.color.ColourUtils
import me.han.muffin.client.utils.combat.CombatUtils
import me.han.muffin.client.utils.combat.CrystalUtils
import me.han.muffin.client.utils.combat.DamageData
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.extensions.kotlin.square
import me.han.muffin.client.utils.extensions.kotlin.step
import me.han.muffin.client.utils.extensions.kotlin.synchronized
import me.han.muffin.client.utils.extensions.mc.block.*
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.utils.extensions.mc.entity.realHealth
import me.han.muffin.client.utils.extensions.mc.item.firstItem
import me.han.muffin.client.utils.extensions.mc.item.hotbarSlots
import me.han.muffin.client.utils.extensions.mc.item.swapToSlot
import me.han.muffin.client.utils.extensions.mc.world.getClosestVisibleSideStrict
import me.han.muffin.client.utils.extensions.mixin.netty.id
import me.han.muffin.client.utils.extensions.mixin.netty.packetAction
import me.han.muffin.client.utils.extensions.mixin.netty.placedBlockDirection
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.math.RandomUtils
import me.han.muffin.client.utils.math.RayTraceUtils
import me.han.muffin.client.utils.math.VectorUtils.distanceTo
import me.han.muffin.client.utils.math.VectorUtils.toBlockPos
import me.han.muffin.client.utils.math.VectorUtils.toVec3d
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
import me.han.muffin.client.utils.math.pattern.GaussianPattern
import me.han.muffin.client.utils.math.pattern.Pattern
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.math.rotation.RotationUtils.getRotationDifference
import me.han.muffin.client.utils.math.rotation.Vec2f
import me.han.muffin.client.utils.math.rotation.VecRotation
import me.han.muffin.client.utils.render.GlStateUtils
import me.han.muffin.client.utils.render.ProjectionUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.utils.threading.onMainThread
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Enchantments
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.init.SoundEvents
import net.minecraft.item.ItemAir
import net.minecraft.item.ItemEndCrystal
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.network.Packet
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.network.play.server.*
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11.glScalef
import org.lwjgl.opengl.GL11.glTranslated
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.math.*

/**
 * @author han
 * TODO: improve chasing calculation
 * TODO: rewrite these shitty code
 */
internal object AutoCrystalModule: Module("AutoCrystal", Category.COMBAT, "Automatically place and break crystals around enemies.", 50) {
    private val page = EnumValue(Pages.Break, "Page")

    // Breaking Page //
    private val breakCrystal = Value({ page.value == Pages.Break }, true, "Break")
    private val breakFactor = NumberValue({ page.value == Pages.Break && breakCrystal.value && server.value == Servers.Fast }, 5, 1, 10, 1, "BreakFactor")
    private val breakDelay = NumberValue({ page.value == Pages.Break && breakCrystal.value }, 20, 0, 1000, 5, "BreakDelay")
    private val breakRange = NumberValue({ page.value == Pages.Break && breakCrystal.value }, 5.0, 0.1, 10.0, 0.2, "BreakRange")
    private val breakWallRange = NumberValue({ page.value == Pages.Break && breakCrystal.value }, 3.0, 0.0, 10.0, 0.2, "BreakWallRange")
    private val breakChance = NumberValue({ page.value == Pages.Break && breakCrystal.value }, 100, 0, 100, 1, "BreakChance")
    private val hitAttempts = NumberValue({ page.value == Pages.Break && breakCrystal.value }, 2, 0, 20, 1, "HitsBeforeSwitch")

    private val breakEfficiency = NumberValue({ page.value == Pages.Break && breakCrystal.value }, 2.5, 0.0, 10.0, 0.5, "BreakEfficiency")
    private val minHitDamage = NumberValue({ page.value == Pages.Break && breakCrystal.value }, 8.0, 0.0, 20.0, 0.5, "MinHitDamage")
    private val maxSelfHitDmg = NumberValue({ page.value == Pages.Break && breakCrystal.value }, 8.0, 0.0, 20.0, 0.5, "MaxLocalHitDamage")
    private val antiWeakness = Value({ page.value == Pages.Break && breakCrystal.value }, false, "AntiWeakness")
    private val swapDelay = NumberValue({ page.value == Pages.Break && breakCrystal.value }, 5, 0, 10, 1, "SwapDelay")
    private val waitDelay = NumberValue({ page.value == Pages.Break && breakCrystal.value }, 0.3, 0.0, 10.0, 0.1, "WaitDelay")

    private val breakOwnOnly = Value({ page.value == Pages.Break && breakCrystal.value }, false, "BreakOwnOnly")
    private val breakOnlyEnemy = Value({ page.value == Pages.Break && breakCrystal.value }, false, "BreakOnlyEnemy")
    private val checkMinDamage = Value({ page.value == Pages.Break && breakCrystal.value && breakOnlyEnemy.value }, false, "CheckMinDamage")
    private val manualBreak = Value({ page.value == Pages.Break && breakCrystal.value }, true, "ManualBreak")
    // Breaking Page //

    // Placing Page //
    private val place = Value({ page.value == Pages.Place }, true, "Place")
    val placeMethod = EnumValue({ page.value == Pages.Place && place.value }, PlaceMethod.New, "PlaceMethod")
    private val placeMode = EnumValue({ page.value == Pages.Place && place.value }, PlaceMode.All, "PlaceMode")
    private val placeSync = Value({ page.value == Pages.Place && place.value }, false, "PlaceSync")
    val onePointThirteen = Value({ page.value == Pages.Place && place.value }, false, ">1.13")

    private val placeFactor = NumberValue({ page.value == Pages.Place && place.value && server.value == Servers.Fast }, 3, 1, 10, 1, "PlaceFactor")
    private val placeDelay = NumberValue({ page.value == Pages.Place && place.value }, 20, 0, 1000, 5, "PlaceDelay")
    private val placeRange = NumberValue({ page.value == Pages.Place && place.value }, 4.5, 0.1, 10.0, 0.2, "PlaceRange")
    val placeWallRange = NumberValue({ page.value == Pages.Place && place.value }, 3.0, 0.0, 10.0, 0.2, "PlaceWallRange")
    val placeEfficiency = NumberValue({ page.value == Pages.Place && place.value }, 4.0, 0.0, 10.0, 0.5, "PlaceEfficiency")
    val minPlaceDamage = NumberValue({ page.value == Pages.Place && place.value }, 8.0, 0.0, 20.0, 0.5, "MinPlaceDamage")

    private val maxSelfDmg = NumberValue({ page.value == Pages.Place && place.value }, 12.0, 0.0, 20.0, 0.5, "MaxLocalPlaceDmg")
    private val compromise = NumberValue({ page.value == Pages.Place && place.value }, 0.5, 0.0, 2.0, 0.1, "Compromise")
    private val maxCrystals = NumberValue({ page.value == Pages.Place && place.value }, 2, 1, 5, 1, "MaxCrystals")
    private val constrict = Value({ page.value == Pages.Place && place.value }, true, "Constrict")
    private val rayTraceRange = NumberValue({ page.value == Pages.Place && place.value }, 3.0, 0.0, 10.0, 0.2, "RayTrace")
    private val wallScaling = NumberValue({ page.value == Pages.Place && place.value }, 3.0, 0.0, 10.0, 0.2, "WallScaling")
    val strictDirection = Value({ page.value == Pages.Place && place.value }, false, "StrictDirection")
    private val limitPlace = NumberValue({ page.value == Pages.Place && place.value }, 0, 0, 8, 1, "LimitPlace")
    // Placing Page //

    // Misc Page //
    private val server = EnumValue({ page.value == Pages.Misc }, Servers.Normal, "Server")
    private val actionPriority = EnumValue({ page.value == Pages.Misc }, ActionPriority.Break, "ActionPriority")
    val facePlaceMode = EnumValue({ page.value == Pages.Misc }, FacePlace.Durability, "FacePlace")
    val targetPriority = EnumValue({ page.value == Pages.Misc }, TargetPriority.Unsafe, "TargetPriority")
    private val maxTargets = NumberValue({ page.value == Pages.Misc && targetPriority.value == TargetPriority.Damage }, 5, 1, 15, 1, "MaxTargets")
    private val timing = EnumValue({ page.value == Pages.Misc }, Timing.Sequential, "Timing")
    val swapMode = EnumValue({ page.value == Pages.Misc }, SwapMode.None, "SwapMode")
    private val rotateMode = EnumValue({ page.value == Pages.Misc }, RotateMode.Vanilla, "RotationMode")

    val minDurability = NumberValue({ page.value == Pages.Misc }, 25.0, 0.0, 60.0, 0.1, "MinDurability")
    private val minSpeedFacePlace = NumberValue({ page.value == Pages.Misc }, 20.0, 0.0, 28.0, 0.1, "MinSpeedFacePlace")
    val enemyRange = NumberValue({ page.value == Pages.Misc }, 6.0, 0.1, 16.0, 0.2, "EnemyRange")
    private val antiSuicideHp = NumberValue({ page.value == Pages.Misc }, 1.5, 0.0, 5.0, 0.1, "AntiSuicide")
    val lethalMultiplier = NumberValue({ page.value == Pages.Misc }, 1.5, 0.0, 4.0, 0.1, "LethalMultiplier")
    val lethalHealth = NumberValue({ page.value == Pages.Misc }, 8.0, 0.1, 20.0, 0.5, "LethalHealth")

    private val yawStep = NumberValue({ page.value == Pages.Misc }, 170, 0, 180, 2, "YawStep")
    private val yawStepTicks = NumberValue({ page.value == Pages.Misc }, 2, 0, 5, 1, "YawStepTicks")

    private val maxYOffset = NumberValue({ page.value == Pages.Misc }, 4.0, 0.0, 10.0, 0.1, "MaxYOffset")

    private val overrideMaxLocal = Value({ page.value == Pages.Misc }, true, "OverrideMaxLocal")
    private val sequential = Value({ page.value == Pages.Misc }, true, "Sequential")
    private val fpsBooster = Value({ page.value == Pages.Misc }, false, "FpsBooster")
    private val instant = Value({ page.value == Pages.Misc }, false, "Instant")
    private val pauseWhileMining = Value({ page.value == Pages.Misc }, false, "PauseWhileMining")
    private val pauseWhileEating = Value({ page.value == Pages.Misc }, false, "PauseWhileEating")

    val predictMode = EnumValue({ page.value == Pages.Misc }, PredictMode.Off, "PredictMode")
    val predictTicks = NumberValue({ page.value == Pages.Misc && predictMode.value == PredictMode.New }, 6, -1, 20, 1, "PredictTicks")

    private val damageSync = EnumValue({ page.value == Pages.Misc }, DamageSync.None, "DamageSync")
    private val damageSyncDelay = NumberValue({ page.value == Pages.Misc && damageSync.value != DamageSync.None }, 500, 0, 1000, 5, "DamageSyncDelay")
    private val damageIgnore =  NumberValue({ page.value == Pages.Misc && damageSync.value == DamageSync.Break }, 5.0, 0.0, 10.0, 0.1, "IgnoreDamage")
    private val verifyPos = NumberValue({ page.value == Pages.Misc && damageSync.value != DamageSync.None }, 250, 0, 1000, 5, "Verify")
    // Misc Page //

    // Rendering Page //
    private val renderTarget = EnumValue({ page.value == Pages.Rendering }, RenderTargetMode.Off, "RenderTarget")
    private val hudInfo = EnumValue({ page.value == Pages.Rendering }, HudInfo.Action, "HudInfo")
    private val renderOffhand = Value({ page.value == Pages.Rendering }, false, "OffhandRender")
    private val renderDamage = Value({ page.value == Pages.Rendering }, false, "RenderDamage")
    private val renderBreak = Value({ page.value == Pages.Rendering }, true, "RenderBreak")
    private val breakAlpha = NumberValue({ page.value == Pages.Rendering && renderBreak.value }, 65, 0, 255, 1, "BreakAlpha")
    private val blockAlpha = NumberValue({ page.value == Pages.Rendering }, 45, 0, 255, 1, "BlockAlpha")
    private val lineWidth = NumberValue({ page.value == Pages.Rendering }, 1.5F, 0.1F, 5.0F, 0.1F, "LineWidth")
    // Rendering Page //

    // Dev Page //
    private val alwaysClosest = Value({ page.value == Pages.Dev && breakCrystal.value }, false, "AlwaysClosest")
    private val keepChecking = Value({ page.value == Pages.Dev && breakCrystal.value }, false, "KeepChecking")

    private val syncLastBreak = Value({ page.value == Pages.Dev && breakCrystal.value }, false, "SyncLastBreak")

    private val syncLastPlace = Value({ page.value == Pages.Dev && place.value }, false, "SyncLastPlace")
    private val fastPop = Value({ page.value == Pages.Dev && place.value }, true, "FastPop")

    private val fastBreak = Value({ page.value == Pages.Dev }, true, "FastExplode")
    private val fastBreakDelay = NumberValue({ page.value == Pages.Dev && breakCrystal.value && fastBreak.value }, 10, 0, 1000, 5,"FastExplodeDelay")

    private val farDamage = Value({ page.value == Pages.Dev && place.value }, false, "FarDamage")
    private val calibrationCalc = Value({ page.value == Pages.Dev && place.value}, false, "CalibrationCalc")
    private val popbobCalc = Value({ page.value == Pages.Dev && place.value}, false, "PopbobCalc")
    // Dev Page //

    private val lockObject = Any()

    var inactiveTicks = 20; private set
    private var hitCount = 0

    private var crystalCount = 0
    private var minDamageCount = 0

    private var blockedTicks = 0

    private var lastEntityID = 0

    private var currentDamage = 0.0
    private var currentMaxSelfDamage = 0.0
    private var currentDistance = 0.0

    private var currentMinDamage = 0.0
    private var lastDamage = 0.0

    private var placedCrystalDamage: Float = -1.0F

    private val renderBlock = BlockPos.MutableBlockPos(0, -69, 0)
    var renderingTarget: EntityPlayer? = null

    private var lastPos: BlockPos? = null
    private var lastCrystal: EntityEnderCrystal? = null

    private var lastLookAt = Vec3d.ZERO

    private val placedPos = HashSet<BlockPos>()
    private val brokenPos = HashSet<BlockPos>()

    private var placedPosPair: Pair<BlockPos, DamageData>? = null

    private val ignoredList = Collections.newSetFromMap<EntityEnderCrystal>(WeakHashMap())
    private val crystalList = HashSet<EntityEnderCrystal>()

    private val placedBBMap = HashMap<BlockPos, Pair<AxisAlignedBB, Long>>().synchronized()

    private var crystalMap = emptyMap<EntityEnderCrystal, DamageData>()
    private var placeMap = emptyMap<BlockPos, DamageData>()

    private val totemPops = WeakHashMap<EntityPlayer, Timer>()
    private val packetList = ArrayList<Packet<*>>(3)

    private var shouldAntiSurround = false
    private var canDoublePop = false
    var placeSelector = false
    private var posConfirmed = false

    private var mainHand = false
    private var offHand = false

    private val placeTimer = Timer()
    private val hitTimer = Timer()
    private val instantTimer = Timer()

    private val fastBreakTimer = Timer()
    private val wallTimer = Timer()
    private val stuckTimerCleaner = Timer()
    private val syncTimer = Timer()
    private val manualTimer = Timer()

    private val firstHitTimer = Timer()

    private var placePos: BlockPos? = null
    private var attackCrystal: EntityEnderCrystal? = null

    private val attackCrystalList = ArrayDeque<EntityEnderCrystal>()
    private var attackCrystalMap = HashMap<EntityEnderCrystal, Double>()

    private val crystalSpawnMap = HashMap<Int, Long>().synchronized()

    var minDamage = max(minPlaceDamage.value, minHitDamage.value)
    var settingMaxSelfDamage = min(maxSelfDmg.value, maxSelfHitDmg.value)
    var maxRange = max(placeRange.value, breakRange.value)

    var currentBestPlaceRange = 10.0
    var currentBestConstrictRange = 4.2

    private var hadRotatedTo = false
    private var facedTicks = 0

    private var collidedTicks = 0

    private var shouldInfoLastBreak = false
    private var infoBreakTime = 0L
    private var lastBreakTime = 0L

    private var calculationThread: Thread? = null
    private val calculationExecutor = Executors.newFixedThreadPool(max(2, Runtime.getRuntime().availableProcessors() / 2), ThreadFactoryBuilder().setDaemon(true).setPriority(Thread.MAX_PRIORITY).build())

    private enum class ActionPriority {
        Break, Place
    }

    enum class SwapMode {
        None, Normal, Silent
    }

    private enum class RotateMode {
        None, Vanilla, Strict
    }

    private enum class Pages {
        Break, Place, Misc, Rendering, Dev
    }

    private enum class Servers {
        Normal, Anarchy, Fast
    }

    enum class FacePlace {
        Off, Constant, Durability
    }

    enum class PlaceMode {
        Normal, Multi, All
    }

    enum class PlaceMethod {
        Old, New
    }

    private enum class Timing {
        Vanilla, Adjusted, Adaptive, Sequential
    }

    private enum class DamageSync {
        None, Break, Place
    }

    private enum class HudInfo {
        Action, TargetName, Differentiation
    }

    enum class TargetPriority {
        Distance, Unsafe, Damage, MTDamage
    }

    private enum class RenderTargetMode {
        Off, Box, Chams
    }

    init {
        addSettings(page,
            // Breaking //
            breakCrystal, breakFactor, breakDelay, breakRange, breakWallRange, breakChance, hitAttempts, breakEfficiency,
            minHitDamage, maxSelfHitDmg, swapDelay, waitDelay, breakOwnOnly, breakOnlyEnemy, checkMinDamage, manualBreak,
            // Breaking //

            // Placing //
            place, placeMethod, placeMode, placeFactor, placeDelay, placeRange, placeWallRange, placeEfficiency, minPlaceDamage, compromise, maxSelfDmg, maxCrystals, placeSync, //
            onePointThirteen, constrict, rayTraceRange, wallScaling, strictDirection,
            // Placing //

            // Misc //
            server, actionPriority, targetPriority, maxTargets, facePlaceMode, timing, swapMode, antiWeakness, rotateMode, minDurability, minSpeedFacePlace, enemyRange, antiSuicideHp, lethalMultiplier, lethalHealth, yawStep, yawStepTicks, // TOO LONG
            maxYOffset, overrideMaxLocal, sequential, fpsBooster, instant, pauseWhileMining, pauseWhileEating, predictMode, predictTicks, damageSync, damageSyncDelay, damageIgnore, verifyPos,
            // Misc //

            // Rendering //
            renderTarget, hudInfo, renderOffhand, renderDamage, renderBreak, breakAlpha, blockAlpha, lineWidth,
            // Rendering //

            // Dev //
            keepChecking, alwaysClosest, syncLastBreak, syncLastPlace, fastPop, fastBreak, fastBreakDelay, farDamage, calibrationCalc, popbobCalc, limitPlace
            // Dev //
        )
    }

    override fun onEnable() {
        if (fullNullCheck()) {
            return
        }
    }

    override fun onDisable() {
        if (fullNullCheck()) return

        placedCrystalDamage = -1.0F
        renderBlock.setNull()

        lastLookAt = Vec3d.ZERO
        lastCrystal = null
        blockedTicks = 0
        inactiveTicks = 10

        placedBBMap.clear()
        synchronized(packetList) {
            packetList.clear()
        }

//        if (calculationThread != null) {
//            calculationThread!!.interrupt()
//            calculationThread = null
//        }
        resetHotbar()
    }

    override fun onToggle() {
        if (fullNullCheck()) return

        shouldInfoLastBreak = false
        infoBreakTime = 0L
        lastBreakTime = 0L

        hadRotatedTo = false
        facedTicks = 0
        collidedTicks = 0

        placedPos.clear()
        brokenPos.clear()
        totemPops.clear()
        placedPosPair = null

        resetRotation()
    }

    override fun getHudInfo(): String? {
        return when (hudInfo.value) {
            HudInfo.Action -> {
                if (renderBlock.isNotNull && attackCrystal != null) return if (RandomUtils.random.nextBoolean()) "Placing" else "Breaking"
                if (renderBlock.isNotNull) return "Placing"
                return if (attackCrystal != null) "Breaking" else null
            }
            HudInfo.TargetName -> renderingTarget?.name
            HudInfo.Differentiation -> {
                if (shouldInfoLastBreak && lastBreakTime != 0L) {
                    infoBreakTime = System.currentTimeMillis() - lastBreakTime
                    lastBreakTime = 0L
                    shouldInfoLastBreak = false
                }
                return if (infoBreakTime != 0L) "%.2f".format(MathUtils.round(infoBreakTime.div(50.0), 2)) else null
            }
            else -> null
        }
    }

    @Listener
    private fun onSyncCurrentPlayItem(event: SyncCurrentPlayItemEvent) {
        if (swapDelay.value > 0) firstHitTimer.reset()
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (fullNullCheck()) return

        if (event.stage == EventStageable.EventStage.PRE) {
            inactiveTicks++
            updateDesperateBreak()
        }

        if (timing.value == Timing.Sequential && packetList.size == 0) {
            doAutoCrystal()
        } else if (timing.value == Timing.Vanilla) {
            doAutoCrystal()
        }

        if (event.stage == EventStageable.EventStage.POST) {
            if (getHand() == EnumHand.OFF_HAND) resetHotbar()

            if (inactiveTicks > 20) {
                resetHotbar()
                resetRotation()
            }
        }

    }

    @Listener
    private fun onClientTick(event: ClientTickEvent) {
        if (fullNullCheck()) return

        if (timing.value == Timing.Adaptive && packetList.size == 0) doAutoCrystal()
        else if (timing.value == Timing.Adjusted) doAutoCrystal()
    }

    private fun doAutoCrystal() {
        if (shouldPause()) {
            resetStuff(pos = true, crystal = true, target = true, rotation = true)
            return
        }

        if (collidedTicks > 25) collidedTicks = 0
        if (collidedTicks <= 10) doCalculateCrystal()

        when (actionPriority.value) {
            ActionPriority.Break -> {
                if (breakCrystal.value) {
                    if (inactiveTicks > 20 && attackCrystal == null && attackCrystalList.pollFirst() == null && ignoredList.isNotEmpty()) {
                        ignoredList.clear()
                        hitCount = 0
                    } else if (server.value != Servers.Fast && attackCrystal != null) {
                        breakCrystal(attackCrystal!!)
                        if (!sequential.value) return
                    } else if (server.value == Servers.Fast && attackCrystalList.isNotEmpty()) {
                        val crystal = attackCrystalList.pollFirst()
                        if (crystal != null) {
                            breakCrystal(crystal)
                            if (!sequential.value) return
                        }
                    }
                }
                if (place.value) {
                    placeCrystal()
                }
            }

            ActionPriority.Place -> {
                if (place.value) {
                    placeCrystal()
                    if (!sequential.value) return
                }
                if (breakCrystal.value) {
                    if (inactiveTicks > 20 && attackCrystal == null && attackCrystalList.pollFirst() == null && ignoredList.isNotEmpty()) {
                        ignoredList.clear()
                        hitCount = 0
                    } else if (server.value != Servers.Fast && attackCrystal != null) {
                        breakCrystal(attackCrystal!!)
                    } else if (server.value == Servers.Fast && attackCrystalList.isNotEmpty()) {
                        val crystal = attackCrystalList.pollFirst()
                        if (crystal != null) {
                            breakCrystal(crystal)
                        }
                    }
                }

            }
        }

        doManualBreak()
    }


    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (fullNullCheck()) {
            return
        }

        if (event.stage == EventStageable.EventStage.PRE) {
            updateRotationHitable()
            if (rotateMode.value != RotateMode.None && inactiveTicks <= 20 && lastLookAt != Vec3d.ZERO) {
                addMotion { rotate(getLastRotation()) }
            }
        }

        if (timing.value != Timing.Vanilla && timing.value != Timing.Adjusted && event.stage == EventStageable.EventStage.POST) {
            synchronized(packetList) {
                for (packet in packetList) sendPacketDirect(packet)
                packetList.clear()
            }
        }

    }

    private fun breakCrystal(crystal: EntityEnderCrystal) {
        if (antiWeakness.value && !CrystalUtils.canBreakWeakness()) {
            CombatUtils.equipBestWeapon(allowTool = true)
            resetHotbar()
            return
        }

        if (System.currentTimeMillis() - LocalHotbarManager.swapTime < swapDelay.value * 50) {
            return
        }

        inactiveTicks = 0
        lastLookAt = if (rotateMode.value == RotateMode.Strict) crystal.positionVector.subtract(0.5, 0.0, 0.5) else crystal.positionVector

        if (!hadRotatedTo) return

        if (hitAttempts.value != 0 && crystal == lastCrystal) {
            hitCount++
            if (hitCount >= hitAttempts.value) ignoredList.add(crystal)
        } else {
            hitCount = 0
        }

        if (hitTimer.passed(breakDelay.value.toDouble())) {
            explodeCrystalAction(CPacketUseEntity(crystal), crystal)
            hitTimer.reset()
        }

        if (lastBreakTime == 0L) {
            lastBreakTime = System.currentTimeMillis()
            shouldInfoLastBreak = false
        }

        val eyePos = Globals.mc.player.eyePosition
        if (placeSync.value) {
            synchronized(placedBBMap) {
                for ((_, pair) in placedBBMap) {
                    val pos = pair.first.center.subtract(0.0, 1.0, 0.0)
                    if (pos.distanceTo(eyePos) > placeRange.value) continue
                    val currentTarget = renderingTarget ?: AutoCrystalHelper.target ?: EntityUtil.findClosestTarget(enemyRange.value + placeRange.value) ?: continue
                    val damage = CrystalUtils.calculateDamage(pos, currentTarget)
                    val selfDamage = CrystalUtils.calculateDamage(pos, Globals.mc.player)
                    if (!checkDamagePlace(damage, selfDamage)) continue
                    crystalCount++
                }
            }
        }

        brokenPos.add(crystal.flooredPosition.down())
        renderingTarget?.let { Globals.mc.player.setLastAttackedEntity(it) }
        lastCrystal = crystal
    }

    private fun explodeCrystalAction(packet: CPacketUseEntity, crystal: EntityEnderCrystal?) {
        if (server.value == Servers.Normal && RandomUtils.nextInt(0, 100) < 65) {
            for (i in 0..RandomUtils.nextInt(2, 6)) {
                if (RandomUtils.nextInt(0, 100) < breakChance.value) {
                    doBreaking(packet, crystal)
                }
            }
        } else if (server.value == Servers.Fast) {
            for (i in 0 until breakFactor.value) {
                if (RandomUtils.nextInt(0, 100) < breakChance.value) {
                    doBreaking(packet, crystal)
                }
            }
        } else {
            if (RandomUtils.nextInt(0, 100) < breakChance.value) {
                doBreaking(packet, crystal)
            }
        }
    }

    private fun placeCrystal() {
        var crystalLimit = maxCrystals.value

        if (placeTimer.passed(placeDelay.value.toDouble())) {
            renderBlock.setNull()

             mapBestBlock()
             // updateBlockCalcMT()
            // ChatManager.sendMessage("Main PlacePos: ${placePos.toString()}")
            if (renderingTarget == null || placePos == null) {
                renderBlock.setNull()
                return
            }

            // placePos = shouldLimitPlaceSelection(placePos!!, DamageData(currentDamage.toFloat(), currentMaxSelfDamage.toFloat(), currentDistance))

            if (swapMode.value != SwapMode.None) {
                Globals.mc.player.hotbarSlots.firstItem(Items.END_CRYSTAL)?.let {
                    if (swapMode.value == SwapMode.Silent) spoofHotbar(it.hotbarSlot, 1000L) else swapToSlot(it)
                }
            }

            val hand = getHand() ?: return

            if (placeMethod.value == PlaceMethod.New && shouldAntiSurround) crystalLimit = 1

            if ((crystalCount < crystalLimit || (!placeSelector && lastPos != null && lastPos!! == placePos)) && (minDamageCount < crystalLimit || currentDamage > currentMinDamage)) {
                val damageOffset = if (damageSync.value == DamageSync.Break) damageIgnore.value - 5.0 else 0.0
                if (currentDamage - damageOffset > lastDamage || syncTimer.passed(damageSyncDelay.value.toDouble()) || damageSync.value == DamageSync.None) {
                    if (damageSync.value != DamageSync.Break) lastDamage = currentDamage

                    inactiveTicks = 0
                    lastLookAt = if (rotateMode.value == RotateMode.Strict) placePos!!.toVec3d() else placePos!!.toVec3dCenter(0.0, 0.5, 0.0)
                    renderBlock.setPos(placePos!!)

                    if (!hadRotatedTo) return

                    if (canDoublePop) totemPops[renderingTarget!!] = Timer()

                    placedPos.add(placePos!!)

                    if (server.value == Servers.Fast || server.value == Servers.Normal && RandomUtils.nextInt(0, 100) < 65) {
                        if (server.value == Servers.Fast) {
                            for (i in 0 until placeFactor.value) {
                                doPlacing(placePos!!, hand)
                            }
                        } else {
                            for (i in 0 until RandomUtils.nextInt(1, 3)) {
                                doPlacing(placePos!!, hand)
                            }
                        }
                    } else {
                        doPlacing(placePos!!, hand)
                    }

                    val crystalPos = placePos!!.up()
                     placedBBMap[crystalPos] = CrystalUtils.getCrystalBB(crystalPos) to System.currentTimeMillis()

                    lastPos = placePos
                    placedPosPair = placePos!! to DamageData(currentDamage.toFloat(), currentMaxSelfDamage.toFloat(), currentDistance)

                    posConfirmed = false
                    if (syncTimer.passed(damageSyncDelay.value.toDouble())) syncTimer.reset()

                    if (fastBreak.value && damageSync.value == DamageSync.None && isValidForInstant(crystalPos)) {

                            val selfDamage = CrystalUtils.calculateDamage(crystalPos, Globals.mc.player)
                            if (placedBBMap.containsKey(crystalPos)) {
                                if (selfDamage <= maxSelfHitDmg.value && doSuicideCheck(selfDamage)) {
                                    if (fastBreakTimer.passed(fastBreakDelay.value.toDouble())) {
                                        attackCrystalInstant(++lastEntityID, crystalPos.toVec3dCenter())
                                        fastBreakTimer.reset()
                                    }
                                }
                            } else {
                                val currentTarget = renderingTarget ?: AutoCrystalHelper.target ?: EntityUtil.findClosestTarget(enemyRange.value + breakRange.value)
                                if (currentTarget != null) {
                                    val targetDamage = CrystalUtils.calculateDamage(crystalPos, currentTarget)
                                    if (currentTarget.getDistanceSq(crystalPos) <= enemyRange.value && targetDamage > minHitDamage.value && doSuicideCheck(selfDamage) && checkMaxLocalBreak(currentTarget, selfDamage, targetDamage, minHitDamage.value)) {
                                        if (fastBreakTimer.passed(fastBreakDelay.value.toDouble())) {
                                            attackCrystalInstant(++lastEntityID, crystalPos.toVec3d())
                                            fastBreakTimer.reset()
                                        }
                                    }
                                }
                            }
                            //if (!placedPos.contains(placePos!!.down())) return@synchronized
                            //        if (fastBreakTimer.passed(fastBreakDelay.value.toDouble())) {
                            //            attackCrystalInstant(lastEntityID + 1, crystalPos.toVec3d(0.5, 0.0, 0.5))
                            //            fastBreakTimer.reset()
                            //        }

                    }

                    placeTimer.reset()

                }
            }
        }

    }

    private fun updateMap() {
        placeMap = AutoCrystalHelper.placeMap
        crystalMap = AutoCrystalHelper.crystalMap

        crystalList.clear()
        crystalList.addAll(CrystalUtils.getCrystalList(max(breakRange.value, breakWallRange.value)))

        synchronized(placedBBMap) {
            placedBBMap.values.removeIf { System.currentTimeMillis() - it.second > max(InfoUtils.ping(), 100) }
        }

        if (inactiveTicks > 20) {
            if (placePos == null && placedBBMap.isNotEmpty()) {
                placedBBMap.clear()
            }
        }

        if (stuckTimerCleaner.passed(2500.0)) {
            ignoredList.clear()
            hitCount = 0
            stuckTimerCleaner.reset()
        }

        if (wallTimer.passed(4000.0)) {
            blockedTicks = 0
            wallTimer.reset()
        }

    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (event.packet is CPacketPlayerTryUseItemOnBlock && Globals.mc.player.getHeldItem(event.packet.hand).item is ItemEndCrystal) {
            if (event.packet.pos.y >= Globals.mc.world.height - 1 && event.packet.direction == EnumFacing.UP) {
                event.packet.placedBlockDirection = EnumFacing.DOWN
            }
        }

        /*
                if (instant.value && event.packet is CPacketUseEntity) {
                    val attackingCrystal = event.packet.getEntityFromWorld(Globals.mc.world) ?: return
                    if (attackingCrystal !is EntityEnderCrystal) return
                    if (!attackingCrystal.isDead) {
                        attackingCrystal.setDead()
                        Globals.mc.world.removeEntityFromWorld(attackingCrystal.entityId)
                        ignoredList.clear()
                        hitCount = 0
                    }
                }
         */

        /*
        if (fastBreak.value && event.packet is CPacketUseEntity) {
            if ((event.packet as ICPacketUseEntity).entityID == lastEntityID) {
                if (event.packet.getEntityFromWorld(Globals.mc.world) !is EntityEnderCrystal) {
                    ChatManager.sendMessage("Cancelling")
                    event.cancel()
                }
            }
        }
         */
    }

    private fun isValidForInstant(pos: BlockPos): Boolean {
        val crystalCenter = pos.toVec3d(0.5, 0.0, 0.5)
        val eyesPos = Globals.mc.player.eyePosition

        if (eyesPos.squareDistanceTo(crystalCenter) > breakRange.value.square) return false

        return if (eyesPos.squareDistanceTo(crystalCenter) > breakWallRange.value.square) {
            EntityUtil.canSeeVec3d(crystalCenter.add(0.0, 1.700000047683716, 0.0))
        } else {
            true
        }

    }

    private fun attackCrystalInstant(entityID: Int, vector: Vec3d) {
        inactiveTicks = 0
        lastLookAt = vector

        val attackPacket = CPacketUseEntity().apply {
            id = entityID
            packetAction = CPacketUseEntity.Action.ATTACK
        }

        synchronized(lockObject) {
            explodeCrystalAction(attackPacket, null)
        }
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (event.packet is SPacketSpawnObject) {
            lastEntityID = event.packet.entityID

            if (event.packet.type == 51) {
                val vector = Vec3d(event.packet.x, event.packet.y, event.packet.z)
                val instantPos = vector.toBlockPos()
                val downPos = instantPos.down()

                if (downPos == renderBlock) {
                    collidedTicks = 0
                }

                val selfDamage = CrystalUtils.calculateDamage(instantPos, Globals.mc.player)
                if (instant.value && damageSync.value == DamageSync.None && isValidForInstant(instantPos)) {
                    if (swapDelay.value > 0 && !firstHitTimer.passed(500.0)) return

                    if (placedPos.contains(downPos)) {
                        if (selfDamage <= maxSelfHitDmg.value && doSuicideCheck(selfDamage)) {
                            if (instantTimer.passed(breakDelay.value.toDouble())) {
                                attackCrystalInstant(event.packet.entityID, vector.add(0.5, 0.0, 0.5))
                                placedBBMap.remove(instantPos)?.let {
                                    posConfirmed = true
                                }
                                instantTimer.reset()
                            }
                        }
                    } else {
                        val currentTarget = renderingTarget ?: AutoCrystalHelper.target ?: EntityUtil.findClosestTarget(enemyRange.value + breakRange.value)
                        if (currentTarget != null) {
                            val targetDamage = CrystalUtils.calculateDamage(instantPos, currentTarget)
                            if (currentTarget.getDistanceSq(instantPos) <= enemyRange.value && targetDamage > minHitDamage.value && doSuicideCheck(selfDamage) && checkMaxLocalBreak(currentTarget, selfDamage, targetDamage, minHitDamage.value)) {
                                if (instantTimer.passed(breakDelay.value.toDouble())) {
                                    attackCrystalInstant(event.packet.entityID, vector.add(0.5, 0.0, 0.5))
                                    placedBBMap.remove(instantPos)?.let {
                                        posConfirmed = true
                                    }
                                    instantTimer.reset()
                                }
                            }
                        }
                    }
                }
            }
        }

        if (event.packet is SPacketEntityStatus) {
            if (event.packet.opCode == 35.toByte()) {
                val entity = event.packet.getEntity(Globals.mc.world)
                if (entity is EntityPlayer) totemPops[entity] = Timer()
            }
        }

        if (event.packet is SPacketExplosion) {
            val pos = BlockPos(event.packet.x, event.packet.y, event.packet.z).down()
            removePos(pos)
        }

        if (event.packet is SPacketDestroyEntities) {
            onMainThread {
                for (id in event.packet.entityIDs) {
                    val entity = Globals.mc.world.getEntityByID(id)
                    if (entity !is EntityEnderCrystal) continue

                    shouldInfoLastBreak = true

                    val downPos = entity.flooredPosition.down()
                    brokenPos.remove(downPos)
                    placedPos.remove(downPos)
                }
            }
        }

        if (event.packet is SPacketSoundEffect && event.packet.category == SoundCategory.BLOCKS && event.packet.sound == SoundEvents.ENTITY_GENERIC_EXPLODE) {
            val pos = BlockPos(event.packet.x, event.packet.y, event.packet.z)
            onMainThread {
                val crystalList = CrystalUtils.getCrystalList(Vec3d(event.packet.x, event.packet.y, event.packet.z), 6.0)

                for (crystal in crystalList) {
                    shouldInfoLastBreak = true

                    crystal.setDead()
                    if (!fastBreak.value) Globals.mc.world.removeEntityFromWorld(crystal.entityId)
                }

                removePos(pos)
                ignoredList.clear()
                hitCount = 0
            }
        }
    }

    private fun removePos(pos: BlockPos) {
        if (damageSync.value == DamageSync.Place && placedPos.remove(pos)) {
            posConfirmed = true
        }
        if (damageSync.value == DamageSync.Break && brokenPos.remove(pos)) {
            posConfirmed = true
        }
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck() || renderBlock.isNull) return

        var placeAlpha = blockAlpha.value
        if (!hadRotatedTo) placeAlpha -= 15
        if (blockAlpha.value > 10 && placeAlpha < 10) placeAlpha = 10
        if (placeAlpha < 0) placeAlpha = 0

        val placeColour = ColourUtils.getClientColour(placeAlpha)

        if (offHand || mainHand)  {
            when (RenderModeModule.autoCrystal.value) {
                RenderModeModule.RenderMode.Solid -> RenderUtils.drawBlockESP(renderBlock, placeColour)
                RenderModeModule.RenderMode.Outline -> RenderUtils.drawBlockOutlineESP(renderBlock, placeColour, lineWidth.value)
                RenderModeModule.RenderMode.Full -> RenderUtils.drawBlockFullESP(renderBlock, placeColour, lineWidth.value)
            }
        }

        if (!renderBreak.value) return

        val attackingCrystal = attackCrystal ?: return
        val attackingCrystalPosition = attackingCrystal.flooredPosition.down() ?: return
        if (!attackingCrystalPosition.isFullBox) return

        val breakColour = ColourUtils.getClientColour(breakAlpha.value)

        when (RenderModeModule.autoCrystal.value) {
            RenderModeModule.RenderMode.Solid -> RenderUtils.drawBlockESP(attackingCrystalPosition, breakColour)
            RenderModeModule.RenderMode.Outline -> RenderUtils.drawBlockOutlineESP(attackingCrystalPosition, breakColour, lineWidth.value)
            RenderModeModule.RenderMode.Full -> RenderUtils.drawBlockFullESP(attackingCrystalPosition, breakColour, lineWidth.value)
        }

    }

    @Listener
    private fun onRenderEntityModel(event: RenderEntityModelEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck() || renderTarget.value != RenderTargetMode.Chams || renderingTarget == null || event.entity != renderingTarget) return

        val colour = ColourUtils.getClientColour(30)
        when (renderTarget.value) {
            RenderTargetMode.Chams -> {
                ChamsRewriteModule.doFilledChams(event.modelBase, event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale, colour, colour, true)
            }
        }
        event.cancel()
    }

    @Listener
    private fun onRender2D(event: Render2DEvent) {
        if (!renderDamage.value || renderBlock.isNull) return

        if (!offHand && !mainHand) return

        val damage = placedCrystalDamage
        if (damage != 1.0F && RenderUtils.isInViewFrustum(renderBlock)) {
            GlStateUtils.rescaleActual()

            GlStateUtils.matrix(true)
            GlStateUtils.texture2d(true)

            val textPos = ProjectionUtils.toScreenPos(renderBlock.toVec3dCenter())

            val damageText = (if (floor(damage) == damage) damage.toInt() else "%.1f".format(damage)).toString()
            glTranslated(textPos.x, textPos.y, 0.0)
            glScalef(2.5f, 2.5f, 1f)
            val fontWidth = Muffin.getInstance().fontManager.getStringWidth(damageText) / -2
            Muffin.getInstance().fontManager.drawStringWithShadow(damageText, fontWidth.toFloat(), 0f, -1)

            GlStateUtils.matrix(false)

            GlStateUtils.rescaleMc()
        }
    }

    private fun checkIsValidCrystal(crystal: EntityEnderCrystal, distance: Double) =
        crystal.isAlive &&
                !ignoredList.contains(crystal) && isOwnPos(crystal) && distance <= breakRange.value &&
                (breakWallRange.value == 0.0 || Globals.mc.player.canEntityBeSeen(crystal) || EntityUtil.canEntityFeetBeSeen(crystal) ||
                        doStrictWallCheck(distance, crystal) ||
                        !Globals.mc.player.canEntityBeSeen(crystal) && distance <= breakWallRange.value) &&
                (maxYOffset.value == 0.0 || abs(Globals.mc.player.posY - crystal.posY) <= maxYOffset.value) &&
                doCheckValidSpawnTime(crystal)

    private fun doStrictWallCheck(distance: Double, crystal: EntityEnderCrystal): Boolean {
        val rotationTo = RotationUtils.getRotationToEntity(crystal)
        val result = RayTraceUtils.getRayTraceResult(rotationTo, max(breakRange.value, breakWallRange.value).toFloat())
        val goodRange = if (result.typeOfHit == RayTraceResult.Type.MISS) breakRange.value else breakWallRange.value
        return distance <= goodRange
    }

    private fun isOwnPos(crystal: EntityEnderCrystal): Boolean {
        if (!breakOwnOnly.value) return true
        return placedPos.any { crystal.getDistanceSq(it) <= 9 }
    }

    private fun doCalculateCrystal() {
        attackCrystal = null

        if (server.value == Servers.Fast && breakFactor.value != 1) {
            attackCrystalList.clear()
            attackCrystalMap.clear()
        }

        crystalCount = 0
        minDamageCount = 0

        var counted = false
        var countedMinDamage = false

        var enderCrystal: EntityEnderCrystal? = null
        var crystalDamage = 0.5
        var crystalDiff = 0.5
        var selfProtectDamage = 10000.0
        var crystalDistanceMultiplied = 69420.0

        val eyesPos = Globals.mc.player.eyePosition
        val target = renderingTarget ?: AutoCrystalHelper.target ?: EntityUtil.findClosestTarget(enemyRange.value + breakRange.value)

        // if (AutoCrystalHelper.target == null) AutoCrystalHelper.target = target
        // Command.sendChatMessage(target?.name ?: "no break target")

            if (fpsBooster.value && crystalMap.isNotEmpty()) {

                for ((crystal, calculation) in crystalMap.entries) {

                    if (!(Globals.mc.player.canEntityBeSeen(crystal) || EntityUtil.canEntityFeetBeSeen(crystal)) && calculation.distance > breakRange.value) {
                        blockedTicks++
                    }

                    if (!checkIsValidCrystal(crystal, calculation.distance)) continue

                    if (target == null || target.getDistanceSq(crystal) >= enemyRange.value.square) {
                        continue
                    }

                    if (alwaysClosest.value && attackCrystal != null && Globals.mc.player.getDistanceSq(crystal) >= Globals.mc.player.getDistanceSq(attackCrystal!!)) {
                        continue
                    }

                    val distance = CrystalUtils.getMostDistanced(crystal, target)
                    if (keepChecking.value && attackCrystal != null && (distance >= crystalDistanceMultiplied || distance == -1.0)) {
                        continue
                    }

                    var (minHitDmg, efficiency) = getMinDamageAndFactor(facePlaceMode.value, target, calculation.targetDamage, minHitDamage.value, breakEfficiency.value, lethalHealth.value, lethalMultiplier.value)

                    if (HoleUtils.fullCheckInHole(target) && calculation.selfDamage < 4) {
                        minHitDmg = 1.8
                        efficiency = 0.1
                    }

                    if (calculation.targetDamage < minHitDmg) {
                        continue
                    }

                    if (syncLastBreak.value && attackCrystal != null && CrystalUtils.calculateDamage(attackCrystal!!, target) > calculation.targetDamage) {
                        continue
                    }

                    if (calculation.targetDamage - calculation.selfDamage < efficiency) continue

                    if (!checkMaxLocalBreak(target, calculation.selfDamage, calculation.targetDamage, minHitDmg)) continue
                    if (!doSuicideCheck(calculation.selfDamage)) continue

                    if (calculation.selfDamage > calculation.targetDamage && calculation.targetDamage < target.realHealth) continue

                    if (calculation.targetDamage - calculation.selfDamage <= crystalDiff && calculation.targetDamage <= crystalDamage && (calculation.targetDamage < crystalDamage || calculation.selfDamage >= selfProtectDamage)) {
                        continue
                    }

                    //                    if (selfDamage > triple.first && (triple.first < minHitDmg || breakSafety.value == SafetyType.Balance && triple.first - selfDamage < costFactor) && triple.first < target.realHealth) {
                    //                        continue
                    //                    }

                    //                   if (triple.first > crystalDamage) {
                    crystalDiff = (calculation.targetDamage - calculation.selfDamage).toDouble()
                    crystalDamage = calculation.targetDamage.toDouble()
                    selfProtectDamage = calculation.selfDamage.toDouble()
                    crystalDistanceMultiplied = distance
                    enderCrystal = crystal
                    //                        }

                    if (server.value != Servers.Fast || breakFactor.value == 1) {
                        counted = true
                        countedMinDamage = true
                    } else {
                        if (attackCrystalMap[crystal] != null && attackCrystalMap[crystal]!! >= crystalDamage) continue
                        attackCrystalMap[crystal] = crystalDamage
                    }
                    
                    if (!countedMinDamage) continue
                    ++minDamageCount
                    if (!counted) continue
                    ++crystalCount
                }

                if (enderCrystal == null) {
                    for ((crystal, calculation) in crystalMap.entries) {

                        if (!(Globals.mc.player.canEntityBeSeen(crystal) || EntityUtil.canEntityFeetBeSeen(crystal)) && calculation.distance > breakRange.value) {
                            blockedTicks++
                        }

                        if (!checkIsValidCrystal(crystal, calculation.distance)) continue

                        if (breakOnlyEnemy.value && (target == null || !target.isAlive || target.getDistanceSq(crystal) >= enemyRange.value.square)) continue

                        if (breakOnlyEnemy.value && checkMinDamage.value && calculation.targetDamage < minHitDamage.value) continue
                        if (breakOnlyEnemy.value && checkMinDamage.value && calculation.targetDamage - calculation.selfDamage < breakEfficiency.value) continue

                        if (alwaysClosest.value && attackCrystal != null && Globals.mc.player.getDistanceSq(crystal) >= Globals.mc.player.getDistanceSq(attackCrystal!!)) {
                            continue
                        }

                        if (target != null && target.isAlive && target.getDistanceSq(crystal) <= enemyRange.value.square) {
                            if (calculation.targetDamage < 1.5F) continue
                            if (calculation.targetDamage - calculation.selfDamage < 0.1) continue

                            if (!checkMaxLocalBreak(target, calculation.selfDamage, calculation.targetDamage, 1.5)) {
                                continue
                            }

                            if (calculation.selfDamage > calculation.targetDamage && calculation.targetDamage < target.realHealth) {
                                continue
                            }

                        } else {
                            if (!checkMaxLocalBreak(null, calculation.selfDamage, calculation.targetDamage, 1.5)) {
                                continue
                            }
                        }

                        if (!doSuicideCheck(calculation.selfDamage)) continue

                        if (calculation.selfDamage < selfProtectDamage) {
                            selfProtectDamage = calculation.selfDamage.toDouble()
                            enderCrystal = crystal
                        }

                        if (server.value != Servers.Fast || breakFactor.value == 1) {
                            counted = true
                            countedMinDamage = true
                        } else {
                            if (attackCrystalMap[crystal] != null && attackCrystalMap[crystal]!! >= crystalDamage) continue
                            attackCrystalMap[crystal] = crystalDamage
                        }

                        if (!countedMinDamage) continue
                        ++minDamageCount
                        if (!counted) continue
                        ++crystalCount
                    }
                }
            } else {

                for (crystal in crystalList) {
                    val crystalVector = crystal.positionVector
                    val distanceTo = eyesPos.distanceTo(crystalVector)

                    if (!(Globals.mc.player.canEntityBeSeen(crystal) || EntityUtil.canEntityFeetBeSeen(crystal)) && distanceTo > breakRange.value) {
                        blockedTicks++
                    }

                    if (!checkIsValidCrystal(crystal, distanceTo)) continue

                    if (target == null || !target.isAlive || target.getDistanceSq(crystal) >= enemyRange.value.square) {
                        continue
                    }

                    if (alwaysClosest.value && attackCrystal != null && Globals.mc.player.getDistanceSq(crystal) >= Globals.mc.player.getDistanceSq(attackCrystal!!)) {
                        continue
                    }

                    val distance = CrystalUtils.getMostDistanced(crystal, target)
                    if (keepChecking.value && attackCrystal != null && (distance >= crystalDistanceMultiplied || distance == -1.0)) {
                        continue
                    }

                    val predictionTarget = AutoCrystalHelper.getPredictionSpecified(target)
                    val targetDamage = CrystalUtils.calculateDamage(crystal, target, predictionTarget.first, predictionTarget.second)

                    val selfDamage = CrystalUtils.calculateDamage(crystal, Globals.mc.player)

                    var (minHitDmg, efficiency) = getMinDamageAndFactor(facePlaceMode.value, target, targetDamage, minHitDamage.value, breakEfficiency.value, lethalHealth.value, lethalMultiplier.value)

                    if (HoleUtils.fullCheckInHole(target) && selfDamage < 4) {
                        minHitDmg = 1.8
                        efficiency = 0.1
                    }

                    if (targetDamage < minHitDmg) continue
                    if (targetDamage - selfDamage < efficiency) continue

                    if (syncLastBreak.value && attackCrystal != null && CrystalUtils.calculateDamage(attackCrystal!!, target) > targetDamage) continue
                    if (!checkMaxLocalBreak(target, selfDamage, targetDamage, minHitDmg)) continue

                    if (!doSuicideCheck(selfDamage)) continue
                    if (selfDamage > targetDamage && targetDamage < target.realHealth) continue

                    if (targetDamage - selfDamage <= crystalDiff && targetDamage <= crystalDamage && (targetDamage < crystalDamage || selfDamage >= selfProtectDamage)) {
                        continue
                    }

                    crystalDiff = (targetDamage - selfDamage).toDouble()
                    crystalDamage = targetDamage.toDouble()
                    selfProtectDamage = selfDamage.toDouble()
                    crystalDistanceMultiplied = distance
                    enderCrystal = crystal


                    if (server.value != Servers.Fast || breakFactor.value == 1) {
                        counted = true
                        countedMinDamage = true
                    } else {
                        if (attackCrystalMap[crystal] != null && attackCrystalMap[crystal]!! >= crystalDamage) continue
                        attackCrystalMap[crystal] = crystalDamage
                    }

                    if (!countedMinDamage) continue
                    ++minDamageCount
                    if (!counted) continue
                    ++crystalCount
                }

                if (enderCrystal == null) {
                    for (crystal in crystalList) {
                        if (!(Globals.mc.player.canEntityBeSeen(crystal) || EntityUtil.canEntityFeetBeSeen(crystal)) && eyesPos.distanceTo(crystal.positionVector) > breakRange.value) {
                            blockedTicks++
                        }

                        if (!checkIsValidCrystal(crystal, eyesPos.distanceTo(crystal.positionVector))) continue

                        if (breakOnlyEnemy.value && (target == null || !target.isAlive || target.getDistanceSq(crystal) >= enemyRange.value.square))
                            continue

                        if (alwaysClosest.value && attackCrystal != null && Globals.mc.player.getDistanceSq(crystal) >= Globals.mc.player.getDistanceSq(attackCrystal!!)) {
                            continue
                        }

                        val selfDamage = CrystalUtils.calculateDamage(crystal, Globals.mc.player)

                        if (target != null && target.isAlive && target.getDistanceSq(crystal) <= enemyRange.value.square) {
                            val prediction = AutoCrystalHelper.getPredictionSpecified(target)
                            val targetDamage = CrystalUtils.calculateDamage(crystal, target, prediction.first, prediction.second)

                            if (breakOnlyEnemy.value && checkMinDamage.value) {
                                if (targetDamage < minHitDamage.value) continue
                                if (targetDamage - selfDamage < breakEfficiency.value) continue
                            } else {
                                if (targetDamage < 1.5F) continue
                                if (targetDamage - selfDamage < 0.1) continue
                            }

                            if (!checkMaxLocalBreak(target, selfDamage, targetDamage, 1.5)) {
                                continue
                            }

                            if (selfDamage > targetDamage && targetDamage < target.realHealth) {
                                continue
                            }

                        } else {
                            if (!checkMaxLocalBreak(null, selfDamage, 0.0F, 1.5)) {
                                continue
                            }
                        }

                        if (!doSuicideCheck(selfDamage)) continue

                        if (selfDamage < selfProtectDamage) {
                            selfProtectDamage = selfDamage.toDouble()
                            enderCrystal = crystal
                        }

                        if (server.value != Servers.Fast || breakFactor.value == 1) {
                            counted = true
                            countedMinDamage = true
                        } else {
                            if (attackCrystalMap[crystal] != null && attackCrystalMap[crystal]!! >= crystalDamage) continue
                            attackCrystalMap[crystal] = crystalDamage
                        }

                        if (!countedMinDamage) continue
                        ++minDamageCount
                        if (!counted) continue
                        ++crystalCount
                    }
                }
            }

        /*
        if (waitForBreak.value && placePos != null) {
            if (server.value != Servers.Fast || breakFactor.value == 1) {
                if (enderCrystal == null) {

                }
            } else {
                if (attackCrystalMap.isEmpty()) {

                }
            }
        }
         */

        if (damageSync.value == DamageSync.Break && (crystalDamage > lastDamage || syncTimer.passed(damageSyncDelay.value.toDouble()) || damageSync.value == DamageSync.None)) {
            lastDamage = crystalDamage
        }

        //  if (Globals.mc.gameSettings.keyBindUseItem.isKeyDown && ((offHand && Globals.mc.player.activeHand == EnumHand.OFF_HAND) || (this.mainHand && Globals.mc.player.activeHand == EnumHand.MAIN_HAND)) && crystalDamage < currentMinDamage) {
        //      attackCrystal = null
        //      return
        //  }

        if (server.value != Servers.Fast || breakFactor.value == 1) {
            attackCrystal = enderCrystal
        } else {
            attackCrystalMap = MathUtils.sortByValue(attackCrystalMap, true)
            for (entry in attackCrystalMap.entries) {
                ++crystalCount
                attackCrystalList.add(entry.key)
                ++minDamageCount
            }
        }

    }

    private fun mapBestBlock() {
        val player = AutoCrystalHelper.target

        var bestRange = placeRange.value
        currentBestPlaceRange = 10.0

        if (constrict.value && blockedTicks > 0) {
            if (timing.value == Timing.Adaptive) blockedTicks /= 10
            bestRange -= blockedTicks * RenderUtils.renderPartialTicks
            if (placeRange.value > placeWallRange.value) bestRange = placeWallRange.value
            if (bestRange < placeWallRange.value) bestRange = placeWallRange.value
            currentBestPlaceRange = bestRange
        }

        currentBestConstrictRange = bestRange
        canDoublePop = false

        var currentTarget: EntityPlayer? = null
        var targetPos: BlockPos? = null

        var targetBlockDamage = 0.5F
        var targetBlockDiff = Float.MIN_VALUE
        var targetDistanceTo = 10000.0

        var blockDamageDistance = 69420.0

        var minSelfDamage = Float.MIN_VALUE
        var maxSelfDamage = Float.MAX_VALUE

        var enemyMinDistance = Float.MAX_VALUE

        val eyesPos = Globals.mc.player.eyePosition

        if (placeMap.isNotEmpty() && player != null) {
            for ((pos, calculation) in placeMap) {
                if (calculation.distance > bestRange) continue

                if (player.getDistanceSq(pos.x + 0.5, pos.y + 1.0, pos.z + 0.5) >= enemyRange.value.square) continue
                // if (player.distanceTo(hitVec) >= enemyRange.value) continue

                if (!maxYOffsetCheck(pos) || !isValidPos(pos, calculation.distance) || doCollideCheck(pos)) continue

                val hitVec = pos.toVec3dCenter(0.0, 0.5, 0.0)
                if (placeWallRange.value > 0.0) {
                    val hitPos = RayTraceUtils.rayTraceTo(hitVec.subtract(0.0, 0.5, 0.0))?.blockPos ?: pos
                    if (hitPos.distanceTo(pos) > 1.0 && calculation.distance > placeWallRange.value) continue
                }

                if (!doSuicideCheck(calculation.selfDamage)) continue

                if (placeSync.value) {
                    val bb = CrystalUtils.getCrystalBB(pos.up())
                    val intercepted = synchronized(placedBBMap) {
                        placedBBMap.values.any { it.first.intersects(bb) }
                    }
                    if (intercepted) continue
                }

                if (isDoublePopable(player, calculation.targetDamage) && (targetPos == null || player.getDistanceSq(pos) < player.getDistanceSq(targetPos))) {
                    targetBlockDiff = calculation.targetDamage - calculation.selfDamage
                    targetBlockDamage = calculation.targetDamage
                    minSelfDamage = calculation.selfDamage
                    maxSelfDamage = calculation.selfDamage
                    targetDistanceTo = calculation.distance

                    canDoublePop = true

                    currentTarget = player
                    targetPos = pos
                    continue
                }

                if (placedPosPair != null && shouldLimitPlaceSelection(pos, calculation)) {
                    val lastCalculation = placedPosPair!!.second

                    targetBlockDiff = lastCalculation.targetDamage - lastCalculation.selfDamage
                    targetBlockDamage = lastCalculation.targetDamage
                    minSelfDamage = lastCalculation.selfDamage
                    maxSelfDamage = lastCalculation.selfDamage
                    targetDistanceTo = lastCalculation.distance

                    currentTarget = player
                    targetPos = placedPosPair!!.first
                    break
                }

                if (syncLastPlace.value && placePos != null) {
                    if (CrystalUtils.calculateDamage(placePos!!, player) >= calculation.targetDamage) continue
                }

                val (minPlaceDmg, efficiency) = getMinDamageAndFactor(facePlaceMode.value, player, calculation.targetDamage, minPlaceDamage.value, placeEfficiency.value, lethalHealth.value, lethalMultiplier.value)

                currentMinDamage = minPlaceDmg

                if (calculation.targetDamage < minPlaceDmg) continue
                if (placeEfficiency.value > 0.0 && calculation.targetDamage - calculation.selfDamage < efficiency) continue

                if (!checkMaxLocalPlace(player, calculation.selfDamage, calculation.targetDamage, minPlaceDmg)) continue
                if (calculation.selfDamage > calculation.targetDamage && calculation.targetDamage < player.realHealth) continue

                val distance = CrystalUtils.getMostDistanced(pos, player)
                if (placePos != null && (distance >= blockDamageDistance || distance == -1.0)) continue

                if (canDoublePop) continue

                val enemyDistance = player.getDistanceSq(pos).toFloat()

                if (farDamage.value && eyesPos.distanceTo(hitVec) > placeRange.value / 1.5F) {
                    if ((calculation.targetDamage - calculation.selfDamage > targetBlockDiff || calculation.targetDamage > targetBlockDamage) && maxSelfDamage > calculation.selfDamage && (!popbobCalc.value || enemyMinDistance > enemyDistance)) {
                        targetBlockDiff = calculation.targetDamage - calculation.selfDamage
                        targetBlockDamage = calculation.targetDamage
                        minSelfDamage = calculation.selfDamage
                        maxSelfDamage = calculation.selfDamage
                        targetDistanceTo = calculation.distance

                        enemyMinDistance = enemyDistance

                        blockDamageDistance = distance

                        currentTarget = player
                        targetPos = pos
                    } else if (calibrationCalc.value && calculation.targetDamage == targetBlockDamage && targetBlockDamage > 3.5F) {
                        targetBlockDiff = calculation.targetDamage - calculation.selfDamage
                        targetBlockDamage = calculation.targetDamage
                        minSelfDamage = calculation.selfDamage
                        maxSelfDamage = calculation.selfDamage
                        targetDistanceTo = calculation.distance

                        enemyMinDistance = enemyDistance

                        blockDamageDistance = distance

                        currentTarget = player
                        targetPos = pos
                    }

                } else {
                    if (calculation.targetDamage - calculation.selfDamage > targetBlockDiff && calculation.targetDamage > targetBlockDamage && (calculation.targetDamage >= targetBlockDamage || calculation.selfDamage < minSelfDamage) && (!popbobCalc.value || enemyMinDistance > enemyDistance)) {
                        targetBlockDiff = calculation.targetDamage - calculation.selfDamage
                        targetBlockDamage = calculation.targetDamage
                        minSelfDamage = calculation.selfDamage
                        maxSelfDamage = calculation.selfDamage
                        targetDistanceTo = calculation.distance

                        enemyMinDistance = enemyDistance

                        blockDamageDistance = distance

                        currentTarget = player
                        targetPos = pos
                    } else if (calibrationCalc.value && calculation.targetDamage == targetBlockDamage && targetBlockDamage > 3.5F) {
                        targetBlockDiff = calculation.targetDamage - calculation.selfDamage
                        targetBlockDamage = calculation.targetDamage
                        minSelfDamage = calculation.selfDamage
                        maxSelfDamage = calculation.selfDamage
                        targetDistanceTo = calculation.distance

                        enemyMinDistance = enemyDistance
                        blockDamageDistance = distance

                        currentTarget = player
                        targetPos = pos
                    }

                }

            }
        }

    //    println("calc time = " + ((System.nanoTime() - time) / 1000000))

//        if (player == null) {
//            var targetCounter = 0
//
//            val blocks =
//                if (placeMethod.value == PlaceMethod.Old)
//                    CrystalUtils.findCrystalBlocks(Globals.mc.player, currentBestPlaceRange, placeSelector, onePointThirteen.value, strictDirection.value)
//                else
//                    CrystalUtils.findCrystalBlocks(null, Globals.mc.player, currentBestPlaceRange, onePointThirteen.value, strictDirection.value)
//
//            for (pos in blocks) {
//                val hitVec = pos.toVec3dCenter(0.0, 0.5, 0.0)
//                val distanceTo = eyesPos.distanceTo(hitVec)
//
//                if (distanceTo > bestRange) continue
//
//                if (!maxYOffsetCheck(pos) || !isValidPos(pos, distanceTo) || doCollideCheck(pos)) continue
//
//                if (placeWallRange.value > 0.0) {
//                    val hitPos = RayTraceUtils.rayTraceTo(hitVec.subtract(0.0, 0.5, 0.0))?.blockPos ?: pos
//                    if (hitPos.distanceTo(pos) > 1.0 && distanceTo > placeWallRange.value) continue
//                }
//
//                val selfDamage = CrystalUtils.calculateDamage(pos, Globals.mc.player)
//                if (!doSuicideCheck(selfDamage)) continue
//
//                if (placeSync.value) {
//                    val bb = CrystalUtils.getCrystalBB(pos.up())
//                    val intercepted = synchronized(placedBBMap) {
//                        placedBBMap.values.any { it.first.intersects(bb) }
//                    }
//                    if (intercepted) continue
//                }
//
//                if (!checkYawSpeed(RotationUtils.getRotationTo(hitVec).x)) continue
//
//                for (currentPlayer in AutoCrystalHelper.getLinkedTargetListDamage(enemyRange.value + placeRange.value)) {
//                    val prediction = AutoCrystalHelper.getPredictionSpecified(currentPlayer)
//                    val targetDamage = CrystalUtils.calculateDamage(pos, currentPlayer, prediction.first, prediction.second)
//
//                    if (placedPosPair != null && shouldLimitPlaceSelection(pos, DamageData(targetDamage, selfDamage, distanceTo))) {
//                        currentTarget = currentPlayer
//                        break
//                    }
//
//                    if (isDoublePopable(currentPlayer, targetDamage) && (targetPos == null || currentPlayer.getDistanceSq(pos) < currentPlayer.getDistanceSq(targetPos))) {
//                        currentTarget = currentPlayer
//                        break
//                    }
//
//                    if (syncLastPlace.value && placePos != null) {
//                        if (CrystalUtils.calculateDamage(placePos!!, currentPlayer, prediction.first, prediction.second) >= targetDamage) continue
//                    }
//
//                    val minPlaceDmgAndFactor = getMinDamageAndFactor(facePlaceMode.value, currentPlayer, targetDamage, minPlaceDamage.value, placeEfficiency.value, lethalHealth.value, lethalMultiplier.value)
//                    val minPlaceDmg = minPlaceDmgAndFactor.first
//                    val efficiency = minPlaceDmgAndFactor.second
//                    currentMinDamage = minPlaceDmg
//
//                    if (targetDamage < minPlaceDmg) continue
//                    if (placeEfficiency.value > 0.0 && targetDamage - selfDamage < efficiency) continue
//
//                    if (!checkMaxLocalPlace(currentPlayer, selfDamage, targetDamage, minPlaceDmg)) continue
//                    if (selfDamage > targetDamage && targetDamage < CombatUtils.getHealthSmart(currentPlayer)) continue
//
//                    val distance = CrystalUtils.getMostDistanced(pos, currentPlayer)
//                    if (placePos != null && (distance >= blockDamageDistance || distance == -1.0)) continue
//
//                    if (farDamage.value && currentPlayer.getDistanceSq(Globals.mc.player) >= MathUtils.square(bestRange.div(2.0))) {
//                        if (canDoublePop || targetDamage <= targetBlockDamage) {
//                            continue
//                        }
//                    } else {
//                        if (canDoublePop || targetDamage - selfDamage <= targetBlockDiff && targetDamage <= targetBlockDamage && (targetDamage < targetBlockDamage || selfDamage >= maxSelfDamage)) {
//                            continue
//                        }
//                    }
//
//                    targetBlockDiff = (targetDamage - selfDamage).toDouble()
//                    targetBlockDamage = targetDamage.toDouble()
//                    maxSelfDamage = selfDamage.toDouble()
//                    targetDistanceTo = distanceTo
//
//                    blockDamageDistance = distance
//
//                    currentTarget = currentPlayer
//                    targetPos = pos
//
//                    if (++targetCounter >= maxTargets.value) break
//                }
//
//            }
//        }

        if (player == null && targetPriority.value != TargetPriority.MTDamage) {
            var targetCounter = 0

            for (currentPlayer in AutoCrystalHelper.getLinkedTargetListDamage(enemyRange.value + placeRange.value)) {
                if (EntityUtil.isntValid(currentPlayer, enemyRange.value + placeRange.value)) continue
                if (currentPlayer.getDistanceSq(Globals.mc.player) > (enemyRange.value + placeRange.value).square) continue

                val blocks =
                    if (placeMethod.value == PlaceMethod.Old)
                        CrystalUtils.findCrystalBlocks(Globals.mc.player, currentBestPlaceRange, placeSelector, onePointThirteen.value, strictDirection.value)
                    else
                        CrystalUtils.findCrystalBlocks(currentPlayer, Globals.mc.player, currentBestPlaceRange, onePointThirteen.value, strictDirection.value)

                if (blocks.isNotEmpty()) {
                    for (altPos in blocks) {
                        val hitVec = altPos.toVec3dCenter(0.0, 0.5, 0.0)
                        val distanceTo = eyesPos.distanceTo(hitVec)

                        if (distanceTo > bestRange) continue
                        if (currentPlayer.distanceTo(hitVec) >= enemyRange.value) continue

                        if (!maxYOffsetCheck(altPos) || !isValidPos(altPos, distanceTo) || doCollideCheck(altPos)) continue

                        if (placeWallRange.value > 0.0) {
                            val hitPos = RayTraceUtils.rayTraceTo(hitVec.subtract(0.0, 0.5, 0.0))?.blockPos ?: altPos
                            if (hitPos.distanceTo(altPos) > 1.0 && distanceTo > placeWallRange.value) continue
                        }

                        val selfDamage = CrystalUtils.calculateDamage(altPos, Globals.mc.player)
                        if (!doSuicideCheck(selfDamage)) continue

                        if (placeSync.value) {
                            val bb = CrystalUtils.getCrystalBB(altPos.up())
                            val intercepted = synchronized(placedBBMap) {
                                placedBBMap.values.any { it.first.intersects(bb) }
                            }
                            if (intercepted) continue
                        }

                        val prediction = AutoCrystalHelper.getPredictionSpecified(currentPlayer)
                        val targetDamage = CrystalUtils.calculateDamage(altPos, currentPlayer, prediction.first, prediction.second)

                   //     if (shouldLimitPlaceSelection(altPos, DamageData(targetDamage, selfDamage, distanceTo))) continue

                        if (isDoublePopable(currentPlayer, targetDamage) && (targetPos == null || currentPlayer.getDistanceSq(altPos) < currentPlayer.getDistanceSq(targetPos))) {
                            targetBlockDiff = targetDamage - selfDamage
                            targetBlockDamage = targetDamage
                            minSelfDamage = selfDamage
                            targetDistanceTo = distanceTo

                            canDoublePop = true

                            currentTarget = currentPlayer
                            targetPos = altPos
                            continue
                        }

                        if (placedPosPair != null && shouldLimitPlaceSelection(altPos, DamageData(targetDamage, selfDamage, distanceTo))) {
                            val lastCalculation = placedPosPair!!.second

                            targetBlockDiff = lastCalculation.targetDamage - lastCalculation.selfDamage
                            targetBlockDamage = lastCalculation.targetDamage
                            minSelfDamage = lastCalculation.selfDamage
                            targetDistanceTo = lastCalculation.distance

                            currentTarget = currentPlayer
                            targetPos = placedPosPair!!.first
                            break
                        }

                        if (syncLastPlace.value && placePos != null) {
                            if (CrystalUtils.calculateDamage(placePos!!, currentPlayer, prediction.first, prediction.second) >= targetDamage) continue
                        }

                        val (minPlaceDmg, efficiency)  = getMinDamageAndFactor(facePlaceMode.value, currentPlayer, targetDamage, minPlaceDamage.value, placeEfficiency.value, lethalHealth.value, lethalMultiplier.value)

                        currentMinDamage = minPlaceDmg

                        if (targetDamage < minPlaceDmg) continue
                        if (placeEfficiency.value > 0.0 && targetDamage - selfDamage < efficiency) continue

                        if (!checkMaxLocalPlace(currentPlayer, selfDamage, targetDamage, minPlaceDmg)) continue
                        if (selfDamage > targetDamage && targetDamage < currentPlayer.realHealth) continue

                        val distance = CrystalUtils.getMostDistanced(altPos, currentPlayer)
                        if (placePos != null && (distance >= blockDamageDistance || distance == -1.0)) continue

                        if (canDoublePop) continue

                        if (farDamage.value && eyesPos.distanceTo(hitVec) > placeRange.value / 1.5F) {
                            if ((targetDamage - selfDamage > targetBlockDiff || targetDamage > targetBlockDamage) && maxSelfDamage > selfDamage) {
                                targetBlockDiff = targetDamage - selfDamage
                                targetBlockDamage = targetDamage
                                minSelfDamage = selfDamage
                                maxSelfDamage = selfDamage
                                targetDistanceTo = distanceTo

                                blockDamageDistance = distance

                                currentTarget = player
                                targetPos = altPos
                            }
                        } else {
                            if (targetDamage - selfDamage > targetBlockDiff && targetDamage > targetBlockDamage && (targetDamage >= targetBlockDamage || selfDamage < minSelfDamage)) {
                                targetBlockDiff = targetDamage - selfDamage
                                targetBlockDamage = targetDamage
                                minSelfDamage = selfDamage
                                maxSelfDamage = selfDamage
                                targetDistanceTo = distanceTo

                                blockDamageDistance = distance

                                currentTarget = player
                                targetPos = altPos
                            }
                        }

                    }
                }

                if (++targetCounter >= maxTargets.value) break
            }
        }

        if (renderDamage.value && targetPos != null) placedCrystalDamage = targetBlockDamage

        if (targetPos != null && currentTarget != null && currentTarget.isAlive) {
            if (placeMode.value != PlaceMode.Normal) {
                if (CrystalUtils.calculateDamage(targetPos, currentTarget) > 11 && currentTarget.onGround) {
                    shouldAntiSurround = false
                }
            } else {
                shouldAntiSurround = false
            }
        }

        // if (AutoCrystalHelper.target == null) AutoCrystalHelper.target = currentTarget

        renderingTarget = currentTarget

        currentDamage = targetBlockDamage.toDouble()
        currentMaxSelfDamage = minSelfDamage.toDouble()
        currentDistance = targetDistanceTo

        placePos = targetPos
    }

    fun updateBlockCalcMT() {
        if (calculationThread == null || !calculationThread!!.isAlive || calculationThread!!.isInterrupted) {
            calculationThread = thread(start = false) {
            //    placePos = null
                mapBestBlock()
            }
            calculationExecutor.execute(calculationThread!!)
        }
    }

    private fun doManualBreak() {
        if (manualBreak.value && (offHand || mainHand) && manualTimer.passed(breakDelay.value.toDouble()) && Globals.mc.gameSettings.keyBindUseItem.isKeyDown &&
            Globals.mc.player.heldItemOffhand.item != Items.GOLDEN_APPLE &&
            Globals.mc.player.inventory.getCurrentItem().item != Items.GOLDEN_APPLE &&
            Globals.mc.player.inventory.getCurrentItem().item != Items.BOW &&
            Globals.mc.player.inventory.getCurrentItem().item != Items.EXPERIENCE_BOTTLE) {

            val result = Globals.mc.objectMouseOver ?: return

            when (result.typeOfHit) {
                RayTraceResult.Type.ENTITY -> {
                    val entity = result.entityHit
                    if (entity is EntityEnderCrystal) {
                        val selfDamage = CrystalUtils.calculateDamage(entity, Globals.mc.player)
                        if (selfDamage > maxSelfHitDmg.value) return
                        explodeCrystalAction(CPacketUseEntity(entity), entity)
                        manualTimer.reset()
                    }
                }
                RayTraceResult.Type.BLOCK -> {
                    val mousePosBB = result.blockPos.up().collisionBox
                    for (target in Globals.mc.world.getEntitiesWithinAABBExcludingEntity(null, mousePosBB)) {
                        if (target !is EntityEnderCrystal) continue
                        val selfDamage = CrystalUtils.calculateDamage(target, Globals.mc.player)
                        if (selfDamage > maxSelfHitDmg.value) continue
                        explodeCrystalAction(CPacketUseEntity(target), target)
                        manualTimer.reset()
                    }
                }
            }

        }
    }

    /*
    private fun checkIsValidEntity(entity: EntityLivingBase): Boolean {
        if (isEntityAlive(entity)) {
            return entity is EntityPlayer && players.value && entity != Globals.mc.player &&
                    entity.getEntityId() != -1337 && (!Muffin.getInstance().friendManager.isFriend(entity.getName())) ||
                    (EntityTypeUtils.isMonster(entity) || EntityTypeUtils.isHugeEntity(entity)) && monster.value ||
                    EntityTypeUtils.isChillAggressiveAnimals(entity) && neutrals.value ||
                        EntityTypeUtils.isAnimals(entity as Entity) && animals.value
        }
        return false
    }
     */

    private fun getPlacePacket(pos: BlockPos, hand: EnumHand): CPacketPlayerTryUseItemOnBlock {
        val serverIP = InfoUtils.getServerIP()?.toLowerCase() ?: return CPacketPlayerTryUseItemOnBlock()

        val isRotateOn = rotateMode.value != RotateMode.None
        val result = if (isRotateOn) RayTraceUtils.getStrictResult(lastLookAt) else RayTraceResult(Vec3d(0.5, 1.0, 0.5), EnumFacing.UP)

        var sideHit = result.sideHit

        val clickingSidePos = pos.offset(sideHit)
        if (strictDirection.value && clickingSidePos.isFullBox) {
            sideHit = pos.getClosestVisibleSideStrict() ?: EnumFacing.UP
        }

        val hitVecX = result.hitVec.x.toFloat() - if (isRotateOn) pos.x.toFloat() else 0.0F
        val hitVecY = result.hitVec.y.toFloat() - if (isRotateOn) pos.y.toFloat() else if (serverIP.startsWith("constantiam")) 0.5F else 0.0F
        val hitVecZ = result.hitVec.z.toFloat() - if (isRotateOn) pos.z.toFloat() else 0.0F

        return CPacketPlayerTryUseItemOnBlock(pos, sideHit, hand, hitVecX, hitVecY, hitVecZ)
    }

    private fun sendOrQueuePacket(packet: Packet<*>) {
        val yawDiff = abs(RotationUtils.normalizeAngle(LocalMotionManager.serverSideRotation.x - getLastRotation().x))
        if (yawDiff < yawStepTicks.value.times(10)) {
            sendPacketDirect(packet)
        } else {
            synchronized(packetList) {
                packetList.add(packet)
            }
        }
    }

    private fun sendPacketDirect(packet: Packet<*>) {
        if (packet is CPacketAnimation && renderOffhand.value) Globals.mc.player.swingArm(packet.hand)
        else Globals.mc.connection?.sendPacket(packet)
    }

    private fun searchCenter(vectorTo: Vec3d, throughWalls: Boolean = true, distance: Float,
                             expectedTarget: BlockPos? = null,
                             pattern: Pattern = GaussianPattern
    ): VecRotation? {
        val preferredSpot = pattern.spot(vectorTo)
        val preferredRotation = RotationUtils.getRotationTo(preferredSpot)

        val eyesPos = Globals.mc.player.eyePosition

        var visibleRot: VecRotation? = null
        var notVisibleRot: VecRotation? = null

        for (x in 0.1..0.9 step 0.1) for (y in 0.1..0.9 step 0.1) for (z in 0.1..0.9 step 0.1) {
            val vector = vectorTo.add(x, y, z)

            val eyesToVec = eyesPos.distanceTo(vector)
            if (eyesToVec > distance) continue

            val visible = if (expectedTarget != null) RayTraceUtils.facingBlock(eyesPos, vector, expectedTarget) else RayTraceUtils.isVisible(eyesPos, vector)

            if (throughWalls || visible) {
                val rotation = RotationUtils.getRotationTo(vector)
                if (visible) {
                    // Calculate next spot to preferred spot
                    if (visibleRot == null || getRotationDifference(rotation, preferredRotation) < getRotationDifference(visibleRot.rotation, preferredRotation)) {
                        visibleRot = VecRotation(vector, rotation)
                    }
                } else if (throughWalls) {
                    // Calculate next spot to preferred spot
                    if (notVisibleRot == null || getRotationDifference(rotation, preferredRotation) < getRotationDifference(notVisibleRot.rotation, preferredRotation)) {
                        notVisibleRot = VecRotation(vector, rotation)
                    }
                }

            }
        }

        return visibleRot ?: notVisibleRot
    }

    private fun updateRotationHitable() {
        if (yawStep.value == 0 || yawStepTicks.value == 0) {
            hadRotatedTo = true
            return
        }

        if (renderingTarget != null && lastLookAt == renderingTarget?.positionVector ?: Vec3d.ZERO) {
            hadRotatedTo = false
            return
        }

        val currentRotation = getLastRotation()
        val angleDifference = abs(RotationUtils.normalizeAngle(RotationUtils.getAngleDifference(currentRotation.x, LocalMotionManager.serverSideRotation.x)))

        if (angleDifference <= yawStepTicks.value.times(17.5)) ++facedTicks else facedTicks = 0
        hadRotatedTo = facedTicks >= yawStepTicks.value
    }

    private fun getLastRotation(): Vec2f {
        val directRotation = RotationUtils.getRotationTo(lastLookAt)

        val rotationTo = if (rotateMode.value == RotateMode.Strict) {
            val (_, rotation) = searchCenter(lastLookAt, throughWalls = true, distance = placeRange.value.toFloat(), expectedTarget =  lastLookAt.toBlockPos()) ?: VecRotation(lastLookAt, directRotation)
            rotation
        } else {
            directRotation
        }

        return if (yawStep.value == 0 || yawStepTicks.value == 0) {
            directRotation
        } else {
            val maxTurnSpeed = yawStep.value
            val minTurnSpeed = maxTurnSpeed.div(2.555)
            RotationUtils.limitAngleChange(LocalMotionManager.serverSideRotation, rotationTo, (Math.random() * (maxTurnSpeed - minTurnSpeed) + minTurnSpeed).toFloat())
        }
    }

    private fun resetRotation() {
        lastLookAt = Vec3d.ZERO // renderingTarget?.positionVector ?: Vec3d.ZERO
    }

    fun checkArmourBreakable(player: EntityPlayer, checkDurability: Boolean, durability: Double): Boolean {
        for (armors in player.armorInventoryList) {
            if (armors == null || armors.isEmpty || armors.item is ItemAir || (checkDurability && EntityUtil.getArmorPct(armors) < durability)) {
                return true
            }
        }
        return false
    }

    fun getMinDamageAndFactor(facePlace: FacePlace, player: EntityPlayer, targetDamage: Float, minDamage: Double, factor: Double, facePlaceHp: Double, lethalMultiplier: Double): Pair<Double, Double> {
        var minDmg = minDamage
        var factorDiff = factor

        val facePlaceAble = player.realHealth < facePlaceHp
        val armourChecker = checkArmourBreakable(player, minDurability.value > 0, minDurability.value)
        val multiplyDmg = lethalMultiplier > 0 && targetDamage * (1.0f + lethalMultiplier) >= player.realHealth

        val isEnemyInHole = HoleUtils.fullCheckInHole(player)
        val isValidSpeedCheck = minSpeedFacePlace.value > 0 && player.speedKmh < minSpeedFacePlace.value

        when (facePlace) {
            FacePlace.Durability -> {
                if (facePlaceAble || multiplyDmg || armourChecker) {
                    if (isEnemyInHole || isValidSpeedCheck) {
                        minDmg = 1.8
                        factorDiff = 0.2
                        shouldAntiSurround = true
                    } else {
                        shouldAntiSurround = false
                    }
                } else {
                    shouldAntiSurround = false
                }
            }
            FacePlace.Constant -> {
                if ((isEnemyInHole && HoleUtils.fullCheckInHole(Globals.mc.player)) || armourChecker || facePlaceAble || multiplyDmg) {
                    minDmg = 1.8
                    factorDiff = 0.2
                    shouldAntiSurround = true
                } else {
                    shouldAntiSurround = false
                }
            }
            FacePlace.Off -> {
                if (facePlaceAble || multiplyDmg) {
                    if (isEnemyInHole || isValidSpeedCheck) {
                        minDmg = 1.8
                        factorDiff = 0.2
                        shouldAntiSurround = true
                    } else {
                        shouldAntiSurround = false
                    }
                } else {
                    shouldAntiSurround = false
                }
            }
        }

        return minDmg to factorDiff
    }

    private fun shouldPause(): Boolean {
        if (fullNullCheck()) return true

        if (HoleFillerModule.isProcessing) return true
        if (WebModule.isProcessing) return true

        if (pauseWhileMining.value && Globals.mc.gameSettings.keyBindAttack.isKeyDown && Globals.mc.playerController.isHittingBlock && Globals.mc.player.heldItemMainhand.item is ItemTool) return true
        if (pauseWhileEating.value && EntityUtil.isEating()) return true

        // renderingTarget = null
        placeSelector = when (placeMode.value) {
            PlaceMode.All -> RandomUtils.random.nextBoolean() && !shouldAntiSurround
            PlaceMode.Multi -> !shouldAntiSurround
            else -> false
        }

        maxRange = max(placeRange.value, breakRange.value)

        canDoublePop = false
        mainHand = (Globals.mc.player.heldItemMainhand.item == Items.END_CRYSTAL)
        offHand = (Globals.mc.player.heldItemOffhand.item == Items.END_CRYSTAL)
        placePos = null

        currentMinDamage = 0.0
        currentDamage = 0.0
        currentMaxSelfDamage = 0.0
        currentDistance = 0.0

        updateMap()
        if (collidedTicks <= 10) doCalculateCrystal()

        if (!posConfirmed && damageSync.value != DamageSync.None && syncTimer.passed(verifyPos.value.toDouble())) {
            syncTimer.resetTimeSkipTo(damageSyncDelay.value.toLong() + 1)
        }

        return false
    }

    private fun isDoublePopable(player: EntityPlayer, damage: Float): Boolean {
        val health = player.realHealth
        if (fastPop.value && health <= 1.0 && damage > health + 0.5 && damage <= 4.0) {
            val timer = totemPops[player]
            return timer == null || timer.passed(500.0)
        }
        return false
    }

    private fun doBreaking(packet: CPacketUseEntity, crystal: EntityEnderCrystal?) {
        // (Globals.mc.playerController as IPlayerControllerMP).syncCurrentPlayItemVoid()
        val critAble = crystal != null &&
                Globals.mc.player.fallDistance > 0.0f &&
                !Globals.mc.player.onGround &&
                !Globals.mc.player.isOnLadder &&
                !Globals.mc.player.isInWater &&
                !Globals.mc.player.isPotionActive(MobEffects.BLINDNESS) &&
                Globals.mc.player.ridingEntity == null

        val haveSharpnessSword =
            crystal != null &&
                Globals.mc.player.heldItemMainhand.item is ItemSword &&
                EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, Globals.mc.player.heldItemMainhand) > 0

        val currentHand = getHand() ?: EnumHand.MAIN_HAND

        if (timing.value != Timing.Vanilla && timing.value != Timing.Adjusted) {
            sendOrQueuePacket(CPacketAnimation(currentHand))
            sendOrQueuePacket(packet)

            // if (crystal != null) Globals.mc.player.attackTargetEntityWithCurrentItem(crystal) // Globals.mc.playerController.attackEntity(Globals.mc.player, crystal)
        } else if (timing.value == Timing.Vanilla || timing.value == Timing.Adjusted) {
            when (currentHand) {
                EnumHand.MAIN_HAND -> Globals.mc.player.swingArm(EnumHand.MAIN_HAND)
                EnumHand.OFF_HAND -> if (renderOffhand.value) Globals.mc.player.swingArm(EnumHand.OFF_HAND)
                else Globals.mc.connection?.sendPacket(CPacketAnimation(EnumHand.OFF_HAND))
            }

            Globals.mc.connection?.sendPacket(packet)
            // if (crystal != null) Globals.mc.player.attackTargetEntityWithCurrentItem(crystal) //Globals.mc.playerController.attackEntity(Globals.mc.player, crystal)
        }

        if (critAble) Globals.mc.player.onCriticalHit(crystal!!)
        if (haveSharpnessSword) Globals.mc.player.onEnchantmentCritical(crystal!!)
        Globals.mc.player.resetCooldown()
    }

    private fun doPlacing(pos: BlockPos, hand: EnumHand) {
        if (timing.value != Timing.Vanilla && timing.value != Timing.Adjusted) {
            sendOrQueuePacket(getPlacePacket(pos, hand))
            sendOrQueuePacket(CPacketAnimation(hand))
        } else if (timing.value == Timing.Vanilla || timing.value == Timing.Adjusted) {
            Globals.mc.connection?.sendPacket(getPlacePacket(pos, hand))
            if (renderOffhand.value) Globals.mc.player.swingArm(hand) else Globals.mc.connection?.sendPacket(CPacketAnimation(hand))
        }
    }

    private fun getHand(): EnumHand? {
        return when (Items.END_CRYSTAL) {
            Globals.mc.player.heldItemOffhand.item -> EnumHand.OFF_HAND
            Globals.mc.player.heldItemMainhand.item -> EnumHand.MAIN_HAND
            Globals.mc.player.serverSideItem.item -> EnumHand.MAIN_HAND
            else -> null
        }
    }

    fun maxYOffsetCheck(pos: BlockPos): Boolean {
        return (maxYOffset.value == 0.0 || abs(Globals.mc.player.posY - pos.y) <= maxYOffset.value)
    }

    fun isValidPos(pos: BlockPos, distanceToPos: Double): Boolean {
        if (rayTraceRange.value > 0.0 && distanceToPos > rayTraceRange.value && !RayTraceUtils.rayTracePlaceCheck(Globals.mc.player, pos)) return false
        return combinedTraceCheck(pos.toVec3dCenter(0.0, 0.5, 0.0))
    }

    private fun combinedTraceCheck(vector: Vec3d): Boolean {
        if (Globals.mc.player.distanceTo(vector) <= wallScaling.value) return true
        if (!EntityUtil.canSeeVec3d(vector.add(0.0, 1.700000047683716, 0.0))) return false
        return true
//        if (Globals.mc.player.distanceTo(vector) <= wallScaling.value) return true

//        if (!EntityUtil.canSeeVec3d(vector.add(0.0, 1.700000047683716, 0.0))) return false
//
//        val eyesPos = Globals.mc.player.eyePosition
//        val placePosition = vector.toBlockPos().down()
//        val centerPos = vector.subtract(0.0, 0.5, 0.0)
//        val centerResult = Globals.mc.world.rayTraceBlocks(
//            eyesPos, centerPos, false, true, false
//        )
//
//        return  centerResult == null ||
//                centerResult.blockPos == placePosition ||
//                centerResult.hitVec != null &&
//                placePosition.distanceSqToCenter(centerResult.hitVec.x, centerResult.hitVec.y, centerResult.hitVec.z) <= 1.0
    }

    private fun checkDamagePlace(damage: Float, selfDamage: Float) = (damage >= minPlaceDamage.value) && (selfDamage <= maxSelfDmg.value)

    fun checkMaxLocalPlace(player: EntityPlayer, selfDamage: Float, targetDamage: Float, minDamage: Double): Boolean {
        if (maxSelfDmg.value == 0.0) return true

        if (selfDamage > maxSelfDmg.value) {
            val targetHp = player.realHealth
            if (overrideMaxLocal.value && targetDamage > targetHp + 1.0F) {
                return true
            } else if (antiSuicideHp.value == 0.0) {
                return (targetDamage >= minDamage || targetHp <= lethalHealth.value && targetDamage > 1.5)
            }
            return false
        }

        return compromise.value == 0.0 || targetDamage * compromise.value > selfDamage
    }

    private fun checkMaxLocalBreak(player: EntityPlayer?, selfDamage: Float, targetDamage: Float, minDamage: Double): Boolean {
        if (maxSelfHitDmg.value == 0.0) return true

        if (player != null) {
            if (selfDamage > maxSelfHitDmg.value) {
                val targetHp = player.realHealth
                if (overrideMaxLocal.value && targetDamage > targetHp + 1.0F) {
                    return true
                } else if (antiSuicideHp.value == 0.0) {
                    return (targetDamage >= minDamage || targetHp <= lethalHealth.value && targetDamage > 1.5)
                }
                return false
            }
            return compromise.value == 0.0 || targetDamage * compromise.value > selfDamage
        }

        return selfDamage <= maxSelfHitDmg.value
    }

    fun doSuicideCheck(selfDamage: Float): Boolean {
        return antiSuicideHp.value == 0.0 ||
            (selfDamage < Globals.mc.player.realHealth - antiSuicideHp.value &&
            Globals.mc.player.realHealth - selfDamage > antiSuicideHp.value)
    }

    fun doCollideCheck(pos: BlockPos): Boolean {
        if (placeMethod.value == PlaceMethod.Old) return false
        return if (placeSelector) !CrystalUtils.canPlaceCollide(pos) else !CrystalUtils.canPlaceCollideAntiSurround(pos)
    }

    private fun shouldLimitPlaceSelection(pos: BlockPos, data: DamageData): Boolean {
        if (limitPlace.value == 0) return false
        val lastPair = placedPosPair ?: return false

        val lastPos = lastPair.first

        if (lastPos == pos && renderingTarget != null) {
            val lastData = lastPair.second
//            val targetDamageDiff = data.targetDamage - lastData.targetDamage
//            val selfDamageDiff = data.selfDamage - lastData.selfDamage
//            val distanceDiff = data.distance - lastData.distance

            val eyesPos = Globals.mc.player.eyePosition
            val gapDiff = lastPos.subtract(pos)
            val squaredGap = sqrt((gapDiff.x * gapDiff.x + gapDiff.y * gapDiff.y + gapDiff.z * gapDiff.z).toDouble())

            val targetDamageDiff = lastData.targetDamage - data.targetDamage
            val selfDamageDiff = lastData.selfDamage - data.selfDamage
            val distanceDiff = lastData.distance - data.distance

//            val targetDamageDiff = CrystalUtils.calculateDamage(lastPair.first, renderingTarget!!) - data.targetDamage
//            val selfDamageDiff = CrystalUtils.calculateDamage(lastPair.first, Globals.mc.player) - data.selfDamage
//            val distanceDiff = Globals.mc.player.eyePosition.distanceTo(lastPair.first) - data.distance

//            val minDiffDamage = when (limitPlace.value) {
//                1..3 -> 2.5
//                4..6 -> 4.0
//                7..8 -> 5.0
//                else -> 0.0
//            }

            val minDiffDamage = when (limitPlace.value) {
                in 1..3 -> 2.5
                in 4..6 -> 4.0
                in 7..8 -> 5.0
                else -> 0.0
            }

            if (isValidPos(lastPos, eyesPos.distanceTo(lastPos.toVec3dCenter(0.0, 0.5, 0.0))) && targetDamageDiff <= minDiffDamage && selfDamageDiff <= 2 && squaredGap < limitPlace.value) { // && distanceDiff <= 1.5 && squaredGap < limitPlace.value) {
                // ChatManager.sendMessage("Limiting")
                return true
            }
        }

        return false
    }

    private fun updateDesperateBreak() {
        if (placeMode.value != PlaceMode.Normal) {
            collidedTicks = 0
            return
        }

        if (renderBlock.isNull) return

        val eyesPos = Globals.mc.player.eyePosition
        val placingBB = CrystalUtils.getCrystalPlacingBB(renderBlock)
        val collidingEntity = Globals.mc.world.getEntitiesWithinAABBExcludingEntity(null, placingBB).firstOrNull { it.isAlive }
        if (collidingEntity != null) collidedTicks++ else collidedTicks = 0

//        ChatManager.sendMessage(collidedTicks.toString())
//        ChatManager.sendMessage(collidingEntity?.positionVector.toString())

        if (collidedTicks <= 10) return
        if (collidingEntity == null || collidingEntity !is EntityEnderCrystal) return

        val selfDamage = CrystalUtils.calculateDamage(collidingEntity, Globals.mc.player)
        if (!checkIsValidCrystal(collidingEntity, eyesPos.distanceTo(collidingEntity.positionVector)) || selfDamage > maxSelfHitDmg.value) return

        if (server.value != Servers.Fast || breakFactor.value == 1) {
            // ChatManager.sendMessage("Before: " + attackCrystal?.positionVector.toString())
            if (attackCrystal == null) {
                // ChatManager.sendMessage("here")
                attackCrystal = collidingEntity
                // ChatManager.sendMessage("After: " + attackCrystal?.positionVector.toString())
            }
        } else {
            val polledCrystal = attackCrystalList.pollFirst()
            if (attackCrystalList.isEmpty() || polledCrystal == null) {
                attackCrystalList.add(collidingEntity)
            }
        }
    }

    private fun doCheckValidSpawnTime(crystal: EntityEnderCrystal): Boolean {
        if (waitDelay.value == 0.0) return true

        val waitDelay = waitDelay.value * 100.0
        return System.currentTimeMillis() - getSpawnTime(crystal) > waitDelay
    }

    private fun getSpawnTime(crystal: EntityEnderCrystal): Long {
        return crystalSpawnMap.getOrPut(crystal.entityId) { System.currentTimeMillis() }
    }

    private fun resetStuff(pos: Boolean, crystal: Boolean, target: Boolean, rotation: Boolean) {
        if (pos) {
            renderBlock.setNull()
        }
        if (target) {
            renderingTarget = null
        }
        if (rotation) {
            resetRotation()
        }
    }

}