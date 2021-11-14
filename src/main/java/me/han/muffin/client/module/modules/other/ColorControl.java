package me.han.muffin.client.module.modules.other;

import com.mojang.realmsclient.gui.ChatFormatting;
import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.client.TickEvent;
import me.han.muffin.client.event.events.client.UpdateEvent;
import me.han.muffin.client.manager.managers.GuiManager;
import me.han.muffin.client.module.Module;
import me.han.muffin.client.utils.render.BlockRenderer;
import me.han.muffin.client.value.EnumValue;
import me.han.muffin.client.value.NumberValue;
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener;

public class ColorControl extends Module {

    private final EnumValue<TextColor> textColor = new EnumValue<>(TextColor.AQUA, "TextColor");
    public EnumValue<ModuleListMode> moduleListMode = new EnumValue<>(ModuleListMode.STATIC, "ColorMode");

    public NumberValue<Integer> red = new NumberValue<>(v -> moduleListMode.getValue() != ModuleListMode.RAINBOW,90, 0, 255, 1, "Red");
    public NumberValue<Integer> green = new NumberValue<>(v -> moduleListMode.getValue() != ModuleListMode.RAINBOW,0, 0, 255, 1, "Green");
    public NumberValue<Integer> blue = new NumberValue<>(v -> moduleListMode.getValue() != ModuleListMode.RAINBOW,255, 0, 255, 1, "Blue");

    public NumberValue<Float> rainbowSpeed = new NumberValue<>(v -> moduleListMode.getValue() != ModuleListMode.STATIC,3f, 1f, 15f, 0.05f, "RainbowSpeed");
    public NumberValue<Float> rainbowBrightness = new NumberValue<>(v -> moduleListMode.getValue() != ModuleListMode.STATIC,0.6f, 0.25f, 1.0f, 0.05f, "RainbowBrightness");
    public NumberValue<Float> rainbowWidth = new NumberValue<>(v -> moduleListMode.getValue() != ModuleListMode.STATIC,10f, 1f, 20f, 0.05f, "RainbowWidth");

    private GuiManager guiManager;
    private BlockRenderer blockRenderer;
    public static ColorControl INSTANCE;

    public ColorControl() {
        super("Colours", Category.OTHERS, "Control client colours.", true, true, false);
        addSettings(textColor, moduleListMode, red, green, blue, rainbowSpeed, rainbowBrightness, rainbowWidth);
        INSTANCE = this;
    }


    private void doSetupColour() {
        if (guiManager == null) guiManager = Muffin.getInstance().guiManager;
        guiManager.setModuleListColors(red.getValue(), green.getValue(), blue.getValue());

        if (blockRenderer == null) blockRenderer = Muffin.getInstance().getBlockRenderer();

        if (textColor.getValue().equals(TextColor.BLACK)) {
            guiManager.setTextColor(ChatFormatting.BLACK.toString());
            guiManager.setDarkTextColor(ChatFormatting.WHITE.toString());
        } else if (textColor.getValue().equals(TextColor.DARK_BLUE)) {
            guiManager.setTextColor(ChatFormatting.DARK_BLUE.toString());
            guiManager.setDarkTextColor(ChatFormatting.BLUE.toString());
        } else if (textColor.getValue().equals(TextColor.DARK_GREEN)) {
            guiManager.setTextColor(ChatFormatting.DARK_GREEN.toString());
            guiManager.setDarkTextColor(ChatFormatting.GREEN.toString());
        } else if (textColor.getValue().equals(TextColor.DARK_AQUA)) {
            guiManager.setTextColor(ChatFormatting.DARK_AQUA.toString());
            guiManager.setDarkTextColor(ChatFormatting.AQUA.toString());
        } else if (textColor.getValue().equals(TextColor.DARKRED)) {
            guiManager.setTextColor(ChatFormatting.DARK_RED.toString());
            guiManager.setDarkTextColor(ChatFormatting.RED.toString());
        } else if (textColor.getValue().equals(TextColor.DARKPURPLE)) {
            guiManager.setTextColor(ChatFormatting.DARK_PURPLE.toString());
            guiManager.setDarkTextColor(ChatFormatting.LIGHT_PURPLE.toString());
        } else if (textColor.getValue().equals(TextColor.GOLD)) {
            guiManager.setTextColor(ChatFormatting.GOLD.toString());
            guiManager.setDarkTextColor(ChatFormatting.YELLOW.toString());
        } else if (textColor.getValue().equals(TextColor.GRAY)) {
            guiManager.setTextColor(ChatFormatting.GRAY.toString());
            guiManager.setDarkTextColor(ChatFormatting.DARK_GRAY.toString());
        } else if (textColor.getValue().equals(TextColor.DARK_GRAY)) {
            guiManager.setTextColor(ChatFormatting.DARK_GRAY.toString());
            guiManager.setDarkTextColor(ChatFormatting.GRAY.toString());
        } else if (textColor.getValue().equals(TextColor.BLUE)) {
            guiManager.setTextColor(ChatFormatting.BLUE.toString());
            guiManager.setDarkTextColor(ChatFormatting.DARK_BLUE.toString());
        } else if (textColor.getValue().equals(TextColor.GREEN)) {
            guiManager.setTextColor(ChatFormatting.GREEN.toString());
            guiManager.setDarkTextColor(ChatFormatting.DARK_GREEN.toString());
        } else if (textColor.getValue().equals(TextColor.AQUA)) {
            guiManager.setTextColor(ChatFormatting.AQUA.toString());
            guiManager.setDarkTextColor(ChatFormatting.DARK_AQUA.toString());
        } else if (textColor.getValue().equals(TextColor.RED)) {
            guiManager.setTextColor(ChatFormatting.RED.toString());
            guiManager.setDarkTextColor(ChatFormatting.DARK_RED.toString());
        } else if (textColor.getValue().equals(TextColor.LIGHTPURPLE)) {
            guiManager.setTextColor(ChatFormatting.LIGHT_PURPLE.toString());
            guiManager.setDarkTextColor(ChatFormatting.DARK_PURPLE.toString());
        } else if (textColor.getValue().equals(TextColor.YELLOW)) {
            guiManager.setTextColor(ChatFormatting.YELLOW.toString());
            guiManager.setDarkTextColor(ChatFormatting.GOLD.toString());
        } else if (textColor.getValue().equals(TextColor.WHITE)) {
            guiManager.setTextColor(ChatFormatting.WHITE.toString());
            guiManager.setDarkTextColor(ChatFormatting.BLACK.toString());
        } else {
            guiManager.setTextColor(ChatFormatting.WHITE.toString());
            guiManager.setDarkTextColor(ChatFormatting.WHITE.toString());
        }
    }

    @Override
    public void onEnable() {
        doSetupColour();
    }

    @Override
    public void onDisable() {
        doSetupColour();
    }

    @Listener
    private void onSettingsChange(UpdateEvent event) {
        if (event.getStage() != EventStageable.EventStage.PRE) return;
        doSetupColour();
    }

    @Listener
    private void onTicking(TickEvent event) {
        blockRenderer.setColor();
    }

    public enum TextColor {
        BLACK,
        DARK_BLUE,
        DARK_GREEN,
        DARK_AQUA,
        DARKRED,
        DARKPURPLE,
        GOLD,
        GRAY,
        DARK_GRAY,
        BLUE,
        GREEN,
        AQUA,
        RED,
        LIGHTPURPLE,
        YELLOW,
        WHITE
    }

    public enum ModuleListMode {
        STATIC,
        RAINBOW
    }

}
