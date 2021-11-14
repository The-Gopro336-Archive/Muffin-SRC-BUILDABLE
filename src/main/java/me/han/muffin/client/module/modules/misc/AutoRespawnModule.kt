package me.han.muffin.client.module.modules.misc

import me.han.muffin.client.command.commands.LiveCommand
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.gui.GuiScreenEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.ChatIDs
import me.han.muffin.client.utils.client.ChatUtils
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.value.Value
import net.minecraft.client.gui.GuiGameOver
import net.minecraft.network.play.client.CPacketChatMessage
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.round

internal object AutoRespawnModule: Module("AutoRespawn", Category.MISC, true, "Automatically respawn after dying.") {
    private val logCoords = Value(true, "LogCoord")
    private val maBaoGuo = Value(false, "MaBaoGuo")

    private var tickRun = 0
    private var containsSuicide = false

    init {
        addSettings(logCoords, maBaoGuo)
    }

    @Listener
    private fun onGuiScreenDisplayed(event: GuiScreenEvent.Displayed) {
        if (event.screen !is GuiGameOver) return

        Globals.mc.player.respawnPlayer()
        Globals.mc.displayGuiScreen(null)

        if (maBaoGuo.value) if (!containsSuicide) Globals.mc.player.connection.sendPacket(CPacketChatMessage("年轻人不讲武德, 来骗, 来偷袭, 我劝这位年轻人耗子尾汁."))
    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (!logCoords.value || LiveCommand.isLive) return

        if (!Globals.mc.player.isAlive) {
            ++tickRun

            if (tickRun == 1)
                ChatUtils.sendMessage(round(Globals.mc.player.posX).toString() + " " + round(Globals.mc.player.posY) + " " + round(Globals.mc.player.posZ), ChatIDs.RESPAWN_COORDS)
        }

        if (Globals.mc.player.health > 5.0F) tickRun = 0

    }

}