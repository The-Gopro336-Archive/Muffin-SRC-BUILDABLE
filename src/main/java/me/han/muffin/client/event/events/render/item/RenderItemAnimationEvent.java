package me.han.muffin.client.event.events.render.item;

import me.han.muffin.client.event.EventCancellable;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

public class RenderItemAnimationEvent extends EventCancellable {
    private final ItemStack stack;
    private final EnumHand hand;

    private RenderItemAnimationEvent(ItemStack stack, EnumHand hand) {
        this.stack = stack;
        this.hand = hand;
    }

    public static class Transform extends RenderItemAnimationEvent {
        private final float ticks;

        public Transform(ItemStack stack, EnumHand hand, float ticks) {
            super(stack, hand);
            this.ticks = ticks;
        }

        public float getTicks() {
            return ticks;
        }

    }

    public static class Render extends RenderItemAnimationEvent {
        public Render(ItemStack stack, EnumHand hand) {
            super(stack, hand);

        }
    }

    public EnumHand getHand() {
        return hand;
    }

    public ItemStack getStack() {
        return stack;
    }

}