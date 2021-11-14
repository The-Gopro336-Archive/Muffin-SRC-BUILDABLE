package me.han.muffin.client.waypoints

import com.google.gson.GsonBuilder
import com.mojang.authlib.GameProfile
import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.utils.InfoUtils
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.extensions.kotlin.synchronized
import me.han.muffin.client.utils.math.VectorUtils.distanceTo
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.Vec3d
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.*
import kotlin.concurrent.thread

object WaypointManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val waypoints = arrayListOf<Waypoint>()
    private val playerCache = hashMapOf<String, EntityPlayer>().synchronized()
    private val logoutCache = hashMapOf<String, PlayerData>().synchronized()

    private val waypointFile = File(Muffin.getInstance().getDirectory(), "waypoints.json")

    private var playerCacheThread: Thread? = null

    init {
        Muffin.getInstance().eventManager.addEventListener(this)

        with(waypointFile) {
            if (!exists()) createNewFile()
        }

        val cacheArray = FileReader(waypointFile).buffered().use {
            gson.fromJson(it, Array<Waypoint>::class.java)
        }

        waypoints.clear()
        waypoints.addAll(cacheArray)
    }

    fun addWaypoint(type: Waypoint.Type, name: String, pos: Vec3d, dimension: Int) {
        val newWaypoint = Waypoint(name, pos, type, InfoUtils.getServerIP() ?: "Singleplayer", dimension)
        waypoints.add(newWaypoint)
        doSave()
    }

    fun removeWaypoint(name: String?): Boolean {
        if (waypoints.isEmpty()) return false

        if (name == null) {
            waypoints.removeAt(waypoints.size - 1)
            return true
        }

        val waypointToRemove = getWaypointByName(name)?.let {
            waypoints.remove(it)
            doSave()
        }

        return waypointToRemove != null
    }

    fun editWaypoint(name: String, vector: Vec3d): Boolean {
        val pointToEdit = getWaypointByName(name)?.let {
            it.vector = vector
            doSave()
        }
        return pointToEdit != null
    }

    fun getWaypointByName(name: String): Waypoint? {
        return waypoints.firstOrNull { it.displayName == name }
    }

    private fun doSave() {
        try {
            with(waypointFile) {
                if (!exists()) createNewFile()
            }

            FileWriter(waypointFile).buffered().use {
                gson.toJson(waypoints, it)

                it.flush()
                it.close()
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (EntityUtil.fullNullCheck()) return

        updatePlayerCache()
    }

    private fun updatePlayerCache() {
        if (playerCacheThread == null || !playerCacheThread!!.isAlive || playerCacheThread!!.isInterrupted) {
            playerCacheThread = thread {
                for (player in Globals.mc.world.playerEntities) {
                    if (player == null || player == Globals.mc.player) continue
                    playerCache[player.gameProfile.id.toString()] = player
                }
            }
        }
    }

    private fun hasPlayerLogged(uuid: String): Boolean {
        return logoutCache.containsKey(uuid)
    }

    fun isOutOfRange(data: PlayerData): Boolean {
        return Globals.mc.player.distanceTo(data.vector) > 200
    }

    fun removeLogoutCache(uuid: String) {
        logoutCache.remove(uuid)
        thread { EntityUtil.getNameFromUUID(uuid)?.let { removeWaypoint(it) } }
    }

    class PlayerData(var vector: Vec3d, var profile: GameProfile)

}