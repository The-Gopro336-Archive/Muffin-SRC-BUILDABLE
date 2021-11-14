package me.han.muffin.client.event.events.render.entity;

import me.han.muffin.client.event.EventCancellable;

public class RenderEnderCrystalEvent extends EventCancellable {

    private final EventStage stage;

    public RenderEnderCrystalEvent(EventStage stage) {
        this.stage = stage;
    }

    @Override
    public EventStage getStage() {
        return stage;
    }

}
