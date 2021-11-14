package me.han.muffin.client.event.events.world;

import me.han.muffin.client.event.EventCancellable;

public class ClientItemSpawnEvent extends EventCancellable {

    private final int x;
    private final int y;
    private final int z;

    public ClientItemSpawnEvent(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int[] getCoordinate() {
        return new int[]{this.x, this.y, this.z};
    }

}
