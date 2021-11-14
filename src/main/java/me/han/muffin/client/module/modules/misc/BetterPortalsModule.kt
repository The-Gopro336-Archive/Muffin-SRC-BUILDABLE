package me.han.muffin.client.module.modules.misc

import me.han.muffin.client.event.events.entity.MaxInPortalTimeEvent
import me.han.muffin.client.event.events.entity.PortalCooldownEvent
import me.han.muffin.client.event.events.entity.PortalScreenEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object BetterPortalsModule: Module("BetterPortals", Category.MISC, "Tweaks for Portals") {

    private val portalChat = Value(true, "Chat")
    private val fastPortal = Value(false, "FastPortal")

    private val coolDown = NumberValue({ fastPortal.value },5, 1, 10, 1, "Cooldown")
    private val time = NumberValue({ fastPortal.value },5, 0, 80, 2, "Time")

    init {
        addSettings(portalChat, fastPortal, coolDown, time)
    }

    @Listener
    private fun onPortalCooldown(event: PortalCooldownEvent) {
        if (fastPortal.value) {
            event.cooldown = coolDown.value
            event.cancel()
        }
    }

    @Listener
    private fun onMaxInPortalTime(event: MaxInPortalTimeEvent) {
        if (fastPortal.value) {
            event.time = time.value
            event.cancel()
        }
    }

    @Listener
    private fun onPortalScreen(event: PortalScreenEvent) {
        if (portalChat.value) event.cancel()
    }

}