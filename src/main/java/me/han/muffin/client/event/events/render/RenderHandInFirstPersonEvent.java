package me.han.muffin.client.event.events.render;

import me.han.muffin.client.event.EventCancellable;

public class RenderHandInFirstPersonEvent extends EventCancellable {
    private float x;
    private float y;
    private float z;

    public RenderHandInFirstPersonEvent(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setX(float x) {
        this.x = x;
    }

}