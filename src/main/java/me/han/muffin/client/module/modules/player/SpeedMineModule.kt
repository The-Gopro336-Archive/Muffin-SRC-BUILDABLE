package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.entity.PlayerSyncCurrentItemEvent
import me.han.muffin.client.event.events.entity.SyncCurrentPlayItemEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.event.events.world.block.ClickBlockEvent
import me.han.muffin.client.event.events.world.block.DamageBlockEvent
import me.han.muffin.client.manager.managers.LocalHotbarManager
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.extensions.mc.block.*
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.item.armorSlots
import me.han.muffin.client.utils.extensions.mc.world.getClosestVisibleSideStrict
import me.han.muffin.client.utils.extensions.mixin.entity.blockHitDelay
import me.han.muffin.client.utils.extensions.mixin.entity.curBlockDamageMP
import me.han.muffin.client.utils.extensions.mixin.entity.syncCurrentPlayItem
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.network.LagCompensator
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.init.Enchantments
import net.minecraft.init.MobEffects
import net.minecraft.inventory.ClickType
import net.minecraft.item.ItemAir
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.awt.Color
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

internal object SpeedMineModule: Module("SpeedMine", Category.PLAYER, "Allows you to break blocks faster.") {
    val tweaks = EnumValue(Tweaks.None, "Tweaks")

    private val instantAfter = Value({ tweaks.value == Tweaks.Packet }, true, "InstantAfter")
    private val remainMining = Value({ tweaks.value == Tweaks.Packet }, true, "RemainMining")
    private val switchRotate = Value({ tweaks.value == Tweaks.Packet && instantAfter.value}, false, "SwitchRotate")
    private val strictDirectionSwitch = Value({ tweaks.value == Tweaks.Packet && instantAfter.value && switchRotate.value }, true, "StrictDirection")

    val packetInstantMode = EnumValue({ tweaks.value == Tweaks.PacketInstant }, PacketInstantMode.Old, "PacketInstantMode")

    private val spamDelay = NumberValue({ tweaks.value == Tweaks.Sequence },0, -1, 20, 1, "SpamDelay")
    private val breakDelay = NumberValue({ tweaks.value == Tweaks.Sequence },0, 0, 20, 1, "BreakDelay")

    private val autoSwitch = Value({ tweaks.value != Tweaks.None && tweaks.value != Tweaks.Damage }, true, "AutoSwitch")
    private val silentBreak = Value({ tweaks.value != Tweaks.None && tweaks.value != Tweaks.Damage && !autoSwitch.value }, false, "SilentBreak")

    private val swingMode = EnumValue({ tweaks.value == Tweaks.Packet || tweaks.value == Tweaks.PacketInstant }, SwingMode.None, "Swing")
    private val rotate = Value({ tweaks.value == Tweaks.Packet || tweaks.value == Tweaks.PacketInstant }, false, "Rotate")
    private val packetRange = NumberValue({ tweaks.value == Tweaks.Packet || tweaks.value == Tweaks.PacketInstant }, 5.0, 1.0, 6.0, 0.1, "Range")
    private val render = Value({ tweaks.value == Tweaks.Packet || tweaks.value == Tweaks.PacketInstant }, true, "Render")

    private val autoTool = Value(false, "AutoTool")
    private val noDelay = Value(false, "NoDelay")
    private val doubleBreak = Value(false, "DoubleBreak")

    private val speed = NumberValue(0.1, 0.0, 1.0, 0.1, "Speed")
    private val startDelay = NumberValue(0.0, 0.0, 1.0, 0.1, "StartDelay")

    private val delaySyncTimer = Timer()
    private val startDelayTimer = Timer()

    private val waitTimer = Timer()
    private val currentPos = BlockPos.MutableBlockPos(0, -69, 0)

    private var currentBlockState: IBlockState? = null
    private var currentFacing: EnumFacing? = null
    private var hasBlock = true
    private var shouldGreen = false

    private var bestToolSlot = -1
    private var shouldStrictRotate = false

    private var spamTicks = 0
    private var breakTicks = 0

    enum class Tweaks {
        None, Packet, PacketInstant, Sequence, Damage, Instant
    }

    private enum class SwingMode {
        None, Packet, Render
    }

    enum class PacketInstantMode {
        Old, New
    }

    private val isPacketMode get() = tweaks.value == Tweaks.Packet || tweaks.value == Tweaks.PacketInstant

    init {
        addSettings(
            tweaks,
            instantAfter, remainMining, switchRotate, strictDirectionSwitch,
            packetInstantMode, spamDelay, breakDelay,
            autoSwitch, silentBreak,
            swingMode, rotate, packetRange,
            render, autoTool,
            noDelay, doubleBreak,
            speed, startDelay
        )
    }

    override fun getHudInfo(): String = speed.value.toString()

    override fun onDisable() {
        breakTicks = 0
    }

    @Listener
    private fun onSyncSwitchPacket(event: SyncCurrentPlayItemEvent) {
        if (isPacketMode && switchRotate.value && shouldGreen && bestToolSlot in 0..8 && event.slot == bestToolSlot && currentPos.isNotNull && currentFacing != null) {
            val eyesPos = Globals.mc.player.eyePosition
            val facing = if (strictDirectionSwitch.value) (currentPos.getClosestVisibleSideStrict() ?: currentFacing ?: EnumFacing.UP) else currentFacing ?: EnumFacing.UP
            val hitVec = currentPos.getHitVec(facing)
            if (eyesPos.distanceTo(hitVec) <= Globals.mc.playerController.blockReachDistance) {
                // addMotion { rotate(rotationTo) }
                // Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, currentPos, facing))
                shouldStrictRotate = true
            }
        }
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (fullNullCheck()) return

        if (tweaks.value == Tweaks.Sequence || noDelay.value) Globals.mc.playerController.blockHitDelay = 0

        if (isPacketMode || tweaks.value == Tweaks.Sequence) {

            if (switchRotate.value && shouldStrictRotate && event.stage == EventStageable.EventStage.PRE) {
                val facing = if (strictDirectionSwitch.value) (currentPos.getClosestVisibleSideStrict() ?: currentFacing ?: EnumFacing.UP) else currentFacing ?: EnumFacing.UP
                val rotationTo = RotationUtils.getRotationTo(currentPos.getHitVec(facing))
                addMotion { rotate(rotationTo) }
                if (tweaks.value == Tweaks.PacketInstant) {
                    Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, currentPos, facing))
                }
                shouldStrictRotate = false
            }

            if (currentPos.isNotNull && currentFacing != null) {
                val bestTool = findBestTool(currentPos.state, false)

                if (waitTimer.passed(getMineTime2(currentPos, bestTool).times(1000.0) * LagCompensator.factor)) shouldGreen = true

                bestToolSlot = bestTool.second

                if (tweaks.value == Tweaks.Packet) {
                    if (!instantAfter.value) {
                        if (currentPos.distanceToCenter > packetRange.value) {
                            shouldGreen = false
                            currentPos.setNull()
                            currentFacing = null
                            return
                        }
                        if (currentPos.state != currentBlockState || currentPos.isAir) {
                            shouldGreen = false
                            hasBlock = false
                            currentPos.setNull()
                            currentBlockState = null
                        }
                    } else {
                        if (currentPos.isAir) {
                            shouldGreen = false
                            hasBlock = false
                        }
                        if (currentPos.distanceToCenter > packetRange.value) {
                            shouldGreen = false
                            currentPos.setNull()
                            currentFacing = null
                        }
                    }
                } else  {
                    if (currentPos.isAir) {
                        shouldGreen = false
                        hasBlock = false
                    }
                    if (currentPos.distanceToCenter > 8) {
                        shouldGreen = false
                        currentPos.setNull()
                        currentFacing = null
                    }
                }

                if (currentPos.isNull) return


                when (event.stage) {
                    EventStageable.EventStage.PRE -> {
                        if (rotate.value) {
                            addMotion { rotate(RotationUtils.getRotationTo(currentPos.toVec3dCenter())) }
                        }

                        if (tweaks.value == Tweaks.Sequence && spamDelay.value != -1 && currentFacing != null && spamTicks++ == spamDelay.value) {
                            doBreakSequence(currentPos, currentFacing!!)
                            // println("spamming")
                            spamTicks = 0
                        }

                    }
                    EventStageable.EventStage.POST -> {
                        when (swingMode.value) {
                            SwingMode.Packet -> Globals.mc.player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
                            SwingMode.Render -> Globals.mc.player.swingArm(EnumHand.MAIN_HAND)
                        }
                    }
                }
            } else {
                hasBlock = true
            }
        }
    }

    private fun doBreakSequence(pos: BlockPos, facing: EnumFacing) {
        val currentSlot = LocalHotbarManager.serverSideHotbar

        if (autoSwitch.value || currentSlot == bestToolSlot) {
            if (currentSlot != bestToolSlot && bestToolSlot in 0..8) doAutoTool(pos)

            Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, facing))
            Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, facing))

            if (silentBreak.value) InventoryUtils.swapSlot(currentSlot)
        }

    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return
        if (!Globals.mc.gameSettings.keyBindAttack.isKeyDown) startDelayTimer.reset()
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (!render.value || fullNullCheck()) return

        val playerView = Globals.mc.renderViewEntity ?: Globals.mc.player ?: return
        val interpView = MathUtils.interpolateEntity(playerView, RenderUtils.renderPartialTicks)

        if (currentPos.isNull || !hasBlock) return

        val bb = currentPos.state.getSelectedBoundingBox(Globals.mc.world, currentPos)
            .grow(RenderUtils.BBGrow)
            .offset(-interpView.x, -interpView.y, -interpView.z)

        val colour = Color(if (shouldGreen) 0 else 200, if (shouldGreen) 200 else 0, 0, 30)
        RenderUtils.drawBoxFullESP(bb, colour.red, colour.green, colour.blue, colour.alpha, 0.5F)
    }

    @Listener
    private fun onPlayerSyncItem(event: PlayerSyncCurrentItemEvent) {
        if (!delaySyncTimer.passed(100.0)) return

        val iBlockState = event.pos.state

        if (speed.value > 0.0F && iBlockState.material != Material.AIR) {
            event.curBlockDamage += iBlockState.getPlayerRelativeBlockHardness(Globals.mc.player, Globals.mc.world, event.pos) * speed.value.toFloat()
            if (startDelayTimer.passed(startDelay.value * 1000.0f)) event.blockHitDelay = 0
        }

        if (autoTool.value) doAutoTool(event.pos)
    }

    @Listener
    private fun onDamageBlock(event: DamageBlockEvent) {
        if (tweaks.value == Tweaks.None) return

        val blockHardness = event.pos.blockHardness

        if (tweaks.value == Tweaks.Damage) {
            if (blockHardness != -1F) {
                if (Globals.mc.playerController.curBlockDamageMP >= 0.7F) Globals.mc.playerController.curBlockDamageMP = 1.0F
            }
            if (doubleBreak.value) doDoubleBreak(event.pos.up(), event.direction)
            return
        }

        if (tweaks.value == Tweaks.PacketInstant && packetInstantMode.value == PacketInstantMode.New) {
            if (!event.pos.isAir && blockHardness != -1F) {

                Globals.mc.playerController.curBlockDamageMP = 0.9F
                Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, event.pos, event.direction))

                shouldGreen = false
                hasBlock = true
                currentPos.setPos(event.pos)
                currentFacing = event.direction
                waitTimer.reset()
            }
        }

        if (blockHardness != -1F && (tweaks.value == Tweaks.Packet || tweaks.value == Tweaks.Instant || tweaks.value == Tweaks.Sequence)) {
            val bestTool = findBestTool(event.pos.state, false)
            val bestSlot = bestTool.second

            val lastSlot = Globals.mc.player.inventory.currentItem
            if (bestSlot != -1 && !autoTool.value && (autoSwitch.value || silentBreak.value)) InventoryUtils.swapSlot(bestSlot)

            when (tweaks.value) {
                Tweaks.Packet -> {
                    shouldGreen = false
                    hasBlock = true
                    currentPos.setPos(event.pos)
                    currentFacing = event.direction
                    currentBlockState = currentPos.state
                    waitTimer.reset()

                    Globals.mc.player.swingArm(EnumHand.MAIN_HAND)
                    if (!remainMining.value) Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, event.pos, event.direction))
                    Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, event.pos, event.direction))
                    if (!remainMining.value) event.cancel()
                }

                Tweaks.Instant -> {
                    Globals.mc.player.swingArm(EnumHand.MAIN_HAND)
                    Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, event.pos, event.direction))
                    Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, event.pos, event.direction))
                    Globals.mc.playerController.onPlayerDestroyBlock(event.pos)
                    Globals.mc.world.setBlockToAir(event.pos)
                }
                Tweaks.Sequence -> {
                    if (currentPos.isNull || currentPos != event.pos) {
                        Globals.mc.player.swingArm(EnumHand.MAIN_HAND)
                        Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, event.pos, event.direction))
                        shouldGreen = false
                        hasBlock = true
                        currentPos.setPos(event.pos)
                        currentFacing = event.direction
                        currentBlockState = currentPos.state
                        waitTimer.reset()
                    }

                    if (breakDelay.value <= breakTicks++ && currentFacing != null) {
                        doBreakSequence(currentPos, currentFacing!!)
                        breakTicks = 0
                    }
                }
            }

            if (!autoSwitch.value && silentBreak.value) InventoryUtils.swapSlot(lastSlot)
        }

        if (doubleBreak.value) doDoubleBreak(event.pos.up(), event.direction)
    }

    @Listener
    private fun onClickBlock(event: ClickBlockEvent) {
        if (!event.pos.isAir && event.pos.blockHardness != -1F && tweaks.value == Tweaks.Packet && instantAfter.value) {
            Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, event.pos, event.facing))
        }
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (event.packet is CPacketPlayerDigging && tweaks.value == Tweaks.PacketInstant) {
            when (event.packet.action) {
                CPacketPlayerDigging.Action.START_DESTROY_BLOCK -> {
                    if (packetInstantMode.value == PacketInstantMode.Old) {
                        if (!event.packet.position.isAir && event.packet.position.blockHardness != -1F) {
                            Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, event.packet.position, event.packet.facing))
            //                Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, event.packet.position, event.packet.facing))
                            shouldGreen = false
                            hasBlock = true
                            currentPos.setPos(event.packet.position)
                            currentFacing = event.packet.facing
                            waitTimer.reset()
                        }
                    }
                }
                CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK -> {
                }
                CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK -> {
                    if (event.packet.position != currentPos) {
                        shouldGreen = false
                        currentPos.setNull()
                        currentFacing = null
                    }
                    event.cancel()
                }
            }
        }

        if (event.packet is CPacketPlayerTryUseItemOnBlock && (tweaks.value == Tweaks.PacketInstant || (tweaks.value == Tweaks.Packet && instantAfter.value))) {
            val offsetPos = event.packet.pos.offset(event.packet.direction)
            if (currentPos.isNotNull && offsetPos == currentPos) {
                hasBlock = true
                shouldGreen = true
            }
        }
    }

    private fun doAutoTool(pos: BlockPos) {
        val bestTool = findBestTool(pos.state, true)
        val bestSlot = bestTool.second

        if (pos.blockHardness == -1.0F || bestSlot == -1) return

        if (bestSlot < 9) {
            Globals.mc.player.inventory.currentItem = bestSlot
            Globals.mc.playerController.syncCurrentPlayItem()
            return
        }

        Globals.mc.playerController.windowClick(0, bestSlot, Globals.mc.player.inventory.currentItem, ClickType.SWAP, Globals.mc.player)
    }

    private fun doDoubleBreak(abovePos: BlockPos, facing: EnumFacing) {
        if (abovePos.blockHardness == -1F || Globals.mc.player.getDistance(abovePos.x.toDouble(), abovePos.y.toDouble(), abovePos.z.toDouble()) > 5F) return

        Globals.mc.player.swingArm(EnumHand.MAIN_HAND)
        Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, abovePos, facing))
        Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, abovePos, facing))
        Globals.mc.playerController.onPlayerDestroyBlock(abovePos)
        Globals.mc.world.setBlockToAir(abovePos)
    }

    private fun getMineTime2(pos: BlockPos, bestTool: Pair<ItemStack, Int>): Double {
        val state = pos.state

        var bestStack = bestTool.first
        if (bestStack == ItemStack.EMPTY) bestStack = Globals.mc.player.heldItemMainhand

        val bestSlot = bestTool.second

        var efficiency: Int
        var speedMultiplier = bestStack.getDestroySpeed(state)

        speedMultiplier +=
            (if (EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, bestStack).also { efficiency = it } > 0.0) efficiency.toDouble().pow(2.0) + 1 else 0.0).toFloat()

        Globals.mc.player.getActivePotionEffect(MobEffects.HASTE)?.let {
            // val hasteAmplifier = max(it.amplifier + 1, 0)
            speedMultiplier *= (1.0 + it.amplifier.times(0.2)).toFloat()
        }

        Globals.mc.player.getActivePotionEffect(MobEffects.MINING_FATIGUE)?.let {
            speedMultiplier *= when (it.amplifier) {
                0 -> 0.3F
                1 -> 0.09F
                2 -> 0.0027F
                else -> 0.00081F
            }
        }

        if (Globals.mc.player.isInWater && EnchantmentHelper.getEnchantmentLevel(Enchantments.AQUA_AFFINITY, Globals.mc.player.armorSlots[0].stack) == 0) speedMultiplier /= 5.0F
        if (!Globals.mc.player.onGround) speedMultiplier /= 5.0F

        val ticks = speedMultiplier / state.getBlockHardness(Globals.mc.world, pos) / if (bestStack.canHarvestBlock(state)) 30 else 100
        if (ticks > 1) return 0.0

        return (1.0 / ticks).roundToInt() / 20.0

    }

    private fun getMineTime(pos: BlockPos): Double {
        val state = pos.state
        var stack = Globals.mc.player.heldItemMainhand
        var toolSpeed = stack.getDestroySpeed(state)
        var eff: Int

        var hasteAmplifier = 0
        Globals.mc.player.getActivePotionEffect(MobEffects.HASTE)?.let {
            hasteAmplifier = max(it.amplifier + 1, 0)
        }

        val bestTool = findBestTool(state, autoTool.value)
        val bestStack = bestTool.first
        val bestSlot = bestTool.second
        if (bestSlot != -1) {
            stack = Globals.mc.player.inventory.getStackInSlot(bestSlot)
            toolSpeed = stack.getDestroySpeed(state)
            toolSpeed += (if (EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack).also { eff = it } > 0.0) eff.toDouble().pow(2.0) + 1 else 0.0).toFloat()
        }

        toolSpeed *= (1.0 + (hasteAmplifier * 0.2)).toFloat()
        if (!Globals.mc.player.onGround) toolSpeed /= 5

        val ticks = toolSpeed / state.getBlockHardness(Globals.mc.world, pos) / if (bestStack.canHarvestBlock(state)) 30 else 100
        if (ticks > 1) return 0.0

        return (1.0 / ticks).roundToInt() / 20.0
    }

    private fun findBestTool(state: IBlockState, findInventory: Boolean): Pair<ItemStack, Int> {
        var bestSlot = -1
        var bestStack = ItemStack.EMPTY
        var max = 0.0
        if (state.material == Material.AIR) return ItemStack.EMPTY to -1

        for (i in 0 until if (findInventory) 36 else 9) {
            val stack = Globals.mc.player.inventory.getStackInSlot(i)
            if (stack.item is ItemAir || stack.isEmpty) continue

            var speed = stack.getDestroySpeed(state)
            var eff: Int

            if (speed > 1) {
                speed += (if (EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack).also { eff = it } > 0.0) eff.toDouble().pow(2.0) + 1 else 0.0).toFloat()
                if (speed > max) {
                    max = speed.toDouble()
                    bestSlot = i
                    bestStack = stack
                }
            }
        }

        return bestStack to bestSlot
    }


}