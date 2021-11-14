package me.han.muffin.client.utils.color;

import me.han.muffin.client.module.modules.other.ColorControl;

import java.awt.*;

public class BetterColour extends Color {

	public BetterColour(double red, double green, double blue) {
		super((int)clamp(red, 0, 255), (int)clamp(green, 0, 255), (int)clamp(blue, 0, 255));
	}
	
	public BetterColour(double red, double green, double blue, double alpha) {
		super((int)clamp(red, 0, 255), (int)clamp(green, 0, 255), (int)clamp(blue, 0, 255), (int)clamp(alpha, 0, 255));
	}
	
	public BetterColour(Color color) {
		super((int)clamp(color.getRed(), 0, 255), (int)clamp(color.getGreen(), 0, 255), (int)clamp(color.getBlue(), 0, 255));
	}
	
	public BetterColour(int hex) {
		super(hex);
	}
	
	@Override
	public final BetterColour brighter() {
		return new BetterColour(super.brighter().brighter().brighter().brighter());
	}
	
	@Override
	public final BetterColour darker() {
		return new BetterColour(super.darker().darker().darker().darker());
	}
	
	public final BetterColour brighter(int brighteningValue) {
		final Color color = this;
		Color newColor = color;
		for (int i = 0; i < brighteningValue; i++) {
			newColor = newColor.brighter();
		}
		return new BetterColour(newColor).setAlpha(color.getAlpha());
	}
	
	public final BetterColour darker(int darkeningValue) {
		final Color color = this;
		Color newColor = color;
		for (int i = 0; i < darkeningValue; i++) {
			newColor = newColor.darker();
		}
		return new BetterColour(newColor).setAlpha(color.getAlpha());
	}
	
	public final BetterColour addColoring(int value) {
		final Color color = this;
		return new BetterColour(color.getRed() + value, color.getGreen() + value, color.getBlue() + value, color.getAlpha());
	}
	
	public final BetterColour addColoring(int red, int green, int blue) {
		final Color color = this;
		return new BetterColour(color.getRed() + red, color.getGreen() + green, color.getBlue() + blue, color.getAlpha());
	}
	
	public final BetterColour setAlpha(int alpha) {
		alpha = (int) clamp(alpha, 0, 255);
		return new BetterColour(this.getRed(), this.getGreen(), this.getBlue(), alpha);
	}
	
	static final double clamp(double value, double min, double max) {
		return value > max ? max : value < min ? min : value;
	}
	
	public final static BetterColour getRainbow(double rainbowOffset) {
		final double speed = ((50 - ColorControl.INSTANCE.rainbowSpeed.getValue()) + 1) * 100;
		float hue = (float) ((System.currentTimeMillis() + (rainbowOffset * ColorControl.INSTANCE.rainbowWidth.getValue())) % speed);
		hue /= speed;
		return new BetterColour(Color.getHSBColor(hue, .6F, 1));
	}
	
    public final static BetterColour getHue(double value) {
        final float hue = (float) (1.0F - value / 360.0F);
        final int color = Color.HSBtoRGB(hue, 1.0F, 1.0F);
        return new BetterColour(new Color(color));
    }

	@Override
	public final String toString() {
		return String.format("[%s,%s,%s]", getRed(), getGreen(), getBlue());
	}
}