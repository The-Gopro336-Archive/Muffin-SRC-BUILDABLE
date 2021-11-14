package me.han.muffin.client.value;

import java.util.function.Predicate;

public class StringValue<T extends String> extends Value<T> {

    public StringValue(T value, String... aliases) {
        super(value, aliases);
    }

    public StringValue(Predicate<T> visibility, T value, String... aliases) {
        super(visibility, value, aliases);
    }

    public StringValue(String[] aliases, T value) {
        super(aliases, value);
    }

    public StringValue(String[] aliases, T value, Predicate<T> visibility) {
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