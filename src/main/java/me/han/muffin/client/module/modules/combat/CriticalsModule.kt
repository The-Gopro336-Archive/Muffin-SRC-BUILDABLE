package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.AttackSyncEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.extensions.mixin.entity.isInWeb
import me.han.muffin.client.utils.extensions.mixin.netty.onGround
import me.han.muffin.client.utils.network.LagCompensator
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.CPacketPlayer
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.floor

object CriticalsModule : Module("Criticals", Category.COMBAT, "Hit criticals on every attacks.") {
    private val mode = EnumValue(Mode.Packet, "Mode")
    private val sync = Value(false, "Sync")
    private val delayValue = NumberValue({!sync.value},2, 0, 20, 1, "Delay")
    private val hurtTimeValue = NumberValue(10, 0, 10, 1, "HurtTime")
    private val swordOnly = Value(true, "SwordOnly")
    private val timer = Timer()

    private enum class Mode {
        Packet, Ncp, NoGround, Hop, TpHop, Jump, MiniJump
    }

    init {
        addSettings(mode, sync, delayValue, hurtTimeValue, swordOnly)
    }

    override fun getHudInfo(): String {
        return mode.fixedValue
    }

    override fun onEnable() {
        if (mode.value == Mode.NoGround) {
            Globals.mc.player.jump()
        }
    }

    @Listener
    private fun onAttack(event: AttackSyncEvent) {
        if (fullNullCheck()) return

        if (event.entity !is EntityLivingBase) return

        if (swordOnly.value && Globals.mc.player.heldItemMainhand.item !is ItemSword) {
            return
        }

        doCritical(event.entity)
    }

    fun doCritical(entity: Entity) {
        if (!Globals.mc.player.onGround || !Globals.mc.player.collidedVertically || Globals.mc.player.isOnLadder ||
            Globals.mc.player.isInWeb ||
            Globals.mc.player.isInWater ||
            Globals.mc.player.isInLava ||
            EntityUtil.isInWater(Globals.mc.player) ||
            Globals.mc.player.ridingEntity != null ||
            entity is EntityLivingBase && entity.hurtTime > hurtTimeValue.value) {
            return
        }

        val delay = if (sync.value) floor(20.0 - LagCompensator.tickRate).toInt() else delayValue.value

        if (!timer.passedTicks(delay)) return

        val x = Globals.mc.player.posX
        val y = Globals.mc.player.posY
        val z = Globals.mc.player.posZ

        when (mode.value) {
            Mode.Packet -> {
                Globals.mc.connection?.sendPacket(CPacketPlayer.Position(x, y + 0.0625, z, true))
                Globals.mc.connection?.sendPacket(CPacketPlayer.Position(x, y, z, false))
                Globals.mc.connection?.sendPacket(CPacketPlayer.Position(x, y + 1.1E-5, z, false))
                Globals.mc.connection?.sendPacket(CPacketPlayer.Position(x, y, z, false))
                Globals.mc.player.onCriticalHit(entity)
            }
            Mode.Ncp -> {
                Globals.mc.connection?.sendPacket(CPacketPlayer.Position(x, y + 0.11, z, false))
                Globals.mc.connection?.sendPacket(CPacketPlayer.Position(x, y + 0.1100013579, z, false))
                Globals.mc.connection?.sendPacket(CPacketPlayer.Position(x, y + 0.0000013579, z, false))
                Globals.mc.player.onCriticalHit(entity)
            }
            Mode.Hop -> {
                Globals.mc.player.motionY = 0.1
                Globals.mc.player.fallDistance = 0.1f
                Globals.mc.player.onGround = false
            }
            Mode.TpHop -> {
                Globals.mc.connection?.sendPacket(CPacketPlayer.Position(x, y + 0.02, z, false))
                Globals.mc.connection?.sendPacket(CPacketPlayer.Position(x, y + 0.01, z, false))
                Globals.mc.player.setPosition(x, y + 0.01, z)
            }

            Mode.Jump -> Globals.mc.player.motionY = 0.42
            Mode.MiniJump -> Globals.mc.player.motionY = 0.3425

        }

        timer.reset()
    }

    @Listener
    private fun onPacketSend(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE)
            return

        if (event.packet is CPacketPlayer && mode.value == Mode.NoGround)
            event.packet.onGround = false
    }

}