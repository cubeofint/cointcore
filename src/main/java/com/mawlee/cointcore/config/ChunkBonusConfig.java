package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;

/**
 * File: {@code config/cointcore/claims.json} section {@code bonus}.
 */
public final class ChunkBonusConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String DEFAULT_CLAIM_META = "ftbchunks.bonus_claimed";
    private static final String DEFAULT_FORCE_LOAD_META = "ftbchunks.bonus_force_loaded";

    private static boolean enabled = true;
    private static List<String> bonusClaimMetaKeys = List.of(DEFAULT_CLAIM_META);
    private static List<String> bonusForceLoadMetaKeys = List.of(DEFAULT_FORCE_LOAD_META);

    private ChunkBonusConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static List<String> getBonusClaimMetaKeys() {
        return bonusClaimMetaKeys;
    }

    public static List<String> getBonusForceLoadMetaKeys() {
        return bonusForceLoadMetaKeys;
    }

    public static Path getConfigPath() {
        return ClaimsConfigs.path();
    }

    public static void load() {
        ClaimsConfigs.load();
    }

    public static boolean reload() {
        return ClaimsConfigs.reload();
    }

    static void applySection(FileData data) {
        apply(parse(data != null ? data : defaultFileData()));
    }

    static void logReload() {
        LOGGER.info(
                "Reloaded chunk bonus config (enabled={}, claimKeys={}, forceKeys={})",
                enabled,
                bonusClaimMetaKeys.size(),
                bonusForceLoadMetaKeys.size()
        );
    }

    private static LoadedConfig parse(FileData data) {
        boolean parsedEnabled = data.enabled != null ? data.enabled : true;
        List<String> claimKeys = normalizeMetaKeys(data.bonusClaimMetaKeys, DEFAULT_CLAIM_META);
        List<String> forceKeys = normalizeMetaKeys(data.bonusForceLoadMetaKeys, DEFAULT_FORCE_LOAD_META);
        return new LoadedConfig(parsedEnabled, claimKeys, forceKeys);
    }

    private static List<String> normalizeMetaKeys(List<String> keys, String fallback) {
        if (keys == null || keys.isEmpty()) {
            return List.of(fallback);
        }

        return keys.stream()
                .filter(key -> key != null && !key.isBlank())
                .distinct()
                .toList();
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        bonusClaimMetaKeys = loaded.bonusClaimMetaKeys();
        bonusForceLoadMetaKeys = loaded.bonusForceLoadMetaKeys();
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.bonusClaimMetaKeys = List.of(DEFAULT_CLAIM_META);
        data.bonusForceLoadMetaKeys = List.of(DEFAULT_FORCE_LOAD_META);
        return data;
    }

    private record LoadedConfig(
            boolean enabled,
            List<String> bonusClaimMetaKeys,
            List<String> bonusForceLoadMetaKeys
    ) {
    }

    static final class FileData {
        @SerializedName("enabled")
        private Boolean enabled;

        @SerializedName("bonus_claim_meta_keys")
        private List<String> bonusClaimMetaKeys;

        @SerializedName("bonus_force_load_meta_keys")
        private List<String> bonusForceLoadMetaKeys;
    }
}
