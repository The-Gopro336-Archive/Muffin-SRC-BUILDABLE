package me.han.muffin.client.event.events.client;

import me.han.muffin.client.event.EventCancellable;

public class SendChatMessageEvent extends EventCancellable {
    private String message;

    public SendChatMessageEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}