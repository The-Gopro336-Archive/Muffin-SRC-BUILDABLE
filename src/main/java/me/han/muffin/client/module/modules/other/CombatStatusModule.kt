package me.han.muffin.client.module.modules.other

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.KeyPressedEvent
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.MouseEvent
import me.han.muffin.client.event.events.entity.living.EntityUseItemFinishEvent
import me.han.muffin.client.event.events.entity.player.PlayerOnStoppedUsingItemEvent
import me.han.muffin.client.event.events.gui.GuiScreenEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.world.block.CanPlaceBlockAtEvent
import me.han.muffin.client.event.events.world.block.DestroyBlockEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.combat.AutoCrystalModule
import me.han.muffin.client.module.modules.hidden.Pull32k
import me.han.muffin.client.module.modules.player.InteractionTweaksModule
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.client.BindUtils
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.extensions.mc.block.blockBlacklist
import me.han.muffin.client.utils.extensions.mc.block.state
import me.han.muffin.client.utils.extensions.mixin.entity.syncCurrentPlayItem
import me.han.muffin.client.utils.extensions.mixin.netty.placedBlockDirection
import me.han.muffin.client.utils.extensions.mixin.netty.posY
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.utils.timer.TickTimer
import me.han.muffin.client.utils.timer.TimeUnit
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.Entity
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketConfirmTransaction
import net.minecraft.network.play.client.CPacketHeldItemChange
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.network.play.server.SPacketBlockChange
import net.minecraft.network.play.server.SPacketConfirmTransaction
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener


internal object CombatStatusModule : Module("CombatMode", Category.OTHERS, "Allow you to select current combat mode.", true, true, false) {
    private val status = EnumValue(Status.Crystal, "Status")

    private val preventShulkerPlace = Value(false, "PreventChestPlace")
    private val disableChestOpening = Value(false, "PreventChestOpen")
    private val onlyOnCA = Value({ disableChestOpening.value },false, "OnlyOnCA")

    private val betterWall = Value(true, "Assistance")
    private val antiFlag = Value(true, "AntiFlag")
    private val antiDesync = EnumValue(AntiDesyncMode.Packet, "AntiDesync")
    private val antiBlockDesync = Value(true, "AntiBlockDesync")
    private val crystalPvpFix = Value(false, "CPvPFix")

    private var counter = 0
    private var delay = 0
    var shouldSwap = false
    var shouldSwapToGap = false
    private var lastSlot = -1

    private var destroy = false
    private var pos = BlockPos.ORIGIN

    private val timer = TickTimer(TimeUnit.TICKS)

    private var clicked = false

    private enum class AntiDesyncMode {
        Off, Normal, Packet, Test
    }

    init {
        addSettings(status, preventShulkerPlace, disableChestOpening, onlyOnCA, antiFlag, antiDesync, antiBlockDesync, betterWall, crystalPvpFix)
    }

    override fun onToggle() {
        destroy = false
        pos = BlockPos.ORIGIN
    }

    @Listener
    private fun onPlayerOnStoppedUsingItem(event: PlayerOnStoppedUsingItemEvent) {
        if (antiDesync.value != AntiDesyncMode.Normal) return
        if (event.player == Globals.mc.player) shouldSwap = true
    }

    @Listener
    private fun onEntityUseItemFinish(event: EntityUseItemFinishEvent) {
        if (antiDesync.value != AntiDesyncMode.Normal) return
        if (event.entity == Globals.mc.player) shouldSwap = true
    }


    @Listener
    private fun onPlacingBlock(event: CanPlaceBlockAtEvent) {
        if (preventShulkerPlace.value && blockBlacklist.contains(event.block)) {
            event.cancel()
        }
    }

   // @Listener
    private fun onContainerOpen(event: GuiScreenEvent.Displayed) {
        //if (disableChestOpening.value && event.screen is GuiContainer && event.screen !is GuiInventory) {
        //    Globals.mc.displayGuiScreen(null)
       // }
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (disableChestOpening.value && (!onlyOnCA.value || AutoCrystalModule.isEnabled) && event.packet is CPacketPlayerTryUseItemOnBlock) {
            if (blockBlacklist.contains(event.packet.pos.block)) {
                event.cancel()
            }
        }

        if (antiDesync.value == AntiDesyncMode.Packet && event.packet is CPacketHeldItemChange) {
            lastSlot = event.packet.slotId
        }

        if (event.packet is CPacketPlayerTryUseItemOnBlock) {
            if (event.packet.pos.y >= Globals.mc.world.height - 1 && event.packet.direction == EnumFacing.UP) {
                event.packet.placedBlockDirection = EnumFacing.DOWN
            }

            if (betterWall.value) {
                if (clicked) {
                    clicked = false
                    return
                }

                if (Globals.mc.currentScreen == null) {
                    val block = event.packet.pos.block

                    if (block.onBlockActivated(Globals.mc.world, event.packet.pos, event.packet.pos.state, Globals.mc.player, event.packet.hand, event.packet.direction, event.packet.facingX, event.packet.facingY, event.packet.facingZ)) {
                        return
                    }

                    val usable = findUsableBlock(event.packet.hand, event.packet.direction, event.packet.facingX, event.packet.facingY, event.packet.facingZ)

                    if (usable != null) {
                        Globals.mc.player.swingArm(event.packet.hand)
                        Globals.mc.player.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(usable, event.packet.direction, event.packet.hand, event.packet.facingX, event.packet.facingY, event.packet.facingZ))
                        clicked = true

                    } else {
                        val usableEntity = findUsableEntity(event.packet.hand)
                        if (usableEntity != null) {
                            Globals.mc.player.connection.sendPacket(CPacketUseEntity(usableEntity, event.packet.hand))
                            clicked = true
                        }
                    }
                }
            }

        }

    }

    private enum class Status {
        Crystal, Pvp32k
    }

    @Listener
    private fun onMouseInput(event: MouseEvent) {
        if (BindUtils.checkIsClickedToggle(InteractionTweaksModule.liquidInteractBind.value)) InteractionTweaksModule.liquidInteract.value = !InteractionTweaksModule.liquidInteract.value
    }

    @Listener
    private fun onKeyPressed(event: KeyPressedEvent) {
        if (BindUtils.checkIsClickedToggle(InteractionTweaksModule.liquidInteractBind.value)) InteractionTweaksModule.liquidInteract.value = !InteractionTweaksModule.liquidInteract.value
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

//        val vectorIn = VectorUtils.getBlockPosInSphere(Globals.mc.player.getPositionEyes(1.0F), 4.0F)
//        for (vector in vectorIn) {
//            if (vector.block == Blocks.PISTON) {
//                ChatManager.sendMessage(vector.state.getValue(BlockDirectional.FACING).toString())
//            }
//        }

        // ChatManager.sendMessage("Factor: ${TestTpsCalculator.tickRate}")

        if (antiDesync.value == AntiDesyncMode.Normal) {
            if (shouldSwap) {
                InventoryUtils.swapSlot(0)
                shouldSwapToGap = true
                shouldSwap = false
            } else if (shouldSwapToGap && ++delay >= 1) {
                val apple = InventoryUtils.findItem(Items.GOLDEN_APPLE)
                if (apple != -1) InventoryUtils.swapSlot(apple)
                delay = 0
                shouldSwapToGap = false
            }
        } else if (antiDesync.value == AntiDesyncMode.Packet) {
            if (lastSlot != -1 && lastSlot != Globals.mc.player.inventory.currentItem) {
                Globals.mc.player.connection.sendPacket(CPacketHeldItemChange(Globals.mc.player.inventory.currentItem))
            }
        } else if (antiDesync.value == AntiDesyncMode.Test) {
            if (timer.tick(4)) {
                Globals.mc.playerController.syncCurrentPlayItem()
            }
        }

        // click gui .live thing
        if (ClickGUI.startCounting) ++ClickGUI.ticksRun

        if (ClickGUI.ticksRun > 20 && ClickGUI.count < 2) {
            ClickGUI.ticksRun = 0
            ClickGUI.count = 0
            ClickGUI.startCounting = false
        }

        if (HudEditorModule.startCounting)
            ++HudEditorModule.ticksRun

        if (HudEditorModule.ticksRun > 20 && HudEditorModule.count < 2) {
            HudEditorModule.ticksRun = 0
            HudEditorModule.count = 0
            HudEditorModule.startCounting = false
        }

        if (status.value == Status.Pvp32k) {
            Pull32k.enable()
        } else {
            Pull32k.disable()
        }

    }

    @Listener
    private fun onBlockDestroy(event: DestroyBlockEvent) {
        if (antiBlockDesync.value) {
            pos = event.pos
            if (destroy) {
                event.isCanceled = false
                destroy = false
                pos = BlockPos.ORIGIN
            } else {
                event.cancel()
            }
        }
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (crystalPvpFix.value && event.packet is SPacketConfirmTransaction) {
            if (!event.packet.wasAccepted() && (if (event.packet.windowId == 0) Globals.mc.player.inventoryContainer else Globals.mc.player.openContainer) != null) {
                Globals.mc.player.connection.sendPacket(CPacketConfirmTransaction(event.packet.windowId, event.packet.actionNumber, true))
                event.cancel()
            }
        }

        if (antiBlockDesync.value && event.packet is SPacketBlockChange && event.packet.blockPosition == pos) {
            destroy = true
        }

        if (antiFlag.value && event.packet is SPacketPlayerPosLook) {
            event.packet.posY -= 255
        }

    }

    private fun findUsableEntity(hand: EnumHand): Entity? {
        var entity: Entity? = null
        var i = 0
        while (i <= Globals.mc.playerController.blockReachDistance) {
            val bb = traceToBlock(i.toDouble(), RenderUtils.renderPartialTicks)
            var maxDist = Globals.mc.playerController.blockReachDistance
            for (e in Globals.mc.world.getEntitiesWithinAABBExcludingEntity(Globals.mc.player, bb)) {
                val currentDist = Globals.mc.player.getDistance(e)
                if (currentDist <= maxDist) {
                    entity = e
                    maxDist = currentDist
                }
            }
            i++
        }
        return entity
    }

    private fun findUsableBlock(hand: EnumHand, dir: EnumFacing, x: Float, y: Float, z: Float): BlockPos? {
        var i = 0
        while (i <= Globals.mc.playerController.blockReachDistance) {
            val bb = traceToBlock(i.toDouble(), Globals.mc.renderPartialTicks)
            val pos = BlockPos(bb.minX, bb.minY, bb.minZ)
            val block = pos.block
            if (block.onBlockActivated(Globals.mc.world, pos, pos.state, Globals.mc.player, hand, dir, x, y, z)) {
                return BlockPos(pos)
            }
            i++
        }
        return null
    }

    private fun traceToBlock(dist: Double, partialTicks: Float): AxisAlignedBB {
        val pos = Globals.mc.player.getPositionEyes(partialTicks)
        val angles = Globals.mc.player.getLook(partialTicks)
        val end = pos.add(angles.x * dist, angles.y * dist, angles.z * dist)
        return AxisAlignedBB(end.x, end.y, end.z, end.x + 1, end.y + 1, end.z + 1)
    }

}