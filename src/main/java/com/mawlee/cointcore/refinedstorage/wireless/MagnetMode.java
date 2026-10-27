package com.mawlee.cointcore.refinedstorage.wireless;

public enum MagnetMode {
    OFF, INVENTORY, NETWORK;

    public MagnetMode next() { return values()[(ordinal() + 1) % values().length]; }
    public static MagnetMode fromId(int id) { return id >= 0 && id < values().length ? values()[id] : OFF; }
    public String translationKey() { return "gui.cointcore.magnet.mode." + name().toLowerCase(java.util.Locale.ROOT); }
}
