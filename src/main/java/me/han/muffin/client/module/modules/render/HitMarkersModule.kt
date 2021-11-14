package me.han.muffin.client.module.modules.render

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.render.Render2DEvent
import me.han.muffin.client.manager.managers.FriendManager
import me.han.muffin.client.manager.managers.TextureManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.render.Dimension
import me.han.muffin.client.utils.render.GlStateUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.Entity
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundEvent
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object HitMarkersModule : Module("HitMarkers", Category.RENDER, "Render a marker after you hit enemy.") {
    private val thirdPerson = Value(false, "ThirdPerson")
    private val playSounds = EnumValue(SoundType.COD, "Sound")
    private val debug = Value(false, "DebugInfo")
    private val clientColor = Value(false, "ClientColor")
    private val friend = Value(true, "Friend")

    private var attackEntity: Entity? = null
    var hitCoolDown = 0
    var hasHit = false

    private val codHitMarker = ResourceLocation(Muffin.MODID, "cod_hitmarker")
    private val csgoHitMarker = ResourceLocation(Muffin.MODID, "csgo_hitmarker")

    private val COD_EVENT = SoundEvent(codHitMarker).apply {
        registryName = codHitMarker
    }

    private val CSGO_EVENT = SoundEvent(csgoHitMarker).apply {
        registryName = csgoHitMarker
    }

    private enum class SoundType {
        OFF, COD, CSGO
    }

    init {
        addSettings(playSounds, thirdPerson, debug, clientColor, friend)
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (event.packet is CPacketUseEntity && event.packet.action == CPacketUseEntity.Action.ATTACK) {
            val attackEntityWorld = event.packet.getEntityFromWorld(Globals.mc.world) ?: return
            attackEntity = attackEntityWorld
            hasHit = true
        }

    }

    @Listener
    private fun onRender2D(event: Render2DEvent) {
        if (fullNullCheck()) return

        if (!thirdPerson.value && Globals.mc.gameSettings.thirdPersonView != 0) {
            hasHit = false
            return
        }

        if (!debug.value && Globals.mc.gameSettings.showDebugInfo) {
            hasHit = false
            return
        }

        if (!hasHit || attackEntity == null) return

        val hitMarker = TextureManager.getTexture("hitmarker") ?: return

        Dimension.TwoD {
            GlStateUtils.texture2d(true)

            val isFriend = friend.value && FriendManager.isFriend(attackEntity!!.name)

            val posX = event.scaledResolution.scaledWidth_double / 2.0F - 7.6F
            val posY = event.scaledResolution.scaledHeight_double / 2.0F - 8.0F

            if (clientColor.value) {
                if (isFriend) RenderUtils.glColor(90, 255, 255, 255) else RenderUtils.glColorClient(255)
            } else {
                if (isFriend) RenderUtils.glColor(90, 255, 255, 255) else GlStateUtils.resetColour()
            }

            RenderUtils.drawMipMapTexture(hitMarker, posX, posY, 16F)

            if (playSounds.value == SoundType.COD) {
                Globals.mc.player.playSound(COD_EVENT, 1.0f, 1.0F)
            } else if (playSounds.value == SoundType.CSGO) {
                Globals.mc.player.playSound(CSGO_EVENT, 1.0f, 1.0F)
            }

            if (++hitCoolDown == 150) {
                hasHit = false
                hitCoolDown = 0
            }
        }

        GlStateUtils.resetColour()
    }

}