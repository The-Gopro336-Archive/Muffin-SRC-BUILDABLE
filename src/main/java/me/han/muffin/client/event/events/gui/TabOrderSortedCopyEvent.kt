package me.han.muffin.client.event.events.gui

import com.google.common.collect.Ordering
import me.han.muffin.client.event.EventCancellable
import net.minecraft.client.network.NetworkPlayerInfo

data class TabOrderSortedCopyEvent(var ordering: Ordering<NetworkPlayerInfo>?, val elements: Iterable<NetworkPlayerInfo>): EventCancellable() {
    var customOrdering: List<NetworkPlayerInfo>? = emptyList()
}