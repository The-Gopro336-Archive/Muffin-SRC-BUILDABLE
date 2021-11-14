package me.han.muffin.client.module.modules.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.LivingUpdateEvent
import me.han.muffin.client.event.events.entity.player.PlayerIsPotionActiveEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.value.Value
import net.minecraft.init.MobEffects
import net.minecraft.network.play.server.SPacketEntityEffect
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object AntiEffectsModule : Module("AntiEffects", Category.RENDER, "Remove bad effects render client-sided.") {
    private val effects = Value(true, "Effects")
    private val packets = Value(true, "Packet")
    private val blindness = Value(true, "Blindness")
    private val levitation = Value(true, "Levitation")
    private val potionEffects = intArrayOf(9, 14, 15, 25)

    init {
        addSettings(effects, packets, blindness, levitation)
    }

    @Listener
    private fun onPlayerIsPotionActivePost(event: PlayerIsPotionActiveEvent) {
        if (event.entity != Globals.mc.player) return

        if (blindness.value) {
            if (event.potion == MobEffects.NAUSEA || event.potion == MobEffects.BLINDNESS) event.cancel()
        }

        if (levitation.value) if (event.potion == MobEffects.LEVITATION) event.cancel()
    }

    @Listener
    private fun onLivingUpdate(event: LivingUpdateEvent) {
        if (fullNullCheck()) return

        if (effects.value) {
            Globals.mc.player.isInvisible = false
            Globals.mc.player.removePotionEffect(MobEffects.NAUSEA)
            Globals.mc.player.removePotionEffect(MobEffects.INVISIBILITY)
            Globals.mc.player.removePotionEffect(MobEffects.BLINDNESS)
            Globals.mc.player.isInvisible = false
        }
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (packets.value && event.packet is SPacketEntityEffect) {
            if (event.packet.entityId == Globals.mc.player.entityId) {
                if (potionEffects.contains(event.packet.effectId.toInt())) event.cancel()
            }
        }

    }

}