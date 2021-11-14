package me.han.muffin.client.gui.hud.item.component.combat

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.gui.hud.item.HudItem
import me.han.muffin.client.utils.ChatIDs
import me.han.muffin.client.utils.client.ChatUtils
import me.han.muffin.client.utils.extensions.kotlin.square
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemPickaxe
import net.minecraft.network.play.server.SPacketBlockBreakAnim
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object ObsidianWarningItem: HudItem("ObsidianWarning", HudCategory.Combat, 300, 10) {
    private val chatMessage = Value(true, "ChatMessage")
    private val distance = NumberValue(3, 1, 10, 1, "Distance")

    private var warn = false
    private var delay = 0
    private var breakerName: String? = null

    init {
        addSettings(chatMessage, distance)
        width = (Muffin.getInstance().fontManager.getStringWidth(displayName) + 3).toFloat()
        height = (Muffin.getInstance().fontManager.stringHeight + 3).toFloat()
        Muffin.getInstance().eventManager.addEventListener(this)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)

        if (!warn) return
        if (delay++ > 100) warn = false

        val warningMessage = "$breakerName are breaking obsidian around you."
        Muffin.getInstance().fontManager.drawStringWithShadow(warningMessage, x, y)

        if (chatMessage.value && warn) {
            ChatUtils.sendMessage("${ChatFormatting.RED}$breakerName is breaking obby!", ChatIDs.BREAK_OBBY)
        }

    }


    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE || Globals.mc.world == null || Globals.mc.player == null) return

        if (event.packet !is SPacketBlockBreakAnim) return

        val progress = event.packet.progress
        val breakerId = event.packet.breakerId
        val pos = event.packet.position

        val block = pos.block
        val breaker = Globals.mc.world.getEntityByID(breakerId) ?: return

        if (block != Blocks.OBSIDIAN || breaker !is EntityPlayer) return
        if (breaker.heldItemMainhand.isEmpty || breaker.heldItemMainhand.item !is ItemPickaxe) return

        if (pastDistance(Globals.mc.player, pos, distance.value.toDouble())) {
            breakerName = breaker.name
            warn = true
            delay = 0
            if (progress == 255) warn = false
        }

    }

    private fun pastDistance(player: EntityPlayer, pos: BlockPos, dist: Double): Boolean {
        return player.getDistanceSqToCenter(pos) <= dist.square
    }

}