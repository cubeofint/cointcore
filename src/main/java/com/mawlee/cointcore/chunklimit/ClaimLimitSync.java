package com.mawlee.cointcore.chunklimit;

import com.mawlee.cointcore.config.ChunkLimitConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.lang.reflect.Proxy;

/**
 * Keeps {@link TeamLimitIndex} aligned when FTB claims change on an already-loaded chunk.
 */
public final class ClaimLimitSync {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean registered;

    private ClaimLimitSync() {
    }

    public static void register() {
        if (registered || !ModList.get().isLoaded("ftbchunks")) {
            return;
        }
        try {
            Class<?> eventClass = Class.forName("dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent");
            Class<?> afterInterface = Class.forName("dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent$After");
            Class<?> architecturyEvent = Class.forName("dev.architectury.event.Event");

            Object handler = Proxy.newProxyInstance(
                    afterInterface.getClassLoader(),
                    new Class<?>[]{afterInterface},
                    (proxy, method, args) -> {
                        if ("after".equals(method.getName()) && args != null && args.length == 2) {
                            onClaimChanged(args[1]);
                            return null;
                        }
                        return switch (method.getName()) {
                            case "toString" -> "ClaimLimitSyncAfterHandler";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> Boolean.valueOf(proxy == args[0]);
                            default -> null;
                        };
                    }
            );

            for (String field : new String[]{"AFTER_CLAIM", "AFTER_UNCLAIM"}) {
                Object event = eventClass.getField(field).get(null);
                architecturyEvent.getMethod("register", Object.class).invoke(event, handler);
            }
            registered = true;
            LOGGER.info("Registered FTB claim sync for team chunk limits");
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("Failed to register FTB claim sync for team limits", exception);
        }
    }

    private static void onClaimChanged(Object claimedChunk) {
        if (!ChunkLimitConfig.isEnabled() || !ChunkLimitConfig.hasTeamBlockLimits()) {
            return;
        }
        try {
            Object pos = claimedChunk.getClass().getMethod("getPos").invoke(claimedChunk);
            @SuppressWarnings("unchecked")
            ResourceKey<Level> dimKey = (ResourceKey<Level>) pos.getClass().getMethod("dimension").invoke(pos);
            int x = (Integer) pos.getClass().getMethod("x").invoke(pos);
            int z = (Integer) pos.getClass().getMethod("z").invoke(pos);

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return;
            }
            ServerLevel level = server.getLevel(dimKey);
            if (level == null) {
                return;
            }
            ChunkPos chunkPos = new ChunkPos(x, z);
            if (level.hasChunk(x, z)) {
                ChunkLimitIndex.rebuildChunk(level, level.getChunk(x, z));
                MobLimitService.cullOverLimit(level, chunkPos);
                MobLimitService.syncTeamIndex(level, chunkPos);
            } else {
                TeamLimitIndex.clearChunk(level, chunkPos);
                TeamMobLimitIndex.clearChunk(level, chunkPos);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.debug("Claim sync for team limits failed", exception);
        }
    }
}
