package me.han.muffin.client.event.events.render;

public class TextEvent {

    private String text;

    public TextEvent(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public final TextEvent setText(String text) {
        this.text = text;
        return this;
    }

}