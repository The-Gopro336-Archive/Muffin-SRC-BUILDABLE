package me.han.muffin.client.imixin.render

import net.minecraft.item.ItemStack

interface IItemRenderer {

    var itemStackMainHand: ItemStack
    var itemStackOffHand: ItemStack

    var equippedProgressMainHand: Float
    var equippedProgressOffHand: Float

    var prevEquippedProgressMainHand: Float
    var prevEquippedProgressOffHand: Float

}