package me.han.muffin.client.event.events.world;

import me.han.muffin.client.event.EventCancellable;
import net.minecraft.entity.Entity;

public class WorldEntityEvent extends EventCancellable {

    public static class Add extends WorldEntityEvent {
        private final Entity entity;

        public Add(Entity entity) {
            this.entity = entity;
        }

        public Entity getEntity() {
            return entity;
        }
    }

    public static class Remove extends WorldEntityEvent {
        private final Entity entity;

        public Remove(Entity entity) {
            this.entity = entity;
        }

        public Entity getEntity() {
            return entity;
        }
    }

}
