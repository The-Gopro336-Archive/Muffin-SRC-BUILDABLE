package me.han.muffin.client.module.modules.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.render.item.RenderCustomSwingAnimationEvent
import me.han.muffin.client.event.events.render.item.RenderItemAnimationEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.extensions.mixin.render.equippedProgressMainHand
import me.han.muffin.client.utils.extensions.mixin.render.itemStackMainHand
import me.han.muffin.client.utils.extensions.mixin.render.prevEquippedProgressMainHand
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Items
import net.minecraft.item.ItemSword
import net.minecraft.util.EnumHand
import net.minecraft.util.EnumHandSide
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object AnimationModule : Module("Animations", Category.RENDER, "Different animations with Mojang.") {
    private val oldPvp = Value(true, "1.8PvP")
    private val customSwingAnimation = Value(false, "SwingAnimation")

    private val oldPvpHit = Value(true, "OldPvpHitAnimation")
    private val hitThreshold = NumberValue({ oldPvpHit.value },0.9, 0.0, 1.0, 0.1, "HitThreshold")

    init {
        addSettings(oldPvp, customSwingAnimation, oldPvpHit, hitThreshold)
    }

    @Listener
    private fun onUpdate(event: UpdateEvent) {
        if (fullNullCheck() || event.stage != EventStageable.EventStage.PRE) return

        if (oldPvpHit.value) {
            if (Globals.mc.player.heldItemMainhand.item is ItemSword && Globals.mc.entityRenderer.itemRenderer.prevEquippedProgressMainHand >= hitThreshold.value) {
                Globals.mc.entityRenderer.itemRenderer.equippedProgressMainHand = 1.0F
                Globals.mc.entityRenderer.itemRenderer.itemStackMainHand = Globals.mc.player.heldItemMainhand
            }
        }

    }

    private val isConsideredAsBlocking: Boolean
        get() = ((Globals.mc.player.isHandActive && Globals.mc.player.activeHand == EnumHand.OFF_HAND // 1.12
                && Globals.mc.player.heldItemOffhand.item == Items.SHIELD
                && Globals.mc.player.heldItemMainhand.item is ItemSword))

    @Listener
    private fun onRenderItem(event: RenderItemAnimationEvent.Render) {
        if (!oldPvp.value || fullNullCheck()) return

        val oldBlock = Globals.mc.gameSettings.keyBindUseItem.isKeyDown && !Globals.mc.player.heldItemMainhand.isEmpty && Globals.mc.player.heldItemMainhand.item is ItemSword && Globals.mc.player.isHandActive
        if (oldBlock) event.isCanceled = event.stack.item == Items.SHIELD && event.hand == EnumHand.OFF_HAND

    }

    @Listener
    private fun onTransformItem(event: RenderItemAnimationEvent.Transform) {
        if (!oldPvp.value || fullNullCheck()) return

        val oldBlock = (Globals.mc.gameSettings.keyBindUseItem.isKeyDown && /*Globals.mc.player.isBlocking &&*/ !Globals.mc.player.heldItemMainhand.isEmpty && Globals.mc.player.heldItemMainhand.item is ItemSword)
        if (event.hand == EnumHand.MAIN_HAND && (oldBlock /*|| isConsideredAsBlocking*/)) {
            val i = if(Globals.mc.player.primaryHand == EnumHandSide.RIGHT)
                1F else -1F
            // func178096b
            GlStateManager.translate(0.15f * i, 0.3f, 0.0f)
            GlStateManager.rotate(5f * i, 0.0f, 0.0f, 0.0f)

            if (i > 0F) GlStateManager.translate(0.56f, -0.52f, -0.72f * i) else GlStateManager.translate(0.56f, -0.52f, 0.5F)

            GlStateManager.translate(0.0f, 0.2f * -0.6f, 0.0f)
            GlStateManager.rotate(45.0f * i, 0.0f, 1.0f, 0.0f)

            GlStateManager.scale(1.625f, 1.625f, 1.625f)

            GlStateManager.translate(-0.5f, 0.2f, 0.0f)
            GlStateManager.rotate(30.0f * i, 0.0f, 1.0f, 0.0f)
            GlStateManager.rotate(-80.0f, 1.0f, 0.0f, 0.0f)
            GlStateManager.rotate(30.0f * i, 0.0f, 1.0f, 0.0f)
        }
    }

    @Listener
    private fun onSwingAnimation(event: RenderCustomSwingAnimationEvent) {
        if (!customSwingAnimation.value || fullNullCheck()) return
        event.cancel()
    }

}