package me.han.muffin.client.module.modules.misc

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.network.PlayerConnectEvent
import me.han.muffin.client.event.events.render.Render2DEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.event.events.world.WorldEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.ChatIDs
import me.han.muffin.client.utils.color.ColourUtils
import me.han.muffin.client.utils.extensions.kotlin.synchronized
import me.han.muffin.client.utils.extensions.mc.utils.component1
import me.han.muffin.client.utils.extensions.mc.utils.component2
import me.han.muffin.client.utils.extensions.mc.utils.component3
import me.han.muffin.client.utils.extensions.mc.utils.toStringFormat
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.render.GlStateUtils
import me.han.muffin.client.utils.render.ProjectionUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*

internal object LogoutSpotModule: Module("LogoutSpot", Category.MISC, "Show a logout position after a player logout around you.") {
    private val renderMode = EnumValue(RenderMode.Coordinates, "RenderMode")
    private val maxDistance = NumberValue(200, 1, 2000, 20, "MaxDistance")
    private val textScale = NumberValue(1.0F, 0.0F, 4.0F, 0.1F, "TextScale")

    private val spots = hashSetOf<LogoutPos>().synchronized()

    private enum class RenderMode {
        Distance, Coordinates
    }

    init {
        addSettings(renderMode, maxDistance, textScale)
    }

    private fun reset() {
        spots.clear()
    }

    override fun onDisable() {
        reset()
    }

    @Listener
    private fun onPlayerJoin(event: PlayerConnectEvent.Join) {
        synchronized(spots) {
            if (spots.removeIf { it.id == event.uuid }) {
                ChatManager.sendDeleteMessage(event.username + " has joined!", event.username, ChatIDs.LOGOUT_SPOT)
            }
        }
    }

    @Listener
    private fun onPlayerLeave(event: PlayerConnectEvent.Leave) {
        val player = Globals.mc.world.getPlayerEntityByUUID(event.uuid) ?: return

        if (Globals.mc.player != player) {
            val coords = player.positionVector.toStringFormat()
            if (spots.add(LogoutPos(event.uuid, player.name, player, coords))) {
                ChatManager.sendDeleteMessage(player.name + " has disconnected at " + coords + "!", player.name, ChatIDs.LOGOUT_SPOT)
            }
        }
    }


    @Listener
    private fun onRender2D(event: Render2DEvent) {
        if (fullNullCheck()) return

        synchronized(spots) {
            spots.forEach {
                GlStateUtils.rescale(Globals.mc.displayWidth.toDouble(), Globals.mc.displayHeight.toDouble())
                GlStateUtils.matrix(true)
                val top = it.topVec
                val upper = ProjectionUtils.toScreenPos(top)
                val distance = Globals.mc.player.positionVector.distanceTo(top)
                val coords = it.coords
                //             String name = String.format("%s (%.1f)", spot.getName(), coords);

                val placeholder = when (renderMode.value) {
                    RenderMode.Distance -> it.name + " [" + distance + "m ]"
                    RenderMode.Coordinates -> it.name + " [" + coords + "]"
                }

                GL11.glTranslated(upper.x, upper.y, 0.0)
                GL11.glScalef(textScale.value * 2.0f, textScale.value * 2.0f, 1.0f)
                Muffin.getInstance().fontManager.drawStringWithShadow(placeholder, -Muffin.getInstance().fontManager.getStringWidth(placeholder) / 2, -(Muffin.getInstance().fontManager.stringHeight + 1))
                GlStateUtils.matrix(false)
                GlStateUtils.rescaleMc()
            }

            if (spots.size > 10) {
                spots.clear()
            }
        }

    }


    @Listener
    private fun onRender3D(event: Render3DEvent) {
        val colour = ColourUtils.getClientColour(255)

        synchronized(spots) {
            spots.forEach {
                val (posX, posY, posZ) = MathUtils.getInterpolatedRenderPos(it.player, event.partialTicks)

                val renderBox = AxisAlignedBB(
                    0.0, 0.0, 0.0,
                    it.player.width.toDouble(), it.player.height.toDouble(), it.player.width.toDouble()
                ).offset(posX - it.player.width / 2, posY, posZ - it.player.width / 2)

                RenderUtils.drawBoxOutlineESP(renderBox, colour, 1.5f)
            }
        }
    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck() || maxDistance.value == 0) return

        synchronized(spots) {
            spots.removeIf {
                Globals.mc.player.positionVector.distanceTo(it.topVec) > maxDistance.value
            }
        }

    }

    @Listener
    private fun onWorldUnload(event: WorldEvent.Unload) {
        reset()
    }

    @Listener
    private fun onWorldLoad(event: WorldEvent.Load) {
        reset()
    }

    private data class LogoutPos(val id: UUID, val name: String, val player: EntityPlayer, val coords: String) {
        val topVec: Vec3d get() {
            val boundingBox = player.entityBoundingBox
            return Vec3d(
                (boundingBox.minX + boundingBox.maxX) / 2.0,
                     boundingBox.maxY,
                (boundingBox.minZ + boundingBox.maxZ) / 2.0
            )
        }

        override fun equals(other: Any?) = this === other || other is LogoutPos && id == other.id
        override fun hashCode() = id.hashCode()
    }

}