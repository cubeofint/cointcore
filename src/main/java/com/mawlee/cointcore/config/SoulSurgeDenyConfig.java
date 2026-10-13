package com.mawlee.cointcore.config;

import com.google.gson.JsonSyntaxException;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Soul Surge denylist ({@code config/cointcore/soul-surge-deny.json}).
 * Denied target blocks get no extra ticks. Default lists are empty;
 * admins fill {@code denyBlockIds} / optional {@code denyBlockTags}.
 * This file is never merged with bundled defaults so admin removals stick.
 */
public final class SoulSurgeDenyConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DEFAULT_RESOURCE = "/cointcore/default_configs/soul-surge-deny.json";

    private static volatile Set<String> denyBlockIds = Set.of();
    private static volatile List<TagKey<Block>> denyBlockTags = List.of();

    private SoulSurgeDenyConfig() {
    }

    public static boolean isDenied(BlockState state) {
        if (state == null) {
            return false;
        }
        Set<String> ids = denyBlockIds;
        List<TagKey<Block>> tags = denyBlockTags;
        if (ids.isEmpty() && tags.isEmpty()) {
            return false;
        }
        if (!ids.isEmpty()) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (id != null && ids.contains(id.toString())) {
                return true;
            }
        }
        for (TagKey<Block> tag : tags) {
            if (state.is(tag)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDeniedId(String blockId) {
        return blockId != null && denyBlockIds.contains(blockId);
    }

    public static Path getConfigPath() {
        return configPath();
    }

    public static void load() {
        loadFromDisk(false);
    }

    public static boolean reload() {
        boolean ok = loadFromDisk(true);
        if (ok) {
            LOGGER.info(
                    "Reloaded soul-surge-deny.json (ids={} tags={})",
                    denyBlockIds.size(),
                    denyBlockTags.size()
            );
        }
        return ok;
    }

    private static boolean loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                createDefaultConfig(path);
                LOGGER.info("Created soul surge deny config at {}", path);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                SoulSurgeDenyParse.FileData data = SoulSurgeDenyParse.gson()
                        .fromJson(reader, SoulSurgeDenyParse.FileData.class);
                apply(SoulSurgeDenyParse.parse(data));
                return true;
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load soul surge deny config from {}", configPath(), exception);
            if (!reloading) {
                apply(SoulSurgeDenyParse.parse(readBundledDefaults()));
            }
            return !reloading;
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading soul surge deny config from {}", configPath(), exception);
            if (!reloading) {
                apply(SoulSurgeDenyParse.parse(readBundledDefaults()));
            }
            return !reloading;
        }
    }

    private static void apply(SoulSurgeDenyParse.Parsed parsed) {
        Set<String> ids = new LinkedHashSet<>();
        for (String raw : parsed.denyBlockIds()) {
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id == null) {
                LOGGER.warn("Skipping invalid soul-surge deny block id: {}", raw);
                continue;
            }
            ids.add(id.toString());
        }
        List<TagKey<Block>> tags = new ArrayList<>();
        for (String raw : parsed.denyBlockTags()) {
            ResourceLocation tagId = ResourceLocation.tryParse(raw);
            if (tagId == null) {
                LOGGER.warn("Skipping invalid soul-surge deny block tag: {}", raw);
                continue;
            }
            tags.add(TagKey.create(Registries.BLOCK, tagId));
        }
        denyBlockIds = Set.copyOf(ids);
        denyBlockTags = List.copyOf(tags);
    }

    private static void createDefaultConfig(Path path) throws IOException {
        try (InputStream in = SoulSurgeDenyConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in != null) {
                Files.copy(in, path);
                return;
            }
        }
        LOGGER.warn("Missing bundled {}; writing empty deny lists", DEFAULT_RESOURCE);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            SoulSurgeDenyParse.gson().toJson(SoulSurgeDenyParse.FileData.defaults(), writer);
        }
    }

    private static SoulSurgeDenyParse.FileData readBundledDefaults() {
        try (InputStream in = SoulSurgeDenyConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                return new SoulSurgeDenyParse.FileData();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                SoulSurgeDenyParse.FileData data = SoulSurgeDenyParse.gson()
                        .fromJson(reader, SoulSurgeDenyParse.FileData.class);
                return data != null ? data : new SoulSurgeDenyParse.FileData();
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.warn("Failed to read bundled soul surge deny defaults", exception);
            return new SoulSurgeDenyParse.FileData();
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("soul-surge-deny.json");
    }
}
