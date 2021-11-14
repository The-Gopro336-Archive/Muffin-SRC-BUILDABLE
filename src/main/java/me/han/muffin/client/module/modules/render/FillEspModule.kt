package me.han.muffin.client.module.modules.render

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.NotificationManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.ChatIDs
import me.han.muffin.client.utils.block.HoleUtils
import me.han.muffin.client.utils.color.ColourUtils
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.AxisAlignedBB
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object FillEspModule: Module("FillEsp", Category.RENDER, "Let you know if someone burrowed.") {
    private val verboseType = EnumValue(VerboseType.Chat, "VerboseType")
    private val drawEsp = Value(false, "DrawEsp")
    private val radius = NumberValue(10.0, 1.0, 30.0, 0.1, "Radius")

    private val red = NumberValue(165, 0, 255, 1, "Red")
    private val green = NumberValue(0, 0, 255, 1, "Green")
    private val blue = NumberValue(0, 0, 255, 1, "Blue")
    private val alpha = NumberValue(50, 0, 255, 1, "Alpha")

    val filledPlayer = arrayListOf<EntityPlayer>()

    private enum class VerboseType {
        Off, Chat, Notifications
    }

    init {
        addSettings(verboseType, drawEsp, radius, red, green, blue, alpha)
    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        Globals.mc.world.playerEntities
            .filter { !EntityUtil.isntValid(it, radius.value) && !filledPlayer.contains(it) && HoleUtils.isBurrowed(it) }
            .forEach { player ->
                when (verboseType.value) {
                    VerboseType.Chat -> ChatManager.sendDeleteMessage("${ChatFormatting.RED}${player.name} has filled himself.", player.name, ChatIDs.FILLER_NOTIF)
                    VerboseType.Notifications -> NotificationManager.addNotification(NotificationManager.NotificationType.Warning, "", "${player.name} has no longer filled himself.")
                }
                filledPlayer.add(player)
            }

        filledPlayer.removeIf { player ->
            if (EntityUtil.isntValid(player, radius.value) || !HoleUtils.isBurrowed(player)) {
                when (verboseType.value) {
                    VerboseType.Chat -> ChatManager.sendDeleteMessage("${ChatFormatting.GREEN}${player.name} has no longer filled himself.", player.name, ChatIDs.FILLER_NOTIF)
                    VerboseType.Notifications -> NotificationManager.addNotification(NotificationManager.NotificationType.Info, "", "${player.name} has no longer filled himself.")
                }
                true
            } else {
                false
            }
        }

    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck() || !drawEsp.value) return

        filledPlayer.forEach { player ->
            val vec = MathUtils.getInterpolatedRenderPos(player, event.partialTicks)
            val bb = AxisAlignedBB(0.0, 0.0, 0.0, player.width.toDouble(), player.height.toDouble() / 2, player.width.toDouble()).offset(vec.x - player.width / 2, vec.y, vec.z - player.width / 2)
            RenderUtils.drawBoxESP(bb, ColourUtils.toRGBA(red.value, green.value, blue.value, alpha.value))
        }

    }


}