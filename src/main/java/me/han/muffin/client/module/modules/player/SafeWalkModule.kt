package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.module.Module
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener


object SafeWalkModule: Module("SafeWalk", Category.PLAYER, "Stop all movement when you reach on the edge of block.") {

    @Listener
    private fun onMoving(event: MoveEvent) {
        if (fullNullCheck()) return

        var x = event.x
        val y = event.y
        var z = event.z

        if (Globals.mc.player.onGround && !Globals.mc.player.noClip) {
            val increment = 0.05
            while (x != 0.0 && isOffsetBBEmpty(x, -1.0, 0.0)) {
                if (x < increment && x >= -increment) {
                    x = 0.0
                } else if (x > 0.0) {
                    x -= increment
                } else {
                    x += increment
                }
            }

            while (z != 0.0 && isOffsetBBEmpty(0.0, -1.0, z)) {
                if (z < increment && z >= -increment) {
                    z = 0.0
                } else if (z > 0.0) {
                    z -= increment
                } else {
                    z += increment
                }
            }
            while (x != 0.0 && z != 0.0 && isOffsetBBEmpty(x, -1.0, z)) {
                if (x < increment && x >= -increment) {
                    x = 0.0
                } else if (x > 0.0) {
                    x -= increment
                } else {
                    x += increment
                }
                if (z < increment && z >= -increment) {
                    z = 0.0
                } else if (z > 0.0) {
                    z -= increment
                } else {
                    z += increment
                }
            }
        }

        event.x = x
        event.y = y
        event.z = z
    }

    private fun isOffsetBBEmpty(x: Double, y: Double, z: Double): Boolean {
        return Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(x, y, z)).isEmpty()
    }

}