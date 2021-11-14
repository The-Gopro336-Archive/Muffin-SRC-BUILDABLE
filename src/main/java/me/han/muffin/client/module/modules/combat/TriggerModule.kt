package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.manager.managers.FriendManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.extensions.mixin.entity.curBlockDamageMP
import me.han.muffin.client.utils.math.RandomUtils
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import me.han.muffin.client.value.ValueListeners
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.EnumHand
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.random.Random

object TriggerModule: Module("Trigger", Category.COMBAT, "Automatically click for you.") {
    private val leftValue = Value(true, "Left")
    private val rightValue = Value(true, "Right")
    private val jitterValue = Value(false, "Jitter")

    private val entityClicks = Value(true, "ClickEntity")

    private val minLeftCPSValue = NumberValue({ leftValue.value }, 8, 1, 40, 1, "MinLeftCPS")
    private val maxLeftCPSValue = NumberValue({ leftValue.value }, 10, 1, 40, 1, "MaxLeftCPS")

    private val minRightCPSValue = NumberValue({ rightValue.value }, 20, 1, 60, 1, "MinRightCPS")
    private val maxRightCPSValue = NumberValue({ rightValue.value }, 20, 1, 60, 1, "MaxRightCPS")

    private val minEntityCPSValue = NumberValue({ entityClicks.value }, 8, 1, 40, 1, "MinLeftCPS")
    private val maxEntityCPSValue = NumberValue({ entityClicks.value }, 10, 1, 40, 1, "MaxLeftCPS")


    private var leftDelay = RandomUtils.randomClickDelay(minLeftCPSValue.value, maxLeftCPSValue.value)
    private var leftLastSwing = 0L

    private var entityDelay = RandomUtils.randomClickDelay(minEntityCPSValue.value, maxEntityCPSValue.value)
    private var entityLastSwing = 0L

    private var rightDelay = RandomUtils.randomClickDelay(minRightCPSValue.value, maxRightCPSValue.value)
    private var rightLastSwing = 0L

    init {
        addSettings(leftValue, rightValue, jitterValue, entityClicks,
            minLeftCPSValue, maxLeftCPSValue,
            minRightCPSValue, maxRightCPSValue,
            minEntityCPSValue, maxEntityCPSValue)

        minLeftCPSValue.listeners = object : ValueListeners {
            override fun onValueChange(value: Value<*>?) {
                val minCPS = minLeftCPSValue.value
                if (minCPS > maxLeftCPSValue.value) {
                    minLeftCPSValue.value = maxLeftCPSValue.value
                }
            }
        }

        minRightCPSValue.listeners = object : ValueListeners {
            override fun onValueChange(value: Value<*>?) {
                val minCPS = minRightCPSValue.value
                if (minCPS > maxRightCPSValue.value) {
                    minRightCPSValue.value = maxRightCPSValue.value
                }
            }
        }

    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        // Left click
        if (Globals.mc.gameSettings.keyBindAttack.isKeyDown && leftValue.value && System.currentTimeMillis() - leftLastSwing >= leftDelay && Globals.mc.playerController.curBlockDamageMP == 0F) {
            KeyBinding.onTick(Globals.mc.gameSettings.keyBindAttack.keyCode) // Minecraft Click Handling

            leftLastSwing = System.currentTimeMillis()
            leftDelay = RandomUtils.randomClickDelay(minLeftCPSValue.value, maxLeftCPSValue.value)
        }

        // Right click
        if (Globals.mc.gameSettings.keyBindUseItem.isKeyDown && !Globals.mc.player.isHandActive && rightValue.value &&
            System.currentTimeMillis() - rightLastSwing >= rightDelay) {
            KeyBinding.onTick(Globals.mc.gameSettings.keyBindUseItem.keyCode) // Minecraft Click Handling

            rightLastSwing = System.currentTimeMillis()
            rightDelay = RandomUtils.randomClickDelay(minRightCPSValue.value, maxRightCPSValue.value)
        }

        if (entityClicks.value) {
            val rayTrace = Globals.mc.objectMouseOver ?: return
            if (rayTrace.entityHit != null && System.currentTimeMillis() - entityLastSwing >= entityDelay) {
                if (rayTrace.entityHit !is EntityLivingBase || FriendManager.isFriend(rayTrace.entityHit.name) || rayTrace.entityHit.isInvisible) return
                KeyBinding.onTick(Globals.mc.gameSettings.keyBindUseItem.keyCode) // Minecraft Click Handling
                entityLastSwing = System.currentTimeMillis()
                entityDelay = RandomUtils.randomClickDelay(minEntityCPSValue.value, maxEntityCPSValue.value)
            }
        }

    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (jitterValue.value &&
            (leftValue.value && Globals.mc.gameSettings.keyBindAttack.isKeyDown &&
                    Globals.mc.playerController.curBlockDamageMP == 0F
                    || rightValue.value && Globals.mc.gameSettings.keyBindUseItem.isKeyDown && !Globals.mc.player.isHandActive)) {
            if (Random.nextBoolean())
                Globals.mc.player.rotationYaw += if (Random.nextBoolean()) -RandomUtils.nextFloat(0F, 1F) else RandomUtils.nextFloat(0F, 1F)

            if (Random.nextBoolean()) {
                Globals.mc.player.rotationPitch += if (Random.nextBoolean()) -RandomUtils.nextFloat(0F, 1F) else RandomUtils.nextFloat(0F, 1F)

                // Make sure pitch is not going into unlegit values
                if (Globals.mc.player.rotationPitch > 90)
                    Globals.mc.player.rotationPitch = 90F
                else if (Globals.mc.player.rotationPitch < -90)
                    Globals.mc.player.rotationPitch = -90F
            }
        }


        if (leftValue.value && entityClicks.value) {
            val rayTrace = Globals.mc.objectMouseOver
            if (rayTrace?.entityHit != null) {
                val target = Globals.mc.objectMouseOver.entityHit
                if (target !is EntityLivingBase || FriendManager.isFriend(target.name) || target.isInvisible()) return
                    Globals.mc.playerController.attackEntity(Globals.mc.player, target)
                    Globals.mc.player.swingArm(EnumHand.MAIN_HAND)
            }
        }
    }

}