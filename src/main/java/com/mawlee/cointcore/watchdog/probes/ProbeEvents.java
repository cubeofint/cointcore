package com.mawlee.cointcore.watchdog.probes;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.watchdog.WatchdogHotFlags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Existing NeoForge events → cheap probe increments. Read-only.
 */
@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class ProbeEvents {
    private ProbeEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!WatchdogHotFlags.PROBE_CHUNKS) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        WatchdogProbeHub.recordChunkLoad(level, chunk, event.isNewChunk());
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!WatchdogHotFlags.PROBE_BLOCK_UPDATES || event.getLevel().isClientSide()) {
            return;
        }
        WatchdogProbeHub.recordNeighborUpdate();
    }

    @SubscribeEvent
    public static void onLevelSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel)) {
            return;
        }
        if (!WatchdogHotFlags.PROBE_DISK_SAVE) {
            return;
        }
        WatchdogProbeHub.instance().diskSave().onLevelSaveEvent();
    }
}
