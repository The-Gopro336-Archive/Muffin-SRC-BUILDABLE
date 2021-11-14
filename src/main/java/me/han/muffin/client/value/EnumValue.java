package me.han.muffin.client.value;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.client.SettingEvent;

import java.util.function.Predicate;

public class EnumValue<T extends Enum> extends Value<T> {

    public EnumValue(T value, String... aliases) {
        super(value, aliases);
    }

    public EnumValue(String[] aliases, T value) {
        super(aliases, value);
    }

    public EnumValue(Predicate<T> visibility, T value, String... aliases) {
        super(visibility, value, aliases);
    }

    public EnumValue(String[] aliases, T value, Predicate<T> visibility) {
        super(aliases, value, visibility);
    }

    public String getFixedValue() {
        return this.value.name().charAt(0) + this.value.name().toLowerCase().replaceFirst(Character.toString(this.value.name().charAt(0)).toLowerCase(), "");
    }

    public void setEnumValue(String value) {
        final SettingEvent event = new SettingEvent(this);
        Muffin.getInstance().getEventManager().dispatchEvent(event);

        /*
        Enum[] array;
        for (int length = (array = getValue().getClass().getEnumConstants()).length, i = 0; i < length; i++) {
            if (array[i].name().equalsIgnoreCase(value)) {
                this.value = (T) array[i];
            }
        }
         */

        for (Enum<?> e : this.value.getClass().getEnumConstants()) {
            if (e.name().equalsIgnoreCase(value)) {
                this.value = (T) e;
            }
        }
        if (listeners != null) listeners.onValueChange(this);
    }

    public void increment() {
        Enum[] array;
        for (int length = (array = getValue().getClass().getEnumConstants()).length, i = 0; i < length; i++) {
            if (array[i].name().equalsIgnoreCase(getFixedValue())) {
                i++;
                if (i > array.length - 1) {
                    i = 0;
                }
                setEnumValue(array[i].toString());
            }
        }
    }

    public void decrement() {
        Enum[] array;
        for (int length = (array = getValue().getClass().getEnumConstants()).length, i = 0; i < length; i++) {
            if (array[i].name().equalsIgnoreCase(getFixedValue())) {
                i--;
                if (i < 0) {
                    i = array.length - 1;
                }
                setEnumValue(array[i].toString());
            }
        }
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