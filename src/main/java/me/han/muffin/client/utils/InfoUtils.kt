package me.han.muffin.client.utils

import me.han.muffin.client.core.Globals
import me.han.muffin.client.utils.math.Direction
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.network.LagCompensator
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.MathHelper
import java.text.SimpleDateFormat
import java.util.*

object InfoUtils {

    @JvmStatic fun getServerType() = if (Globals.mc.isIntegratedServerRunning) "Singleplayer" else Globals.mc.currentServerData?.serverIP ?: "MainMenu"
    @JvmStatic fun getServerBrand() = if (Globals.mc.currentServerData == null) "Vanilla" else Globals.mc.player.let { Globals.mc.player.serverBrand } ?: "Vanilla"

    @JvmStatic fun ping(player: EntityPlayer = Globals.mc.player) = player.let { Globals.mc.connection?.getPlayerInfo(it.uniqueID)?.responseTime ?: 1 }

    @JvmStatic fun time() = SimpleDateFormat("hh:mm a").format(Date()) ?: "00:00"
    @JvmStatic fun fps() = Minecraft.getDebugFPS()

    fun getServerIP(): String? =
        Globals.mc.currentServerData?.serverIP ?: if (Globals.mc.isIntegratedServerRunning) "Singleplayer" else null

    fun durability() = with(Globals.mc.player.heldItemMainhand) { maxDamage - itemDamage }
    fun memory() = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576L
    fun tps(places: Int) = MathUtils.round(LagCompensator.tickRate, places)

    fun dimension() = when (Globals.mc.player?.dimension) {
        -1 -> "Nether" to -1
        0 -> "Overworld" to 0
        1 -> "End" to 1
        else -> "No Dimension" to -2
    }

    fun getDirection(yaw: Float = Globals.mc.player.rotationYaw): Direction {
        when (MathHelper.floor((yaw * 8.0F / 360.0F).toDouble() + 0.5) and 7) {
            0 -> return Direction.SOUTH
            1 -> return Direction.SOUTH_WEST
            2 -> return Direction.WEST
            3 -> return Direction.NORTH_WEST
            4 -> return Direction.NORTH
            5 -> return Direction.NORTH_EAST
            6 -> return Direction.EAST
            7 -> return Direction.SOUTH_EAST
        }
        return Direction.NORTH
    }


}