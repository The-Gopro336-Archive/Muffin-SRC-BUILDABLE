package me.han.muffin.client.event.events.render

import me.han.muffin.client.event.EventCancellable
import net.minecraft.util.math.Vec3d

class GetSkyColourEvent: EventCancellable() {
    var vec3d: Vec3d = Vec3d.ZERO
}