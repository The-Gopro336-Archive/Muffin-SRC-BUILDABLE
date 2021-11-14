package me.han.muffin.client.event.events.client

import me.han.muffin.client.event.EventCancellable
import me.han.muffin.client.utils.Location
import me.han.muffin.client.utils.math.rotation.Vec2f

class MotionUpdateEvent(private val stage: EventStage, var location: Location, var rotation: Vec2f, var prevRotation: Vec2f): EventCancellable() {
    var rotating = false
    override fun getStage(): EventStage {
        return stage
    }
}