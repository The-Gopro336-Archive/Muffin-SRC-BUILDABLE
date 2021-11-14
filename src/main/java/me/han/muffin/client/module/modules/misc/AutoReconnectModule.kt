package me.han.muffin.client.module.modules.misc

import me.han.muffin.client.module.Module
import me.han.muffin.client.value.NumberValue

internal object AutoReconnectModule: Module("AutoReconnect", Category.MISC, "Automatically reconnects you to your last server") {
    val delay = NumberValue(5.0F, 1.0F, 20.0F, 1.0F, "Delay")

    init {
        addSettings(delay)
    }

}