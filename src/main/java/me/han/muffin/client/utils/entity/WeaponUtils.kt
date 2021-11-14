package me.han.muffin.client.utils.entity

import me.han.muffin.client.core.Globals
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.extensions.mc.item.moveToHotbar
import me.han.muffin.client.utils.extensions.mc.item.quickMoveSlot
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.EnumCreatureAttribute
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.ItemAir
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemSword
import net.minecraft.nbt.NBTTagList

object WeaponUtils {

    @JvmStatic
    fun isSuperWeapon(stack: ItemStack?): Boolean {
        if (stack == null || stack.isEmpty || stack.tagCompound == null || stack.enchantmentTagList.tagType == 0) return false

//        val stackItem = stack.item
//        val damageResult = EntityEquipmentSlot.values().any {
//            val stackAttribute = stackItem.getAttributeModifiers(it, stack)
//            val mainHandDamage = stack    Attribute[SharedMonsterAttributes.ATTACK_DAMAGE.name].firstOrNull()
//            val amount = mainHandDamage?.amount ?: 1.0
//            amount > 120.0
//        }
//        if (damageResult) return damageResult

        val tagCompound = stack.tagCompound ?: return false

        val attributes = tagCompound.getTag("AttributeModifiers")
        if (attributes is NBTTagList) {
            for (i in 0 until attributes.tagCount()) {
                val attribute = attributes.getCompoundTagAt(i)
                if (attribute.getString("AttributeName") != SharedMonsterAttributes.ATTACK_DAMAGE.name) continue
                if (attribute.getInteger("Amount") >= 1269) return true
            }
        }

        val enchants = tagCompound.getTag("ench")
        if (enchants is NBTTagList) {
            for (i in 0 until enchants.tagCount()) {
                val enchant = enchants.getCompoundTagAt(i)
                if (enchant.getInteger("id") != 16) continue
                if (enchant.getInteger("lvl") >= 16) return true
            }
        }

        return false
    }

    fun doSwapSwordFromHopper(swapMode: Boolean) {
        if (isSuperWeapon(Globals.mc.player.heldItemMainhand)) return

        val hopperSlots = Globals.mc.player.openContainer.inventorySlots

        for (i in hopperSlots.indices) {
            if (isSuperWeapon(hopperSlots[i].stack)) {
                val windowID = Globals.mc.player.openContainer.windowId
                if (swapMode) {
                    for (x in 0 until 9) {
                        val hotbarStack = Globals.mc.player.inventory.getStackInSlot(i)
                        if (hotbarStack.isEmpty || hotbarStack.item is ItemAir) {
                            moveToHotbar(windowID, i, x)
                        } else {
                            moveToHotbar(windowID, i, 0)
                        }
                    }
                } else {
                    quickMoveSlot(windowID, i)
                }
                break
            }
        }

    }

    fun doSwitchSwordInventory() {
        if (isSuperWeapon(Globals.mc.player.heldItemMainhand)) return

        for (i in 0 until 9) {
            val hotbarStack = Globals.mc.player.inventory.getStackInSlot(i)
            if (hotbarStack.isEmpty || !isSuperWeapon(hotbarStack)) continue
            InventoryUtils.swapSlot(i)
        }

    }

    fun equipBestWeapon() {
        var bestSlot = -1
        var maxDamage = 0.0

        for (i in 0..8) {
            val stack = Globals.mc.player.inventory.getStackInSlot(i)
            if (stack.isEmpty || stack.item !is ItemSword) continue

            val damage = (stack.item as ItemSword).attackDamage + EnchantmentHelper.getModifierForCreature(stack, EnumCreatureAttribute.UNDEFINED).toDouble()

            if (damage > maxDamage) {
                maxDamage = damage
                bestSlot = i
            }

        }
        if (bestSlot != -1) InventoryUtils.swapSlot(bestSlot)
    }

}