package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.network.LagCompensator
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.roundToInt

object FastBowModule : Module("FastBow", Category.PLAYER, "Releases the bow at a specified server tick.") {
    private val tpsSync = Value(false, "TPSSync")
    private val tickDelay = NumberValue({ !tpsSync.value },3, 0, 20, 1, "TickDelay")
    private val randomDelay = NumberValue({ !tpsSync.value },1, 0, 10, 1, "RandomDelay")

    init {
        addSettings(tpsSync, tickDelay, randomDelay)
    }

    private var randomVariation = 0
    val bowCharge get() = if (isEnabled) 72000.0 - (tickDelay.value.toDouble() + randomDelay.value / 2.0) else null

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck() || Globals.mc.player.isSpectator || SelfShootModule.isEnabled) return

      //  if (Globals.mc.player.inventory.getCurrentItem().item !is ItemBow)
      //      return

        @Suppress("SENSELESS_COMPARISON") // IDE meme
        if (Globals.mc.player.activeHand == null) return

        if (Globals.mc.player.getHeldItem(Globals.mc.player.activeHand).item != Items.BOW) return

        if (!Globals.mc.player.isHandActive) return

        if (Globals.mc.player.itemInUseMaxCount < getBowCharge()) return

        randomVariation = 0
        Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Globals.mc.player.horizontalFacing))
        Globals.mc.player.connection.sendPacket(CPacketPlayerTryUseItem(Globals.mc.player.activeHand))
        Globals.mc.player.stopActiveHand()
    }

    private fun getBowCharge(): Int {
        if (tpsSync.value) return 3 + LagCompensator.syncTicks.roundToInt()
        if (randomVariation == 0) randomVariation = if (randomDelay.value == 0) 0 else (0..randomDelay.value).random()
        return tickDelay.value + randomVariation
    }

}