package me.han.muffin.client.module.modules.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.SettingEvent
import me.han.muffin.client.event.events.gui.GuiScreenEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.render.RenderVignetteEvent
import me.han.muffin.client.event.events.render.entity.RenderEntityEvent
import me.han.muffin.client.event.events.render.overlay.*
import me.han.muffin.client.event.events.world.WorldPlaySoundEvent
import me.han.muffin.client.imixin.gui.IGuiGameOver
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.value.Value
import net.minecraft.client.gui.GuiGameOver
import net.minecraft.client.tutorial.TutorialSteps
import net.minecraft.entity.passive.EntityBat
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.server.SPacketEffect
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.util.SoundCategory
import net.minecraft.util.text.ITextComponent
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import javax.annotation.Nullable

internal object AntiOverlayModule : Module("AntiOverlay", Category.RENDER, true, "Prevent running some overlays.") {
    private val antiDeathScreen = Value(false, "AntiDeathScreen")
    private val portal = Value(false, "Portal")
    private val fire = Value(true, "Fire")
    private val blocks = Value(true, "Blocks")
    private val water = Value(true, "Water")
    private val potion = Value(true, "Potion")
    private val pumpkin = Value(true, "Pumpkin")
    private val vignette = Value(true, "Vignette")
    private val bat = Value(true, "Bat")
    private val iceCake = Value(false, "IceCake")
    private val tutorial = Value(true, "Tutorial")

    init {
        addSettings(antiDeathScreen, fire, blocks, water, potion, pumpkin, vignette, bat, portal, iceCake, tutorial)
    }

    private val BLACKLIST = linkedSetOf(
        SoundEvents.ITEM_ARMOR_EQUIP_GENERIC,
        SoundEvents.ITEM_ARMOR_EQIIP_ELYTRA,
        SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,
        SoundEvents.ITEM_ARMOR_EQUIP_IRON,
        SoundEvents.ITEM_ARMOR_EQUIP_GOLD,
        SoundEvents.ITEM_ARMOR_EQUIP_CHAIN,
        SoundEvents.ITEM_ARMOR_EQUIP_LEATHER
    )

    private val BAT_SOUNDS = linkedSetOf(
        SoundEvents.ENTITY_BAT_AMBIENT,
        SoundEvents.ENTITY_BAT_DEATH,
        SoundEvents.ENTITY_BAT_HURT,
        SoundEvents.ENTITY_BAT_LOOP,
        SoundEvents.ENTITY_BAT_TAKEOFF
    )

    @Listener
    private fun onRenderPotionIcons(event: RenderPotionIconsEvent) {
        if (potion.value) event.cancel()
    }

    @Listener
    private fun onRenderPotionEffects(event: RenderPotionEffectsEvent) {
        if (potion.value) event.cancel()
    }

    @Listener
    private fun onRenderPumpkin(event: RenderPumpkinEvent) {
        if (pumpkin.value) event.cancel()
    }

    @Listener
    private fun onRenderVignette(event: RenderVignetteEvent) {
        if (vignette.value) event.cancel()
    }

    @Listener
    private fun onSetting(event: SettingEvent) {
        if (event.module != this) return
        if (tutorial.value) Globals.mc.gameSettings.tutorialStep = TutorialSteps.NONE
    }

    @Listener
    private fun onGuiDisplayed(event: GuiScreenEvent.Displayed) {
        if (!antiDeathScreen.value || event.screen !is GuiGameOver) return

        val causeOfDeath = ((event.screen as GuiGameOver) as IGuiGameOver).causeOfDeath
        event.screen = GameOverGui(causeOfDeath)
    }

    @Listener
    private fun onRenderEntity(event: RenderEntityEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (bat.value && event.entity is EntityBat) event.cancel()
        if (iceCake.value && event.entity.uniqueID.equals("7d76f8d62eb14ac5b2d8c01e46e1566c")) event.cancel()

    }

    @Listener
    private fun onWorldPlaySound(event: WorldPlaySoundEvent) {
        if (!bat.value) return

        if (BAT_SOUNDS.contains(event.sound)) event.cancel()
    }

    @Listener
    private fun onRenderPortal(event: RenderPortalIconEvent) {
        if (portal.value) event.cancel()
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (event.packet is SPacketSoundEffect) {
            if (BLACKLIST.contains(event.packet.sound) || (event.packet.category == SoundCategory.WEATHER && event.packet.sound == SoundEvents.ENTITY_LIGHTNING_THUNDER)) {
                event.cancel()
            }
        }

        if (event.packet is SPacketEffect) {
            if (event.packet.soundType == 1038 || event.packet.soundType == 1023 || event.packet.soundType == 1028) {
                event.cancel()
            }
        }
    }

    @Listener
    private fun onRenderOverlay(event: RenderOverlayEvent) {
        if (fire.value && event.type == RenderOverlayEvent.OverlayType.FIRE) {
            event.cancel()
        }
        if (water.value && event.type == RenderOverlayEvent.OverlayType.LIQUID) {
            event.cancel()
        }
        if (fire.value && event.type == RenderOverlayEvent.OverlayType.FIRE) {
            event.cancel()
        }
    }

    class GameOverGui(@Nullable causeOfDeath: ITextComponent?): GuiGameOver(causeOfDeath) {
        override fun updateScreen() {
            // If the player is actually alive, yet we're still playing this death screen for some reason
            if (Globals.mc.player.isAlive) {
                // Remove the death screen!
                mc.displayGuiScreen(null)
                mc.setIngameFocus()
            } else {
                super.updateScreen()
            }
        }
    }

}