package me.han.muffin.client.utils.extensions.mc.item

import io.netty.buffer.Unpooled
import me.han.muffin.client.core.Globals
import me.han.muffin.client.utils.extensions.mixin.misc.attackDamage
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Enchantments
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.*
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CPacketCustomPayload
import java.util.function.Predicate

val ItemStack.originalName: String get() = item.getItemStackDisplayName(this)

val Item.id get() = Item.getIdFromItem(this)
val Item.block: Block get() = Block.getBlockFromItem(this)

inline val EntityPlayer.armorSlots: List<Slot>
    get() = inventoryContainer.getSlots(5..8)

val Item.isWeapon get() = this is ItemSword || this is ItemAxe

val ItemFood.foodValue get() = this.getHealAmount(ItemStack.EMPTY)

val ItemFood.saturation get() = foodValue * this.getSaturationModifier(ItemStack.EMPTY) * 2F

inline fun <reified B : Block, T : Slot> Iterable<T>.firstBlock(predicate: Predicate<ItemStack>? = null) =
    firstByStack { itemStack ->
        itemStack.item.let { it is ItemBlock && it.block is B } && (predicate == null || predicate.test(itemStack))
    }

fun <T : Slot> Iterable<T>.firstBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
    firstByStack { itemStack ->
        itemStack.item.let { it is ItemBlock && it.block == block } && (predicate == null || predicate.test(itemStack))
    }

fun <T : Slot> Iterable<T>.firstByStack(predicate: Predicate<ItemStack>? = null): T? =
    firstOrNull {
        (predicate == null || predicate.test(it.stack))
    }

inline val EntityPlayer.hotbarSlots: List<HotbarSlot>
    get() = ArrayList<HotbarSlot>().apply {
        for (slot in 36..44) {
            add(HotbarSlot(inventoryContainer.inventorySlots[slot]))
        }
    }

fun <T : Slot> Iterable<T>.filterByStack(predicate: Predicate<ItemStack>? = null) =
    filter {
        predicate == null || predicate.test(it.stack)
    }

/**
 * Performs inventory clicking in specific window, slot, mouseButton, and click type
 *
 * @return Transaction id
 */

private fun getContainerForID(windowID: Int): Container? =
    if (windowID == 0) Minecraft.getMinecraft().player.inventoryContainer
    else Minecraft.getMinecraft().player.openContainer

val ItemStack.attackDamage
    get() = this.item.baseAttackDamage + EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, this).let {
        if (it > 0) it * 0.5f + 0.5f
        else 0.0f
    }

val Item.baseAttackDamage
    get() = when (this) {
        is ItemSword -> this.attackDamage + 4.0F
        is ItemTool -> this.attackDamage + 1.0F
        else -> 1.0f
    }

fun itemPayload(item: ItemStack, channelIn: String) {
    val buffer = PacketBuffer(Unpooled.buffer())
    buffer.writeItemStack(item)
    Globals.mc.player.connection.sendPacket(CPacketCustomPayload(channelIn, buffer))
}