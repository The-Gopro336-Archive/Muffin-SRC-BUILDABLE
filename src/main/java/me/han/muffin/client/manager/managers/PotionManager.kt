package me.han.muffin.client.manager.managers

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.entity.TotemPopEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.event.events.world.WorldEntityEvent
import me.han.muffin.client.utils.entity.EntityUtil
import net.minecraft.client.resources.I18n
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.component1
import kotlin.collections.set

object PotionManager {
    private val potions = ConcurrentHashMap<EntityPlayer, PotionList>()

    init {
        Muffin.getInstance().eventManager.addEventListener(this)
    }

    @Listener
    private fun onLogout(event: ServerEvent.Disconnect) {
        potions.clear()
    }

    @Listener
    private fun onTotemPop(event: TotemPopEvent) {
        if (event.entity is EntityPlayer) {
            onTotemPop(event.entity)
        }
    }

    @Listener
    private fun onEntityRemoved(event: WorldEntityEvent.Remove) {
        if (event.entity == null || event.entity !is EntityPlayer) return
        onTotemPop(event.entity as EntityPlayer)
    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || EntityUtil.fullNullCheck()) return

        updatePlayer()

        val removeList = arrayListOf<EntityPlayer>()

        for ((playerMap, effects) in potions) {
            var notFound = true

            for (player in Globals.mc.world.playerEntities) {
                if (potions[player] == null) {
                    val list = PotionList()
                    for (effect in player.activePotionEffects) {
                        list.addEffect(effect)
                    }
                    potions[player] = list
                    notFound = false
                }
                if (playerMap == player) {
                    notFound = false
                }
            }
            if (notFound) {
                removeList.add(playerMap)
            }
        }

        for (player in removeList) {
            potions.remove(player)
        }
    }

    private fun updatePlayer() {
        val list = PotionList()
        for (effect in Globals.mc.player.activePotionEffects) list.addEffect(effect)
        potions[Globals.mc.player] = list
    }

    val ownPotions: List<PotionEffect>
        get() = getPlayerPotions(Globals.mc.player)

    private fun getPlayerPotions(player: EntityPlayer): List<PotionEffect> {
        val list = potions[player] ?: return emptyList()
        return list.effects
    }

    private fun onTotemPop(player: EntityPlayer) {
        potions[player] = PotionList()
    }

    fun getImportantPotions(player: EntityPlayer): Array<PotionEffect?> {
        val array = arrayOfNulls<PotionEffect>(3)

        for (effect in getPlayerPotions(player)) {
            val potion = effect.potion
            when (I18n.format(potion.name).toLowerCase()) {
                "strength" -> {
                    array[0] = effect
                }
                "weakness" -> {
                    array[1] = effect
                }
                "speed" -> {
                    array[2] = effect
                }
            }
        }
        return array
    }

    fun getPotionString(effect: PotionEffect): String {
        val potion = effect.potion
        return I18n.format(potion.name) + " " + (effect.amplifier + 1) + " \u00a7f" + Potion.getPotionDurationString(effect, 1.0f)
    }

    fun getColoredPotionString(effect: PotionEffect): String {
        val potion = effect.potion
        when (I18n.format(potion.name)) {
            "Jump Boost", "Speed" -> {
                return "\u00a7b" + getPotionString(effect)
            }
            "Resistance", "Strength" -> {
                return "\u00a7c" + getPotionString(effect)
            }
            "Wither", "Slowness", "Weakness" -> {
                return "\u00a70" + getPotionString(effect)
            }
            "Absorption" -> {
                return "\u00a79" + getPotionString(effect)
            }
            "Haste", "Fire Resistance" -> {
                return "\u00a76" + getPotionString(effect)
            }
            "Regeneration" -> {
                return "\u00a7d" + getPotionString(effect)
            }
            "Night Vision", "Poison" -> {
                return "\u00a7a" + getPotionString(effect)
            }
        }
        return "\u00a7f" + getPotionString(effect)
    }

    fun getTextRadarPotionWithDuration(player: EntityPlayer): String {
        val array = getImportantPotions(player)
        val strength = array[0]
        val weakness = array[1]
        val speed = array[2]

        return "" +
                (if (strength != null) "\u00a7c S" + (strength.amplifier + 1) + " " + Potion.getPotionDurationString(strength, 1.0f) else "") +
                (if (weakness != null) "\u00a78 W " + Potion.getPotionDurationString(weakness, 1.0f) else "") +
                if (speed != null) "\u00a7b S" + (speed.amplifier + 1) + " " + Potion.getPotionDurationString(weakness, 1.0f) else ""
    }

    @JvmStatic
    fun getTextRadarPotion(player: EntityPlayer): String {
        val array = getImportantPotions(player)
        val strength = array[0]
        val weakness = array[1]
        val speed = array[2]

        return "" + (if (strength != null) (if (strength.amplifier == 0) ChatFormatting.RED else ChatFormatting.DARK_RED).toString() + "S" +
                (if (strength.amplifier == 0) "1" else if (strength.amplifier == 1) "2" else strength.amplifier) + " " else "") +
                (if (weakness != null) (if (weakness.amplifier == 0) ChatFormatting.GRAY else ChatFormatting.DARK_GRAY).toString() + "W" +
                        (if (weakness.amplifier == 0) "1" else if (weakness.amplifier == 1) "2" else weakness.amplifier) + " " else "") +
                if (speed != null) (if (speed.amplifier == 0) ChatFormatting.AQUA else ChatFormatting.BLUE).toString() + "S" +
                        (if (speed.amplifier == 0) "1" else if (speed.amplifier == 1) "2" else speed.amplifier) + " " else ""

        //    return "" + (strength != null ? "\u00a7c S" + (strength.getAmplifier() + 1) + " " : "") + (weakness != null ? "\u00a78 W " : "") + (speed != null ? "\u00a7b S" + (speed.getAmplifier() + 1) + " " : "");
    }

    class PotionList {
        val effects = arrayListOf<PotionEffect>()

        fun addEffect(effect: PotionEffect?) {
            if (effect != null) effects.add(effect)
        }
    }

}