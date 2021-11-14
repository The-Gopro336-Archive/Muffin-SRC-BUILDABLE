package me.han.muffin.client.event.events.network

object YggAuthEvent {
    var status = AccountStatus.Premium

    enum class AccountStatus {
        Premium, Cracked
    }

}