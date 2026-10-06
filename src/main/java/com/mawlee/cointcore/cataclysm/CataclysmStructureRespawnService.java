package com.mawlee.cointcore.cataclysm;

import com.mawlee.cointcore.config.CataclysmStructureRespawnConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spot-based respawn for Cataclysm structure-only elites (same rules as Aptrgangr):
 * 1 mob/spot, per-spot cooldown, despawn when no player nearby.
 */
public final class CataclysmStructureRespawnService {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Structure template path → entity to spawn at local (0,1,0). */
    private static final Map<String, ResourceLocation> TEMPLATE_TO_ENTITY = buildTemplateMap();

    /**
     * Jigsaw pool path suffix {@code .../mob/<name>} → entity id.
     * Octohost pools place drowned_host (+ symbiocto companion handled separately).
     */
    private static final Map<String, ResourceLocation> POOL_MOB_TO_ENTITY = buildPoolMap();

    /** Aptrgangr parent-piece fallback locals (Cataclysm 3.27). */
    private static final Map<String, List<BlockPos>> APTRGANGR_PARENT_LOCALS = Map.of(
            "frosted_prison_mid_6", List.of(
                    new BlockPos(16, 0, 2),
                    new BlockPos(16, 0, 44),
                    new BlockPos(14, 23, 22),
                    new BlockPos(27, 42, 1)
            ),
            "frosted_prison_bottom_5", List.of(new BlockPos(29, 6, 23)),
            "frosted_prison_upper_6", List.of(new BlockPos(47, 4, 23)),
            "abandoned_spire", List.of(new BlockPos(40, 1, 12)),
            "abandoned_temple", List.of(new BlockPos(13, 1, 23))
    );

    private static final ResourceLocation APTRGANGR_ID =
            ResourceLocation.fromNamespaceAndPath("cataclysm", "aptrgangr");
    private static final ResourceLocation SYMBIOCTO_ID =
            ResourceLocation.fromNamespaceAndPath("cataclysm", "symbiocto");
    private static final ResourceLocation DROWNED_HOST_ID =
            ResourceLocation.fromNamespaceAndPath("cataclysm", "drowned_host");

    private static final BlockPos ENTITY_LOCAL = new BlockPos(0, 1, 0);
    private static final Pattern TEMPLATE_ID_PATTERN = Pattern.compile("([a-z0-9_.-]+:[a-z0-9_./-]+)");
    private static final double SPOT_OCCUPIED_DIST_SQ = 3.0D * 3.0D;

    private static final ThreadLocal<Boolean> REFILLING = ThreadLocal.withInitial(() -> false);
    /** cooldownKey (spot+entity) → gameTime started */
    private static final Map<String, Long> SPOT_COOLDOWN_START = new ConcurrentHashMap<>();

    private static int tickCounter;
    private static int debugLogCooldown;

    private CataclysmStructureRespawnService() {
    }

    private static Map<String, ResourceLocation> buildTemplateMap() {
        Map<String, ResourceLocation> map = new HashMap<>();
        put(map, "aptrgangr", "aptrgangr");
        put(map, "draugr_axe", "draugr");
        put(map, "draugr_sword", "draugr");
        put(map, "elite_draugr", "elite_draugr");
        put(map, "royal_draugr_sword", "royal_draugr");
        put(map, "royal_draugr_axe", "royal_draugr");
        put(map, "kobolediator", "kobolediator");
        put(map, "wadjet", "wadjet");
        put(map, "cindaria", "cindaria");
        put(map, "clawdian", "clawdian");
        put(map, "hippocamtus", "hippocamtus");
        put(map, "urchinkin", "urchinkin");
        put(map, "scylla", "scylla");
        put(map, "octohost_sword", "drowned_host");
        put(map, "octohost_trident", "drowned_host");
        put(map, "the_watcher", "the_watcher");
        put(map, "the_prowler", "the_prowler");
        put(map, "the_harbinger", "the_harbinger");
        put(map, "ministrosity", "netherite_ministrosity");
        put(map, "monstrosity", "netherite_monstrosity");
        put(map, "coralssus_blue", "coralssus");
        put(map, "coralssus_red", "coralssus");
        put(map, "coralssus_yellow", "coralssus");
        return Map.copyOf(map);
    }

    private static Map<String, ResourceLocation> buildPoolMap() {
        Map<String, ResourceLocation> map = new HashMap<>();
        put(map, "aptrgangr", "aptrgangr");
        put(map, "draugr", "draugr");
        put(map, "elite_draugr", "elite_draugr");
        put(map, "royal_draugr", "royal_draugr");
        put(map, "kobolediator", "kobolediator");
        put(map, "wadjet", "wadjet");
        put(map, "cindaria", "cindaria");
        put(map, "clawdian", "clawdian");
        put(map, "hippocamtus", "hippocamtus");
        put(map, "urchinkin", "urchinkin");
        put(map, "scylla", "scylla");
        put(map, "octohost_sword", "drowned_host");
        put(map, "octohost_trident", "drowned_host");
        put(map, "the_watcher", "the_watcher");
        put(map, "the_prowler", "the_prowler");
        put(map, "the_harbinger", "the_harbinger");
        put(map, "ministrosity", "netherite_ministrosity");
        put(map, "monstrosity", "netherite_monstrosity");
        return Map.copyOf(map);
    }

    private static void put(Map<String, ResourceLocation> map, String key, String entityPath) {
        map.put(key, ResourceLocation.fromNamespaceAndPath("cataclysm", entityPath));
    }

    public static boolean isRefilling() {
        return Boolean.TRUE.equals(REFILLING.get());
    }

    public static void resetRuntimeState() {
        tickCounter = 0;
        debugLogCooldown = 0;
        SPOT_COOLDOWN_START.clear();
    }

    public static void onTrackedMobDeath(ServerLevel level, Entity entity) {
        if (!CataclysmStructureRespawnConfig.isEnabled()) {
            return;
        }
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (!CataclysmStructureRespawnConfig.tracksMob(typeId)) {
            return;
        }

        StructureStart start = findTrackedStructureAt(level, entity.blockPosition());
        if (start == null || !start.isValid()) {
            return;
        }

        Spot spot = nearestSpot(collectSpawnSpots(level, start), entity.blockPosition(), typeId, 6);
        if (spot == null) {
            // Still cooldown a synthetic spot at death pos for this type.
            markSpotCooldown(level, new Spot(entity.blockPosition(), typeId));
            return;
        }
        markSpotCooldown(level, spot);
    }

    public static void tick(MinecraftServer server) {
        if (!ModList.get().isLoaded("cataclysm") || !CataclysmStructureRespawnConfig.isEnabled()) {
            return;
        }

        try {
            tickInner(server);
        } catch (RuntimeException exception) {
            LOGGER.error("Cataclysm structure respawn tick failed", exception);
        }
    }

    private static void tickInner(MinecraftServer server) {
        if (CataclysmStructureRespawnConfig.isDespawnEnabled()) {
            int despawnEvery = Math.max(1, CataclysmStructureRespawnConfig.getDespawnCheckInterval());
            if (server.getTickCount() % despawnEvery == 0) {
                for (ServerLevel level : server.getAllLevels()) {
                    despawnFarFromPlayers(level);
                }
            }
        }

        int interval = Math.max(1, CataclysmStructureRespawnConfig.getTickInterval());
        if (++tickCounter < interval) {
            return;
        }
        tickCounter = 0;
        if (debugLogCooldown > 0) {
            debugLogCooldown--;
        }

        Set<String> processed = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getDifficulty() == Difficulty.PEACEFUL) {
                continue;
            }
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator()) {
                    continue;
                }
                repopulateNearPlayer(level, player, processed, false);
            }
        }
    }

    public static String forceRepopulate(ServerPlayer player) {
        if (!ModList.get().isLoaded("cataclysm")) {
            return "cataclysm not loaded";
        }
        if (!CataclysmStructureRespawnConfig.isEnabled()) {
            return "cataclysm-structure-respawn disabled";
        }
        ServerLevel level = player.serverLevel();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return "difficulty is peaceful";
        }
        StructureStart start = findTrackedStructureNear(level, player);
        if (start == null || !start.isValid()) {
            return "no tracked cataclysm structure nearby";
        }
        SpawnResult result = repopulate(level, start, player, true);
        return "spots=" + result.spots()
                + " empty=" + result.empty()
                + " spawned=" + result.spawned()
                + " " + result.detail();
    }

    public static String statusNear(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        boolean enabled = CataclysmStructureRespawnConfig.isEnabled() && ModList.get().isLoaded("cataclysm");
        StructureStart start = findTrackedStructureNear(level, player);
        if (start == null || !start.isValid()) {
            return "enabled=" + enabled + " structure=none"
                    + " mobs=" + CataclysmStructureRespawnConfig.getMobIds().size()
                    + " despawn=" + (CataclysmStructureRespawnConfig.isDespawnEnabled()
                    ? CataclysmStructureRespawnConfig.getDespawnDistance()
                    : "off");
        }

        List<Spot> spots = collectSpawnSpots(level, start);
        int occupied = 0;
        int cooling = 0;
        Map<String, int[]> byType = new LinkedHashMap<>();
        for (Spot spot : spots) {
            String path = spot.entityId().getPath();
            int[] counts = byType.computeIfAbsent(path, ignored -> new int[3]);
            counts[0]++; // total spots
            if (isSpotOccupied(level, spot)) {
                occupied++;
                counts[1]++;
            }
            if (respawnDelayRemaining(level, spot) > 0) {
                cooling++;
                counts[2]++;
            }
        }

        StringBuilder types = new StringBuilder();
        for (Map.Entry<String, int[]> entry : byType.entrySet()) {
            if (!types.isEmpty()) {
                types.append(", ");
            }
            int[] c = entry.getValue();
            types.append(entry.getKey()).append("=").append(c[1]).append('/').append(c[0]);
            if (c[2] > 0) {
                types.append("(cd").append(c[2]).append(')');
            }
        }

        BoundingBox box = start.getBoundingBox();
        double dist = distanceToBox(player.getX(), player.getY(), player.getZ(), box);
        return "enabled=" + enabled
                + " structureDist=" + Mth.floor(dist)
                + " spots=" + spots.size()
                + " occupied=" + occupied
                + " cooldown=" + cooling
                + " spotCooldown=" + CataclysmStructureRespawnConfig.getRespawnDelayTicks() + "t"
                + " maxPerSpot=1"
                + " types=[" + types + "]"
                + " despawn=" + (CataclysmStructureRespawnConfig.isDespawnEnabled()
                ? CataclysmStructureRespawnConfig.getDespawnDistance()
                : "off");
    }

    private static void despawnFarFromPlayers(ServerLevel level) {
        if (level.players().isEmpty()) {
            return;
        }
        Set<EntityType<?>> trackedTypes = CataclysmStructureRespawnConfig.getTrackedEntityTypes();
        if (trackedTypes.isEmpty()) {
            return;
        }
        double despawnDistSq = (double) CataclysmStructureRespawnConfig.getDespawnDistance()
                * CataclysmStructureRespawnConfig.getDespawnDistance();
        List<Entity> toDiscard = new ArrayList<>();
        // Type-filtered walk: only Cataclysm tracked types pay distance checks.
        // Full world entity index walk is unavoidable for "far from all players" without a type index.
        for (Entity entity : level.getAllEntities()) {
            if (entity == null || !entity.isAlive() || entity.hasCustomName()) {
                continue;
            }
            if (!trackedTypes.contains(entity.getType())) {
                continue;
            }
            if (anyPlayerWithin(level, entity, despawnDistSq)) {
                continue;
            }
            toDiscard.add(entity);
        }
        for (Entity entity : toDiscard) {
            entity.discard();
        }
    }

    private static boolean anyPlayerWithin(ServerLevel level, Entity entity, double distSq) {
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.distanceToSqr(entity) <= distSq) {
                return true;
            }
        }
        return false;
    }

    private static void repopulateNearPlayer(
            ServerLevel level,
            ServerPlayer player,
            Set<String> processed,
            boolean force
    ) {
        StructureStart start = findTrackedStructureNear(level, player);
        if (start == null || !start.isValid()) {
            return;
        }

        BoundingBox box = start.getBoundingBox();
        String key = level.dimension().location() + "|" + structureKey(box);
        if (!processed.add(key)) {
            return;
        }

        double dist = distanceToBox(player.getX(), player.getY(), player.getZ(), box);
        if (dist > CataclysmStructureRespawnConfig.getPlayerActivationBlocks()) {
            return;
        }

        SpawnResult result = repopulate(level, start, player, force);
        if (result.spawned() > 0 || debugLogCooldown <= 0) {
            LOGGER.info(
                    "Cataclysm structure respawn: player={} dim={} dist={} spots={} empty={} spawned={} {}",
                    player.getGameProfile().getName(),
                    level.dimension().location(),
                    Mth.floor(dist),
                    result.spots(),
                    result.empty(),
                    result.spawned(),
                    result.detail()
            );
            debugLogCooldown = 6;
        }
    }

    private static SpawnResult repopulate(
            ServerLevel level,
            StructureStart start,
            ServerPlayer player,
            boolean force
    ) {
        List<Spot> spots = collectSpawnSpots(level, start);
        if (spots.isEmpty()) {
            return new SpawnResult(0, 0, 0, "noSpots");
        }

        int safetyCap = Math.max(1, CataclysmStructureRespawnConfig.getStructureMax());
        int occupied = 0;
        List<Spot> empty = new ArrayList<>();
        for (Spot spot : spots) {
            if (!CataclysmStructureRespawnConfig.tracksMob(spot.entityId())) {
                continue;
            }
            if (isSpotOccupied(level, spot)) {
                occupied++;
                continue;
            }
            if (!force && isRespawnDelayActive(level, spot)) {
                continue;
            }
            empty.add(spot);
        }

        int allowed = Math.max(0, safetyCap - occupied);
        if (allowed <= 0) {
            return new SpawnResult(spots.size(), 0, 0, "cap full=" + occupied + "/" + safetyCap);
        }

        int maxSpawns = Math.max(1, CataclysmStructureRespawnConfig.getMaxSpawnsPerCycle());
        if (force) {
            maxSpawns = Math.max(maxSpawns, empty.size());
        }

        int minPlayerDist = Math.max(0, CataclysmStructureRespawnConfig.getSpawnMinDistanceFromPlayer());
        double minPlayerDistSq = (double) minPlayerDist * minPlayerDist;
        RandomSource random = level.getRandom();
        int spawned = 0;
        int skippedNearPlayer = 0;
        int failed = 0;

        REFILLING.set(true);
        try {
            for (Spot spot : empty) {
                if (spawned >= maxSpawns || spawned >= allowed) {
                    break;
                }
                if (!level.isLoaded(spot.pos())) {
                    continue;
                }
                if (minPlayerDist > 0 && tooCloseToAnyPlayer(level, spot.pos(), minPlayerDistSq)) {
                    skippedNearPlayer++;
                    continue;
                }
                if (isSpotOccupied(level, spot)) {
                    occupied++;
                    continue;
                }
                SpawnAttempt attempt = spawnAt(level, spot, random);
                if (attempt == SpawnAttempt.OK || attempt == SpawnAttempt.CANCELLED_FORCED) {
                    markSpotCooldown(level, spot);
                    spawned++;
                } else {
                    failed++;
                }
            }
        } finally {
            REFILLING.set(false);
        }

        return new SpawnResult(
                spots.size(),
                empty.size(),
                spawned,
                "occupied=" + occupied
                        + " nearPlayerSkip=" + skippedNearPlayer
                        + " fail=" + failed
                        + " spotCd=" + CataclysmStructureRespawnConfig.getRespawnDelayTicks() + "t"
        );
    }

    private static List<Spot> collectSpawnSpots(ServerLevel level, StructureStart start) {
        StructureTemplateManager templates = level.getServer().getStructureManager();
        List<Spot> fromChildren = new ArrayList<>();
        List<Spot> fromParents = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (StructurePiece piece : start.getPieces()) {
            if (!(piece instanceof PoolElementStructurePiece poolPiece)) {
                continue;
            }
            StructurePoolElement element = poolPiece.getElement();
            ResourceLocation templateId = templateIdOf(element);
            if (templateId == null || !"cataclysm".equals(templateId.getNamespace())) {
                continue;
            }

            String path = templateId.getPath();
            ResourceLocation entityId = TEMPLATE_TO_ENTITY.get(path);
            if (entityId != null && CataclysmStructureRespawnConfig.tracksMob(entityId)) {
                BlockPos pos = transformLocal(poolPiece, ENTITY_LOCAL);
                Spot spot = new Spot(pos, entityId);
                if (seen.add(spotKey(spot))) {
                    fromChildren.add(spot);
                }
                // Octohost also carries symbiocto in the same template — second spot nearby.
                if (DROWNED_HOST_ID.equals(entityId)
                        && CataclysmStructureRespawnConfig.tracksMob(SYMBIOCTO_ID)) {
                    Spot companion = new Spot(pos.above(), SYMBIOCTO_ID);
                    if (seen.add(spotKey(companion))) {
                        fromChildren.add(companion);
                    }
                }
                continue;
            }

            if (!(element instanceof SinglePoolElement single)) {
                continue;
            }

            boolean foundPool = false;
            for (StructureTemplate.StructureBlockInfo info : single.getShuffledJigsawBlocks(
                    templates,
                    poolPiece.getPosition(),
                    poolPiece.getRotation(),
                    level.getRandom()
            )) {
                CompoundTag nbt = info.nbt();
                if (nbt == null) {
                    continue;
                }
                String pool = nbt.getString("pool");
                ResourceLocation poolId = ResourceLocation.tryParse(pool);
                if (poolId == null) {
                    continue;
                }
                String mobKey = poolId.getPath();
                int slash = mobKey.lastIndexOf('/');
                if (slash >= 0) {
                    mobKey = mobKey.substring(slash + 1);
                }
                ResourceLocation fromPool = POOL_MOB_TO_ENTITY.get(mobKey);
                if (fromPool == null || !CataclysmStructureRespawnConfig.tracksMob(fromPool)) {
                    continue;
                }
                foundPool = true;
                Spot spot = new Spot(info.pos().above(), fromPool);
                if (seen.add(spotKey(spot))) {
                    fromParents.add(spot);
                }
            }

            if (foundPool) {
                continue;
            }

            // Hardcoded Aptrgangr parent locals only.
            List<BlockPos> locals = APTRGANGR_PARENT_LOCALS.get(path);
            if (locals == null || !CataclysmStructureRespawnConfig.tracksMob(APTRGANGR_ID)) {
                continue;
            }
            for (BlockPos local : locals) {
                Spot spot = new Spot(transformLocal(poolPiece, local).above(), APTRGANGR_ID);
                if (seen.add(spotKey(spot))) {
                    fromParents.add(spot);
                }
            }
        }

        return fromChildren.isEmpty() ? fromParents : fromChildren;
    }

    private static BlockPos transformLocal(PoolElementStructurePiece piece, BlockPos local) {
        Rotation rotation = piece.getRotation();
        return StructureTemplate.calculateRelativePosition(
                new StructurePlaceSettings().setRotation(rotation),
                local
        ).offset(piece.getPosition());
    }

    private static ResourceLocation templateIdOf(StructurePoolElement element) {
        Matcher matcher = TEMPLATE_ID_PATTERN.matcher(element.toString());
        if (!matcher.find()) {
            return null;
        }
        return ResourceLocation.tryParse(matcher.group(1));
    }

    private static boolean isSpotOccupied(ServerLevel level, Spot spot) {
        EntityType<?> mobType = BuiltInRegistries.ENTITY_TYPE.getOptional(spot.entityId()).orElse(null);
        if (mobType == null) {
            return false;
        }
        BlockPos pos = spot.pos();
        AABB area = new AABB(pos).inflate(2.0D);
        for (Entity entity : level.getEntities(mobType, area, Entity::isAlive)) {
            if (entity.distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D) <= SPOT_OCCUPIED_DIST_SQ) {
                return true;
            }
        }
        return false;
    }

    private static Spot nearestSpot(
            List<Spot> spots,
            BlockPos origin,
            ResourceLocation preferredType,
            int maxDist
    ) {
        Spot best = null;
        double bestDist = (double) maxDist * maxDist;
        for (Spot spot : spots) {
            if (preferredType != null && !preferredType.equals(spot.entityId())) {
                continue;
            }
            double dist = spot.pos().distSqr(origin);
            if (dist <= bestDist) {
                bestDist = dist;
                best = spot;
            }
        }
        if (best != null) {
            return best;
        }
        // Fallback: any tracked spot near death.
        for (Spot spot : spots) {
            double dist = spot.pos().distSqr(origin);
            if (dist <= bestDist) {
                bestDist = dist;
                best = spot;
            }
        }
        return best;
    }

    private static boolean tooCloseToAnyPlayer(ServerLevel level, BlockPos pos, double minDistSq) {
        for (ServerPlayer online : level.players()) {
            if (online.isSpectator()) {
                continue;
            }
            if (online.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < minDistSq) {
                return true;
            }
        }
        return false;
    }

    private static SpawnAttempt spawnAt(ServerLevel level, Spot spot, RandomSource random) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(spot.entityId()).orElse(null);
        if (entityType == null) {
            return SpawnAttempt.CREATE_NULL;
        }

        BlockPos spawnPos = spot.pos();
        Entity created = entityType.create(level);
        if (!(created instanceof Mob mob)) {
            if (created != null) {
                created.discard();
            }
            return SpawnAttempt.CREATE_NULL;
        }

        mob.moveTo(
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D,
                random.nextFloat() * 360.0F,
                0.0F
        );

        mob.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(spawnPos),
                MobSpawnType.STRUCTURE,
                null
        );

        boolean wasCancelled = mob.isSpawnCancelled();
        if (wasCancelled) {
            mob.setSpawnCancelled(false);
        }
        if (mob.isRemoved()) {
            return SpawnAttempt.JOIN_FAILED;
        }

        if (!level.noCollision(mob)) {
            mob.moveTo(
                    spawnPos.getX() + 0.5D,
                    spawnPos.getY() - 1.0D,
                    spawnPos.getZ() + 0.5D,
                    mob.getYRot(),
                    0.0F
            );
            if (!level.noCollision(mob)) {
                mob.discard();
                return SpawnAttempt.NO_POS;
            }
        }

        level.addFreshEntityWithPassengers(mob);
        if (!mob.isAlive() || mob.isRemoved() || !mob.isAddedToLevel()) {
            if (!mob.isRemoved()) {
                mob.discard();
            }
            return SpawnAttempt.JOIN_FAILED;
        }

        return wasCancelled ? SpawnAttempt.CANCELLED_FORCED : SpawnAttempt.OK;
    }

    private static void markSpotCooldown(ServerLevel level, Spot spot) {
        SPOT_COOLDOWN_START.put(spotKey(spot), level.getGameTime());
    }

    private static boolean isRespawnDelayActive(ServerLevel level, Spot spot) {
        int delay = CataclysmStructureRespawnConfig.getRespawnDelayTicks();
        if (delay <= 0) {
            return false;
        }
        Long started = SPOT_COOLDOWN_START.get(spotKey(spot));
        return started != null && level.getGameTime() - started < delay;
    }

    private static long respawnDelayRemaining(ServerLevel level, Spot spot) {
        int delay = CataclysmStructureRespawnConfig.getRespawnDelayTicks();
        if (delay <= 0) {
            return 0;
        }
        Long started = SPOT_COOLDOWN_START.get(spotKey(spot));
        if (started == null) {
            return 0;
        }
        return Math.max(0, delay - (level.getGameTime() - started));
    }

    private static StructureStart findTrackedStructureNear(ServerLevel level, ServerPlayer player) {
        StructureStart atPlayer = findTrackedStructureAt(level, player.blockPosition());
        if (atPlayer != null && atPlayer.isValid()) {
            return atPlayer;
        }

        int radius = Math.max(1, CataclysmStructureRespawnConfig.getChunkScanRadius());
        ChunkPos chunkOrigin = player.chunkPosition();
        StructureStart best = StructureStart.INVALID_START;
        double bestDistance = Double.MAX_VALUE;

        for (int chunkX = chunkOrigin.x - radius; chunkX <= chunkOrigin.x + radius; chunkX++) {
            for (int chunkZ = chunkOrigin.z - radius; chunkZ <= chunkOrigin.z + radius; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                for (StructureStart start : level.structureManager().startsForStructure(
                        new ChunkPos(chunkX, chunkZ),
                        candidate -> isTrackedStructure(level, candidate)
                )) {
                    if (start == null || !start.isValid()) {
                        continue;
                    }
                    double distance = distanceToBox(
                            player.getX(), player.getY(), player.getZ(), start.getBoundingBox()
                    );
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = start;
                    }
                }
            }
        }

        return best.isValid() ? best : StructureStart.INVALID_START;
    }

    private static StructureStart findTrackedStructureAt(ServerLevel level, BlockPos pos) {
        StructureStart withPiece = level.structureManager().getStructureWithPieceAt(
                pos,
                holder -> {
                    ResourceLocation id = level.registryAccess()
                            .registryOrThrow(Registries.STRUCTURE)
                            .getKey(holder.value());
                    return CataclysmStructureRespawnConfig.tracksStructure(id);
                }
        );
        if (withPiece != null && withPiece.isValid()) {
            return withPiece;
        }

        for (ResourceLocation structId : CataclysmStructureRespawnConfig.getStructures()) {
            ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, structId);
            Structure structure = level.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE)
                    .getHolder(key)
                    .map(Holder::value)
                    .orElse(null);
            if (structure == null) {
                continue;
            }
            StructureStart atBox = level.structureManager().getStructureAt(pos, structure);
            if (atBox != null && atBox.isValid()) {
                return atBox;
            }
        }
        return StructureStart.INVALID_START;
    }

    private static boolean isTrackedStructure(ServerLevel level, Structure structure) {
        ResourceLocation id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
        return CataclysmStructureRespawnConfig.tracksStructure(id);
    }

    private static double distanceToBox(double x, double y, double z, BoundingBox box) {
        double closestX = Mth.clamp(x, box.minX(), box.maxX() + 1.0D);
        double closestY = Mth.clamp(y, box.minY(), box.maxY() + 1.0D);
        double closestZ = Mth.clamp(z, box.minZ(), box.maxZ() + 1.0D);
        double dx = x - closestX;
        double dy = y - closestY;
        double dz = z - closestZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static String structureKey(BoundingBox box) {
        return box.minX() + ":" + box.minY() + ":" + box.minZ()
                + ":" + box.maxX() + ":" + box.maxY() + ":" + box.maxZ();
    }

    private static String spotKey(Spot spot) {
        BlockPos pos = spot.pos();
        return spot.entityId() + "@" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }

    private record Spot(BlockPos pos, ResourceLocation entityId) {
    }

    private record SpawnResult(int spots, int empty, int spawned, String detail) {
    }

    private enum SpawnAttempt {
        OK,
        CREATE_NULL,
        CANCELLED_FORCED,
        JOIN_FAILED,
        NO_POS
    }
}
