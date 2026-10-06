package com.mawlee.cointcore.justdirethings;

import com.direwolf20.justdirethings.setup.Registration;
import com.mawlee.cointcore.config.JdtPortalChunkConfig;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Clears persisted Just Dire Things portal force-load tickets on server start
 * so old worlds stop keeping far-away bases ticking with nobody online.
 *
 * <p>Uses reflection for {@code TicketOwner} (package-private in NeoForge) so we do not
 * need an access transformer.
 */
public final class JdtPortalTicketCleanup {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation CONTROLLER_ID =
            ResourceLocation.fromNamespaceAndPath("justdirethings", "chunk_loader");

    private JdtPortalTicketCleanup() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        if (!JdtPortalChunkConfig.isEnabled() || !JdtPortalChunkConfig.isClearStaleTicketsOnStart()) {
            return;
        }
        if (!ModList.get().isLoaded("justdirethings")) {
            return;
        }
        clearAll(event.getServer());
    }

    public static void clearAll(MinecraftServer server) {
        int removed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            removed += clearLevel(level);
        }
        if (removed > 0) {
            LOGGER.info("Cleared {} Just Dire Things portal force-load ticket(s)", removed);
        }
    }

    private static int clearLevel(ServerLevel level) {
        ForcedChunksSavedData saved = level.getDataStorage().get(ForcedChunksSavedData.factory(), "chunks");
        if (saved == null) {
            return 0;
        }

        Object tracker = saved.getEntityForcedChunks();
        int removed = 0;
        removed += releaseTickets(level, invokeMap(tracker, "getChunks"), false);
        removed += releaseTickets(level, invokeMap(tracker, "getTickingChunks"), true);
        if (removed > 0) {
            saved.setDirty();
        }
        return removed;
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, LongSet> invokeMap(Object tracker, String methodName) {
        try {
            Method method = tracker.getClass().getMethod(methodName);
            return (Map<Object, LongSet>) method.invoke(tracker);
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("Could not read ForcedChunks ticket map via {}", methodName, exception);
            return Map.of();
        }
    }

    private static int releaseTickets(ServerLevel level, Map<Object, LongSet> tickets, boolean ticking) {
        if (tickets == null || tickets.isEmpty()) {
            return 0;
        }

        List<Map.Entry<Object, LongSet>> matching = new ArrayList<>();
        for (Map.Entry<Object, LongSet> entry : tickets.entrySet()) {
            ResourceLocation id = ticketOwnerId(entry.getKey());
            if (CONTROLLER_ID.equals(id)) {
                matching.add(entry);
            }
        }

        int removed = 0;
        for (Map.Entry<Object, LongSet> entry : matching) {
            UUID owner = ticketOwnerUuid(entry.getKey());
            if (owner == null) {
                continue;
            }
            for (long chunk : entry.getValue().toLongArray()) {
                int chunkX = ChunkPos.getX(chunk);
                int chunkZ = ChunkPos.getZ(chunk);
                Registration.TICKET_CONTROLLER.forceChunk(level, owner, chunkX, chunkZ, false, ticking);
                removed++;
            }
        }
        return removed;
    }

    private static ResourceLocation ticketOwnerId(Object ticketOwner) {
        try {
            Method id = ticketOwner.getClass().getMethod("id");
            return (ResourceLocation) id.invoke(ticketOwner);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static UUID ticketOwnerUuid(Object ticketOwner) {
        try {
            Method owner = ticketOwner.getClass().getMethod("owner");
            Object value = owner.invoke(ticketOwner);
            return value instanceof UUID uuid ? uuid : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }
}
