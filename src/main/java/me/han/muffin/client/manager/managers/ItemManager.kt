package me.han.muffin.client.manager.managers

import me.han.muffin.client.Muffin
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.utils.InventoryUtils
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.concurrent.Executors
import kotlin.concurrent.thread

object ItemManager {
    private var updateThread: Thread? = null
    private val updateExecutor = Executors.newSingleThreadExecutor()

    @JvmField var crystalStack: ItemStack = ItemStack.EMPTY
    @JvmField var expStack: ItemStack = ItemStack.EMPTY
    @JvmField var totemStack: ItemStack = ItemStack.EMPTY
    @JvmField var gAppleStack: ItemStack = ItemStack.EMPTY

    init {
        Muffin.getInstance().eventManager.addEventListener(this)
    }

    private fun updateItemStacks() {
        if ((updateThread == null || !updateThread!!.isAlive || updateThread!!.isInterrupted)) {
            updateThread = thread(start = false) {
                crystalStack = createNewStack(Items.END_CRYSTAL)
                expStack = createNewStack(Items.EXPERIENCE_BOTTLE)
                totemStack = createNewStack(Items.TOTEM_OF_UNDYING)
                gAppleStack = createNewStack(Items.GOLDEN_APPLE)
            }
            updateExecutor.execute(updateThread!!)
        }
    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return
        updateItemStacks()
    }

    private fun createNewStack(item: Item): ItemStack = ItemStack(item, InventoryUtils.getItemsCount(item))

}