package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.math.rotation.Vec2f
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import net.minecraft.block.BlockAir
import net.minecraft.block.BlockLiquid
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.multiplayer.GuiConnecting
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.server.SPacketDisconnect
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextComponentString
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object NoFallModule: Module("NoFall", Category.PLAYER, "Prevents fall damage.") {
    private val mode = EnumValue(Mode.Packet, "Mode")

    private enum class Mode {
        Packet, NCP, AAC, Anti, Bucket, Disconnect, Reconnect
    }

    private val lastElytraFlyTimer = Timer()

    init {
        addSettings(mode)
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (event.packet is CPacketEntityAction) {
            if (event.packet.action == CPacketEntityAction.Action.START_FALL_FLYING) {
                lastElytraFlyTimer.reset()
            }
        }
    }


    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck() && mode.value != Mode.Reconnect) return

        if (Globals.mc.player.isElytraFlying || !lastElytraFlyTimer.passedSeconds(1)) return

        when (mode.value) {
            Mode.Disconnect -> {
                if (Globals.mc.player.fallDistance > 3 && BlockPos(Globals.mc.player.posX, Globals.mc.player.posY - 4.0, Globals.mc.player.posZ).block !is BlockAir) {
                    //          Globals.mc.world.sendQuittingDisconnectingPacket();
                    Globals.mc.player.connection.handleDisconnect(SPacketDisconnect(TextComponentString("disconnect because NoFall mod")))
                    disable()
                }
            }

            Mode.Reconnect -> {
                if (Globals.mc.player.fallDistance > 3 && BlockPos(Globals.mc.player.posX, Globals.mc.player.posY - 4.0, Globals.mc.player.posZ).block !is BlockAir) {
                    if (Globals.mc.currentServerData == null) return
                    val serverData = Globals.mc.currentServerData
                    //Globals.mc.world.sendQuittingDisconnectingPacket()
                    Globals.mc.player.connection.handleDisconnect(SPacketDisconnect(TextComponentString("reconnect because NoFall mod")))
                    Globals.mc.loadWorld(null)
                    if (serverData != null) Globals.mc.displayGuiScreen(GuiConnecting(GuiMultiplayer(GuiMainMenu()), Globals.mc, serverData))
                    disable()
                }
            }

            Mode.Packet -> {
                if (Globals.mc.player.fallDistance > 3.0f) {
              //      event.cancel()
                    event.location.isOnGround = true
                }
            }

            Mode.Bucket -> {
                if (Globals.mc.player.fallDistance > 3.0f) {
                    var selectedPosition: BlockPos? = null

                    for (i in Globals.mc.player.posY.toInt() downTo Globals.mc.player.posY.toInt() - 5 + 1) {
                        val pos = BlockPos(Globals.mc.player.posX.toInt(), i, Globals.mc.player.posZ.toInt())
                        if (Globals.mc.world.isAirBlock(pos)) continue
                        if (pos.block is BlockLiquid) continue
                        selectedPosition = pos
                        break
                    }

                    if (selectedPosition != null) {
                        var hasBucket = false
                        if (Globals.mc.player.heldItemMainhand.item != Items.WATER_BUCKET) {
                            val slot = InventoryUtils.findItem(Items.WATER_BUCKET)
                            if (slot != -1) {
                                hasBucket = true
                                InventoryUtils.swapSlot(slot)
                            }
                        } else hasBucket = true

                        if (hasBucket) {
                            addMotion { rotate(Vec2f(Globals.mc.player.rotationYaw, 90.0F)) }
                            Globals.mc.player.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(selectedPosition, EnumFacing.UP, EnumHand.MAIN_HAND, 0F, 0F, 0F))
                            Globals.mc.player.connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
                        }
                    }

                }
            }

            Mode.AAC -> {
                if (Globals.mc.player.fallDistance > 3.0f) {
                    Globals.mc.player.fallDistance = 0.0f
                    Globals.mc.player.onGround = true
                    Globals.mc.player.capabilities.isFlying = true
                    Globals.mc.player.capabilities.allowFlying = true
                    addMotion { noGround() }
                    Globals.mc.player.velocityChanged = true
                    Globals.mc.player.capabilities.isFlying = false
                    Globals.mc.player.jump()
                } else {
                    addMotion { ground() }
                    Globals.mc.player.capabilities.isFlying = false
                    Globals.mc.player.capabilities.allowFlying = false
                }
            }

            Mode.Anti -> {
                if (Globals.mc.player.fallDistance > 3.0f) Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + 1, Globals.mc.player.posZ, true))
            }

            Mode.NCP -> {
                if (Globals.mc.player.fallDistance > 1.5F) {
                    addMotion { ground(Globals.mc.player.ticksExisted % 2 == 0) }
                }
            }

        }

    }


}