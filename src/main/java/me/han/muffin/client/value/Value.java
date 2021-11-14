package me.han.muffin.client.value;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.client.SettingEvent;
import me.han.muffin.client.module.Module;

import java.util.function.Predicate;

public class Value<T> {

    private final String[] aliases;
    protected T value;
    protected T defaultValue;
    protected T plannedValue;
    private Predicate<T> visibility;
    Module module;
    public ValueListeners listeners;

    public Value(T value, String... aliases) {
        this.value = value;
        this.defaultValue = value;
        this.plannedValue = value;
        this.aliases = aliases;
    }

    public Value(Predicate<T> visibility, T value, String... aliases) {
        this.visibility = visibility;
        this.value = value;
        this.defaultValue = value;
        this.plannedValue = value;
        this.aliases = aliases;
    }

    public Value(String[] aliases, T value) {
        this.value = value;
        this.defaultValue = value;
        this.plannedValue = value;
        this.aliases = aliases;
    }

    public Value(String[] aliases, T value, Predicate<T> visibility) {
        this.value = value;
        this.defaultValue = value;
        this.plannedValue = value;
        this.aliases = aliases;
        this.visibility = visibility;
    }

    public Value(String aliases, T value) {
        this.value = value;
        this.defaultValue = value;
        this.plannedValue = value;
        this.aliases = new String[] { aliases };
    }

    public String[] getAliases() {
        return aliases;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        final SettingEvent event = new SettingEvent(this);
        Muffin.getInstance().getEventManager().dispatchEvent(event);

        this.value = value;
        if (listeners != null) listeners.onValueChange(this);
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    public void setListeners(ValueListeners listeners) {
        this.listeners = listeners;
    }

    public void setVisibility(Predicate<T> visibility) {
        this.visibility = visibility;
    }

    public boolean isVisible() {
        if (this.visibility == null) {
            return true;
        }
        return this.visibility.test(this.getValue());
    }

}