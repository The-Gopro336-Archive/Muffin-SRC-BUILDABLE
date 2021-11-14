package me.han.muffin.client.event.events.render;

import me.han.muffin.client.event.EventCancellable;

public class RenderPutColorMultiplierEvent extends EventCancellable {

    private float opacity;

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public float getOpacity() {
        return opacity;
    }

}
