package me.han.muffin.client.event.events.network;

import me.han.muffin.client.event.EventStageable;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.multiplayer.ServerData;

public class ServerEvent {
    private final ServerData serverData;

    ServerEvent(ServerData serverData) {
        this.serverData = serverData;
    }
    public final ServerData getServerData() {
        return this.serverData;
    }

    public static class Connect extends ServerEvent {
        private final State state;

        public Connect(State state, ServerData serverData) {
            super(serverData);
            this.state = state;
        }

        public final State getState() {
            return this.state;
        }
    }

    public enum State {
        /**
         * Called before the connection attempt
         */
        PRE,

        /**
         * Indicates that the connection attempt was successful
         */
        CONNECT,

        /**
         * Indicates that an exception occurred when trying to connect to the target server.
         * This will be followed by an instance of {@link ServerEvent.Disconnect} being posted.
         */
        FAILED
    }

    public static class Disconnect extends ServerEvent {

        /**
         * State of the event.
         * PRE if before the disconnect processing.
         * POST if disconnect processing has already been executed.
         */
        private final EventStageable.EventStage state;

        /**
         * Whether or not the connection was forcefully closed. True if the
         * server called for the client to be disconnected. False if the
         * client manually disconnected through {@link GuiIngameMenu}.
         */
        private final boolean forced;

        public Disconnect(EventStageable.EventStage state, boolean forced, ServerData serverData) {
            super(serverData);
            this.state = state;
            this.forced = forced;
        }

        /**
         * @return The state of the event
         */
        public EventStageable.EventStage getState() {
            return state;
        }

        /**
         * @return Whether or not the connection was forcefully closed
         */
        public boolean isForced() {
            return this.forced;
        }
    }

}