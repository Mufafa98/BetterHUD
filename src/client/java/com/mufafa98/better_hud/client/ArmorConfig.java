package com.mufafa98.better_hud.client;

public class ArmorConfig {
    // These two might me moved into Config
    public int margin = 4;
    public int screenMargin = 4;

    public boolean renderVertically = true;
    public int textureSize = 16;
    public double centerX = 0.5;
    public double centerY = 0.5;
    public int gapBetweenItems = 4;
    public int gapBetweenIconAndText = 4;

    public TextPosition textPosition = TextPosition.RIGHT;

    public LayoutOrientation layout = LayoutOrientation.VERTICAL;

    public int lowDurabilityPercentage = 15;
    public int lowDurabilityColor = 0xFFFF0000;

    public int mediumDurabilityPercentage = 30;
    public int mediumDurabilityColor = 0xFFFFFF00;

    public ArmorConfig() {
    }

    public int getTopLeftX(int screenWidth, int hudWidth) {
        int centerAbs = (int) (centerX * screenWidth);
        return Math.max(
                screenMargin,
                Math.min(
                        screenWidth - hudWidth - screenMargin,
                        centerAbs - hudWidth / 2));
    }

    public int getTopLeftY(int screenHeight, int hudHeight) {
        int centerAbs = (int) (centerY * screenHeight);
        return Math.max(
                screenMargin,
                Math.min(
                        screenHeight - hudHeight - screenMargin,
                        centerAbs - hudHeight / 2));
    }

    public enum TextPosition {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    public enum LayoutOrientation {
        VERTICAL,
        HORIZONTAL
    }
}