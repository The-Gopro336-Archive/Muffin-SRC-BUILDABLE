package me.han.muffin.client.event.events.client;

import me.han.muffin.client.event.EventCancellable;

public class UpdateEvent extends EventCancellable {

    private final EventStage stage;

    public UpdateEvent(EventStage stage) {
        this.stage = stage;
    }

    @Override
    public EventStage getStage() {
        return this.stage;
    }

}