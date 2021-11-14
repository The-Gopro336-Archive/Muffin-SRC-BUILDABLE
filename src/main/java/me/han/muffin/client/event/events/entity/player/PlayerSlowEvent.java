package me.han.muffin.client.event.events.entity.player;

import me.han.muffin.client.event.EventCancellable;

public class PlayerSlowEvent extends EventCancellable {

    public static class Attack extends PlayerSlowEvent {
        private double factor;
        public boolean isSprinting;

        public Attack(double factor) {
            this.factor = factor;
            this.isSprinting = false;
        }

        public boolean isSprinting() {
            return isSprinting;
        }

        public double getFactor() {
            return factor;
        }

        public void setFactor(double factor) {
            this.factor = factor;
        }

    }

    public static class ActiveHand extends PlayerSlowEvent {
        private float factor;

        public ActiveHand(float factor) {
            this.factor = factor;
        }

        public float getFactor() {
            return factor;
        }

        public void setFactor(float factor) {
            this.factor = factor;
        }

    }

}
