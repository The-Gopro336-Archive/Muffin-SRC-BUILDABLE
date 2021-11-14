package me.han.muffin.client.event.events.network;

import me.han.muffin.client.event.EventCancellable;
import net.minecraft.network.Packet;

public class PacketEvent extends EventCancellable {
    public final Packet<?> packet;

    private PacketEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public static class Send extends PacketEvent {
        private final EventStage stage;
        public Send(Packet<?> packet, EventStage stage) {
            super(packet);
            this.stage = stage;
        }

        @Override
        public EventStage getStage() {
            return stage;
        }

    }

    public static class Receive extends PacketEvent {
        private final EventStage stage;

        public Receive(Packet<?> packet, EventStage stage) {
            super(packet);
            this.stage = stage;
        }

        @Override
        public EventStage getStage() {
            return stage;
        }

    }

    public Packet<?> getPacket() {
        return packet;
    }

}