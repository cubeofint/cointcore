package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Skip Pipez {@code ItemHandlerHelper.insertItem} into Functional Storage Armory
 * Cabinets when the stack is not cabinet-certified, avoiding a 4096-slot walk.
 *
 * <p>File: {@code config/cointcore/lag-fixes.json} section {@code pipez_armory_fast_reject}.
 * Defaults to enabled: rejected non-armory items keep their original remainder
 * without scanning every empty cabinet slot.
 */
public final class PipezArmoryFastRejectConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean enabled = true;
    private static boolean freeSlotHint = true;

    private PipezArmoryFastRejectConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Certified single items go straight to a hinted free cabinet slot (see ArmoryFreeSlotHint). */
    public static boolean isFreeSlotHintEnabled() {
        return freeSlotHint;
    }

    public static Path getConfigPath() {
        return LagFixesConfigs.path();
    }

    public static void load() {
        LagFixesConfigs.load();
    }

    public static boolean reload() {
        return LagFixesConfigs.reload();
    }

    static void applySection(Data data) {
        Data resolved = data != null ? data : defaultFileData();
        enabled = resolved.enabled;
        freeSlotHint = resolved.freeSlotHint;
    }

    static void logReload() {
        LOGGER.info("Reloaded Pipez armory fast-reject config (enabled={}, freeSlotHint={})", enabled, freeSlotHint);
    }

    static Data defaultFileData() {
        return new Data();
    }

    static final class Data {
        @SerializedName("enabled")
        boolean enabled = true;

        @SerializedName("free_slot_hint")
        boolean freeSlotHint = true;
    }
}
