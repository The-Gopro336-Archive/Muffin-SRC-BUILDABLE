package me.han.muffin.client.module.modules.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.entity.player.PlayerSwingArmEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.render.item.RenderItemInFirstPersonEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.combat.AutoCrystalModule
import me.han.muffin.client.module.modules.other.FovModule
import me.han.muffin.client.utils.extensions.mc.item.block
import me.han.muffin.client.utils.extensions.mixin.netty.packetHand
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.Value
import net.minecraft.init.Blocks
import net.minecraft.item.ItemAppleGold
import net.minecraft.item.ItemEndCrystal
import net.minecraft.item.ItemPickaxe
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.util.EnumHand
import net.minecraft.util.EnumHandSide
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object NoHandAnimationModule: Module("NoHandAnimation", Category.RENDER, "Hand will not shake when doing certain things.", true) {
    private val animationAll = Value(false, "All")

    private val clientSwingMode = EnumValue(SwingMode.Off, "ClientSwing")
    private val serverSwingMode = EnumValue(ServerSwingMode.Off, "ServerSwing")

    private enum class SwingMode {
        Off, Cancel, FullCancel, MainHand, OffHand
    }

    private enum class ServerSwingMode {
        Off, Cancel, Offhand, Shuffle
    }

    private var lastServerArm = EnumHandSide.RIGHT
    init {
        addSettings(animationAll, clientSwingMode, serverSwingMode)
    }

    @Listener
    private fun onPacketSend(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (event.packet is CPacketAnimation) {
            when (serverSwingMode.value) {
                ServerSwingMode.Cancel -> event.cancel()
                ServerSwingMode.Offhand -> {
                    event.packet.packetHand = EnumHand.OFF_HAND
                //    Globals.mc.player.swingingHand = EnumHand.OFF_HAND
                }
                ServerSwingMode.Shuffle -> {
                    val oppositeSide = lastServerArm.opposite()

                    event.packet.packetHand = getEnumHand(oppositeSide)
                    Globals.mc.player.primaryHand = oppositeSide
                    lastServerArm = oppositeSide
                }
            }
        }

    }

    private fun getEnumHand(handSide: EnumHandSide): EnumHand {
        return when (handSide) {
            EnumHandSide.LEFT -> EnumHand.OFF_HAND
            EnumHandSide.RIGHT -> EnumHand.MAIN_HAND
        }
    }

    @Listener
    private fun onPlayerSwingArm(event: PlayerSwingArmEvent) {
        if (animationAll.value || isMainHandItemsValid() || isOffHandItemsValid()) {
            when (clientSwingMode.value) {
                SwingMode.Cancel -> {
                    event.cancel()
                    Globals.mc.player.connection.sendPacket(CPacketAnimation(event.hand))
                }
                SwingMode.FullCancel -> event.cancel()
                SwingMode.MainHand -> event.hand = EnumHand.MAIN_HAND
                SwingMode.OffHand -> event.hand = EnumHand.OFF_HAND
            }
        }
    }

    private fun isMainHandItemsValid(): Boolean {
        val mainHandItem = Globals.mc.player.heldItemMainhand.item
        return (mainHandItem.block == Blocks.OBSIDIAN || mainHandItem is ItemPickaxe || mainHandItem is ItemAppleGold ||
                (AutoCrystalModule.isEnabled && mainHandItem is ItemEndCrystal))
    }

    private fun isOffHandItemsValid(): Boolean {
        val offHandItem = Globals.mc.player.heldItemOffhand.item
        return ((AutoCrystalModule.isEnabled && offHandItem is ItemEndCrystal) || offHandItem is ItemAppleGold)
    }

    @Listener
    private fun onRenderItemInFirstPersonMainHand(event: RenderItemInFirstPersonEvent.MainHand) {
        if (fullNullCheck()) return

        if (animationAll.value) {
            event.cancel()
            return
        }

        if (isMainHandItemsValid()) {
            if (FovModule.isEnabled && (FovModule.handHeight.value == FovModule.HandMode.Main || FovModule.handHeight.value == FovModule.HandMode.Both)) {
                event.height = FovModule.mainHeight.value
            } else {
                event.height = 0F
            }
            event.cancel()

            /*
        val a = 1.0f - ((Globals.mc.itemRenderer as IItemRenderer).prevEquippedProgressMainHand + ((Globals.mc.itemRenderer as IItemRenderer).equippedProgressMainHand - (Globals.mc.itemRenderer as IItemRenderer).prevEquippedProgressMainHand) * event.partialTicks)

        if (event.height < 0) {
            event.height = a
        }
        if (event.height > 0)
            event.height = 0f
         */

        }


    }

    @Listener
    private fun onRenderItemInFirstPersonOffHand(event: RenderItemInFirstPersonEvent.OffHand) {
        if (fullNullCheck()) return

        if (animationAll.value) {
            event.cancel()
            return
        }

        if (isOffHandItemsValid()) {
            if (FovModule.isEnabled && (FovModule.handHeight.value == FovModule.HandMode.Offhand || FovModule.handHeight.value == FovModule.HandMode.Both)) {
                event.height = FovModule.offHeight.value
            } else {
                event.height = 0F
            }
            event.cancel()
            /*
                        val a = 1.0f - ((Globals.mc.itemRenderer as IItemRenderer).prevEquippedProgressOffHand + ((Globals.mc.itemRenderer as IItemRenderer).equippedProgressOffHand - (Globals.mc.itemRenderer as IItemRenderer).prevEquippedProgressOffHand) * event.partialTicks)
                        if (event.height < 0) {
                            event.height = a
                        }
                        if (event.height > 0)
                            event.height = 0f
             */

        }

    }

}