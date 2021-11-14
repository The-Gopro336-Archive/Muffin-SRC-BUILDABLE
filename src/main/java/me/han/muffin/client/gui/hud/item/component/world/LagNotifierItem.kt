package me.han.muffin.client.gui.hud.item.component.world

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.Muffin
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.gui.hud.item.HudItem
import me.han.muffin.client.utils.timer.Timer
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.text.DecimalFormat

object LagNotifierItem: HudItem("LagNotifier", HudCategory.World,50, 50) {
    private val timer = Timer()
    private var format = "Server has not responded for %ss"

    init {
        width = Muffin.getInstance().fontManager.getStringWidth(displayName) + 3F
        height = Muffin.getInstance().fontManager.stringHeight + 3F
        Muffin.getInstance().eventManager.addEventListener(this)
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return
        timer.reset()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)

        if (timer.passed(1000.0)) {
            val secondsFormat = DecimalFormat("0.0").format(timer.currentTime().toDouble() / 1000)
            val placeholder = String.format(format, "" + ChatFormatting.WHITE + secondsFormat)

            Muffin.getInstance().fontManager.drawStringWithShadow(placeholder, x, y)

            width = Muffin.getInstance().fontManager.getStringWidth(placeholder).toFloat()
            height = Muffin.getInstance().fontManager.stringHeight + 3F
        }
    }

}