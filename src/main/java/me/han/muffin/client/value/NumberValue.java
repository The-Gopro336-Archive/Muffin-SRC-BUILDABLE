package me.han.muffin.client.value;

import java.util.function.Predicate;

public class NumberValue<T extends Number> extends Value<T> {

    private final T minimum, maximum, increment;
    private boolean clamp;

    public NumberValue(T value, T minimum, T maximum, T increment, String... aliases) {
        super(value, aliases);
        clamp = true;
        this.minimum = minimum;
        this.maximum = maximum;
        this.increment = increment;
    }

    public NumberValue(Predicate<T> visibility, T value, T minimum, T maximum, T increment, String... aliases) {
        super(visibility, value, aliases);
        clamp = true;
        this.minimum = minimum;
        this.maximum = maximum;
        this.increment = increment;
    }

    public NumberValue(String[] aliases, T value, T minimum, T maximum, T increment) {
        super(aliases, value);
        clamp = true;
        this.minimum = minimum;
        this.maximum = maximum;
        this.increment = increment;
    }

    public NumberValue(String[] aliases, T value, T minimum, T maximum, T increment, Predicate<T> visibility) {
        super(aliases, value, visibility);
        clamp = true;
        this.minimum = minimum;
        this.maximum = maximum;
        this.increment = increment;
    }

    public NumberValue(T value, String... aliases) {
        super(value, aliases);
        clamp = false;
        this.minimum = maximum = increment = null;
    }

    public T getIncrement() {
        return increment;
    }

    public T getMaximum() {
        return maximum;
    }

    public T getMinimum() {
        return minimum;
    }

    public void setClamp(boolean clamp) {
        this.clamp = clamp;
    }

    @Override
    public T getValue() {
        return super.getValue();
    }

    @Override
    public void setValue(T value) {
        if (clamp) {
            if (value instanceof Integer) {
                if (value.intValue() > maximum.intValue()) {
                    value = maximum;
                } else if (value.intValue() < minimum.intValue()) {
                    value = minimum;
                }
            } else if (value instanceof Float) {
                if (value.floatValue() > maximum.floatValue()) {
                    value = maximum;
                } else if (value.floatValue() < minimum.floatValue()) {
                    value = minimum;
                }
            } else if (value instanceof Double) {
                if (value.doubleValue() > maximum.doubleValue()) {
                    value = maximum;
                } else if (value.doubleValue() < minimum.doubleValue()) {
                    value = minimum;
                }
            } else if (value instanceof Long) {
                if (value.longValue() > maximum.longValue()) {
                    value = maximum;
                } else if (value.longValue() < minimum.longValue()) {
                    value = minimum;
                }
            } else if (value instanceof Short) {
                if (value.shortValue() > maximum.shortValue()) {
                    value = maximum;
                } else if (value.shortValue() < minimum.shortValue()) {
                    value = minimum;
                }
            } else if (value instanceof Byte) {
                if (value.byteValue() > maximum.byteValue()) {
                    value = maximum;
                } else if (value.byteValue() < minimum.byteValue()) {
                    value = minimum;
                }
            }
        }
        super.setValue(value);
    }


    public void setValueConfig(T value) {
        super.setValue(value);
    }

    @Override
    public void setVisibility(Predicate<T> visibility) {
        super.setVisibility(visibility);
    }

    @Override
    public boolean isVisible() {
        return super.isVisible();
    }

}