package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Farm-spawner byproducts: XP and EvilCraft blood into adjacent handlers.
 * Without a matching tank, XP becomes Ars Nouveau experience gems and blood becomes condensed blood.
 */
public final class SpawnerByproductConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String DEFAULT_XP_FLUID = "mob_grinding_utils:fluid_xp";
    private static final String DEFAULT_XP_ITEM = "ars_nouveau:experience_gem";
    private static final String LEGACY_XP_ITEM = "actuallyadditions:solidified_experience";
    private static final String DEFAULT_BLOOD_FLUID = "evilcraft:blood";
    private static final String DEFAULT_BLOOD_ITEM = "evilcraft:condensed_blood";

    private static boolean enabled = true;
    private static ResourceLocation xpFluidId = ResourceLocation.parse(DEFAULT_XP_FLUID);
    private static int xpMbPerPoint = 20;
    private static ResourceLocation xpItemId = ResourceLocation.parse(DEFAULT_XP_ITEM);
    private static int xpItemValue = 3;
    private static ResourceLocation bloodFluidId = ResourceLocation.parse(DEFAULT_BLOOD_FLUID);
    private static ResourceLocation bloodItemId = ResourceLocation.parse(DEFAULT_BLOOD_ITEM);
    private static int bloodItemMb = 500;
    private static double bloodMinMultiplier = 5.0D;
    private static double bloodMaxMultiplier = 40.0D;

    private SpawnerByproductConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static ResourceLocation getXpFluidId() {
        return xpFluidId;
    }

    public static int getXpMbPerPoint() {
        return xpMbPerPoint;
    }

    public static ResourceLocation getXpItemId() {
        return xpItemId;
    }

    public static int getXpItemValue() {
        return xpItemValue;
    }

    public static ResourceLocation getBloodFluidId() {
        return bloodFluidId;
    }

    public static ResourceLocation getBloodItemId() {
        return bloodItemId;
    }

    public static int getBloodItemMb() {
        return bloodItemMb;
    }

    public static double getBloodMinMultiplier() {
        return bloodMinMultiplier;
    }

    public static double getBloodMaxMultiplier() {
        return bloodMaxMultiplier;
    }

    public static Path getConfigPath() {
        return configPath();
    }

    public static void load() {
        apply(loadFromDisk(false));
    }

    public static boolean reload() {
        LoadedConfig loaded = loadFromDisk(true);
        if (loaded == null) {
            return false;
        }
        apply(loaded);
        LOGGER.info(
                "Reloaded spawner byproduct config (enabled={}, xpFluid={}, xpMbPerPoint={}, xpItem={}, xpItemValue={}, bloodFluid={}, bloodItem={}, bloodItemMb={}, bloodMult={}-{})",
                enabled,
                xpFluidId,
                xpMbPerPoint,
                xpItemId,
                xpItemValue,
                bloodFluidId,
                bloodItemId,
                bloodItemMb,
                bloodMinMultiplier,
                bloodMaxMultiplier
        );
        return true;
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        xpFluidId = loaded.xpFluidId();
        xpMbPerPoint = loaded.xpMbPerPoint();
        xpItemId = loaded.xpItemId();
        xpItemValue = loaded.xpItemValue();
        bloodFluidId = loaded.bloodFluidId();
        bloodItemId = loaded.bloodItemId();
        bloodItemMb = loaded.bloodItemMb();
        bloodMinMultiplier = loaded.bloodMinMultiplier();
        bloodMaxMultiplier = loaded.bloodMaxMultiplier();
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                save(defaults, path);
                LOGGER.info("Created default spawner byproduct config at {}", path);
                return parse(defaults);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return parse(data != null ? data : defaultFileData());
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load spawner byproduct config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading spawner byproduct config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static LoadedConfig parse(FileData data) {
        boolean on = data.enabled == null || data.enabled;
        ResourceLocation xpFluid = parseId(data.xpFluidId, DEFAULT_XP_FLUID, "xpFluidId");
        int mbPer = clampPositive(data.xpMbPerPoint != null ? data.xpMbPerPoint : 20, 1, 10_000);
        ResourceLocation xpItem = parseId(data.xpItemId, DEFAULT_XP_ITEM, "xpItemId");
        int itemValue = clampPositive(data.xpItemValue != null ? data.xpItemValue : 3, 1, 10_000);
        if (LEGACY_XP_ITEM.equals(xpItem.toString()) && (data.xpItemValue == null || data.xpItemValue == 8)) {
            xpItem = ResourceLocation.parse(DEFAULT_XP_ITEM);
            itemValue = 3;
        }
        ResourceLocation bloodFluid = parseId(data.bloodFluidId, DEFAULT_BLOOD_FLUID, "bloodFluidId");
        ResourceLocation bloodItem = parseId(data.bloodItemId, DEFAULT_BLOOD_ITEM, "bloodItemId");
        int bloodMb = clampPositive(data.bloodItemMb != null ? data.bloodItemMb : 500, 1, 100_000);
        double minMult = data.bloodMinMultiplier != null ? data.bloodMinMultiplier : 5.0D;
        double maxMult = data.bloodMaxMultiplier != null ? data.bloodMaxMultiplier : 40.0D;
        if (minMult < 0.0D) {
            minMult = 0.0D;
        }
        if (maxMult < minMult) {
            maxMult = minMult;
        }
        return new LoadedConfig(on, xpFluid, mbPer, xpItem, itemValue, bloodFluid, bloodItem, bloodMb, minMult, maxMult);
    }

    private static ResourceLocation parseId(String raw, String fallback, String field) {
        String value = raw != null && !raw.isBlank() ? raw.trim() : fallback;
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            LOGGER.warn("Invalid {} '{}', using {}", field, value, fallback);
            return ResourceLocation.parse(fallback);
        }
        return id;
    }

    private static int clampPositive(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.xpFluidId = DEFAULT_XP_FLUID;
        data.xpMbPerPoint = 20;
        data.xpItemId = DEFAULT_XP_ITEM;
        data.xpItemValue = 3;
        data.bloodFluidId = DEFAULT_BLOOD_FLUID;
        data.bloodItemId = DEFAULT_BLOOD_ITEM;
        data.bloodItemMb = 500;
        data.bloodMinMultiplier = 5.0D;
        data.bloodMaxMultiplier = 40.0D;
        return data;
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("spawner-byproduct.json");
    }

    private record LoadedConfig(
            boolean enabled,
            ResourceLocation xpFluidId,
            int xpMbPerPoint,
            ResourceLocation xpItemId,
            int xpItemValue,
            ResourceLocation bloodFluidId,
            ResourceLocation bloodItemId,
            int bloodItemMb,
            double bloodMinMultiplier,
            double bloodMaxMultiplier
    ) {
    }

    private static final class FileData {
        @SerializedName("enabled")
        private Boolean enabled;

        @SerializedName("xpFluidId")
        private String xpFluidId;

        @SerializedName("xpMbPerPoint")
        private Integer xpMbPerPoint;

        @SerializedName("xpItemId")
        private String xpItemId;

        @SerializedName("xpItemValue")
        private Integer xpItemValue;

        @SerializedName("bloodFluidId")
        private String bloodFluidId;

        @SerializedName("bloodItemId")
        private String bloodItemId;

        @SerializedName("bloodItemMb")
        private Integer bloodItemMb;

        @SerializedName("bloodMinMultiplier")
        private Double bloodMinMultiplier;

        @SerializedName("bloodMaxMultiplier")
        private Double bloodMaxMultiplier;
    }
}
