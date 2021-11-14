package me.han.muffin.client.module.modules.misc

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.value.Value
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object Debugger: Module("Debugger", Category.MISC) {
    private val debugXYZ = Value(false, "DebugDiff")

    private val motionY = Value(false, "MotionY")
    private val postMotionY = Value(false, "PostMotionY")

    private val IDs = Value(false, "IDs")

    init {
        addSettings(debugXYZ, motionY, postMotionY, IDs)
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
//        Command.sendChatMessage("KEYBOARDNONE = " + Keyboard.KEY_ESCAPE.toString())
//        Command.sendChatMessage("MOUSE1 = " + Mouse.getButtonName(1))

        when (event.stage) {
            EventStageable.EventStage.PRE -> {
                if (motionY.value) ChatManager.sendMessage("PRE: ${Globals.mc.player.motionY}")
            }
            EventStageable.EventStage.POST -> {
                if (postMotionY.value) ChatManager.sendMessage("POST: ${Globals.mc.player.motionY}")
            }
        }
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (fullNullCheck()) return

        if (event.stage == EventStageable.EventStage.PRE && event.packet is CPacketPlayerTryUseItemOnBlock) {
            if (debugXYZ.value) {
                ChatManager.sendMessage("DiffX: ${event.packet.facingX}, DiffY: ${event.packet.facingY}, DiffZ: ${event.packet.facingZ}. \n ${event.packet.direction}")
            }
        }

        if (IDs.value && event.packet is CPacketEntityAction) {
            when (event.stage) {
                EventStageable.EventStage.PRE -> {
                    ChatManager.sendMessage("${ChatFormatting.RED}PRE: ${ChatFormatting.RESET}${event.packet.action}, IDs: ${event.packet.auxData}")
                }
                EventStageable.EventStage.POST -> {
                    ChatManager.sendMessage("${ChatFormatting.GREEN}POST: ${ChatFormatting.RESET}${event.packet.action}, IDs: ${event.packet.auxData}")
                }
            }

        }
    }

}