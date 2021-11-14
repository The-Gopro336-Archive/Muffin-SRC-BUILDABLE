package me.han.muffin.client.value;

import me.han.muffin.client.utils.color.BetterColour;

import java.util.function.Predicate;

public class ColourValue<T extends BetterColour> extends Value<T> {

    public ColourValue(T value, String... aliases) {
        super(value, aliases);
    }

    public ColourValue(Predicate<T> visibility, T value, String... aliases) {
        super(visibility, value, aliases);
    }

    public ColourValue(String[] aliases, T value) {
        super(aliases, value);
    }

    public ColourValue(String[] aliases, T value, Predicate<T> visibility) {
        super(aliases, value, visibility);
    }

    @Override
    public void setValue(T value) {
        super.setValue(value);
    }

    @Override
    public T getValue() {
        return super.getValue();
    }

    @Override
    public boolean isVisible() {
        return super.isVisible();
    }

    @Override
    public void setVisibility(Predicate<T> visibility) {
        super.setVisibility(visibility);
    }

}