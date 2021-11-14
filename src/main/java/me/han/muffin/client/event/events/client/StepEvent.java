package me.han.muffin.client.event.events.client;

import me.han.muffin.client.event.EventCancellable;

public class StepEvent extends EventCancellable {
    private final EventStage stage;
    private float height;

    public StepEvent(EventStage stage ,float height) {
        this.stage = stage;
        this.height = height;
    }


    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    @Override
    public EventStage getStage() {
        return stage;
    }
}