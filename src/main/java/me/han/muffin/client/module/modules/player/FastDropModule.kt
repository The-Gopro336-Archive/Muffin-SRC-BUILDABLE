package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.NumberValue
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object FastDropModule: Module("FastDrop", Category.PLAYER, "Drop items faster.") {
    private val delay = NumberValue(10, 0, 50, 1, "Delay")

    private val timer = Timer()

    init {
        addSettings(delay)
    }

    override fun getHudInfo(): String {
        return "Normal"
    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (!Globals.mc.gameSettings.keyBindDrop.isKeyDown) return

        if (timer.passedTicks(delay.value)) {
            Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.DROP_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            //     Globals.mc.player.dropItem(Globals.mc.player.getHeldItemMainhand().getItem(), 1);
            timer.reset()
        }
    }

    /*
    @Listener
    private fun onPacketSend(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.POST) return
        if (event.getPacket() is CPacketPlayerDigging) {
            val packet = event.getPacket() as CPacketPlayerDigging
            if (packet.action == CPacketPlayerDigging.Action.DROP_ITEM) {
            }
        }
    }
     */

}