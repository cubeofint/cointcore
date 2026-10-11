package com.mawlee.cointcore.prefix;

import com.mawlee.cointcore.config.PrefixConfig;
import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import com.mawlee.cointcore.luckperms.LuckPermsUsers;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Pushes the quest-track prefix catalog to the site through AzLink.
 *
 * <p>A {@code full:true} payload is authoritative: the site deletes groups missing from it.
 * Startup and {@code GroupDeleteEvent} always POST (even an empty {@code groups} list).
 */
public final class PrefixCatalogExporter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long MIN_BACKOFF_MS = 5_000L;
    private static final long MAX_BACKOFF_MS = 300_000L;

    private static final AtomicBoolean IN_FLIGHT = new AtomicBoolean();
    private static MinecraftServer server;
    private static int ticks;
    private static int debounceTicks = -1;
    private static boolean debounceIgnoreHash;
    private static int nextPeriodicTick;
    private static int startupTick = -1;
    private static long nextAttemptAt;
    private static long backoffMs;
    private static boolean pendingIgnoreHash;
    private static boolean listenersBound;

    private PrefixCatalogExporter() {
    }

    public static void init(MinecraftServer minecraftServer) {
        server = minecraftServer;
        ticks = 0;
        debounceTicks = -1;
        debounceIgnoreHash = false;
        startupTick = -1;
        nextPeriodicTick = 0;
        nextAttemptAt = 0L;
        backoffMs = 0L;
        pendingIgnoreHash = false;
        if (!LuckPermsIntegration.isAvailable()) {
            LOGGER.info("Prefix catalog export inactive: LuckPerms is not loaded");
            return;
        }
        if (!listenersBound) {
            LuckPermsIntegration.registerGroupCatalogListener(PrefixCatalogExporter::onGroupChanged);
            LuckPermsIntegration.registerGroupDeleteListener(PrefixCatalogExporter::onGroupDeleted);
            listenersBound = true;
        }
        LOGGER.info("Prefix catalog export ready (track={})", PrefixConfig.get().ranksTrack());
    }

    public static void onServerStarted(MinecraftServer minecraftServer) {
        server = minecraftServer;
        PrefixConfig.Settings settings = PrefixConfig.get();
        startupTick = settings.catalogStartupDelaySeconds() * 20;
        nextPeriodicTick = settings.catalogHashIntervalSeconds() * 20;
    }

    public static void tick(MinecraftServer minecraftServer) {
        server = minecraftServer;
        ticks++;
        PrefixConfig.Settings settings = PrefixConfig.get();
        if (!settings.enabled() || !settings.siteSyncEnabled()) {
            return;
        }
        if (startupTick > 0 && ticks >= startupTick) {
            startupTick = -1;
            request(true);
        }
        if (debounceTicks >= 0 && ticks >= debounceTicks) {
            boolean ignoreHash = debounceIgnoreHash;
            debounceTicks = -1;
            debounceIgnoreHash = false;
            request(ignoreHash);
        }
        if (nextPeriodicTick > 0 && ticks >= nextPeriodicTick) {
            nextPeriodicTick = ticks + settings.catalogHashIntervalSeconds() * 20;
            request(false);
        }
        if (System.currentTimeMillis() >= nextAttemptAt && backoffMs > 0L) {
            request(pendingIgnoreHash);
        }
    }

    public static void onGroupChanged() {
        scheduleDebounce(false);
    }

    public static void onGroupDeleted() {
        scheduleDebounce(true);
    }

    private static void scheduleDebounce(boolean ignoreHash) {
        MinecraftServer current = server;
        if (current == null) {
            return;
        }
        current.execute(() -> {
            int delay = PrefixConfig.get().catalogDebounceSeconds() * 20;
            debounceTicks = ticks + delay;
            debounceIgnoreHash = debounceIgnoreHash || ignoreHash;
        });
    }

    public static void export(MinecraftServer minecraftServer, Consumer<ExportResult> callback) {
        server = minecraftServer;
        run(minecraftServer, true, callback);
    }

    static void request(boolean ignoreStoredHash) {
        MinecraftServer current = server;
        if (current == null) {
            return;
        }
        run(current, ignoreStoredHash, null);
    }

    private static void run(MinecraftServer minecraftServer, boolean ignoreStoredHash, Consumer<ExportResult> callback) {
        PrefixConfig.Settings settings = PrefixConfig.get();
        if (!settings.enabled() || !settings.siteSyncEnabled()) {
            complete(callback, ExportResult.unavailable());
            return;
        }
        if (!LuckPermsIntegration.isAvailable() || !AzLinkHttp.isAvailable()) {
            complete(callback, ExportResult.unavailable());
            return;
        }
        pendingIgnoreHash = pendingIgnoreHash || ignoreStoredHash;
        long now = System.currentTimeMillis();
        if (!pendingIgnoreHash && now < nextAttemptAt) {
            complete(callback, ExportResult.retrying());
            return;
        }
        if (!IN_FLIGHT.compareAndSet(false, true)) {
            complete(callback, ExportResult.busy());
            return;
        }
        boolean ignoreHash = pendingIgnoreHash;
        try {
            List<PrefixCatalogEntry> groups = LuckPermsUsers.catalogEntries();
            String hash = PrefixCatalog.hash(groups);
            PrefixSyncSavedData data = PrefixSyncSavedData.get(minecraftServer);
            if (!ignoreHash && hash.equals(data.lastCatalogHash())) {
                backoffMs = 0L;
                nextAttemptAt = 0L;
                pendingIgnoreHash = false;
                IN_FLIGHT.set(false);
                complete(callback, ExportResult.unchanged(hash, groups.size()));
                return;
            }
            String payload = PrefixCatalog.toPayload(groups, hash, true).toString();
            AzLinkHttp.exportCatalog(payload).whenComplete((ignored, error) -> {
                try {
                    if (error != null) {
                        fail(now);
                        LOGGER.debug("Prefix catalog POST failed: {}", error.toString());
                        complete(callback, ExportResult.failed(hash, groups.size(), error));
                        return;
                    }
                    minecraftServer.execute(() -> PrefixSyncSavedData.get(minecraftServer).acceptCatalogHash(hash));
                    backoffMs = 0L;
                    nextAttemptAt = 0L;
                    pendingIgnoreHash = false;
                    LOGGER.info("Prefix catalog exported ({} groups, hash={})", groups.size(), hash);
                    complete(callback, ExportResult.sent(hash, groups.size()));
                } finally {
                    IN_FLIGHT.set(false);
                }
            });
        } catch (RuntimeException exception) {
            fail(now);
            IN_FLIGHT.set(false);
            LOGGER.warn("Prefix catalog export failed", exception);
            complete(callback, ExportResult.failed("", 0, exception));
        }
    }

    private static void fail(long now) {
        backoffMs = backoffMs <= 0L ? MIN_BACKOFF_MS : Math.min(MAX_BACKOFF_MS, backoffMs * 2);
        nextAttemptAt = now + backoffMs;
    }

    private static void complete(Consumer<ExportResult> callback, ExportResult result) {
        if (callback != null) {
            callback.accept(result);
        }
    }

    public record ExportResult(Kind kind, String hash, int groups, Throwable error) {
        public enum Kind {
            SENT,
            UNCHANGED,
            BUSY,
            RETRYING,
            UNAVAILABLE,
            FAILED
        }

        static ExportResult sent(String hash, int groups) {
            return new ExportResult(Kind.SENT, hash, groups, null);
        }

        static ExportResult unchanged(String hash, int groups) {
            return new ExportResult(Kind.UNCHANGED, hash, groups, null);
        }

        static ExportResult busy() {
            return new ExportResult(Kind.BUSY, "", 0, null);
        }

        static ExportResult retrying() {
            return new ExportResult(Kind.RETRYING, "", 0, null);
        }

        static ExportResult unavailable() {
            return new ExportResult(Kind.UNAVAILABLE, "", 0, null);
        }

        static ExportResult failed(String hash, int groups, Throwable error) {
            return new ExportResult(Kind.FAILED, hash, groups, error);
        }
    }
}
