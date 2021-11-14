package me.han.muffin.client.manager.managers

import me.han.muffin.client.utils.extensions.kotlin.synchronized
import me.han.muffin.client.utils.timer.Timer

object NotificationManager {
    @JvmField val notifications = arrayListOf<Notification>().synchronized()

    fun addNotification(type: NotificationType, title: String, description: String) {
        notifications.add(Notification(type, title, description))
    }

    class Notification(val type: NotificationType, val title: String, val description: String) {
        private val decayTimer = Timer()
        private val decayTime = 2500
        val isDecayed: Boolean get() = decayTimer.passed(decayTime.toDouble())
        init {
            decayTimer.reset()
        }
    }

    enum class NotificationType {
        Info, Success, Warning, Error
    }

}