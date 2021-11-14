package me.han.muffin.client.utils.color

enum class DyeColours(val color: Colour) {
    BLACK(Colour(0, 0, 0)),
    RED(Colour(250, 32, 32)),
    GREEN(Colour(32, 250, 32)),
    BROWN(Colour(180, 100, 48)),
    BLUE(Colour(48, 48, 255)),
    PURPLE(Colour(137, 50, 184)),
    CYAN(Colour(64, 230, 250)),
    LIGHT_GRAY(Colour(160, 160, 160)),
    GRAY(Colour(80, 80, 80)),
    PINK(Colour(255, 128, 172)),
    LIME(Colour(132, 240, 32)),
    YELLOW(Colour(255, 232, 0)),
    LIGHT_BLUE(Colour(100, 160, 255)),
    MAGENTA(Colour(220, 64, 220)),
    ORANGE(Colour(255, 132, 32)),
    WHITE(Colour(255, 255, 255)),
    RAINBOW(Colour(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE));
}