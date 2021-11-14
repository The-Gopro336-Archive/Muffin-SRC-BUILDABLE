package me.han.muffin.client.manager.managers;

import com.mojang.realmsclient.gui.ChatFormatting;

public class GuiManager {

    private String textColor = ChatFormatting.GRAY.toString();
    private String darkTextColor = ChatFormatting.DARK_GRAY.toString();

    private int moduleListRed = 255;
    private int moduleListGreen = 255;
    private int moduleListBlue = 255;


    public void setModuleListColors(int r, int g, int b) {
        this.moduleListRed = r;
        this.moduleListGreen = g;
        this.moduleListBlue = b;
    }

    public String getTextColor() {
        return this.textColor;
    }

    public String getDarkTextColor() {
        return darkTextColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public void setDarkTextColor(String darkTextColor) {
        this.darkTextColor = darkTextColor;
    }

    public int getModuleListRed() {
        return this.moduleListRed;
    }

    public int getModuleListGreen() {
        return this.moduleListGreen;
    }

    public int getModuleListBlue() {
        return this.moduleListBlue;
    }

}

