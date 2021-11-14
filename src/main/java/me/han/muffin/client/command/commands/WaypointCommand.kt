package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.core.Globals
import me.han.muffin.client.manager.managers.CommandManager
import me.han.muffin.client.utils.InfoUtils
import me.han.muffin.client.utils.extensions.mc.utils.toStringFormat
import me.han.muffin.client.utils.math.VectorUtils.toBlockPos
import me.han.muffin.client.utils.math.VectorUtils.toVec3d
import me.han.muffin.client.waypoints.Waypoint
import me.han.muffin.client.waypoints.WaypointManager
import net.minecraft.util.math.BlockPos

object WaypointCommand {
    val COORDS_PATTERN = Regex("[0-9-]+,[0-9-]+,[0-9-]+")

    object WaypointAdd: Command(arrayOf("waypoint", "wp", "wpoint"), Argument("add"), Argument("name"), Argument("x"), Argument("y"), Argument("z")) {
        override fun dispatch(): String {
            val (dimensionName, id) = InfoUtils.dimension()

            val nameArgument = getArgument("name")?.value ?:
            return confirm("Unnamed", Globals.mc.player.positionVector.toBlockPos())
                .also { WaypointManager.addWaypoint(Waypoint.Type.Normal, "Unnamed", Globals.mc.player.positionVector, id) }

            val posX = getArgument("x")?.value ?: return "Invalid x argument"
            val posY = getArgument("y")?.value ?: return "Invalid y argument"
            val posZ = getArgument("z")?.value ?: return "Invalid z argument"

            if (!posX.matches(COORDS_PATTERN) || !posY.matches(COORDS_PATTERN) || !posZ.matches(COORDS_PATTERN)) {
                return "You have to enter custom coordinates in the format of '&7x,y,z&f', for example '&7${CommandManager.prefix}waypoint add \"My Waypoint\" 400,60,-100&f', but you can also leave it blank to use the current coordinates"
            }

            val coordinate = BlockPos(posX.toInt(), posY.toInt(), posZ.toInt())
            WaypointManager.addWaypoint(Waypoint.Type.Normal, nameArgument, coordinate.toVec3d(), id)
            return confirm(nameArgument, coordinate)
        }
    }

    object WaypointRemove: Command(arrayOf("waypoint", "wp", "wpoint"), Argument("delete/remove/del"), Argument("name")) {
        override fun dispatch(): String {
            val action = getArgument("delete/remove/del")?.value ?: return "Invalid action arguments"
            val name = getArgument("name")?.value ?: return "Invalid name arguments"

            if (action.equals("delete", ignoreCase = true) || action.equals("remove", ignoreCase = true) || action.equals("del", ignoreCase = true)) {
                if (WaypointManager.removeWaypoint(name)) return "Successfully removed the waypoint named $name"
                return "Fail to remove $name."
            }
            return "Should not be here."
        }
    }

    object WaypointEdit: Command(arrayOf("waypoint", "wp", "wpoint"), Argument("edit"), Argument("name"), Argument("x"), Argument("y"), Argument("z")) {
        override fun dispatch(): String {
            val name = getArgument("name")?.value ?: return "Invalid name arguments"

            val posX = getArgument("x")?.value ?: return "Invalid x argument"
            val posY = getArgument("y")?.value ?: return "Invalid y argument"
            val posZ = getArgument("z")?.value ?: return "Invalid z argument"


            if (!posX.matches(COORDS_PATTERN) || !posY.matches(COORDS_PATTERN) || !posZ.matches(COORDS_PATTERN)) {
                return "You have to enter custom coordinates in the format of '&7x,y,z&f', for example '&7${CommandManager.prefix}waypoint add \"My Waypoint\" 400,60,-100&f', but you can also leave it blank to use the current coordinates"
            }

            val coordinate = BlockPos(posX.toInt(), posY.toInt(), posZ.toInt())
            if (WaypointManager.editWaypoint(name, coordinate.toVec3d())) return "Successfully edited the waypoint named $name"
            return "Fail to edit $name."
        }
    }

    private fun confirm(name: String, pos: BlockPos): String {
        val (dimensionName, id) = InfoUtils.dimension()
        return "Added waypoint at ${pos.toStringFormat()} in the $dimensionName with name '&7$name&f'."
    }

    /*
    private var confirmTime = 0L

    object WaypointAdd: Command(arrayOf("waypoint", "wp", "wpoint"), Argument("add"), Argument("name"), Argument("x"), Argument("y"), Argument("z")) {
        override fun dispatch(): String {
            if (getArgument("name") != null) {
                if (getArgument("x") != null && getArgument("y") != null && getArgument("z") != null) {
                    if (!getArgument("x").value!!.matches(Regex("[0-9-]+,[0-9-]+,[0-9-]+")) || !getArgument("y").value!!.matches(Regex("[0-9-]+,[0-9-]+,[0-9-]+")) || !getArgument("z").value!!.matches(Regex("[0-9-]+,[0-9-]+,[0-9-]+"))) {
                        return "You have to enter custom coordinates in the format of '&7x,y,z&f', for example '&7${getCommandPrefix()}waypoint add \"My Waypoint\" 400,60,-100&f', but you can also leave it blank to use the current coordinates"
                    }
                    val coordinate = BlockPos(getArgument("x").value.toInt(), getArgument("y").value.toInt(), getArgument("z").value.toInt())
                    return confirm(getArgument("name").value, WaypointManager.add(coordinate, getArgument("name").value).pos)
                } else {
                    return confirm(getArgument("name").value, WaypointManager.add(getArgument("name").value).pos)
                }
            } else {
                return confirm("Unnamed", WaypointManager.add("Unnamed").pos)
            }
        }
    }

    object WaypointRemove: Command(arrayOf("waypoint", "wp", "wpoint"), Argument("delete/remove/del"), Argument("name")) {
        override fun dispatch(): String {
            return delete(getArgument("name").value)
        }
    }



    private fun listWaypoints(stashes: Boolean) {
        val waypoints = WaypointManager.waypoints
        if (waypoints.isEmpty()) {
            if (!stashes) {
                Command.sendChatMessage("No waypoints have been saved.")
            } else {
                Command.sendChatMessage("No stashes have been logged.")
            }
        } else {
            if (!stashes) {
                Command.sendChatMessage("List of waypoints:")
            } else {
                Command.sendChatMessage("List of logged stashes:")
            }
            val stashRegex = Regex("(\\(.* chests, .* shulkers, .* droppers, .* dispensers\\))")
            for (waypoint in WaypointManager.waypoints) {
                if (stashes) {
                    if (waypoint.name.matches(stashRegex)) {
                        Command.sendRawChatMessage(format(waypoint, ""))
                    }
                } else {
                    if (!waypoint.name.matches(stashRegex)) {
                        Command.sendRawChatMessage(format(waypoint, ""))
                    }
                }
            }
        }
    }

    private fun searchWaypoints(search: String) {
        var found = false
        var first = true

        for (waypoint in WaypointManager.waypoints) {
            if (waypoint.name.contains(search)) {
                if (first) {
                    Command.sendChatMessage("Result of search for &7$search&f: ")
                    first = false
                }
                Command.sendRawChatMessage(format(waypoint, search))
                found = true
            }
        }
        if (!found) {
            Command.sendChatMessage("No results for &7$search&f")
        }
    }

    private fun delete(args: Array<out String?>): String {
        return if (args[1] != null) {
            if (WaypointManager.remove(args[1]!!)) {
                "Removed waypoint with ID " + args[1]
            } else {
                "No waypoint with ID " + args[1]
            }
        } else {
            "You must provide a waypoint ID delete a waypoint. Use '&7${Command.getCommandPrefix()}wp list&f' to list saved waypoints and their IDs"
        }
    }

    private fun format(waypoint: WaypointManager.Waypoint, search: String): String {
        val message = "${formattedID(waypoint.id)} [${waypoint.server}] ${waypoint.name} (${bothConverted(waypoint.dimension, waypoint.pos)})"
        return message.replace(search.toRegex(), "&7$search&f")
    }

    private fun formattedID(id: Int): String { // massive meme to format the spaces for the width of id lmao
        return " ".repeat((5 - id.toString().length).coerceAtLeast(0)) + "[$id]"
    }

    private fun confirm(name: String, pos: BlockPos) = "Added waypoint at ${pos.asString()} in the ${InfoUtils.dimension()} with name '&7$name&f'."
     */

}