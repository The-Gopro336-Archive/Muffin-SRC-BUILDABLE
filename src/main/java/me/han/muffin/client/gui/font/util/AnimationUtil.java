package me.han.muffin.client.gui.font.util;

public class AnimationUtil {

    public static float calculateCompensation(float target, float current, long delta, double speed) {
        float diff = current - target;
        if (delta < 1) {
            delta = 1;
        }
        if (delta > 1000) {
            delta = 16;
        }
        if (diff > speed) {
            double xD = (Math.max(speed * delta / (1000 / 60), 0.5));
            current -= xD;
            if (current < target) {
                current = target;
            }
        } else if (diff < -speed) {
            double xD = (Math.max(speed * delta / (1000 / 60), 0.5));
            current += xD;
            if (current > target) {
                current = target;
            }
        } else {
            current = target;
        }
        return current;
    }

    /**
     * In-out-easing function
     * https://github.com/jesusgollonet/processing-penner-easing
     * @param t Current iteration
     * @param d Total iterations
     * @return Eased value
     */
    public static float easeOut(float t, float d) {
        return (t = t / d - 1) * t * t + 1;
    }

    public static double linear(double from, double to, double incline) {
        return from < to - incline ? from + incline : (from > to + incline ? from - incline : to);
    }

    public static double parabolic(double from, double to, double incline) {
        return from + (to - from) / incline;
    }

}