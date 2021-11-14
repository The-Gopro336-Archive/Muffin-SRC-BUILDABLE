package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.extensions.mc.item.firstItem
import me.han.muffin.client.utils.extensions.mc.item.hotbarSlots
import me.han.muffin.client.utils.extensions.mc.item.moveToSlot
import me.han.muffin.client.utils.math.rotation.Vec2f
import me.han.muffin.client.utils.network.LagCompensator
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.item.ItemTippedArrow
import net.minecraft.potion.PotionEffect
import net.minecraft.potion.PotionUtils
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.roundToInt

internal object SelfShootModule: Module("SelfShoot", Category.PLAYER, "Always shoot yourself with good arrows.", 275) {
    private val swapBack = Value(false, "SwapBack")
    private val tpsSync = Value(false, "TPSSync")
    private val delay = NumberValue({ !tpsSync.value },3, 0, 20, 1, "Delay")

    val goodEffects = hashSetOf(MobEffects.STRENGTH, MobEffects.SPEED, MobEffects.HEALTH_BOOST, MobEffects.FIRE_RESISTANCE, MobEffects.INVISIBILITY)
    val badEffects = hashSetOf(MobEffects.WEAKNESS, MobEffects.JUMP_BOOST, MobEffects.SLOWNESS, MobEffects.POISON)

    private var shootingArrow = false

    private var bowSlot = -1

    private var lastEffect: PotionEffect? = null
    private val totalArrows = hashMapOf<Int, PotionEffect>()

    private var lastSlot = -1
    private var arrowSlot = -1

    private var shouldWaitForSwitchBack = false

    init {
        addSettings(swapBack, tpsSync, delay)
    }

    override fun onEnable() {
        if (fullNullCheck()) return

        shouldWaitForSwitchBack = false

        bowSlot = Globals.mc.player.hotbarSlots.firstItem(Items.BOW)?.hotbarSlot ?: -1
        if (bowSlot == -1) return

        arrowSlot = -1

        // find good arrows
        for (i in 9 until 45) {
            val stack = Globals.mc.player.inventoryContainer.inventory[i]
            //Globals.mc.player.inventory.getStackInSlot(i)
            if (stack.isEmpty || stack.item !is ItemTippedArrow) continue
            val effects = PotionUtils.getEffectsFromStack(stack)

            for (effect in effects) {
                if (effect.potion.isBadEffect || badEffects.contains(effect.potion)) continue

                if (lastEffect != null && lastEffect != effect && Globals.mc.player.isPotionActive(effect.potion)) continue

                if (goodEffects.contains(effect.potion)) {
                    totalArrows[i] = effect
                    arrowSlot = i
                    lastEffect = effect
                    ChatManager.sendMessage("Found good arrow: ${stack.displayName}")
                }
            }
        }


    }

    override fun onDisable() {
        shouldWaitForSwitchBack = false
        shootingArrow = false
        arrowSlot = -1
        lastSlot = -1
        //        KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindUseItem.keyCode, false)
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (bowSlot == -1) {
            ChatManager.sendMessage("There are no bow in your hotbar.")
            toggle()
            return
        }

        if (arrowSlot == -1) {
            ChatManager.sendMessage("There are no good arrow from your inventory.")
            toggle()
            return
        }

        if (shootingArrow) addMotion { rotate(Vec2f(Globals.mc.player.rotationYaw, -90F)) }

        if (shouldWaitForSwitchBack) {
            moveToSlot(9, arrowSlot)

            if (swapBack.value && lastSlot != -1) {
                InventoryUtils.swapSlot(lastSlot)
                lastSlot = -1
            }

            shootingArrow = false
            toggle()
            return
        }

        val shootDelay = if (tpsSync.value) 3 + LagCompensator.syncTicks.roundToInt() else delay.value
        if (shootingArrow && Globals.mc.player.isHandActive && Globals.mc.player.itemInUseMaxCount >= shootDelay) {
            KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindUseItem.keyCode, false)
            shouldWaitForSwitchBack = true
            return
        }

        if (shootingArrow) return

        lastSlot = Globals.mc.player.inventory.currentItem
        InventoryUtils.swapSlot(bowSlot)
        moveToSlot(arrowSlot, 9)

        shootingArrow = true
        KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindUseItem.keyCode, true)
    }

  //  @Listener
  //  private fun onPlayerStopUseItem(event: PlayerOnStoppedUsingItemEvent) {
        //    if (shootingArrow) event.cancel()
 //   }


}