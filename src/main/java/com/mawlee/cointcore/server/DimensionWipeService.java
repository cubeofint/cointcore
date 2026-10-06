package com.mawlee.cointcore.server;

import com.mawlee.cointcore.config.DimensionWipeConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Comparator;
import java.util.stream.Stream;

public final class DimensionWipeService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String OVERWORLD_ID = "minecraft:overworld";
    private static final Set<String> SENT_MARKERS = ConcurrentHashMap.newKeySet();

    private DimensionWipeService() {
    }

    public static void tick(MinecraftServer server) {
        DimensionWipeConfig.Settings settings = DimensionWipeConfig.get();
        if (!settings.enabled()) {
            return;
        }
        if (settings.schedule().isEmpty() || settings.dimensions().isEmpty()) {
            return;
        }
        if (DimensionWipePending.exists()) {
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(settings.zoneId());
        boolean warningWindow = now.getSecond() <= 1;

        for (DimensionWipeConfig.ScheduleSlot slot : settings.schedule()) {
            ZonedDateTime wipeAt = ZonedDateTime.of(slot.date(), slot.time(), settings.zoneId());
            String slotLabel = slot.date().format(DATE_FORMAT) + " " + slot.time().format(TIME_FORMAT);

            if (warningWindow) {
                for (int minutesBefore : settings.warningsMinutesBefore()) {
                    ZonedDateTime warningAt = wipeAt.minusMinutes(minutesBefore);
                    if (!sameMinute(now, warningAt)) {
                        continue;
                    }
                    String marker = "warn:" + slot.id() + ":" + minutesBefore;
                    if (SENT_MARKERS.add(marker)) {
                        broadcastWarning(server, minutesBefore, slotLabel);
                    }
                }
            }

            long secondsUntil = ChronoUnit.SECONDS.between(now, wipeAt);
            if (secondsUntil < 0 || secondsUntil > settings.restartDelaySeconds()) {
                continue;
            }

            String fireMarker = "fire:" + slot.id();
            if (!SENT_MARKERS.add(fireMarker)) {
                continue;
            }

            if (!beginWipeRestart(server, settings, slot.id(), (int) Math.max(1, secondsUntil))) {
                SENT_MARKERS.remove(fireMarker);
            }
        }
    }

    /**
     * Manual wipe: write pending and restart immediately or with countdown.
     */
    public static boolean triggerNow(MinecraftServer server, int delaySeconds) {
        DimensionWipeConfig.Settings settings = DimensionWipeConfig.get();
        if (settings.dimensions().isEmpty()) {
            return false;
        }
        return beginWipeRestart(server, settings, "manual", Math.max(0, delaySeconds));
    }

    public static Optional<DimensionWipeConfig.ScheduleSlot> nextUpcomingSlot() {
        DimensionWipeConfig.Settings settings = DimensionWipeConfig.get();
        if (!settings.enabled() || settings.schedule().isEmpty()) {
            return Optional.empty();
        }
        ZonedDateTime now = ZonedDateTime.now(settings.zoneId());
        return settings.schedule().stream()
                .filter(slot -> !ZonedDateTime.of(slot.date(), slot.time(), settings.zoneId()).isBefore(now))
                .min(Comparator.comparing((DimensionWipeConfig.ScheduleSlot slot) ->
                        ZonedDateTime.of(slot.date(), slot.time(), settings.zoneId())));
    }

    public static String statusSummary() {
        DimensionWipeConfig.Settings settings = DimensionWipeConfig.get();
        String next = nextUpcomingSlot()
                .map(slot -> slot.date().format(DATE_FORMAT) + " " + slot.time().format(TIME_FORMAT))
                .orElse("none");
        String dims = settings.dimensions().isEmpty()
                ? "none"
                : String.join(", ", settings.dimensions().stream().map(ResourceLocation::toString).toList());
        return "enabled=" + settings.enabled()
                + ", pending=" + DimensionWipePending.exists()
                + ", next=" + next
                + ", dims=[" + dims + "]"
                + ", zone=" + settings.zoneId();
    }

    public static void executePendingWipe(MinecraftServer server) {
        Optional<DimensionWipePending.PendingWipe> pendingOpt = DimensionWipePending.read();
        if (pendingOpt.isEmpty()) {
            return;
        }

        DimensionWipePending.PendingWipe pending = pendingOpt.get();
        LOGGER.info("Executing pending dimension wipe for slot '{}'", pending.slotId());

        Set<String> wipeDims = resolveWipeDimensions(pending.dimensions(), pending.allowOverworldWipe());
        if (wipeDims.isEmpty()) {
            LOGGER.warn("Pending dimension wipe had no valid dimensions; clearing marker");
            DimensionWipePending.clear();
            return;
        }

        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        int relocated = relocatePlayersInWipeDims(worldRoot, wipeDims, pending.spawnX(), pending.spawnY(), pending.spawnZ());
        int deleted = 0;
        for (String dimId : wipeDims) {
            deleted += wipeDimensionFolders(worldRoot, ResourceLocation.parse(dimId));
        }

        DimensionWipePending.clear();
        LOGGER.info(
                "Dimension wipe complete (slot={}, playersRelocated={}, foldersDeleted={})",
                pending.slotId(),
                relocated,
                deleted
        );
    }

    public static void resetRuntimeState() {
        SENT_MARKERS.clear();
    }

    private static boolean beginWipeRestart(
            MinecraftServer server,
            DimensionWipeConfig.Settings settings,
            String slotId,
            int delaySeconds
    ) {
        List<String> dims = resolveWipeDimensions(
                settings.dimensions().stream().map(ResourceLocation::toString).toList(),
                settings.allowOverworldWipe()
        ).stream().sorted().toList();
        if (dims.isEmpty()) {
            LOGGER.warn("Dimension wipe skipped: no valid dimensions configured");
            return false;
        }

        SpawnPoint spawn = readOverworldSpawn(server);
        boolean wrote = DimensionWipePending.write(new DimensionWipePending.PendingWipe(
                slotId,
                dims,
                settings.allowOverworldWipe(),
                spawn.x(),
                spawn.y(),
                spawn.z()
        ));
        if (!wrote) {
            return false;
        }

        broadcastWipeRestart(server, String.join(", ", dims));

        try {
            if (delaySeconds <= 1) {
                ServerRestartService.executeImmediateRestart(server);
            } else {
                ServerRestartService.scheduleRestart(
                        server,
                        delaySeconds,
                        CointCoreMessages.DIMWIPE_RESTART_SCHEDULED,
                        delaySeconds
                );
            }
            return true;
        } catch (Throwable exception) {
            LOGGER.error("Failed to schedule restart for dimension wipe", exception);
            DimensionWipePending.clear();
            return false;
        }
    }

    private static Set<String> resolveWipeDimensions(List<String> configured, boolean allowOverworldWipe) {
        Set<String> result = new HashSet<>();
        for (String raw : configured) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String id;
            try {
                id = ResourceLocation.parse(raw.trim()).toString();
            } catch (RuntimeException exception) {
                LOGGER.warn("Skipping invalid wipe dimension '{}'", raw);
                continue;
            }
            if (OVERWORLD_ID.equals(id) && !allowOverworldWipe) {
                LOGGER.warn("Skipping overworld wipe (allowOverworldWipe=false)");
                continue;
            }
            result.add(id);
        }
        return result;
    }

    private static SpawnPoint readOverworldSpawn(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            BlockPos spawn = overworld.getSharedSpawnPos();
            return new SpawnPoint(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        }
        return new SpawnPoint(0.5D, 64.0D, 0.5D);
    }

    private static int relocatePlayersInWipeDims(
            Path worldRoot,
            Set<String> wipeDims,
            double spawnX,
            double spawnY,
            double spawnZ
    ) {
        Path playerDataDir = worldRoot.resolve("playerdata");
        if (!Files.isDirectory(playerDataDir)) {
            LOGGER.warn("Playerdata directory missing at {}", playerDataDir);
            return 0;
        }

        int relocated = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(playerDataDir, "*.dat")) {
            for (Path playerFile : stream) {
                if (relocatePlayerFile(playerFile, wipeDims, spawnX, spawnY, spawnZ)) {
                    relocated++;
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to scan playerdata for dimension wipe", exception);
        }
        return relocated;
    }

    private static boolean relocatePlayerFile(
            Path playerFile,
            Set<String> wipeDims,
            double spawnX,
            double spawnY,
            double spawnZ
    ) {
        try {
            CompoundTag tag = NbtIo.readCompressed(playerFile, NbtAccounter.unlimitedHeap());
            if (tag == null || !tag.contains("Dimension", Tag.TAG_STRING)) {
                return false;
            }

            String dimension = tag.getString("Dimension");
            ResourceLocation dimId;
            try {
                dimId = ResourceLocation.parse(dimension);
            } catch (RuntimeException exception) {
                return false;
            }
            if (!wipeDims.contains(dimId.toString())) {
                return false;
            }

            tag.putString("Dimension", OVERWORLD_ID);

            ListTag pos = new ListTag();
            pos.add(DoubleTag.valueOf(spawnX));
            pos.add(DoubleTag.valueOf(spawnY));
            pos.add(DoubleTag.valueOf(spawnZ));
            tag.put("Pos", pos);

            ListTag motion = new ListTag();
            motion.add(DoubleTag.valueOf(0.0D));
            motion.add(DoubleTag.valueOf(0.0D));
            motion.add(DoubleTag.valueOf(0.0D));
            tag.put("Motion", motion);

            NbtIo.writeCompressed(tag, playerFile);
            return true;
        } catch (IOException exception) {
            LOGGER.warn("Failed to relocate playerdata {}", playerFile.getFileName(), exception);
            return false;
        }
    }

    private static int wipeDimensionFolders(Path worldRoot, ResourceLocation dimId) {
        List<Path> targets = dimensionDataFolders(worldRoot, dimId);
        int deleted = 0;
        for (Path folder : targets) {
            if (!Files.isDirectory(folder)) {
                continue;
            }
            try {
                deleteRecursively(folder);
                deleted++;
                LOGGER.info("Deleted dimension data folder {}", folder);
            } catch (IOException exception) {
                LOGGER.error("Failed to delete dimension folder {}", folder, exception);
            }
        }
        return deleted;
    }

    private static List<Path> dimensionDataFolders(Path worldRoot, ResourceLocation dimId) {
        List<Path> folders = new ArrayList<>(3);
        if (OVERWORLD_ID.equals(dimId.toString())) {
            folders.add(worldRoot.resolve("region"));
            folders.add(worldRoot.resolve("entities"));
            folders.add(worldRoot.resolve("poi"));
            return folders;
        }

        Path dimRoot = worldRoot.resolve("dimensions").resolve(dimId.getNamespace()).resolve(dimId.getPath());
        folders.add(dimRoot.resolve("region"));
        folders.add(dimRoot.resolve("entities"));
        folders.add(dimRoot.resolve("poi"));
        return folders;
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static boolean sameMinute(ZonedDateTime a, ZonedDateTime b) {
        return a.toLocalDate().equals(b.toLocalDate())
                && a.getHour() == b.getHour()
                && a.getMinute() == b.getMinute();
    }

    private static void broadcastWarning(MinecraftServer server, int minutesBefore, String slotLabel) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(
                    player,
                    CointCoreMessages.DIMWIPE_WARNING,
                    slotLabel,
                    minutesBefore
            ));
        }
    }

    private static void broadcastWipeRestart(MinecraftServer server, String dims) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(
                    player,
                    CointCoreMessages.DIMWIPE_STARTING,
                    dims
            ));
        }
    }

    private record SpawnPoint(double x, double y, double z) {
    }
}
