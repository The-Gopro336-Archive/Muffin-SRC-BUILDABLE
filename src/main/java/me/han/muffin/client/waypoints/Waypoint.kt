package me.han.muffin.client.waypoints

import net.minecraft.util.math.Vec3d

class Waypoint(val displayName: String, var vector: Vec3d, val type: Type, val serverIP: String, val dimension: Int) {
    override fun toString(): String {
        return "$vector Type: $type"
    }

    val x: Double get() = vector.x
    val y: Double get() = vector.y
    val z: Double get() = vector.z

    enum class Type {
        Normal, Logout, Death, CoordTPExploit
    }

}