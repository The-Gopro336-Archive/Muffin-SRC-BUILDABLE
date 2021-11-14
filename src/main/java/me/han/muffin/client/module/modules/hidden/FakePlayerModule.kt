package me.han.muffin.client.module.modules.hidden

import com.mojang.authlib.GameProfile
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.world.WorldEvent
import me.han.muffin.client.module.Module
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Enchantments
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.item.ItemStack
import net.minecraft.potion.PotionEffect
import net.minecraft.world.GameType
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*

internal object FakePlayerModule : Module("FakePlayer", Category.HIDDEN, true) {
    private val fakePlayerIdList = ArrayList<Int>()
    private var fakePlayer: EntityOtherPlayerMP? = null

    private const val FAKE_ID = -695812592

    override fun onEnable() {
        if (fullNullCheck()) return

        fakePlayerIdList.clear()

        val tempPlayer = EntityOtherPlayerMP(Globals.mc.world, GameProfile(UUID.randomUUID(), "popbob")).apply {
            setGameType(GameType.SURVIVAL)
            copyLocationAndAnglesFrom(Globals.mc.player)
            rotationYawHead = Globals.mc.player.rotationYawHead

            inventory.copyInventory(Globals.mc.player.inventory)

            copyPotions(Globals.mc.player)
            addMaxArmor()
            addGappleEffects()

            health = Globals.mc.player.health
            absorptionAmount = Globals.mc.player.absorptionAmount
        }

        Globals.mc.world.addEntityToWorld(FAKE_ID, tempPlayer)
        fakePlayerIdList.add(FAKE_ID)
        fakePlayer = tempPlayer
    }

    @Listener
    private fun onWorldUnload(event: WorldEvent.Unload) {
        disable()
    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fakePlayerIdList.isEmpty()) disable()
        if (fakePlayer != null && Globals.mc.player.getDistance(fakePlayer!!) > 30) disable()
    }

    override fun onDisable() {
        if (fullNullCheck()) return
        for (id in fakePlayerIdList) {
            fakePlayer?.setDead()
            Globals.mc.world.removeEntityFromWorld(id)
        }
    }

    private fun EntityPlayer.copyPotions(otherPlayer: EntityPlayer) {
        for (potionEffect in otherPlayer.activePotionEffects) {
            addPotionEffectForce(PotionEffect(potionEffect.potion, Int.MAX_VALUE, potionEffect.amplifier))
        }
    }

    private fun EntityPlayer.addMaxArmor() {
        inventory.armorInventory[3] = ItemStack(Items.DIAMOND_HELMET).apply {
            addMaxEnchantment(Enchantments.PROTECTION)
            addMaxEnchantment(Enchantments.UNBREAKING)
            addMaxEnchantment(Enchantments.RESPIRATION)
            addMaxEnchantment(Enchantments.AQUA_AFFINITY)
            addMaxEnchantment(Enchantments.MENDING)
        }
        inventory.armorInventory[2] = ItemStack(Items.DIAMOND_CHESTPLATE).apply {
            addMaxEnchantment(Enchantments.PROTECTION)
            addMaxEnchantment(Enchantments.UNBREAKING)
            addMaxEnchantment(Enchantments.MENDING)
        }
        inventory.armorInventory[1] = ItemStack(Items.DIAMOND_LEGGINGS).apply {
            addMaxEnchantment(Enchantments.BLAST_PROTECTION)
            addMaxEnchantment(Enchantments.UNBREAKING)
            addMaxEnchantment(Enchantments.MENDING)
        }
        inventory.armorInventory[0] = ItemStack(Items.DIAMOND_BOOTS).apply {
            addMaxEnchantment(Enchantments.PROTECTION)
            addMaxEnchantment(Enchantments.FEATHER_FALLING)
            addMaxEnchantment(Enchantments.DEPTH_STRIDER)
            addMaxEnchantment(Enchantments.UNBREAKING)
            addMaxEnchantment(Enchantments.MENDING)
        }
    }

    private fun ItemStack.addMaxEnchantment(enchantment: Enchantment) {
        addEnchantment(enchantment, enchantment.maxLevel)
    }

    private fun EntityPlayer.addGappleEffects() {
        addPotionEffectForce(PotionEffect(MobEffects.REGENERATION, Int.MAX_VALUE, 1))
        addPotionEffectForce(PotionEffect(MobEffects.ABSORPTION, Int.MAX_VALUE, 3))
        addPotionEffectForce(PotionEffect(MobEffects.RESISTANCE, Int.MAX_VALUE, 0))
        addPotionEffectForce(PotionEffect(MobEffects.FIRE_RESISTANCE, Int.MAX_VALUE, 0))
    }

    private fun EntityPlayer.addPotionEffectForce(potionEffect: PotionEffect) {
        // forceRemovePotionEffect(potionEffect)
        addPotionEffect(potionEffect)
        potionEffect.potion.applyAttributesModifiersToEntity(this, this.attributeMap, potionEffect.amplifier)
    }

    private fun EntityPlayer.forceRemovePotionEffect(potionEffect: PotionEffect) {
        val effectRemove = activePotionMap.remove(potionEffect.potion) ?: return
        effectRemove.potion.removeAttributesModifiersFromEntity(this, attributeMap, effectRemove.amplifier)
    }

}