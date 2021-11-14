package me.han.muffin.client.event.events.gui;

import me.han.muffin.client.event.EventCancellable;
import net.minecraft.client.gui.GuiScreen;

public class GuiScreenEvent extends EventCancellable {

    private GuiScreen screen;

    public GuiScreenEvent(GuiScreen screen) {
        super();
        this.screen = screen;
    }

    public GuiScreen getScreen() {
        return screen;
    }

    public void setScreen(GuiScreen screen) {
        this.screen = screen;
    }

    public static class Displayed extends GuiScreenEvent {
        public Displayed(GuiScreen screen) {
            super(screen);
        }
    }

    public static class Closed extends GuiScreenEvent {
        public Closed(GuiScreen screen) {
            super(screen);
        }
    }

}