package com.mawlee.cointcore.jeistuff;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * JEI Stuff builds its loot-table JEI recipes lazily in {@code OnDatapackSyncEvent}: the first player to join after a
 * start or {@code /reload} froze the server for ~50 s (every loot table rolled through all loot modifiers). This fills
 * its cache right after start and after each datapack reload instead, before anyone is waiting on it.
 */
public final class JeiStuffWarmup {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOOT_RECIPES = "cy.jdkdigital.jeistuff.recipe.LootRecipes";

    private JeiStuffWarmup() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        warm(event.getServer(), "server start");
    }

    /** Registered at LOWEST priority so it runs after JEI Stuff cleared its cache for the reload. */
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isRunning()) {
            warm(server, "datapack reload");
        }
    }

    private static void warm(MinecraftServer server, String reason) {
        if (server == null || !ModList.get().isLoaded("jeistuff")) {
            return;
        }
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        long start = System.nanoTime();
        try {
            Method method = Class.forName(LOOT_RECIPES).getMethod("getAllLootRecipes", ServerLevel.class);
            Object recipes = method.invoke(null, overworld);
            int size = recipes instanceof java.util.Collection<?> collection ? collection.size() : -1;
            LOGGER.info("Pre-built JEI Stuff loot recipes after {} ({} recipes, {} ms)", reason, size,
                    (System.nanoTime() - start) / 1_000_000L);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Could not pre-build JEI Stuff loot recipes (mod API changed?)", exception);
        } catch (RuntimeException exception) {
            LOGGER.warn("JEI Stuff loot recipe pre-build failed; it will be built on the next login instead", exception);
        }
    }
}
