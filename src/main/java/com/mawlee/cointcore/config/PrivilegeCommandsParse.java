package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.privilege.ExpAddUnit;
import com.mawlee.cointcore.privilege.PrivilegeCommandId;

/**
 * Minecraft-free parse of {@code config/cointcore/privilege-commands.json}.
 */
public final class PrivilegeCommandsParse {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final int DEFAULT_REPAIR_COOLDOWN = 300;
    public static final int DEFAULT_CHARGE_COOLDOWN = 300;
    public static final int DEFAULT_REPAIR_ALL_COOLDOWN = 1800;
    public static final int DEFAULT_CHARGE_ALL_COOLDOWN = 1800;
    public static final int DEFAULT_EXP_ADD_COOLDOWN = 3600;
    public static final int DEFAULT_EXP_ADD_MAX = 1000;
    public static final int MAX_COOLDOWN_SECONDS = 7 * 24 * 60 * 60;

    private PrivilegeCommandsParse() {
    }

    public static Parsed parseJson(String json) {
        FileData data = GSON.fromJson(json, FileData.class);
        return parse(data);
    }

    static Parsed parse(FileData data) {
        FileData source = data != null ? data : FileData.defaults();
        return new Parsed(
                clampCooldown(source.repairCooldownSeconds, DEFAULT_REPAIR_COOLDOWN),
                clampCooldown(source.chargeCooldownSeconds, DEFAULT_CHARGE_COOLDOWN),
                clampCooldown(source.repairAllCooldownSeconds, DEFAULT_REPAIR_ALL_COOLDOWN),
                clampCooldown(source.chargeAllCooldownSeconds, DEFAULT_CHARGE_ALL_COOLDOWN),
                clampCooldown(source.expAddCooldownSeconds, DEFAULT_EXP_ADD_COOLDOWN),
                clampAmount(source.expAddMaxAmount, DEFAULT_EXP_ADD_MAX),
                ExpAddUnit.parse(source.expAddUnit, ExpAddUnit.POINTS)
        );
    }

    static Gson gson() {
        return GSON;
    }

    private static int clampCooldown(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(0, Math.min(value, MAX_COOLDOWN_SECONDS));
    }

    private static int clampAmount(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(1, Math.min(value, Integer.MAX_VALUE));
    }

    public record Parsed(
            int repairCooldownSeconds,
            int chargeCooldownSeconds,
            int repairAllCooldownSeconds,
            int chargeAllCooldownSeconds,
            int expAddCooldownSeconds,
            int expAddMaxAmount,
            ExpAddUnit expAddUnit
    ) {
        public int cooldownSeconds(PrivilegeCommandId id) {
            switch (id) {
                case REPAIR -> {
                    return repairCooldownSeconds;
                }
                case CHARGE -> {
                    return chargeCooldownSeconds;
                }
                case REPAIR_ALL -> {
                    return repairAllCooldownSeconds;
                }
                case CHARGE_ALL -> {
                    return chargeAllCooldownSeconds;
                }
                case EXP_ADD -> {
                    return expAddCooldownSeconds;
                }
                default -> {
                    throw new IllegalStateException("Unhandled privilege command: " + id);
                }
            }
        }
    }

    static final class FileData {
        @SerializedName("repairCooldownSeconds")
        Integer repairCooldownSeconds;

        @SerializedName("chargeCooldownSeconds")
        Integer chargeCooldownSeconds;

        @SerializedName("repairAllCooldownSeconds")
        Integer repairAllCooldownSeconds;

        @SerializedName("chargeAllCooldownSeconds")
        Integer chargeAllCooldownSeconds;

        @SerializedName("expAddCooldownSeconds")
        Integer expAddCooldownSeconds;

        @SerializedName("expAddMaxAmount")
        Integer expAddMaxAmount;

        @SerializedName("expAddUnit")
        String expAddUnit;

        static FileData defaults() {
            FileData data = new FileData();
            data.repairCooldownSeconds = DEFAULT_REPAIR_COOLDOWN;
            data.chargeCooldownSeconds = DEFAULT_CHARGE_COOLDOWN;
            data.repairAllCooldownSeconds = DEFAULT_REPAIR_ALL_COOLDOWN;
            data.chargeAllCooldownSeconds = DEFAULT_CHARGE_ALL_COOLDOWN;
            data.expAddCooldownSeconds = DEFAULT_EXP_ADD_COOLDOWN;
            data.expAddMaxAmount = DEFAULT_EXP_ADD_MAX;
            data.expAddUnit = ExpAddUnit.POINTS.configKey();
            return data;
        }
    }
}
