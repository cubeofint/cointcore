package com.mawlee.cointcore.ae;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class NonStackableItemTagPack {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int PACK_FORMAT = 48;

    private static volatile int generatedItemCount;

    private NonStackableItemTagPack() {
    }

    public static int getGeneratedItemCount() {
        return generatedItemCount;
    }

    public static void registerPackFinder(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }

        Path datapackRoot = ensureGenerated();
        PackLocationInfo locationInfo = new PackLocationInfo(
                "file/cointcore_me_tags",
                Component.literal("CointCore ME Tags"),
                PackSource.SERVER,
                Optional.empty()
        );

        Pack pack = Pack.readMetaAndCreate(
                locationInfo,
                BuiltInPackSource.fromName(name -> new PathPackResources(locationInfo, datapackRoot)),
                PackType.SERVER_DATA,
                new PackSelectionConfig(true, Pack.Position.TOP, false)
        );

        if (pack == null) {
            LOGGER.error("Failed to register CointCore ME tag datapack from {}", datapackRoot);
            return;
        }

        event.addRepositorySource(consumer -> consumer.accept(pack));
    }

    public static Path ensureGenerated() {
        MeUniqueFilterConfig.load();

        Path datapackRoot = FMLPaths.CONFIGDIR.get()
                .resolve(CointCore.MOD_ID)
                .resolve("me-tags-datapack");

        List<String> itemIds = collectNonStackableItemIds();
        generatedItemCount = itemIds.size();

        try {
            writeDatapack(datapackRoot, itemIds);
            LOGGER.info(
                    "CointCore ME filter tags #{} and #{} contain {} candidate item types. "
                            + "With CointCore on the client, #{} shows only x1 entries, while #{} shows "
                            + "merged entries with amount >= {} (used to spot heavy bases).",
                    MeFilterTags.TAG_LOCATION,
                    MeFilterTags.MERGED_TAG_LOCATION,
                    generatedItemCount,
                    MeFilterTags.TAG_LOCATION,
                    MeFilterTags.MERGED_TAG_LOCATION,
                    MeUniqueFilterConfig.getMergedMinAmount()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to generate CointCore ME tag datapack", exception);
        }

        return datapackRoot;
    }

    private static List<String> collectNonStackableItemIds() {
        List<String> itemIds = new ArrayList<>();

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            if (MeUniqueEntryRules.isCandidateItemType(item)) {
                itemIds.add(BuiltInRegistries.ITEM.getKey(item).toString());
            }
        }

        itemIds.sort(Comparator.naturalOrder());
        return itemIds;
    }

    private static void writeDatapack(Path datapackRoot, List<String> itemIds) throws IOException {
        Path dataDir = datapackRoot.resolve("data")
                .resolve(CointCore.MOD_ID)
                .resolve("tags")
                .resolve("item");
        Files.createDirectories(dataDir);

        writePackMeta(datapackRoot);
        writeTagFile(dataDir.resolve(MeFilterTags.TAG_ID + ".json"), itemIds);
        writeTagFile(dataDir.resolve(MeFilterTags.MERGED_TAG_ID + ".json"), itemIds);
        // Drop the old long tag path so AE2 search no longer offers an untypable name.
        Files.deleteIfExists(dataDir.resolve(MeFilterTags.MERGED_TAG_ID_LEGACY + ".json"));
    }

    private static void writePackMeta(Path datapackRoot) throws IOException {
        Path packMeta = datapackRoot.resolve("pack.mcmeta");
        if (Files.exists(packMeta)) {
            return;
        }

        Files.createDirectories(datapackRoot);
        try (Writer writer = Files.newBufferedWriter(packMeta, StandardCharsets.UTF_8)) {
            GSON.toJson(new PackMeta(
                    new PackMetaSection("CointCore ME filter tags", PACK_FORMAT)
            ), writer);
        }
    }

    private static void writeTagFile(Path tagFile, List<String> itemIds) throws IOException {
        try (Writer writer = Files.newBufferedWriter(tagFile, StandardCharsets.UTF_8)) {
            GSON.toJson(new ItemTagFile(itemIds), writer);
        }
    }

    private record PackMeta(PackMetaSection pack) {
    }

    private record PackMetaSection(String description, int pack_format) {
    }

    private record ItemTagFile(List<String> values) {
    }
}
