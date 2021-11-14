package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.client.ClientTickEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.extensions.mc.entity.realHealth
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.NumberValue
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.init.Items
import net.minecraft.inventory.ClickType
import net.minecraft.util.EnumHand
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object StrictTotemModule: Module("StrictTotem", Category.COMBAT, "AutoTotem for strict anticheat server.") {
    private val health = NumberValue(8.0, 0.0, 36.0, 0.2, "Health")
    private val delay = NumberValue(0.0, 0.0, 1.0, 0.01, "Delay")

    private var canSwitch  = false
    private val timer = Timer()

    init {
        addSettings(health, delay)
    }

    @Listener
    private fun onClientTicking(event: ClientTickEvent) {
        if (fullNullCheck()) return

        if (!canSwitch) timer.reset()

        if (shouldSwitchToTotem() && (Globals.mc.currentScreen !is GuiContainer || Globals.mc.currentScreen is GuiInventory) && Globals.mc.player.getHeldItem(EnumHand.OFF_HAND).item != Items.TOTEM_OF_UNDYING && !Globals.mc.player.isCreative) {
            val inventoryStack = Globals.mc.player.inventory.itemStack
            val stackItem = inventoryStack.item
            val isStackEmpty = inventoryStack.isEmpty

            for (i in 44 downTo 9) {
                if (Globals.mc.player.inventoryContainer.getSlot(i).stack.item == Items.TOTEM_OF_UNDYING) {
                    canSwitch = true
                    if (timer.passed(delay.value * 1000.0F) && stackItem != Items.TOTEM_OF_UNDYING) {
                        Globals.mc.playerController.windowClick(0, i, 0, ClickType.PICKUP, Globals.mc.player)
                    }
                    if (timer.passed(delay.value * 2000.0F) && stackItem == Items.TOTEM_OF_UNDYING) {
                        Globals.mc.playerController.windowClick(0, 45, 0, ClickType.PICKUP, Globals.mc.player)
                        if (isStackEmpty) {
                            canSwitch = false
                            return
                        }
                    }
                    if (timer.passed(delay.value * 3000.0F) && !isStackEmpty && Globals.mc.player.getHeldItem(EnumHand.OFF_HAND).item == Items.TOTEM_OF_UNDYING) {
                        Globals.mc.playerController.windowClick(0, i, 0, ClickType.PICKUP, Globals.mc.player)
                        canSwitch = false
                    }
                }
            }
        }
    }

    private fun shouldSwitchToTotem(): Boolean {
        return health.value > 0.0 && health.value >= Globals.mc.player.realHealth
    }

}