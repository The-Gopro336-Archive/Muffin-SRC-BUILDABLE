package me.han.muffin.client.value;

import java.util.function.Predicate;

public class BindValue<T extends Integer> extends Value<T> {

    public BindValue(T value, String... aliases) {
        super(value, aliases);
    }

    public BindValue(Predicate<T> visibility, T value, String... aliases) {
        super(visibility, value, aliases);
    }

    public BindValue(String[] aliases, T value) {
        super(aliases, value);
    }

    public BindValue(String[] aliases, T value, Predicate<T> visibility) {
        super(aliases, value, visibility);
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



    //  public void setValue(String value) {
  //       this.value = (T) (Integer) Keyboard.getKeyIndex(value);
  //  }

}