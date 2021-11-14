package me.han.muffin.client.module.modules.combat

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.ClientTickEvent
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.entity.TotemPopEvent
import me.han.muffin.client.event.events.gui.GuiScreenEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.LocalHotbarManager
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.RotateMode
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.entity.WeaponUtils
import me.han.muffin.client.utils.extensions.kotlin.square
import me.han.muffin.client.utils.extensions.mc.block.*
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.item.*
import me.han.muffin.client.utils.extensions.mc.world.*
import me.han.muffin.client.utils.extensions.mixin.netty.id
import me.han.muffin.client.utils.extensions.mixin.netty.packetAction
import me.han.muffin.client.utils.extensions.mixin.netty.windowId
import me.han.muffin.client.utils.math.PlaceInfo
import me.han.muffin.client.utils.math.RandomUtils
import me.han.muffin.client.utils.math.VectorUtils
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.block.Block
import net.minecraft.block.BlockDispenser
import net.minecraft.block.BlockShulkerBox
import net.minecraft.client.gui.GuiHopper
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagList
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.SPacketCloseWindow
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.lang.Thread.sleep
import java.util.*
import java.util.function.Predicate
import kotlin.concurrent.thread

internal object Auto32kModule2: Module("Auto32k", Category.COMBAT, "Automatically place and kill people with 32k.") {
    private val page = EnumValue(Pages.General, "Page")

    private val timing = EnumValue({ page.value == Pages.General }, Timing.Vanilla, "Timing")
    private val meta = EnumValue({ page.value == Pages.General }, Meta.Hopper, "Meta")
    private val mode = EnumValue({ page.value == Pages.General }, Mode.Looking, "Mode")
    private val rotateMode = EnumValue({ page.value == Pages.General }, RotateMode.Tick, "Rotation")

    private val swapSword = Value({ page.value == Pages.General },true, "SwapSword")
    private val fastKill = Value({ page.value == Pages.General }, false, "FastKill")
    private val waitDelay = NumberValue({ page.value == Pages.General && swapSword.value }, 100, 0, 1000, 2, "WaitDelay")
    private val swapDelay = NumberValue({ page.value == Pages.General && swapSword.value }, 1, 0, 10, 1, "SwapDelay")

    private val strictDirection = Value({ page.value == Pages.General && meta.value == Meta.Dispenser }, false, "StrictDirection")
    private val targetRange = NumberValue({ page.value == Pages.General },6.5, 0.1, 15.0, 0.1, "TargetRange")

    private val smartPosition = Value({ page.value == Pages.Auto }, false, "SmartPosition")
    private val autoRange = NumberValue({ page.value == Pages.Auto },5.0, 0.1, 8.0, 0.1, "AutoRange")

    private val lookingRange = NumberValue({ page.value == Pages.Looking },4.2, 0.1, 8.0, 0.1, "LookingRange")

    private val killAura = Value({ page.value == Pages.Aura }, true, "KillAura")
    private val attackTiming = EnumValue({ page.value == Pages.Aura && killAura.value }, UpdateMode.Vanilla, "AttackTiming")
    private val attackRotate = Value({ page.value == Pages.Aura && killAura.value }, true, "AttackRotate")
    private val killAttempt = Value({ page.value == Pages.Aura && killAura.value }, true, "KillAttempt")

    private val inhibit = Value({ page.value == Pages.Aura && killAura.value }, true, "Inhibit")
    private val swingArm = Value({ page.value == Pages.Aura && killAura.value }, true, "SwingArm")

    private val nativeAttack = Value({ page.value == Pages.Aura && killAura.value }, false, "NativeAttack")
    private val nativeDelay = NumberValue({ page.value == Pages.Aura && killAura.value && nativeAttack.value }, 5, 0, 1000, 1, "NativeDelay")

    private val attackFactor = NumberValue({ page.value == Pages.Aura && killAura.value }, 1, 1, 20, 1, "AttackFactor")
    private val attackChance = NumberValue({ page.value == Pages.Aura && killAura.value }, 100, 1, 100, 1, "AttackChance")
    private val attackRange = NumberValue({ page.value == Pages.Aura && killAura.value }, 5.5, 0.1, 10.0, 0.1, "AttackRange")

    private val attackDelay = NumberValue({ page.value == Pages.Aura && killAura.value }, 50.0, 0.0, 1000.0, 1.0, "AttackDelay")
    private val minRandomDelay = NumberValue({ page.value == Pages.Aura && killAura.value }, 50.0F, 0.0F, 1000.0F, 1.0F, "MinRandomDelay")
    private val maxRandomDelay = NumberValue({ page.value == Pages.Aura && killAura.value }, 100.0F, 0.0F, 1000.0F, 1.0F, "MaxRandomDelay")

    private val aimTarget = NumberValue({ page.value == Pages.Aura && killAura.value }, 1, 1, 10, 1, "AimTarget")

    private val extraAttack = Value({ page.value == Pages.ExtraAura && killAura.value }, true, "ExtraAttack")
    private val extraAPS = NumberValue({ page.value == Pages.ExtraAura && killAura.value && extraAttack.value }, 5, 0, 20, 1, "ExtraAPS")

    private val extraCrit = Value({ page.value == Pages.ExtraAura && killAura.value && extraAttack.value }, false, "ExtraCrit")
    private val extraCritDelay = NumberValue({ page.value == Pages.ExtraAura && killAura.value && extraAttack.value && extraCrit.value }, 120.0, 0.0, 1000.0, 2.0, "ExtraCritDelay")
    private val extraCritFactor = NumberValue({ page.value == Pages.ExtraAura && killAura.value && extraAttack.value && extraCrit.value }, 1, 0, 10, 1, "ExtraCritFactor")
    private val extraCritAPS = NumberValue({ page.value == Pages.ExtraAura && killAura.value && extraAttack.value && extraCrit.value }, 8, 0, 20, 1, "ExtraCritAPS")

    private val dropReverted = Value({ page.value == Pages.Drop }, false, "DropReverted")
    private val dropHand = Value({ page.value == Pages.Drop && dropReverted.value }, true, "DropHand")
    private val dropMode = EnumValue({ page.value == Pages.Drop && dropReverted.value && dropHand.value }, DropMode.Sharp5, "DropHand")

    private val dropHotbar = Value({ page.value == Pages.Drop && dropReverted.value }, false, "DropHotbar")
    private val dropHopper = Value({ page.value == Pages.Drop && dropReverted.value }, false, "DropHopper")
    private val dropDelay = NumberValue({ page.value == Pages.Drop && dropReverted.value }, 20.0, 0.0, 500.0, 1.0, "DropDelay")


    private var currentStructure: HopperCalculation.BlockInfo? = null
    private var currentTarget: EntityPlayer? = null

    private val popsMap = hashMapOf<EntityPlayer, Long>()

    private var blockedPlaced = 0
    private var hasOpenedHopperGuiScreen = false

    private var rotationVector = Vec3d.ZERO
    private var waitingForSword = false

    private var isFirstWait = true

    private val placeTimer = Timer()

    private val waitTimer = Timer()
    private val oneTapTimer = Timer()

    private val attackTimer = Timer()
    private val apsTimer = Timer()

    private val critTimer = Timer()
    private val critAPSTimer = Timer()

    private val dropTimer = Timer()
    private val nativeTimer = Timer()

    private var hopperScreen: GuiHopper? = null

    private enum class Pages {
        General, Auto, Looking, Aura, ExtraAura, Drop
    }

    private enum class Timing {
        Sequential, Vanilla
    }

    private enum class Meta {
        Hopper, Dispenser
    }

    private enum class Mode {
        Auto, Looking
    }

    private enum class UpdateMode {
        Vanilla, Sequential, Adaptive
    }

    private enum class DropMode {
        Sharp5, Sharp32, Both
    }

    init {
        addSettings(
            page, timing, meta, mode, rotateMode, swapSword, fastKill, waitDelay, swapDelay, strictDirection,
            targetRange, smartPosition, autoRange,
            lookingRange,
            killAura, attackTiming,
            attackRotate, killAttempt, inhibit, swingArm,
            nativeAttack, nativeDelay,
            attackFactor, attackChance, attackRange, attackDelay,
            minRandomDelay, maxRandomDelay, aimTarget,
            extraAttack, extraAPS, extraCrit, extraCritDelay, extraCritFactor, extraCritAPS,
            dropReverted, dropHand, dropHotbar, dropHopper, dropDelay
        )
    }

    override fun onEnable() {
        if (fullNullCheck()) return

        updateTarget()
        currentStructure = getHopperStructure()
    }

    override fun onDisable() {
        if (fullNullCheck()) return
    }

    override fun onToggle() {
        if (fullNullCheck()) return

        waitingForSword = false
        isFirstWait = true

        waitTimer.reset()
        oneTapTimer.reset()

        currentTarget = null
        rotationVector = Vec3d.ZERO
        blockedPlaced = 0
        hasOpenedHopperGuiScreen = false
        hopperScreen = null

        popsMap.clear()
    }

    @Listener
    private fun onGuiClosed(event: GuiScreenEvent.Closed) {
        if (fullNullCheck()) return

        if (event.screen is GuiHopper) disable()
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (fullNullCheck()) return

        if (event.stage == EventStageable.EventStage.PRE && rotateMode.value != RotateMode.Off && rotationVector != Vec3d.ZERO) {
            addMotion { rotate(RotationUtils.getRotationTo(rotationVector)) }
        }

        if (timing.value != Timing.Sequential) return

        if (waitingForSword && Globals.mc.currentScreen is GuiHopper) {
            hasOpenedHopperGuiScreen = true
            hopperScreen = Globals.mc.currentScreen as GuiHopper
        }

        if (hasOpenedHopperGuiScreen && Globals.mc.currentScreen !is GuiHopper) {
            disable()
            return
        }

        popsMap.values.removeIf { System.currentTimeMillis() - it > 3000 }

        if (event.stage == EventStageable.EventStage.PRE) {
            updateTarget()

            currentStructure = getHopperStructure()
            doSwapSwordFromHopper()
            doThrowingReverted()

            if (attackTiming.value == UpdateMode.Sequential) doKillAura()
        } else if (event.stage == EventStageable.EventStage.POST) {
            doPlaceHopperStructure(currentStructure)
        }
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck() || timing.value != Timing.Vanilla) return

        if (waitingForSword && Globals.mc.currentScreen is GuiHopper) {
            hasOpenedHopperGuiScreen = true
            hopperScreen = Globals.mc.currentScreen as GuiHopper
        }

        if (hasOpenedHopperGuiScreen && Globals.mc.currentScreen !is GuiHopper) {
            disable()
            return
        }

        updateTarget()
        doPlaceHopperStructure( getHopperStructure())

        doSwapSwordFromHopper()
        doThrowingReverted()

        if (attackTiming.value == UpdateMode.Vanilla) doKillAura()
    }

    @Listener
    private fun onGameLoop(event: ClientTickEvent) {
        if (fullNullCheck()) return
        if (attackTiming.value == UpdateMode.Adaptive) doKillAura()
    }

    @Listener
    private fun onTotemPop(event: TotemPopEvent) {
        if (fullNullCheck() || event.entity !is EntityPlayer) return

        popsMap[event.entity] = System.currentTimeMillis()
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (event.packet is SPacketCloseWindow) {
            if (waitingForSword && hopperScreen != null && event.packet.windowId == hopperScreen!!.inventorySlots.windowId) {
                disable()
            }
        }
    }

    private fun updateTarget() {
        currentTarget = EntityUtil.findClosestTarget(targetRange.value)
    }

    private fun getMouseData(): HopperCalculation.MouseData? {
        val mouseObject = Globals.mc.player.rayTrace(lookingRange.value, 1.0F) ?: return null

        val mousePos = mouseObject.blockPos ?: return null
        val mouseHitSide = mouseObject.sideHit ?: return null
        val mouseHitVec = mouseObject.hitVec ?: return null

        return HopperCalculation.MouseData(mouseObject, mousePos, mouseHitSide, mouseHitVec)
    }

    private fun getHopperStructure(): HopperCalculation.BlockInfo? {
        val autoHopperStructure = HopperCalculation.getHopperAutoPlaceStructure()

        return if (mode.value == Mode.Looking) {
            val mouseData = getMouseData() ?: return autoHopperStructure
            if (!mouseData.mousePos.hasNeighbour) return null
            return HopperCalculation.getHopperManualPlaceStructure(mouseData) ?: autoHopperStructure
        } else {
            autoHopperStructure
        }
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (event.packet is CPacketPlayerTryUseItemOnBlock) {

        }
    }

    private fun doPlaceBlock(info: HopperCalculation.BlockInfo) {
        val (origin, structure, slot) = info

        val lastSlot = Globals.mc.player.inventory.currentItem

        val needSwap = LocalHotbarManager.serverSideHotbar != slot
        val needSneak = (rightClickableBlock.contains(structure.pos.block) || structure.pos.needTileSneak) && !Globals.mc.player.isSneaking
        val isSprinting = Globals.mc.player.isSprinting

        if (needSwap) InventoryUtils.swapSlot(slot)
        if (isSprinting) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SPRINTING))
        if (needSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))

        if (rotateMode.value == RotateMode.Speed) RotationUtils.faceVectorWithPositionPacket(structure.hitVec)
        else if (rotateMode.value == RotateMode.Tick) rotationVector = structure.hitVec

        placeBlock(structure, packet = false, packetRotate = false, swingArm = true, noGhost = false)

        if (needSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
        if (isSprinting) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SPRINTING))
        if (needSwap) InventoryUtils.swapSlot(lastSlot)
    }

    private fun doPlaceHopperStructure(hopperInfo: HopperCalculation.BlockInfo?) {
        if (waitingForSword) return

        val shulkerSlot = Globals.mc.player.hotbarSlots.firstByStack { it.item.block is BlockShulkerBox }?.hotbarSlot

        if (hopperInfo == null || shulkerSlot == null) {
            ChatManager.sendMessage("${ChatFormatting.RED}No valid position found or missing items.")
            disable()
            return
        }

        val (hopperPos, hopperStructure, hopperSlot) = hopperInfo
        doPlaceBlock(hopperInfo)

        val shulkerInfo = HopperCalculation.getShulkerManualPlaceStructure(hopperInfo) ?: return
        doPlaceBlock(shulkerInfo)

        if (Globals.mc.player.isSneaking) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
        if (rotateMode.value == RotateMode.Speed) RotationUtils.faceVectorWithPositionPacket(hopperPos.toVec3dCenter())
        else if (rotateMode.value == RotateMode.Tick) rotationVector = hopperPos.toVec3dCenter()

        val visibleSide = hopperPos.getClosestVisibleSideStrict(true) ?: Globals.mc.player.horizontalFacing.opposite
        val hitVecOffset = visibleSide.hitVecOffset

        Globals.mc.player.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(hopperPos, visibleSide, EnumHand.MAIN_HAND, hitVecOffset.x.toFloat(), hitVecOffset.y.toFloat(), hitVecOffset.z.toFloat()))
        Globals.mc.player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))

        waitingForSword = true
        // disable()

        if (blockedPlaced > 1) {
            disable()
        }

    }

    private fun doSwapSwordFromHopper() {
        if (!swapSword.value) return

        if (isFirstWait && !waitTimer.passed(waitDelay.value.toDouble())) return

        WeaponUtils.doSwapSwordFromHopper(!fastKill.value)
        WeaponUtils.doSwitchSwordInventory()
        isFirstWait = false

        waitTimer.reset()
    }


    private fun doKillAura() {
        if (!killAura.value || currentTarget == null || !WeaponUtils.isSuperWeapon(Globals.mc.player.heldItemMainhand)) return

        if (System.currentTimeMillis() - LocalHotbarManager.swapTime < swapDelay.value * 50) {
            return
        }

        var targetCounter = 0

        if (aimTarget.value == 1) {
            currentTarget?.let {
                if (Globals.mc.player.getDistanceSq(it) <= attackRange.value.square) {
                    doNormalAttack(it)
                    if (canBeOneTap(it) && oneTapTimer.passed(getOneTapDelay())) {
                        doSendAttackPacket(it)
                        oneTapTimer.reset()
                    }
                    doSendHandAnimation()
                }
            }
        } else {
            val playerEntities = Globals.mc.world.playerEntities
                .asSequence()
                .filter { !EntityUtil.isntValid(it, attackRange.value) }
                .sortedBy { Globals.mc.player.getDistanceSq(it) }
                .toList()

            for (player in playerEntities) {
                doNormalAttack(player)
                if (canBeOneTap(player) && oneTapTimer.passed(getOneTapDelay())) {
                    doSendAttackPacket(player)
                    oneTapTimer.reset()
                }
                doSendHandAnimation()
                if (targetCounter++ > aimTarget.value) break
            }
        }

    }

    private fun getOneTapDelay(): Double {
        return when (attackTiming.value) {
            UpdateMode.Vanilla -> 35.0
            UpdateMode.Sequential -> 15.0
            UpdateMode.Adaptive -> 100.0
        }
    }

    private fun doNormalAttack(target: EntityPlayer) {
        val randomDelay = (Math.random() * (maxRandomDelay.value - minRandomDelay.value) + minRandomDelay.value)
        val delay = if (minRandomDelay.value == 0F || maxRandomDelay.value == 0F) attackDelay.value else attackDelay.value + randomDelay

        if (!attackTimer.passed(delay)) return

        for (factor in 0 until attackFactor.value) doSendAttackPacket(target)
        if (nativeAttack.value) doNativeAttack(target)

        if (extraAttack.value) {
            if (extraAPS.value > 0 && apsTimer.passedAPS(extraAPS.value)) {
                doSendAttackPacket(target)
                apsTimer.reset()
            }

            if (extraCrit.value) {
                if (critTimer.passed(extraCritDelay.value)) {
                    if (extraCritFactor.value > 0) {
                        for (factor in 0 until extraCritFactor.value) doSendAttackPacket(target)
                    }
                    critTimer.reset()
                }
                if (extraCritAPS.value > 0 && critAPSTimer.passedAPS(extraCritAPS.value)) {
                    doSendAttackPacket(target)
                    critAPSTimer.reset()
                }
            }

        }

        attackTimer.reset()
    }

    private fun doSendAttackPacket(target: EntityPlayer) {
        val usePacket = CPacketUseEntity().apply {
            id = target.entityId
            packetAction = CPacketUseEntity.Action.ATTACK
        }

        if (attackRotate.value) addMotion { rotate(RotationUtils.getRotationToEntityClosestStrict(target)) }

        if (RandomUtils.nextInt(0, 100) < attackChance.value) {
            Globals.mc.connection?.sendPacket(usePacket)
            if (!inhibit.value) Globals.mc.player.attackTargetEntityWithCurrentItem(target)
        }

    }

    private fun doSendHandAnimation() {
        if (swingArm.value) Globals.mc.player.swingArm(EnumHand.MAIN_HAND) else Globals.mc.connection?.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
        Globals.mc.player.resetCooldown()
    }

    private fun doNativeAttack(target: EntityPlayer) {
        thread {
            sleep(nativeDelay.value.toLong())
            doSendAttackPacket(target)
        }
    }

    private fun doThrowingReverted() {
        if (!dropReverted.value) return

        if (dropHand.value) {
            val mainHand = Globals.mc.player.heldItemMainhand
            if (shouldDropWeapon(mainHand) && dropTimer.passed(dropDelay.value)) {
                Globals.mc.player.dropItem(true)
                dropTimer.reset()
            }
        }

        if (dropHotbar.value) {
            for (i in 0 until 9) {
                val hotbarStack = Globals.mc.player.inventory.getStackInSlot(i)
                if (hotbarStack.isEmpty || !shouldDropWeapon(hotbarStack)) continue
                if (dropTimer.passed(dropDelay.value)) {
                    clickSlot(Globals.mc.player.openContainer.windowId, i + 32, 999, ClickType.THROW)
                    dropTimer.reset()
                    break
                }
            }
        }

        if (dropHopper.value) {
            val hopperSlots = Globals.mc.player.openContainer.inventorySlots

            for (i in hopperSlots.indices) {
                val hopperStack = hopperSlots[i].stack
                if (hopperStack.isEmpty || !shouldDropWeapon(hopperStack)) continue

                if (dropTimer.passed(dropDelay.value)) {
                    clickSlot(Globals.mc.player.openContainer.windowId, i + 32, 999, ClickType.THROW)
                    dropTimer.reset()
                    break
                }
            }

        }

    }

    private fun canBeOneTap(target: EntityPlayer): Boolean {
        if (!killAttempt.value) return false

        val timeSincePop = popsMap[target] ?: return false
        return System.currentTimeMillis() - timeSincePop > 455
    }

    private fun shouldDropWeapon(stack: ItemStack?): Boolean {
        if (stack == null || stack.isEmpty || stack.tagCompound == null || stack.enchantmentTagList.tagType == 0) return false

        val tagCompound = stack.tagCompound ?: return false

        if (dropMode.value == DropMode.Sharp32) {
            val attributes = tagCompound.getTag("AttributeModifiers")
            if (attributes is NBTTagList) {
                for (i in 0 until attributes.tagCount()) {
                    val attribute = attributes.getCompoundTagAt(i)
                    if (attribute.getString("AttributeName") != SharedMonsterAttributes.ATTACK_DAMAGE.name) continue
                    if (attribute.getInteger("Amount") >= 1269) return true
                }
            }
        }

        val enchants = tagCompound.getTag("ench") as NBTTagList
        for (i in 0 until enchants.tagCount()) {
            val enchant = enchants.getCompoundTagAt(i)
            if (enchant.getInteger("id") != 16) continue
            val level = enchant.getInteger("lvl")

            if (dropMode.value == DropMode.Sharp5 && level <= 5) return true
            if (dropMode.value == DropMode.Sharp32 && level >= 16) return true
            if (dropMode.value == DropMode.Both && (level <= 5 || level >= 16)) return true
        }

        return false
    }


    private object HopperCalculation {
        data class MouseData(val objectTrace: RayTraceResult, val mousePos: BlockPos, val mouseSide: EnumFacing, val mouseHitVec: Vec3d)
        data class BlockInfo(val hopperPos: BlockPos, val placeInfo: PlaceInfo, val slot: Int)

        fun getShulkerManualPlaceStructure(hopperInfo: BlockInfo): BlockInfo? {
            val (hopperPos, placeInfo, slot) = hopperInfo
            val shulkerSlot = Globals.mc.player.hotbarSlots.firstByStack { it.item.block is BlockShulkerBox }?.hotbarSlot ?: return null

            val eyesPos = Globals.mc.player.eyePosition

            val shulkerPos = placeInfo.placedPos.up()
            val shulkerFace = shulkerPos.getVisibleSidesStrict().firstOrNull { it == EnumFacing.UP } ?: shulkerPos.getClosestVisibleSideStrict() ?: shulkerPos.firstSide?.opposite ?: EnumFacing.UP
            val shulkerHitVec = shulkerPos.getHitVec(shulkerFace)

            val shulkerNeighbour = searchForNeighbour(shulkerPos, 1, lookingRange.value.toFloat(), true) ?:
            PlaceInfo(shulkerPos, shulkerFace, eyesPos.distanceTo(shulkerHitVec), shulkerHitVec, shulkerFace.hitVecOffset, hopperPos)

            return BlockInfo(shulkerPos, shulkerNeighbour, shulkerSlot)
        }

        fun <T : Slot> Iterable<T>.firstBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
            firstByStack { itemStack ->
                itemStack.item.let { it is ItemBlock && it.block == block } && (predicate == null || predicate.test(itemStack))
            }

        fun getHopperManualPlaceStructure(mouseData: MouseData): BlockInfo? {
            val (mouseObject, mousePos, mouseHitSide, mouseHitVec) = mouseData

            val hopperPos = mousePos.offset(mouseHitSide)
            if (!hopperPos.isPlaceable()) return null

            val hopperSlot = Globals.mc.player.hotbarSlots.firstBlock(Blocks.HOPPER)?.hotbarSlot ?: return null
            val eyesPos = Globals.mc.player.eyePosition

            val hopperHitVec = hopperPos.getHitVec(mouseHitSide)

            val hopperNeighbour = searchForNeighbour(hopperPos, 1, lookingRange.value.toFloat(), true) ?:
            PlaceInfo(hopperPos, mouseHitSide, eyesPos.distanceTo(hopperHitVec), mouseHitVec, mouseHitSide.hitVecOffset, mousePos)

            return BlockInfo(hopperPos, hopperNeighbour, hopperSlot)
        }

        fun getHopperAutoPlaceStructure(): BlockInfo? {
            val eyesPos = Globals.mc.player.eyePosition

            val availableBlocks = VectorUtils.getBlockPosInSphere(eyesPos, autoRange.value.toFloat()).sortedBy { Globals.mc.player.getDistanceSq(it) }
            if (availableBlocks.isEmpty()) return null

            val placeTargets = TreeSet<BlockPos>(
                if (smartPosition.value && currentTarget != null) compareByDescending { currentTarget!!.getDistanceSq(it) }
                else compareBy { Globals.mc.player.getDistanceSq(it) }
            )

            for (pos in availableBlocks) {
                if (!pos.isPlaceable() || !pos.hasNeighbour || !pos.up().state.isReplaceable) continue
                // if (!pos.up().isPlaceable() || !pos.up().hasNeighbour) continue

                placeTargets.add(pos)
            }

            val hopperPos = placeTargets.firstOrNull { it.isPlaceable() } ?: return null
            val hopperSlot = Globals.mc.player.hotbarSlots.firstBlock(Blocks.HOPPER)?.hotbarSlot ?: return null

            val hopperFace = hopperPos.closestVisibleSide ?: hopperPos.firstSide?.opposite ?: EnumFacing.UP
            val hopperHitVec = hopperPos.getHitVec(hopperFace)

            // TODO: FIX THIS
            val hopperNeighbour = searchForNeighbour(hopperPos, 1, autoRange.value.toFloat(), false) ?:
            PlaceInfo(hopperPos, hopperFace, eyesPos.distanceTo(hopperHitVec), hopperHitVec, hopperFace.hitVecOffset, hopperPos.offset(hopperFace.opposite))

            return BlockInfo(hopperPos, hopperNeighbour, hopperSlot)
        }
    }

    private object DispenserCalculation {

        fun <T : Slot> Iterable<T>.firstBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
            firstByStack { itemStack ->
                itemStack.item.let { it is ItemBlock && it.block == block } && (predicate == null || predicate.test(itemStack))
            }

        fun getManualPlaceStructure(): List<Pair<PlaceInfo, Int>>? {
            val mouseObject = Globals.mc.player.rayTrace(lookingRange.value, 1.0F) ?: return null

            val mousePos = mouseObject.blockPos ?: return null
            val mouseHitSide = mouseObject.sideHit ?: return null
            val mouseHitVec = mouseObject.hitVec ?: return null

            val obsidianPos = mousePos.offset(mouseHitSide)

            if (!obsidianPos.isPlaceable()) return null

            val hotbarSlots = Globals.mc.player.hotbarSlots

            val obsidianSlot = hotbarSlots.firstBlock(Blocks.OBSIDIAN)?.hotbarSlot ?: return emptyList()
            val hopperSlot = hotbarSlots.firstBlock(Blocks.HOPPER)?.hotbarSlot ?: return emptyList()
            val redstoneBlockSlot = hotbarSlots.firstBlock(Blocks.REDSTONE_BLOCK)?.hotbarSlot ?: return emptyList()
            val shulkerSlot = hotbarSlots.firstByStack { it.item.block is BlockShulkerBox }?.hotbarSlot ?: return emptyList()

            val eyesPos = Globals.mc.player.eyePosition

            val dispenserData = getDispenserData(obsidianPos) ?: return null


            return arrayListOf()
        }

        fun getAutoPlaceStructure(): List<Pair<PlaceInfo, Int>>? {
            val eyesPos = Globals.mc.player.eyePosition

            val availableBlocks = VectorUtils.getBlockPosInSphere(eyesPos, autoRange.value.toFloat()).sortedBy { Globals.mc.player.getDistanceSq(it) }
            if (availableBlocks.isEmpty()) return null

            val placeTargets = TreeSet<BlockPos>(
                if (smartPosition.value && currentTarget != null) compareByDescending { currentTarget!!.getDistanceSq(it) }
                else compareBy { Globals.mc.player.getDistanceSq(it) }
            )

            for (pos in availableBlocks) {
                if (!pos.isPlaceable() || !pos.hasNeighbour || !pos.up().isAir) continue
                // if (!pos.up().isPlaceable() || !pos.up().hasNeighbour) continue

                placeTargets.add(pos)
            }

            val hopperPos = placeTargets.firstOrNull { it.isPlaceable() } ?: return null

            val hotbarSlots = Globals.mc.player.hotbarSlots

            val obsidianSlot = hotbarSlots.firstBlock(Blocks.OBSIDIAN)?.hotbarSlot ?: return emptyList()
            val hopperSlot = hotbarSlots.firstBlock(Blocks.HOPPER)?.hotbarSlot ?: return emptyList()
            val redstoneBlockSlot = hotbarSlots.firstBlock(Blocks.REDSTONE_BLOCK)?.hotbarSlot ?: return emptyList()
            val shulkerSlot = hotbarSlots.firstByStack { it.item.block is BlockShulkerBox }?.hotbarSlot ?: return emptyList()

            return arrayListOf()
        }


        private fun getDispenserData(basePos: BlockPos): DispenserData? {
            if (!basePos.hasNeighbour) return null

            val isBaseObsidian = basePos.block == Blocks.OBSIDIAN
            val baseDispensePosition = getDispensePosition(basePos)
            val canDownBasePlaceHopper = baseDispensePosition.down().hasNeighbour

            val dispenserPos = if (isBaseObsidian && canDownBasePlaceHopper) basePos else basePos.up()

            return DispenserData(dispenserPos, getDispensePosition(dispenserPos))
        }

        private fun getRedstonePosition(dispenserPos: BlockPos): Pair<BlockPos, EnumFacing>? {
            if (!dispenserPos.hasNeighbour) return null
            val dispenserSide = (if (strictDirection.value) dispenserPos.getClosestVisibleSideStrict() else dispenserPos.firstSide?.opposite) ?: EnumFacing.UP
            return dispenserPos.offset(dispenserSide) to dispenserSide
        }

        private fun canPlaceHopper(dispenserData: DispenserData): Boolean {
            val shulkerPosition = dispenserData.dispensePosition
            val hopperPosition = shulkerPosition.down()
            return hopperPosition.hasNeighbour
        }

        /**
         * @see net.minecraft.block.BlockDispenser.getDispensePosition
         */
        fun getDispensePosition(coords: BlockPos): BlockPos {
            val enumFacing = coords.state.getValue(BlockDispenser.FACING)

            val x = coords.x + 0.7 * enumFacing.xOffset.toDouble()
            val y = coords.y + 0.7 * enumFacing.yOffset.toDouble()
            val z = coords.z + 0.7 * enumFacing.zOffset.toDouble()

            return BlockPos(x, y, z)
        }

        private class DispenserData(val dispenserPos: BlockPos, val dispensePosition: BlockPos)
    }


}