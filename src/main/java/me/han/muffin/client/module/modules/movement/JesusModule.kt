package me.han.muffin.client.module.modules.movement

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.entity.HandleJumpWaterEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.world.WorldClientInitEvent
import me.han.muffin.client.event.events.world.block.BlockCollisionBoundingBoxEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.extensions.mixin.netty.packetY
import me.han.muffin.client.utils.extensions.mixin.netty.y
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.Value
import net.minecraft.block.BlockLiquid
import net.minecraft.entity.item.EntityBoat
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketVehicleMove
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.math.AxisAlignedBB
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object JesusModule: Module("Jesus", Category.MOVEMENT, "Allows you to walk on water.") {

    private val mode = EnumValue(Mode.Solid, "Mode")
    private val boost = Value(true, "Boost")

    private var wasWater = false
    private var ticks = 0
    private var boostTicks = 0

    private val timer = Timer()

    private val motionArr = doubleArrayOf(
        0.5,
        0.46879999999995,
        0.43759999999989996,
        0.40639999999984994,
        0.3751999999997999,
        0.3439999999997499,
        0.3127999999996999,
        0.28159999999964985,
        0.25039999999959983,
        0.21919999999954984,
        0.18799999999949987,
        0.15679999999944988,
        0.1255999999993999,
        0.09439999999934989,
        0.0631999999992999,
        0.0319999999992499,
        7.999999991998991E-4,
        -0.06240000000010001,
        -0.09360000000015001,
        -0.12480000000020003,
        -0.15600000000025005,
        -0.18720000000030004,
        -0.21840000000035,
        -0.24960000000039997,
        -0.32300800628701304,
        -0.3949478538480405,
        -0.4654489058299911,
        -0.5345399381170035,
        -0.6022491510760825,
        -0.6686041810674306
    )


    private enum class Mode {
        Solid, Dolphin, Trampoline
    }

    init {
        addSettings(mode, boost) //prevY, bigger, equals, negative)
    }

    override fun getHudInfo(): String? = mode.fixedValue

    /*
        @Listener
        private fun onPlayerUpdate(event: UpdateEvent) {
            if (event.stage != EventStageable.EventStage.PRE) return

            if (fullNullCheck()) return


            if (mode.value == Mode.Dolphin) {
                if (!Globals.mc.player.movementInput.sneak && !Globals.mc.player.movementInput.jump && PlayerUtil.isInLiquid()) {
                    Globals.mc.player.motionY = 0.131
                }
            } else if (mode.value == Mode.Trampoline) {
                if (!Globals.mc.player.onGround) {
                    if (ticks > 0 && ticks <= motionArr.size) {
                        Globals.mc.player.motionY = motionArr[ticks - 1]
                        ++ticks
                        return
                    }
                    if (Globals.mc.player.isInWater || Globals.mc.player.isInLava) {
                        Globals.mc.player.motionY = 0.1
                        wasWater = true
                        ticks = 0
                    } else {
                        if (wasWater) {
                            Globals.mc.player.motionY = 0.5
                            ++ticks
                        }
                        wasWater = false
                    }
                } else {
                    ticks = 0
                }
            } else if (mode.value == Mode.Solid) {
                if (!Globals.mc.player.movementInput.sneak && !Globals.mc.player.movementInput.jump && PlayerUtil.isInLiquid()) {
                    Globals.mc.player.motionY = 0.08//0.1
                }

                if (PlayerUtil.isOnLiquid() && canJesus() && boost.value) {
                    when (boostTicks) {
                        0 -> {
                            Globals.mc.player.motionX *= 1.1
                            Globals.mc.player.motionZ *= 1.1
                        }
                        1 -> {
                            Globals.mc.player.motionX *= 1.27
                            Globals.mc.player.motionZ *= 1.27
                        }
                        2 -> {
                            Globals.mc.player.motionX *= 1.51
                            Globals.mc.player.motionZ *= 1.51
                        }
                        3 -> {
                            Globals.mc.player.motionX *= 1.15
                            Globals.mc.player.motionZ *= 1.15
                        }
                        4 -> {
                            Globals.mc.player.motionX *= 1.23
                            Globals.mc.player.motionZ *= 1.23
                        }
                    }
                    ++boostTicks
                    if (boostTicks > 4) {
                        boostTicks = 0
                    }
                }
            }

        }

     */

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (event.packet is SPacketPlayerPosLook) {
            boostTicks = 0
        }

    }


    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (mode.value == Mode.Dolphin) {
            if (!Globals.mc.player.movementInput.sneak && !Globals.mc.player.movementInput.jump && EntityUtil.isInWater(Globals.mc.player)) {
                Globals.mc.player.motionY = 0.131
            }
        } else if (mode.value == Mode.Trampoline) {
            if (!Globals.mc.player.onGround) {
                if (ticks > 0 && ticks <= motionArr.size) {
                    Globals.mc.player.motionY = motionArr[ticks - 1]
                    ++ticks
                    return
                }
                if (Globals.mc.player.isInWater || Globals.mc.player.isInLava) {
                    Globals.mc.player.motionY = 0.1
                    wasWater = true
                    ticks = 0
                } else {
                    if (wasWater) {
                        Globals.mc.player.motionY = 0.5
                        ++ticks
                    }
                    wasWater = false
                }
            } else {
                ticks = 0
            }
        } else if (mode.value == Mode.Solid) {
            if (!Globals.mc.player.movementInput.sneak && !Globals.mc.player.movementInput.jump && EntityUtil.isInWater(Globals.mc.player)) {
                Globals.mc.player.motionY = 0.085 //0.1
                Globals.mc.player.ridingEntity?.let {
                    if (it !is EntityBoat) {
                        it.motionY = 0.3
                    }
                }
            }

            if (boost.value && EntityUtil.isAboveWater(Globals.mc.player) && canJesus()) {
                when (boostTicks) {
                    0 -> {
                        Globals.mc.player.motionX *= 1.1
                        Globals.mc.player.motionZ *= 1.1
                    }
                    1 -> {
                        Globals.mc.player.motionX *= 1.27
                        Globals.mc.player.motionZ *= 1.27
                    }
                    2 -> {
                        Globals.mc.player.motionX *= 1.51
                        Globals.mc.player.motionZ *= 1.51
                    }
                    3 -> {
                        Globals.mc.player.motionX *= 1.15
                        Globals.mc.player.motionZ *= 1.15
                    }
                    4 -> {
                        Globals.mc.player.motionX *= 1.23
                        Globals.mc.player.motionZ *= 1.23
                    }
                }
                ++boostTicks
                if (boostTicks > 4) {
                    boostTicks = 0
                }
            }
        }

    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (mode.value != Mode.Solid) return

        if (!Globals.mc.player.isRiding && event.packet is CPacketPlayer) {
            if (canJesus() && EntityUtil.isAboveWater(Globals.mc.player)) {

                if (!MovementUtils.isMoving()) {
                    event.cancel()
                    return
                }

                if (Globals.mc.player.ticksExisted % 2 == 0) {
                    event.packet.y -= 1.0E-6
                } else {
                    event.packet.y += 1.0E-6
                }
            }
        } else if (event.packet is CPacketVehicleMove) {
            Globals.mc.player.ridingEntity?.let {
                if (canJesus() && EntityUtil.isAboveWater(it)) {
                    if (it.ticksExisted % 2 == 0) {
                        event.packet.packetY -= 0.05
                    } else {
                        event.packet.packetY += 0.05
                    }
                }
            }
        }
    }

    @Listener
    private fun onHandleJumpWater(event: HandleJumpWaterEvent) {
        if (fullNullCheck()) return

        if ((Globals.mc.player.isInWater || Globals.mc.player.isInLava) && (Globals.mc.player.motionY == 0.1 || Globals.mc.player.motionY == 0.5)) event.cancel()
    }

    @Listener
    private fun onWorldLoad(event: WorldClientInitEvent) {
        timer.reset()
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (!timer.passed(800.0)) return

        if (mode.value == Mode.Solid) {
            if (Globals.mc.player.fallDistance > 3.0f) return

            if ((Globals.mc.player.isInLava || Globals.mc.player.isInWater) && !Globals.mc.player.isSneaking) {
                Globals.mc.player.motionY = 0.1
                return
            }
            if (EntityUtil.isInWater(Globals.mc.player) && !Globals.mc.player.isSneaking) {
                Globals.mc.player.motionY = 0.1
            }
        }
    }

    @Listener
    private fun onBlockCollisionBoundingBox(event: BlockCollisionBoundingBoxEvent) {
        if (fullNullCheck() || mode.value == Mode.Dolphin || EntityUtil.isInWater(Globals.mc.player)) return

        if (mode.value == Mode.Solid && !EntityUtil.isAboveWater(Globals.mc.player)) return

        if (event.pos.block is BlockLiquid && !Globals.mc.player.isSneaking && Globals.mc.player.fallDistance < 3.0F && !Globals.mc.player.isRowingBoat) {
            val pos = event.pos
//            event.boundingBox = Block.FULL_BLOCK_AABB
            if (Globals.mc.player.ridingEntity != null) {
                event.boundingBox = AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1 - 0.05, 1.0)
            } else if (mode.value == Mode.Solid) {
                event.boundingBox = AxisAlignedBB(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), pos.x + 1.0, pos.y + if (Globals.mc.player.movementInput.jump) 0.95 else 0.99, pos.z + 1.0)
            }
            event.cancel()
        }
    }

    private fun canJesus(): Boolean {
        return Globals.mc.player.fallDistance < 3.0F && !Globals.mc.player.movementInput.jump && !EntityUtil.isInWater(Globals.mc.player) && !Globals.mc.player.isSneaking
    }


}