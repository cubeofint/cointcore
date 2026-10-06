package com.mawlee.cointcore.ae;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MeUniqueFilterConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int DEFAULT_MERGED_MIN_AMOUNT = 2;
    private static final int MIN_MERGED_MIN_AMOUNT = 2;
    private static final int MAX_MERGED_MIN_AMOUNT = 1_000_000;

    private static Set<ResourceLocation> excludedItemIds = Set.of();
    private static Set<TagKey<Item>> excludedTags = Set.of();
    private static int mergedMinAmount = DEFAULT_MERGED_MIN_AMOUNT;

    static {
        apply(parse(defaultFileData()));
    }

    private MeUniqueFilterConfig() {
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
        return true;
    }

    /**
     * Minimum stored amount for an entry to be reported by the
     * {@code #cointcore:ns_heavy} / {@code #ns_heavy} terminal search.
     */
    public static int getMergedMinAmount() {
        return mergedMinAmount;
    }

    public static boolean isExcludedItem(Item item) {
        if (item == null || item == Items.AIR) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id != null && excludedItemIds.contains(id)) {
            return true;
        }
        for (TagKey<Item> tag : excludedTags) {
            if (item.builtInRegistryHolder().is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                save(defaults, path);
                LOGGER.info("Created default ME unique filter config at {}", path);
                return parse(defaults);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                LoadedConfig loaded = parse(data != null ? data : defaultFileData());
                if (reloading) {
                    LOGGER.info("Reloaded ME unique filter config from {}", path);
                }
                return loaded;
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load ME unique filter config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static void apply(LoadedConfig loaded) {
        excludedItemIds = loaded.excludedItemIds;
        excludedTags = loaded.excludedTags;
        mergedMinAmount = loaded.mergedMinAmount;
    }

    private static LoadedConfig parse(FileData data) {
        Set<ResourceLocation> itemIds = new HashSet<>();
        if (data.excludeItemIds != null) {
            for (String raw : data.excludeItemIds) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(raw.trim());
                if (id == null) {
                    LOGGER.warn("Skipping invalid excludeItemIds entry: {}", raw);
                    continue;
                }
                itemIds.add(id);
            }
        }

        Set<TagKey<Item>> tags = new HashSet<>();
        if (data.excludeTags != null) {
            for (String raw : data.excludeTags) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(raw.trim());
                if (id == null) {
                    LOGGER.warn("Skipping invalid excludeTags entry: {}", raw);
                    continue;
                }
                tags.add(TagKey.create(net.minecraft.core.registries.Registries.ITEM, id));
            }
        }

        int minAmount = data.mergedMinAmount != null ? data.mergedMinAmount : DEFAULT_MERGED_MIN_AMOUNT;
        if (minAmount < MIN_MERGED_MIN_AMOUNT || minAmount > MAX_MERGED_MIN_AMOUNT) {
            LOGGER.warn(
                    "mergedMinAmount {} is out of range [{}, {}], clamping",
                    minAmount,
                    MIN_MERGED_MIN_AMOUNT,
                    MAX_MERGED_MIN_AMOUNT
            );
            minAmount = Math.clamp(minAmount, MIN_MERGED_MIN_AMOUNT, MAX_MERGED_MIN_AMOUNT);
        }

        return new LoadedConfig(Set.copyOf(itemIds), Set.copyOf(tags), minAmount);
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(CointCore.MOD_ID)
                .resolve("me-unique-filter.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.excludeItemIds = new ArrayList<>(List.of(
                "minecraft:potion",
                "minecraft:splash_potion",
                "minecraft:lingering_potion",
                "minecraft:tipped_arrow",
                "minecraft:spectral_arrow",
                "minecraft:experience_bottle",
                "minecraft:ominous_bottle",
                "minecraft:firework_rocket",
                "minecraft:firework_star",
                "minecraft:written_book",
                "minecraft:writable_book",
                "minecraft:map",
                "minecraft:filled_map",
                "minecraft:goat_horn",
                "minecraft:knowledge_book",
                "minecraft:bundle"
        ));
        data.excludeTags = new ArrayList<>(List.of(
                "minecraft:potions"
        ));
        data.mergedMinAmount = DEFAULT_MERGED_MIN_AMOUNT;
        return data;
    }

    private record LoadedConfig(
            Set<ResourceLocation> excludedItemIds,
            Set<TagKey<Item>> excludedTags,
            int mergedMinAmount
    ) {
    }

    private static final class FileData {
        @SerializedName("excludeItemIds")
        private List<String> excludeItemIds = new ArrayList<>();

        @SerializedName("excludeTags")
        private List<String> excludeTags = new ArrayList<>();

        @SerializedName("mergedMinAmount")
        private Integer mergedMinAmount = DEFAULT_MERGED_MIN_AMOUNT;
    }
}
