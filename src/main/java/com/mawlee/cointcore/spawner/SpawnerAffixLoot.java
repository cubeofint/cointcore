package com.mawlee.cointcore.spawner;

import com.mawlee.cointcore.config.SpawnerLootConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Cheap Apotheosis affix-item substitute for virtual spawner kills.
 * Most rolls fail after a single {@code nextFloat}; generation runs only on hit.
 */
public final class SpawnerAffixLoot {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean APOTHEOSIS = ModList.get().isLoaded("apotheosis");
    private static final float FALLBACK_AFFIX_CHANCE = 0.075F;
    private static final float EQUIPMENT_DROP_CHANCE = 1.0F;
    private static final long PLAYER_SCAN_INTERVAL = 200L;

    private static volatile float cachedGrantChance = Float.NaN;
    private static final Map<Long, RateLimit> RATE_LIMITS = new HashMap<>();
    private static final Map<Long, ContextCache> CONTEXT_CACHE = new HashMap<>();

    private SpawnerAffixLoot() {
    }

    public static void invalidateCaches() {
        cachedGrantChance = Float.NaN;
        RATE_LIMITS.clear();
        CONTEXT_CACHE.clear();
    }

    public static void tryInsert(ServerLevel level, BlockPos spawnerPos) {
        if (!APOTHEOSIS || !SpawnerLootConfig.affixDropsEnabled()) {
            return;
        }
        if (SpawnerLootConfig.affixMaxPerMinute() <= 0 || SpawnerLootConfig.affixChanceMultiplier() <= 0.0F) {
            return;
        }
        RandomSource random = level.getRandom();
        float chance = grantChance() * EQUIPMENT_DROP_CHANCE * SpawnerLootConfig.affixChanceMultiplier();
        if (chance <= 0.0F) {
            return;
        }
        if (chance < 1.0F && random.nextFloat() >= chance) {
            return;
        }
        if (!tryConsumeRate(level, spawnerPos)) {
            return;
        }
        ItemStack stack = ApothBridge.createAffixItem(level, spawnerPos, random);
        if (stack == null || stack.isEmpty()) {
            return;
        }
        SpawnerLootService.insertGenerated(level, spawnerPos, stack);
        if (SpawnerLootConfig.eliteDrops() && random.nextFloat() < 0.01F) {
            ItemStack elite = ApothBridge.createAffixItem(level, spawnerPos, random);
            if (elite != null && !elite.isEmpty()) {
                SpawnerLootService.insertGenerated(level, spawnerPos, elite);
            }
        }
    }

    private static float grantChance() {
        float cached = cachedGrantChance;
        if (!Float.isNaN(cached)) {
            return cached;
        }
        float resolved = ApothBridge.readGrantChance();
        cachedGrantChance = resolved;
        return resolved;
    }

    private static boolean tryConsumeRate(ServerLevel level, BlockPos pos) {
        int max = SpawnerLootConfig.affixMaxPerMinute();
        if (max <= 0) {
            return false;
        }
        long key = pos.asLong();
        long minute = level.getGameTime() / 1200L;
        RateLimit limit = RATE_LIMITS.get(key);
        if (limit == null || limit.minute != minute) {
            limit = new RateLimit(minute, 0);
            RATE_LIMITS.put(key, limit);
        }
        if (limit.count >= max) {
            return false;
        }
        limit.count++;
        return true;
    }

    private static ContextCache contextCache(ServerLevel level, BlockPos pos) {
        long key = pos.asLong();
        ContextCache cache = CONTEXT_CACHE.get(key);
        if (cache == null
                || cache.dimension != level.dimension()
                || cache.pos.getX() != pos.getX()
                || cache.pos.getY() != pos.getY()
                || cache.pos.getZ() != pos.getZ()) {
            cache = new ContextCache(pos.immutable(), level.dimension(), level.getBiome(pos));
            CONTEXT_CACHE.put(key, cache);
        }
        long gameTime = level.getGameTime();
        if (gameTime - cache.lastPlayerScan >= PLAYER_SCAN_INTERVAL) {
            cache.lastPlayerScan = gameTime;
            Player player = level.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 128.0D, false);
            ApothBridge.refreshPlayer(cache, player);
        }
        return cache;
    }

    private static final class RateLimit {
        private final long minute;
        private int count;

        private RateLimit(long minute, int count) {
            this.minute = minute;
            this.count = count;
        }
    }

    private static final class ContextCache {
        private final BlockPos pos;
        private final ResourceKey<Level> dimension;
        private final Holder<Biome> biome;
        private long lastPlayerScan = Long.MIN_VALUE / 2;
        private Object worldTier;
        private float luck;

        private ContextCache(BlockPos pos, ResourceKey<Level> dimension, Holder<Biome> biome) {
            this.pos = pos;
            this.dimension = dimension;
            this.biome = biome;
        }
    }

    /**
     * Isolated so {@code apotheosis} classes load only after {@link ModList} confirms the mod.
     */
    private static final class ApothBridge {
        private ApothBridge() {
        }

        private static float readGrantChance() {
            try {
                float chance = 0.0F;
                boolean found = false;
                for (dev.shadowsoffire.apotheosis.mobs.types.Augmentation aug
                        : dev.shadowsoffire.apotheosis.mobs.registries.AugmentRegistry.getAll()) {
                    for (dev.shadowsoffire.apotheosis.mobs.util.EntityModifier modifier : aug.modifiers()) {
                        if (modifier instanceof dev.shadowsoffire.apotheosis.mobs.util.EntityModifier.RandomAffixItemModifier) {
                            chance = Math.max(chance, aug.chance());
                            found = true;
                        }
                    }
                }
                if (!found) {
                    return FALLBACK_AFFIX_CHANCE;
                }
                return chance;
            } catch (RuntimeException exception) {
                LOGGER.debug("Apotheosis affix chance read failed, using fallback", exception);
                return FALLBACK_AFFIX_CHANCE;
            }
        }

        private static void refreshPlayer(ContextCache cache, Player player) {
            if (player == null) {
                cache.worldTier = dev.shadowsoffire.apotheosis.tiers.WorldTier.HAVEN;
                cache.luck = 0.0F;
                return;
            }
            cache.worldTier = dev.shadowsoffire.apotheosis.tiers.WorldTier.getTier(player);
            cache.luck = player.getLuck();
        }

        private static ItemStack createAffixItem(ServerLevel level, BlockPos pos, RandomSource random) {
            try {
                ContextCache cache = contextCache(level, pos);
                if (cache.worldTier == null) {
                    refreshPlayer(cache, null);
                }
                dev.shadowsoffire.apotheosis.tiers.WorldTier tier =
                        (dev.shadowsoffire.apotheosis.tiers.WorldTier) cache.worldTier;
                dev.shadowsoffire.apotheosis.tiers.GenContext ctx = new dev.shadowsoffire.apotheosis.tiers.GenContext(
                        random,
                        tier,
                        cache.luck,
                        cache.dimension,
                        cache.biome,
                        Set.of()
                );
                return dev.shadowsoffire.apotheosis.loot.LootController.createRandomLootItem(ctx, null);
            } catch (RuntimeException exception) {
                LOGGER.debug("Apotheosis affix item generation failed", exception);
                return ItemStack.EMPTY;
            }
        }
    }
}
