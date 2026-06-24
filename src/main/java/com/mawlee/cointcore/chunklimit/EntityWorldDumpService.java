package com.mawlee.cointcore.chunklimit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EntityWorldDumpService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            .withZone(ZoneId.systemDefault());

    private EntityWorldDumpService() {
    }

    public static Path getDumpsDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("dumps");
    }

    public static DumpResult writeDump(MinecraftServer server, boolean includeAllEntities) throws IOException {
        Map<ResourceLocation, TypeAccumulator> accumulators = new HashMap<>();

        for (ServerLevel level : server.getAllLevels()) {
            ResourceLocation dimensionId = level.dimension().location();
            for (Entity entity : level.getEntities().getAll()) {
                if (!shouldInclude(entity, includeAllEntities)) {
                    continue;
                }
                ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                if (typeId == null) {
                    continue;
                }
                ChunkPos chunkPos = entity.chunkPosition();
                accumulators
                        .computeIfAbsent(typeId, ignored -> new TypeAccumulator())
                        .record(dimensionId, chunkPos);
            }
        }

        List<TypeEntryFile> types = new ArrayList<>();
        int totalEntities = 0;
        for (Map.Entry<ResourceLocation, TypeAccumulator> entry : accumulators.entrySet()) {
            TypeAccumulator accumulator = entry.getValue();
            totalEntities += accumulator.total;
            types.add(accumulator.toFileEntry(entry.getKey()));
        }
        types.sort(Comparator.comparingInt(TypeEntryFile::total).reversed());

        DumpFileData data = new DumpFileData();
        data.generatedAt = Instant.now().toString();
        data.serverTick = server.getTickCount();
        data.filter = includeAllEntities ? "all_entities" : "mobs";
        data.summary = new SummaryFile(types.size(), totalEntities);
        data.countsByType = types;

        Path directory = getDumpsDirectory();
        Files.createDirectories(directory);
        String suffix = includeAllEntities ? "all-entities" : "mobs";
        Path output = directory.resolve("entity-dump-" + suffix + "-" + FILE_TIME.format(Instant.now()) + ".json");

        try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }

        LOGGER.info(
                "Wrote entity world dump to {} ({} types, {} entities, filter={})",
                output,
                types.size(),
                totalEntities,
                data.filter
        );
        return new DumpResult(output, types.size(), totalEntities, data.filter);
    }

    private static boolean shouldInclude(Entity entity, boolean includeAllEntities) {
        if (entity == null || entity.isRemoved() || entity instanceof Player) {
            return false;
        }
        if (includeAllEntities) {
            return true;
        }
        return entity instanceof Mob;
    }

    public record DumpResult(Path path, int entityTypes, int totalEntities, String filter) {
    }

    private static final class TypeAccumulator {
        private int total;
        private final Map<ResourceLocation, Integer> byDimension = new HashMap<>();
        private final Map<ChunkRef, Integer> byChunk = new HashMap<>();

        private void record(ResourceLocation dimensionId, ChunkPos chunkPos) {
            total++;
            byDimension.merge(dimensionId, 1, Integer::sum);
            byChunk.merge(new ChunkRef(dimensionId, chunkPos.x, chunkPos.z), 1, Integer::sum);
        }

        private TypeEntryFile toFileEntry(ResourceLocation typeId) {
            Map<String, Integer> dimensionCounts = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, Integer> entry : byDimension.entrySet()) {
                dimensionCounts.put(entry.getKey().toString(), entry.getValue());
            }

            ChunkPeak peak = byChunk.entrySet().stream()
                    .max(Comparator.comparingInt(Map.Entry::getValue))
                    .map(entry -> new ChunkPeak(
                            entry.getKey().dimension().toString(),
                            entry.getKey().chunkX(),
                            entry.getKey().chunkZ(),
                            entry.getValue()
                    ))
                    .orElse(null);

            TypeEntryFile fileEntry = new TypeEntryFile();
            fileEntry.id = typeId.toString();
            fileEntry.total = total;
            fileEntry.byDimension = dimensionCounts;
            fileEntry.maxInChunk = peak;
            return fileEntry;
        }
    }

    private record ChunkRef(ResourceLocation dimension, int chunkX, int chunkZ) {
    }

    private static final class DumpFileData {
        @SerializedName("generatedAt")
        private String generatedAt;

        @SerializedName("serverTick")
        private long serverTick;

        @SerializedName("filter")
        private String filter;

        @SerializedName("summary")
        private SummaryFile summary;

        @SerializedName("countsByType")
        private List<TypeEntryFile> countsByType;
    }

    private record SummaryFile(
            @SerializedName("entityTypes") int entityTypes,
            @SerializedName("totalEntities") int totalEntities
    ) {
        private SummaryFile(int entityTypes, int totalEntities) {
            this.entityTypes = entityTypes;
            this.totalEntities = totalEntities;
        }
    }

    private static final class TypeEntryFile {
        @SerializedName("id")
        private String id;

        @SerializedName("total")
        private int total;

        @SerializedName("byDimension")
        private Map<String, Integer> byDimension;

        @SerializedName("maxInChunk")
        private ChunkPeak maxInChunk;

        private int total() {
            return total;
        }
    }

    private static final class ChunkPeak {
        @SerializedName("dimension")
        private String dimension;

        @SerializedName("chunkX")
        private int chunkX;

        @SerializedName("chunkZ")
        private int chunkZ;

        @SerializedName("count")
        private int count;

        private ChunkPeak(String dimension, int chunkX, int chunkZ, int count) {
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.count = count;
        }
    }
}
