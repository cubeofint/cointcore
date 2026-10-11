package com.mawlee.cointcore.prefix;

import com.google.gson.JsonObject;
import com.mawlee.cointcore.config.PrefixConfig;
import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import com.mawlee.cointcore.luckperms.LuckPermsUsers;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Walks every known LuckPerms user and reports quest-prefix availability to the site.
 *
 * <p>Loads {@code loadUser} in batches of 25–50 with a 1–2s pause, one batch at a time,
 * pauses when TPS &lt; 18, keeps a cursor in SavedData, and {@code cleanupUser}s offline users.
 * Report POSTs go out in chunks of 100–200.
 */
public final class PrefixBulkReportService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean LOADING = new AtomicBoolean();
    private static final AtomicBoolean SENDING = new AtomicBoolean();

    private static List<UUID> users;
    private static int index;
    private static final List<JsonObject> pending = new ArrayList<>();
    private static long pauseUntilMs;
    private static boolean finishing;
    private static boolean sessionActive;

    private PrefixBulkReportService() {
    }

    public static boolean isRunning(MinecraftServer server) {
        return sessionActive
                || PrefixSyncSavedData.get(server).reportAllRunning()
                || LOADING.get()
                || SENDING.get();
    }

    public static boolean start(MinecraftServer server) {
        PrefixSyncSavedData data = PrefixSyncSavedData.get(server);
        if (sessionActive || users != null || LOADING.get() || SENDING.get()) {
            return false;
        }
        sessionActive = true;
        if (data.reportAllRunning()) {
            data.resumeReportAll();
        } else {
            data.startReportAll();
        }
        users = null;
        index = 0;
        pending.clear();
        pauseUntilMs = 0L;
        finishing = false;
        LOADING.set(false);
        SENDING.set(false);
        LOGGER.info("Prefix report-all started (cursor={})", data.reportAllCursor());
        return true;
    }

    public static void onServerStarted(MinecraftServer server) {
        if (PrefixSyncSavedData.get(server).reportAllRunning()) {
            sessionActive = true;
            users = null;
            index = 0;
            pending.clear();
            LOGGER.info("Resuming prefix report-all from cursor {}", PrefixSyncSavedData.get(server).reportAllCursor());
        }
    }

    public static void tick(MinecraftServer server) {
        PrefixConfig.Settings settings = PrefixConfig.get();
        if (!settings.enabled() || !settings.siteSyncEnabled()) {
            return;
        }
        PrefixSyncSavedData data = PrefixSyncSavedData.get(server);
        if (!data.reportAllRunning()) {
            return;
        }
        if (!LuckPermsIntegration.isAvailable() || !AzLinkHttp.isAvailable()) {
            return;
        }
        if (LOADING.get() || SENDING.get() || finishing) {
            return;
        }
        if (System.currentTimeMillis() < pauseUntilMs) {
            return;
        }
        if (tps(server) < settings.reportAllMinTps()) {
            return;
        }
        if (users == null) {
            beginListing(server, data);
            return;
        }
        if (index >= users.size()) {
            flushAndFinish(server, data);
            return;
        }
        loadNextBatch(server, data, settings);
    }

    private static void beginListing(MinecraftServer server, PrefixSyncSavedData data) {
        if (!LOADING.compareAndSet(false, true)) {
            return;
        }
        LuckPermsUsers.uniqueUsers().whenComplete((ids, error) -> server.execute(() -> {
            try {
                if (error != null || ids == null) {
                    LOGGER.warn("Prefix report-all failed to list users", error);
                    sessionActive = false;
                    data.finishReportAll();
                    return;
                }
                users = PrefixCatalog.sortAfterCursor(ids, data.reportAllCursor());
                index = 0;
                LOGGER.info("Prefix report-all listed {} remaining users", users.size());
            } finally {
                LOADING.set(false);
            }
        }));
    }

    private static void loadNextBatch(MinecraftServer server, PrefixSyncSavedData data, PrefixConfig.Settings settings) {
        if (!LOADING.compareAndSet(false, true)) {
            return;
        }
        int end = Math.min(users.size(), index + settings.reportAllBatchSize());
        List<UUID> batch = List.copyOf(users.subList(index, end));
        loadBatch(server, batch, 0, new ArrayList<>()).whenComplete((reports, error) -> server.execute(() -> {
            try {
                if (error != null) {
                    LOGGER.debug("Prefix report-all batch failed: {}", error.toString());
                    return;
                }
                pending.addAll(reports);
                index = end;
                if (!batch.isEmpty()) {
                    data.advanceReportAll(batch.get(batch.size() - 1).toString());
                }
                pauseUntilMs = System.currentTimeMillis() + settings.reportAllBatchPauseMs();
                if (pending.size() >= settings.reportAllChunkSize()) {
                    sendChunk(server, settings.reportAllChunkSize());
                }
            } finally {
                LOADING.set(false);
            }
        }));
    }

    private static CompletableFuture<List<JsonObject>> loadBatch(
            MinecraftServer server,
            List<UUID> batch,
            int offset,
            List<JsonObject> acc
    ) {
        if (offset >= batch.size()) {
            return CompletableFuture.completedFuture(acc);
        }
        UUID playerId = batch.get(offset);
        return LuckPermsUsers.loadUser(playerId).handle((user, error) -> {
            if (error == null && user != null) {
                try {
                    PrefixSnapshot snapshot = LuckPermsUsers.snapshotOf(user);
                    acc.add(PrefixSiteBridge.playerReport(server, playerId, snapshot));
                    if (server.getPlayerList().getPlayer(playerId) == null) {
                        LuckPermsUsers.cleanupUser(user);
                    }
                } catch (RuntimeException exception) {
                    LOGGER.debug("Prefix report-all snapshot failed for {}", playerId, exception);
                }
            }
            return acc;
        }).thenCompose(next -> loadBatch(server, batch, offset + 1, next));
    }

    private static void flushAndFinish(MinecraftServer server, PrefixSyncSavedData data) {
        if (!pending.isEmpty()) {
            sendChunk(server, pending.size());
            return;
        }
        finishing = false;
        sessionActive = false;
        users = null;
        index = 0;
        data.finishReportAll();
        LOGGER.info("Prefix report-all finished");
    }

    private static void sendChunk(MinecraftServer server, int size) {
        if (pending.isEmpty() || !SENDING.compareAndSet(false, true)) {
            return;
        }
        int take = Math.min(size, pending.size());
        List<JsonObject> chunk = new ArrayList<>(pending.subList(0, take));
        pending.subList(0, take).clear();
        PrefixSiteBridge.postReports(chunk).whenComplete((ignored, error) -> {
            if (error != null) {
                LOGGER.debug("Prefix report-all chunk POST failed: {}", error.toString());
                pending.addAll(0, chunk);
            } else {
                LOGGER.info("Prefix report-all sent {} players", chunk.size());
            }
            SENDING.set(false);
        });
    }

    static double tps(MinecraftServer server) {
        double mspt = server.getAverageTickTimeNanos() / 1_000_000.0D;
        if (mspt <= 0.0D) {
            return 20.0D;
        }
        return Math.min(20.0D, 1000.0D / mspt);
    }

}
