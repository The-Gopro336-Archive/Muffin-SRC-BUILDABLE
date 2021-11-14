package me.han.muffin.client.utils.combat

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.extensions.mc.item.attackDamage
import me.han.muffin.client.utils.extensions.mc.item.filterByStack
import me.han.muffin.client.utils.extensions.mc.item.hotbarSlots
import me.han.muffin.client.utils.extensions.mc.item.swapToSlot
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.MobEffects
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.util.CombatRules
import net.minecraft.util.DamageSource
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*
import kotlin.math.max
import kotlin.math.round

object CombatUtils {
    private val cachedArmorValues = WeakHashMap<EntityLivingBase, Pair<Float, Float>>()

    init {
        Muffin.getInstance().eventManager.addEventListener(this)
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (EntityUtil.fullNullCheck()) return

        for (entity in Globals.mc.world.loadedEntityList) {
            if (entity !is EntityLivingBase) continue
            val armorValue = entity.totalArmorValue.toFloat()
            val toughness = entity.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).attributeValue.toFloat()

            cachedArmorValues[entity] = armorValue to toughness
        }
    }

    @Listener
    private fun onDisconnect(event: ServerEvent.Disconnect) {
        cachedArmorValues.clear()
    }

    fun calcDamageFromPlayer(entity: EntityPlayer, assumeCritical: Boolean = false): Float {
        val itemStack = entity.heldItemMainhand
        var damage = itemStack.attackDamage

        if (assumeCritical) damage *= 1.5F
        return calcDamage(Globals.mc.player, damage)
    }

    fun calcDamageFromMob(entity: EntityMob): Float {
        var damage = entity.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).attributeValue.toFloat()
        damage += EnchantmentHelper.getModifierForCreature(entity.heldItemMainhand, Globals.mc.player.creatureAttribute)
        return calcDamage(Globals.mc.player, damage)
    }

    fun calcDamage(entity: EntityLivingBase, damageIn: Float = 100f, source: DamageSource = DamageSource.GENERIC, roundDamage: Boolean = false): Float {
        if (entity is EntityPlayer && entity.isCreative) return 0.0F // Return 0 directly if entity is a player and in creative mode

        val pair = cachedArmorValues[entity] ?: return 0.0F
        var damage = CombatRules.getDamageAfterAbsorb(damageIn, pair.first, pair.second)

        if (source != DamageSource.OUT_OF_WORLD) {
            entity.getActivePotionEffect(MobEffects.RESISTANCE)?.let {
            //    damage *= (25 - (it.amplifier + 1) * 5) / 25.0f
            //    damage *= max(1f - it.amplifier * 0.2f, 0f)
                damage *= max(1.0F - (it.amplifier + 1) * 0.2F, 0.0F)
            }
        }

        damage *= getProtectionModifier(entity, source)
        return if (roundDamage) round(damage) else damage
    }

    /**
     * @see CombatRules.getDamageAfterMagicAbsorb
     */
    private fun getProtectionModifier(entity: EntityLivingBase, damageSource: DamageSource): Float {
        var modifier = 0

        for (armor in entity.armorInventoryList.toList()) {
            if (armor.isEmpty) continue // Skip if item stack is empty
            val nbtTagList = armor.enchantmentTagList
            for (i in 0 until nbtTagList.tagCount()) {
                val compoundTag = nbtTagList.getCompoundTagAt(i)

                val id = compoundTag.getInteger("id")
                val level = compoundTag.getInteger("lvl")

                Enchantment.getEnchantmentByID(id)?.let {
                    modifier += it.calcModifierDamage(level, damageSource)
                }
            }
        }

        modifier = modifier.coerceIn(0, 20)
        return 1.0F - modifier / 25.0F
    }

    fun ItemStack.getEnchantmentLevel(enchantmentId: Int): Int {
        for (i in 0 until this.enchantmentTagList.tagCount()) {
            val id = this.enchantmentTagList.getCompoundTagAt(i).getInteger("id")
            if (id != enchantmentId) continue
            return this.enchantmentTagList.getCompoundTagAt(i).getInteger("lvl")
        }
        return 0
    }

    fun equipBestWeapon(preferWeapon: PreferWeapon = PreferWeapon.NONE, allowTool: Boolean = false) {
        Globals.mc.player.hotbarSlots.filterByStack {
            val item = it.item
            item is ItemSword || item is ItemAxe || allowTool && item is ItemTool
        }.maxByOrNull {
            val itemStack = it.stack
            val item = itemStack.item
            val damage = itemStack.attackDamage
            when {
                preferWeapon == PreferWeapon.SWORD && item is ItemSword -> damage * 10.0F
                preferWeapon == PreferWeapon.AXE && item is ItemAxe -> damage * 10.0F
                else -> damage
            }
        }?.let {
            swapToSlot(it)
        }
    }

    enum class PreferWeapon {
        SWORD, AXE, NONE
    }

}