package me.han.muffin.client.event.events.world;

import net.minecraft.client.multiplayer.WorldClient;

public class WorldEvent {

    private WorldEvent() {}

    /**
     * Called when the world is loaded
     */
    public static class Load {

        /**
         * World being loaded
         */
        private final WorldClient world;

        public Load(WorldClient world) {
            this.world = world;
        }

        /**
         * @return The world being loaded
         */
        public WorldClient getWorld() {
            return this.world;
        }
    }

    /**
     * Called when the world is unloaded
     */
    public static class Unload {

    }

}

