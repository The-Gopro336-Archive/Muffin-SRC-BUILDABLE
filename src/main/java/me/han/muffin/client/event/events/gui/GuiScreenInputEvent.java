package me.han.muffin.client.event.events.gui;

import me.han.muffin.client.event.EventCancellable;
import me.han.muffin.client.event.EventStageable;

public class GuiScreenInputEvent extends EventCancellable {
    public static class KeyboardInput extends GuiScreenInputEvent {
        private final EventStageable.EventStage stage;
        public KeyboardInput(EventStageable.EventStage stage) {
            this.stage = stage;
        }
        public EventStageable.EventStage getStage() {
            return stage;
        }
    }
}
