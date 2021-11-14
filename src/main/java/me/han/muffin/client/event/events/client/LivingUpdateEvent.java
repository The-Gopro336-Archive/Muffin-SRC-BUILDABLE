package me.han.muffin.client.event.events.client;

import me.han.muffin.client.event.EventCancellable;

public class LivingUpdateEvent extends EventCancellable {

    private final EventStage stage;

    public LivingUpdateEvent(EventStage stage) {
        this.stage = stage;
    }

    @Override
    public EventStage getStage() {
        return stage;
    }

}
