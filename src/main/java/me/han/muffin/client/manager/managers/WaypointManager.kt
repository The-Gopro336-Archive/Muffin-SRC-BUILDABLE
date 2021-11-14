package me.han.muffin.client.manager.managers
/*
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import me.han.muffin.client.Muffin
import me.han.muffin.client.config.utils.ConfigUtils
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.client.WaypointUpdateEvent
import me.han.muffin.client.utils.math.CoordinateConverter
import me.han.muffin.client.utils.math.VectorUtils.toBlockPos
import net.minecraft.util.math.BlockPos
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.LinkedHashSet

object WaypointManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private const val oldConfigName = "KAMIBlueCoords.json" /* maintain backwards compat with old format */
    private const val configName = "KAMIBlueWaypoints.json"
    private val oldFile = File(oldConfigName)
    val file = File(configName)
    private val sdf = SimpleDateFormat("HH:mm:ss dd/MM/yyyy")
    private val mainThread = Thread.currentThread()

    /**
     * LinkedHashSet for all waypoints
     * Since LinkedHashSet isn't thread safe, getting this from another thread will
     * returns a read only copy
     */
    var waypoints = LinkedHashSet<Waypoint>()
        get() {
            return if (Thread.currentThread() != mainThread) {
                synchronized(field) { LinkedHashSet(field) }
            } else field
        }

    /**
     * Reads waypoints from KAMIBlueWaypoints.json into the waypoints ArrayList
     */
    fun loadWaypoints(): Boolean {
        /* backwards compatibility for older configs */
        val localFile = if (legacyFormat()) oldFile else file
        val success = try {
            waypoints = gson.fromJson(FileReader(localFile), object : TypeToken<LinkedHashSet<Waypoint>?>() {}.type)
            Muffin.LOGGER.info("Waypoint loaded")
            if (legacyFormat()) oldFile.delete()
            true
        } catch (e: FileNotFoundException) {
            Muffin.LOGGER.warn("Could not find file $configName, clearing the waypoints list")
            waypoints.clear()
            false
        } catch (e: IllegalStateException) {
            Muffin.LOGGER.warn("$configName is empty!")
            waypoints.clear()
            false
        }
        Muffin.getInstance().eventManager.dispatchEvent(WaypointUpdateEvent(WaypointUpdateEvent.Type.CLEAR, null))
        return success
    }

    /**
     * Saves waypoints from the waypoints ArrayList into KAMIBlueWaypoints.json
     */
    fun saveWaypoints(): Boolean {
        return try {
            val fileWriter = FileWriter(file, false)
            gson.toJson(waypoints, fileWriter)
            fileWriter.flush()
            fileWriter.close()
            Muffin.LOGGER.info("Waypoint saved")
            true
        } catch (e: IOException) {
            Muffin.LOGGER.info("Failed saving waypoint")
            e.printStackTrace()
            false
        }
    }

    /**
     * file deletion does not work on OSX, issue #1044
     * because of this, we must also check if they've used the new format
     */
    private fun legacyFormat(): Boolean {
        return oldFile.exists() && !file.exists()
    }

    fun get(id: String): Waypoint? {
        val waypoint = waypoints.firstOrNull { it.id.toString() == id }
        Muffin.getInstance().eventManager.dispatchEvent(WaypointUpdateEvent(WaypointUpdateEvent.Type.GET, waypoint))
        return waypoint
    }

    fun get(pos: BlockPos, currentDimension: Boolean = false): Waypoint? {
        val waypoint = waypoints.firstOrNull { (if (currentDimension) it.currentPos() else it.pos) == pos }
        Muffin.getInstance().eventManager.dispatchEvent(WaypointUpdateEvent(WaypointUpdateEvent.Type.GET, waypoint))
        return waypoint
    }

    fun add(locationName: String): Waypoint {
        val pos = Globals.mc.player?.positionVector?.toBlockPos()
        return if (pos != null) {
            val waypoint = add(pos, locationName)
            Muffin.getInstance().eventManager.dispatchEvent(WaypointUpdateEvent(WaypointUpdateEvent.Type.ADD, waypoint))
            waypoint
        } else {
            Muffin.LOGGER.error("Error during waypoint adding")
            dateFormatter(BlockPos(0, 0, 0), locationName) // This shouldn't happen
        }
    }

    fun add(pos: BlockPos, locationName: String): Waypoint {
        val waypoint = dateFormatter(pos, locationName)
        waypoints.add(waypoint)
        Muffin.getInstance().eventManager.dispatchEvent(WaypointUpdateEvent(WaypointUpdateEvent.Type.ADD, waypoint))
        return waypoint
    }

    fun remove(pos: BlockPos, currentDimension: Boolean = false): Boolean {
        val waypoint = get(pos, currentDimension)
        val removed = waypoints.remove(waypoint)
        Muffin.getInstance().eventManager.dispatchEvent(WaypointUpdateEvent(WaypointUpdateEvent.Type.REMOVE, waypoint))
        return removed
    }

    fun remove(id: String): Boolean {
        val waypoint = get(id) ?: return false
        val removed = waypoints.remove(waypoint)
        Muffin.getInstance().eventManager.dispatchEvent(WaypointUpdateEvent(WaypointUpdateEvent.Type.REMOVE, waypoint))
        return removed
    }

    fun clear() {
        waypoints.clear()
        Muffin.getInstance().eventManager.dispatchEvent(WaypointUpdateEvent(WaypointUpdateEvent.Type.CLEAR, null))
    }

    fun genServer(): String? {
        return Globals.mc.currentServerData?.serverIP
            ?: if (Globals.mc.isIntegratedServerRunning) "Singleplayer"
            else null
    }

    fun genDimension(): Int {
        return Globals.mc.player?.dimension ?: -2 /* this shouldn't ever happen at all */
    }

    private fun dateFormatter(pos: BlockPos, locationName: String): Waypoint {
        val date = sdf.format(Date())
        return Waypoint(pos, locationName, date)
    }

    init {
        ConfigUtils.fixEmptyJson(file)
    }

    class Waypoint(
        @SerializedName("position")
        val pos: BlockPos,

        @SerializedName("name")
        val name: String,

        @SerializedName("time") // NEEDS to stay "time" to maintain backwards compat
        val date: String
    ) {

        @SerializedName("id")
        val id: Int = genID()

        @SerializedName("server")
        val server: String? = genServer() /* can be null from old configs */

        @SerializedName("dimension")
        val dimension: Int = genDimension()

        fun currentPos() = CoordinateConverter.toCurrent(dimension, pos)

        private fun genID(): Int = waypoints.lastOrNull()?.id?.plus(1) ?: 0

        override fun toString() = currentPos().let { "${it.x}, ${it.y}, ${it.z}" }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Waypoint) return false

            if (pos != other.pos) return false
            if (name != other.name) return false
            if (date != other.date) return false
            if (id != other.id) return false
            if (server != other.server) return false
            if (dimension != other.dimension) return false

            return true
        }

        override fun hashCode(): Int {
            var result = pos.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + date.hashCode()
            result = 31 * result + id
            result = 31 * result + (server?.hashCode() ?: 0)
            result = 31 * result + dimension
            return result
        }
    }

 */