package me.han.muffin.client.utils

import net.minecraft.client.gui.BossInfoClient

class BossInfoCounted(val info: BossInfoClient) {
    @JvmField var count: Int = 0
}