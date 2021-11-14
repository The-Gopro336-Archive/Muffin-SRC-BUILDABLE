package me.han.muffin.client.manager.managers

import me.han.muffin.client.Muffin
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.utils.client.ChatUtils
import net.minecraft.util.text.ITextComponent
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object ChatManager {
    val textColour: String get() = Muffin.getInstance().guiManager.textColor
    val darkTextColour: String get() = Muffin.getInstance().guiManager.darkTextColor

    private val messageIDs = ConcurrentHashMap<Int, MutableMap<String, Int>>()
    private val counter = Counter(1337)

    fun initListener() {
        Muffin.getInstance().eventManager.addEventListener(this)
    }

    @Listener
    private fun onDisconnect(event: ServerEvent.Disconnect) {
        messageIDs.clear()
        counter.reset()
    }

    @JvmStatic
    fun sendMessage(message: String) {
        ChatUtils.sendMessage(message)
    }

    fun sendDeleteMessage(message: String, uniqueWord: String, senderID: Int) {
        val id = messageIDs.computeIfAbsent(senderID) { ConcurrentHashMap() }.computeIfAbsent(uniqueWord) { counter.next() }
        ChatUtils.sendMessage(message, id)
    }

    fun deleteMessage(uniqueWord: String, senderID: Int) {
        val map = messageIDs.remove(senderID) ?: return
        val id = map.remove(uniqueWord) ?: return
        ChatUtils.deleteMessage(id)
    }

    fun sendDeleteComponent(component: ITextComponent, uniqueWord: String, senderID: Int) {
        val id = messageIDs.computeIfAbsent(senderID) { ConcurrentHashMap() }.computeIfAbsent(uniqueWord) { counter.next() }
        ChatUtils.sendComponent(component, id)
    }

    class Counter(private val initial: Int) {
        private val counter = AtomicInteger(initial)

        fun next() = counter.incrementAndGet()
        fun reset() = counter.set(initial)
    }

}