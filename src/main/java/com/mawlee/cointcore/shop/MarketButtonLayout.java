package com.mawlee.cointcore.shop;

/**
 * Client button coordinates. Isolated from FML so unit tests can clamp without Minecraft.
 */
public final class MarketButtonLayout {
    public static final int MIN_COORD = 0;
    public static final int MAX_COORD = 1024;
    public static final int DEFAULT_X = 4;
    public static final int DEFAULT_Y = 42;

    private MarketButtonLayout() {
    }

    public static int clamp(Integer value, int fallback) {
        int coord = value == null ? fallback : value;
        return Math.max(MIN_COORD, Math.min(MAX_COORD, coord));
    }

    public static Coords parse(Integer inventoryX, Integer inventoryY, Integer creativeX, Integer creativeY) {
        return new Coords(
                clamp(inventoryX, DEFAULT_X),
                clamp(inventoryY, DEFAULT_Y),
                clamp(creativeX, DEFAULT_X),
                clamp(creativeY, DEFAULT_Y)
        );
    }

    public record Coords(int inventoryX, int inventoryY, int creativeX, int creativeY) {
    }
}
