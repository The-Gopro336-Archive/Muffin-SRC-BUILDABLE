package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.extensions.mc.block.rightClickableBlock
import me.han.muffin.client.utils.extensions.mixin.misc.rightClickDelayTimer
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.init.Items
import net.minecraft.item.*
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumHand
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object FastPlaceModule: Module("FastPlace", Category.PLAYER, "Removes the right clicking delay so you can place blocks faster.") {

    private val blocks = Value(true, "Blocks")
    private val allItems = Value(false, "AllItems")
    private val expBottles = Value({ !allItems.value },true, "ExpBottles")
    private val endCrystals = Value({ !allItems.value },true, "EndCrystals")
    private val fireworks = Value({ !allItems.value },false, "Fireworks")
    private val delay = NumberValue(0, 0, 20, 1, "Delay")
    private val ghostFix = Value(true, "GhostFix")

    private var lastUsedHand = EnumHand.MAIN_HAND
    private var tickCount = 0

    private val items = arrayListOf<Class<*>>(ItemEnderPearl::class.java)

    init {
        addSettings(blocks, allItems, expBottles, endCrystals, fireworks, delay, ghostFix)
    }

    override fun onDisable() {
        Globals.mc.rightClickDelayTimer = 4
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck() || Globals.mc.player.isSpectator) return

        if (delay.value > 0) {
            if (tickCount <= 0) {
                tickCount = delay.value
            } else {
                tickCount--
                return
            }
        }

        if (passItemCheck(Globals.mc.player.getHeldItem(lastUsedHand).item)) Globals.mc.rightClickDelayTimer = 0
    }

    private fun checkCrystalHand() =
        Globals.mc.player.heldItemOffhand.item !is ItemEndCrystal &&
                (Globals.mc.player.heldItemMainhand.item !is ItemEndCrystal || Globals.mc.player.heldItemMainhand.item is ItemExpBottle)

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (fullNullCheck()) return

        when (event.stage) {
            EventStageable.EventStage.PRE -> {
                when (event.packet) {
                    is CPacketPlayerTryUseItemOnBlock -> {
                        if (ghostFix.value) {
                            val currentHand = Globals.mc.player.getHeldItem(event.packet.hand)
                            val currentItem = currentHand.item

                            if (checkCrystalHand() && !rightClickableBlock.contains(event.packet.pos.block) &&
                                currentItem != Items.ITEM_FRAME && currentItem != Items.SKULL && currentItem != Items.SPAWN_EGG && currentItem != Items.FIREWORKS &&
                                currentItem !is ItemBlock) {
                                    event.cancel()
                            }
                        }
                    }
                }
            }
            EventStageable.EventStage.POST -> {
                when (event.packet) {
                    is CPacketPlayerTryUseItem -> lastUsedHand = event.packet.hand
                    is CPacketPlayerTryUseItemOnBlock -> lastUsedHand = event.packet.hand
                }
            }
        }
    }

    private fun passItemCheck(item: Item): Boolean {
        return item !is ItemAir
            && (allItems.value && item !is ItemBlock
            || blocks.value && item is ItemBlock
            || expBottles.value && item is ItemExpBottle
            || endCrystals.value && item is ItemEndCrystal
            || fireworks.value && item is ItemFirework)
    }

}