package me.han.muffin.client.module.modules.render

import me.han.muffin.client.module.Module

object WaypointModule: Module("Waypoint", Category.RENDER) {
    /*
    val normal = Value("Normal", arrayOf("Normal"), "Displays normal waypoints", true)
    val logoutSpots = Value("LogoutSpots", arrayOf("LogoutSpots"), "Displays players LogoutSpots", true)
    val deathPoints = Value("DeathPoints", arrayOf("DeathPoints"), "Displays players DeathPoints", true)
    val coordTPExploit = Value("CoordTPExploit", arrayOf("CoordTPExploit"), "Displays waypoints created by CoordTPExploit", true)
    val tracers = Value("Tracers", arrayOf("Tracers"), "Points tracers to each waypoint", false)
    val removeDistance = Value("RemoveDistance", arrayOf("RD", "RemoveRange"), "Minimum distance in blocks the player must be away from the spot for it to be removed.", 200, 1, 2000, 1)
     */
}

/*
import me.han.muffin.client.command.Command
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.client.WaypointUpdateEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.gui.font.TextComponent
import me.han.muffin.client.manager.managers.WaypointManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.other.RenderModeModule
import me.han.muffin.client.utils.timer.TimerUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import me.han.muffin.client.value.ValueListeners
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*
import java.util.function.Predicate

object WaypointModule: Module("Waypoint", Category.RENDER) {
    private val dimension = EnumValue(Dimension.Current, "Dimension")
    private val showName = Value(true,"Name")
    private val showDate = Value(false,"Date")
    private val showCoords = Value(true,"Coords")
    private val showDist = Value(true,"Distance")
    private val lines = Value(false,"Lines")
    private val thickness = NumberValue(Predicate { lines.value },2.0F, 0.1F, 10.0F, 0.1F, "LineThickness")
    private val limitDistance = Value(false, "LimitDistance")
    private val maxDistance = NumberValue(Predicate { limitDistance.value }, 5000, 1000, 17000, 5,"MaxDistance")

    private val scale = NumberValue(1.0F, 0.0F, 2.0F, 0.1F,"Scaling")


    // This has to be sorted so the further ones doesn't overlaps the closer ones
    private val waypointMap = TreeMap<BlockPos, TextComponent>(compareByDescending {
        it.distanceSq(
            Globals.mc.player?.position
            ?: BlockPos(0, -69420, 0))
    })
    private var currentServer: String? = null
    private var timer = TimerUtils.TickTimer(TimerUtils.TimeUnit.SECONDS)
    private var prevDimension = -2
    private val lockObject = Any()

    private enum class Dimension {
        Current, All
    }

    init {
        addSettings(dimension, showName, showDate, showCoords, showDist, lines, thickness, limitDistance, maxDistance, scale)

        dimension.listeners = object : ValueListeners {
            override fun onValueChange(value: Value<*>?) {
                synchronized(lockObject) { waypointMap.clear(); updateList() } // This could be called from another thread so we have to synchronize the map
            }
        }

        showName.listeners = object : ValueListeners {
            override fun onValueChange(value: Value<*>?) {
                synchronized(lockObject) { waypointMap.clear(); updateList() } // This could be called from another thread so we have to synchronize the map
            }
        }

        showDate.listeners = object : ValueListeners {
            override fun onValueChange(value: Value<*>?) {
                synchronized(lockObject) { waypointMap.clear(); updateList() } // This could be called from another thread so we have to synchronize the map
            }
        }

        showCoords.listeners = object : ValueListeners {
            override fun onValueChange(value: Value<*>?) {
                synchronized(lockObject) { waypointMap.clear(); updateList() } // This could be called from another thread so we have to synchronize the map
            }
        }

        showDist.listeners = object : ValueListeners {
            override fun onValueChange(value: Value<*>?) {
                synchronized(lockObject) { waypointMap.clear(); updateList() } // This could be called from another thread so we have to synchronize the map
            }
        }
    }

    override fun onEnable() {
        timer.reset(-10000L) // Update the map immediately and thread safely
    }

    override fun onDisable() {
        currentServer = null
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (WaypointManager.genDimension() != prevDimension || timer.tick(10L, false)) {
            if (WaypointManager.genDimension() != prevDimension) waypointMap.clear()
            updateList()
        }
    }

    @Listener
    private fun onWaypointUpdate(event: WaypointUpdateEvent) {
        // This could be called from another thread so we have to synchronize the map
        synchronized(lockObject) {
            when (event.type) {
                WaypointUpdateEvent.Type.ADD -> event.waypoint?.let { updateTextComponent(it) }
                WaypointUpdateEvent.Type.REMOVE -> waypointMap.remove(event.waypoint?.pos)
                WaypointUpdateEvent.Type.CLEAR -> waypointMap.clear()
                WaypointUpdateEvent.Type.RELOAD -> {
                    waypointMap.clear(); updateList()
                }
                else -> {
                }
            }
        }
    }

    @Listener
    private fun onDisconnect(event: ServerEvent.Disconnect) {
        if (event.state != EventStageable.EventStage.PRE) return
        currentServer = null
    }

    private fun updateList() {
        timer.reset()
        prevDimension = WaypointManager.genDimension()
        if (currentServer == null) {
            waypointMap.clear()
            currentServer = WaypointManager.genServer()
        }

        val cacheList = WaypointManager.waypoints.filter { (it.server == null || it.server == currentServer) && (dimension.value == Dimension.All || it.dimension == prevDimension) }

        waypointMap.keys.removeIf { pos -> cacheList.firstOrNull { it.pos == pos } != null }

        for (waypoint in cacheList) updateTextComponent(waypoint)
    }

    private fun updateTextComponent(waypoint: WaypointManager.Waypoint) {
        // Don't wanna update this continuously
        waypointMap.computeIfAbsent(waypoint.pos) {
            TextComponent().apply {
                if (showName.value) addLine(waypoint.name)
                if (showDate.value) addLine(waypoint.date)
                if (showCoords.value) addLine(waypoint.toString())
            }
        }
    }

}
 */