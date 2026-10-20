package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Caps the dig area of Iron's Spellbooks Spectral Hammer. The spell takes radius = power / 2 and
 * depth = power, so with stacked spell power its {@code collectBlocks} flood fill walks millions of
 * blocks in one tick (57 s stalls seen in the End).
 *
 * <p>File: {@code config/cointcore/lag-fixes.json} section {@code irons_spectral_hammer}.
 * Defaults to enabled; normal gear (level 5, power 5–12) stays under the caps.
 */
public final class IronsSpectralHammerConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean enabled = true;
    private static int maxRadius = 8;
    private static int maxDepth = 16;

    private IronsSpectralHammerConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getMaxRadius() {
        return maxRadius;
    }

    public static int getMaxDepth() {
        return maxDepth;
    }

    public static Path getConfigPath() {
        return LagFixesConfigs.path();
    }

    static void applySection(Data data) {
        Data resolved = data != null ? data : defaultFileData();
        enabled = resolved.enabled;
        maxRadius = Math.max(1, resolved.maxRadius);
        maxDepth = Math.max(1, resolved.maxDepth);
    }

    static void logReload() {
        LOGGER.info("Reloaded Spectral Hammer cap config (enabled={}, maxRadius={}, maxDepth={})",
                enabled, maxRadius, maxDepth);
    }

    static Data defaultFileData() {
        return new Data();
    }

    static final class Data {
        @SerializedName("enabled")
        boolean enabled = true;

        @SerializedName("max_radius")
        int maxRadius = 8;

        @SerializedName("max_depth")
        int maxDepth = 16;
    }
}
