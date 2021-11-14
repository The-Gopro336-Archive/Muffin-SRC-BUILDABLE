package me.han.muffin.client.event.events.render.item;

import me.han.muffin.client.event.EventCancellable;

public class RenderItemInFirstPersonEvent extends EventCancellable {
    private final float partialTicks;
    private float width;
    private float height;


    private RenderItemInFirstPersonEvent(float partialTicks, float width, float height) {
        this.partialTicks = partialTicks;
        this.width = width;
        this.height = height;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public static class MainHand extends RenderItemInFirstPersonEvent {

        public MainHand(float partialTicks, float width, float height) {
            super(partialTicks, width, height);
        }

    }

    public static class OffHand extends RenderItemInFirstPersonEvent {

        public OffHand(float partialTicks, float width, float height) {
            super(partialTicks, width, height);
        }

    }

}