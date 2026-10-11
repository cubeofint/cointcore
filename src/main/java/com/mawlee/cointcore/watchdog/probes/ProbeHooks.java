package com.mawlee.cointcore.watchdog.probes;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Thin mixin / event entry points. Disabled probes return immediately.
 */
public final class ProbeHooks {
    private ProbeHooks() {
    }

    public static void onPathRecompute(Entity entity) {
        if (!WatchdogProbeHub.isActive() || !WatchdogProbeSettings.current().pathfinding() || entity == null) {
            return;
        }
        Level level = entity.level();
        if (level == null || level.isClientSide()) {
            return;
        }
        WatchdogProbeHub.instance().pathfinding().increment(
                level.dimension().location().toString(),
                entity.chunkPosition().x,
                entity.chunkPosition().z
        );
    }

    public static void onRandomTickChunk(Level level, LevelChunk chunk, int randomTickSpeed) {
        if (!WatchdogProbeHub.isActive() || !WatchdogProbeSettings.current().blockUpdates() || chunk == null) {
            return;
        }
        if (level == null || level.isClientSide()) {
            return;
        }
        boolean accelerated = randomTickSpeed > 3;
        boolean sampled = ((chunk.getPos().x + chunk.getPos().z + (int) level.getGameTime()) & 15) == 0;
        if (!accelerated && !sampled) {
            return;
        }
        WatchdogProbeHub.instance().blockUpdates().incrementRandomTick(
                level.dimension().location().toString(),
                chunk.getPos().x,
                chunk.getPos().z
        );
    }

    public static void onWorldSaveStart() {
        if (!WatchdogProbeHub.isActive() || !WatchdogProbeSettings.current().diskSave()) {
            return;
        }
        WatchdogProbeHub.instance().diskSave().onWorldSaveStart();
    }

    public static void onWorldSaveEnd(String label) {
        if (!WatchdogProbeHub.isActive() || !WatchdogProbeSettings.current().diskSave()) {
            return;
        }
        WatchdogProbeHub.instance().diskSave().onWorldSaveEnd(label);
    }

    public static void onSavedDataStart() {
        if (!WatchdogProbeHub.isActive() || !WatchdogProbeSettings.current().diskSave()) {
            return;
        }
        WatchdogProbeHub.instance().diskSave().onSavedDataStart();
    }

    public static void onSavedDataEnd(String className) {
        if (!WatchdogProbeHub.isActive() || !WatchdogProbeSettings.current().diskSave()) {
            return;
        }
        WatchdogProbeHub.instance().diskSave().onSavedDataEnd(className);
    }
}
