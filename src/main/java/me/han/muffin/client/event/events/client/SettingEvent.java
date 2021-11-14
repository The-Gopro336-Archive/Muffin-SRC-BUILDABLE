package me.han.muffin.client.event.events.client;

import me.han.muffin.client.module.Module;
import me.han.muffin.client.value.Value;

public class SettingEvent {

    private Module module;
    private Value value;

    public SettingEvent(final Module module) {
        this.module = module;
    }

    public SettingEvent(final Value value) {
        this.value = value;
    }

    public Module getModule() {
        return module;
    }

    public Value getValue() {
        return value;
    }

}