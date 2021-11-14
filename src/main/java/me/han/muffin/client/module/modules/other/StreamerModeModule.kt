package me.han.muffin.client.module.modules.other

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.render.TextEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.client.ChatUtils
import me.han.muffin.client.value.Value
import org.apache.commons.lang3.StringUtils
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object StreamerModeModule: Module("StreamerMode", Category.OTHERS, "Change your name and your skins.") {
    private val resetName = Value(false, "ResetName")
    val allPlayersValue = Value(false, "AllPlayers")
    val skinProtectValue = Value(true, "SkinProtect")
    val hideCA = Value(true, "HideCA")
    var fakeName = "Handsome"

    init {
        addSettings(resetName, allPlayersValue, skinProtectValue, hideCA)
    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return
        if (resetName.value) {
            fakeName = "Handsome"
            resetName.value = false
        }
    }

    @Listener
    private fun onText(event: TextEvent) {
        if (fullNullCheck() || event.text.contains(ChatUtils.PREFIX)) return

        event.text = StringUtils.replace(event.text, Globals.mc.player.name, fakeName)

        if (allPlayersValue.value) {
            for (playerInfo in Globals.mc.player.connection.playerInfoMap) {
                event.text = StringUtils.replace(event.text, playerInfo.gameProfile.name, "Protected User")
            }
        }
    }


}