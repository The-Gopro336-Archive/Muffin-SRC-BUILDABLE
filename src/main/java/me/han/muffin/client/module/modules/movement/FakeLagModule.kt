package me.han.muffin.client.module.modules.movement

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.manager.managers.PotionManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.color.ColourUtils
import me.han.muffin.client.utils.extensions.kotlin.synchronized
import me.han.muffin.client.utils.extensions.mixin.netty.x
import me.han.muffin.client.utils.extensions.mixin.netty.y
import me.han.muffin.client.utils.extensions.mixin.netty.z
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.utils.render.drawBuffer
import me.han.muffin.client.utils.render.pos
import me.han.muffin.client.utils.render.withVertexFormat
import me.han.muffin.client.utils.threading.onMainThread
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.network.Packet
import net.minecraft.network.play.client.CPacketConfirmTeleport
import net.minecraft.network.play.client.CPacketKeepAlive
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketVehicleMove
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

internal object FakeLagModule : Module("FakeLag", Category.MOVEMENT, "Abuses an exploit to save up packets and teleport you.") {
    private val mode = EnumValue(Mode.Blink, "Mode")

    private val pulseDelayValue = NumberValue({ mode.value == Mode.Pulse },1000, 500, 5000, 50, "PulseDelay")
    private val autoReset = NumberValue({ mode.value == Mode.Blink },20, 0, 100, 5, "AutoReset")
    private val entityBlink = Value({ mode.value == Mode.Blink },true, "EntityBlink")
    private val factor = NumberValue({ mode.value == Mode.Blink },1.0, 1.0, 10.0, 0.1, "Factor")
    private val render = Value(true, "Render")
    private val noPackets = Value(false, "NoPackets")

    private val copyInventory = Value({ render.value },true, "Inventory")
    private val effects = Value({ render.value }, true, "Effects")

    private val packets = LinkedBlockingQueue<Packet<*>>()
    private var fakePlayer: EntityOtherPlayerMP? = null
    private var disableLogger = false
    private val positions = LinkedList<Vec3d>().synchronized()
    private val pulseTimer = Timer()

    private const val ENTITY_ID = -69132

    private enum class Mode {
        Blink, Pulse
    }

    init {
        addSettings(mode, autoReset, pulseDelayValue, entityBlink, factor, render, noPackets, copyInventory, effects)
    }

    override fun getHudInfo() = packets.size.toString()

    override fun onEnable() {
        if (fullNullCheck()) return
        prepare()
    }

    override fun onDisable() {
        if (fullNullCheck()) return
        blink()
    }

    @Listener
    private fun onDisconnect(event: ServerEvent.Disconnect) {
        if (event.state != EventStageable.EventStage.PRE) return
        disable()
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE) return
        if (fullNullCheck() || disableLogger) return

        // Cancel all movement stuff
        if (event.packet is CPacketPlayer || event.packet is CPacketConfirmTeleport || event.packet is CPacketKeepAlive || (entityBlink.value && event.packet is CPacketVehicleMove)) {
            packets.add(event.packet)
            event.cancel()
        }

    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (event.packet is SPacketPlayerPosLook) {
            event.cancel()
        }

    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (mode.value == Mode.Blink && autoReset.value > 0 && packets.size >= autoReset.value) {
            blink()
            prepare()
        }


        positions.add(Vec3d(Globals.mc.player.posX, Globals.mc.player.entityBoundingBox.minY, Globals.mc.player.posZ))

        if (mode.value == Mode.Pulse && pulseTimer.passed(pulseDelayValue.value.toDouble())) {
            blink()
            pulseTimer.reset()
        }

    }

    @Listener
    private fun onPlayerDisconnect(event: ServerEvent.Disconnect) {
        if (event.state != EventStageable.EventStage.PRE) return
        packets.clear()
        fakePlayer = null
        positions.clear()
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        synchronized(positions) {
            val clientColour = ColourUtils.getClientColour(255)
            val renderOffset = Vec3d(RenderUtils.renderPosX, RenderUtils.renderPosY, RenderUtils.renderPosZ)

            RenderUtils.prepareGL3D()
            Globals.mc.entityRenderer.disableLightmap()

            GL11.GL_LINE_STRIP withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
                for (pos in positions) {
                    val renderPos = pos.subtract(renderOffset)
                    pos(renderPos.x, renderPos.y, renderPos.z, colour = clientColour)
                }
            }

            RenderUtils.releaseGL3D()
        }

    }

    private fun prepare() {
        if (mode.value == Mode.Blink && render.value) {
            fakePlayer = EntityOtherPlayerMP(Globals.mc.world, Globals.mc.player.gameProfile).apply {
                copyLocationAndAnglesFrom(Globals.mc.player)
                rotationYawHead = Globals.mc.player.rotationYawHead
                if (copyInventory.value) inventory.copyInventory(Globals.mc.player.inventory)
                if (effects.value) for (potionEffect in PotionManager.ownPotions) addPotionEffect(potionEffect)
                noClip = true
            }
            Globals.mc.world.addEntityToWorld(ENTITY_ID, fakePlayer!!)
        }

        positions.add(Vec3d(Globals.mc.player.posX, Globals.mc.player.entityBoundingBox.minY + Globals.mc.player.getEyeHeight() / 2, Globals.mc.player.posZ))
        positions.add(Vec3d(Globals.mc.player.posX, Globals.mc.player.entityBoundingBox.minY, Globals.mc.player.posZ))

        pulseTimer.reset()
    }

    private fun blink() {
        onMainThread {
            if (Globals.mc.player == null) return@onMainThread

            if (noPackets.value || Globals.mc.connection == null) {
                var i = 1.0
                while (i <= factor.value) {
                    packets.peek()?.let {
                        if (it is CPacketPlayer) {
                            Globals.mc.player.setPosition(it.x, it.y, it.z)
                        }
                    }
                    i += 0.5
                }
                packets.clear()
            } else {
                try {
                    disableLogger = true
                    while (packets.isNotEmpty()) {
                        var i = 1.0
                        while (i <= factor.value) {
                            Globals.mc.player.connection.sendPacket(packets.take())
                            i += 1.0
                        }
                    }
                    disableLogger = false
                } catch (e: Exception) {
                    disableLogger = false
                }
            }


            positions.clear()

            fakePlayer?.let {
                it.setDead()
                Globals.mc.world.removeEntityFromWorld(it.entityId)
                fakePlayer = null
            }

        }

    }

}