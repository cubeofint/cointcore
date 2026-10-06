package com.mawlee.cointcore.cataclysm;

import com.mawlee.cointcore.config.SunkenCityRespawnConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Refills Cataclysm sunken_city mobs in a ring around {@code altar_of_abyss}.
 * Natural density: structure-wide hard cap + soft cap near the altar; never on the player.
 * After a tracked mob dies, that type waits {@code respawnDelayTicks} before refill.
 */
public final class SunkenCityRespawnService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation SUNKEN_CITY_ID = ResourceLocation.fromNamespaceAndPath("cataclysm", "sunken_city");
    private static final ResourceKey<Structure> SUNKEN_CITY_KEY = ResourceKey.create(Registries.STRUCTURE, SUNKEN_CITY_ID);

    private static final ThreadLocal<Boolean> REFILLING = ThreadLocal.withInitial(() -> false);

    /** structureKey + ":" + entityId → gameTime of last death */
    private static final Map<String, Long> LAST_DEATH_GAME_TIME = new ConcurrentHashMap<>();

    private static int tickCounter;
    private static int debugLogCooldown;

    private SunkenCityRespawnService() {
    }

    public static boolean isRefilling() {
        return Boolean.TRUE.equals(REFILLING.get());
    }

    public static void resetRuntimeState() {
        tickCounter = 0;
        debugLogCooldown = 0;
        LAST_DEATH_GAME_TIME.clear();
    }

    /**
     * Called when a configured sunken-city mob dies inside the structure.
     */
    public static void onTrackedMobDeath(ServerLevel level, Entity entity) {
        if (!SunkenCityRespawnConfig.isEnabled() || level.dimension() != Level.OVERWORLD) {
            return;
        }

        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (typeId == null || !SunkenCityRespawnConfig.getMobTargets().containsKey(typeId)) {
            return;
        }

        StructureStart start = level.structureManager().getStructureWithPieceAt(
                entity.blockPosition(),
                holder -> holder.is(SUNKEN_CITY_KEY)
        );
        if (start == null || !start.isValid()) {
            Structure structure = level.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE)
                    .getHolder(SUNKEN_CITY_KEY)
                    .map(Holder::value)
                    .orElse(null);
            if (structure == null) {
                return;
            }
            start = level.structureManager().getStructureAt(entity.blockPosition(), structure);
            if (start == null || !start.isValid()) {
                return;
            }
        }

        String key = deathKey(structureKey(start.getBoundingBox()), typeId);
        LAST_DEATH_GAME_TIME.put(key, level.getGameTime());
    }

    private static boolean isRespawnDelayActive(
            ServerLevel level,
            BoundingBox box,
            ResourceLocation typeId,
            boolean force
    ) {
        if (force) {
            return false;
        }
        int delay = SunkenCityRespawnConfig.getRespawnDelayTicks();
        if (delay <= 0) {
            return false;
        }
        Long lastDeath = LAST_DEATH_GAME_TIME.get(deathKey(structureKey(box), typeId));
        if (lastDeath == null) {
            return false;
        }
        return level.getGameTime() - lastDeath < delay;
    }

    private static long respawnDelayRemaining(
            ServerLevel level,
            BoundingBox box,
            ResourceLocation typeId
    ) {
        int delay = SunkenCityRespawnConfig.getRespawnDelayTicks();
        if (delay <= 0) {
            return 0;
        }
        Long lastDeath = LAST_DEATH_GAME_TIME.get(deathKey(structureKey(box), typeId));
        if (lastDeath == null) {
            return 0;
        }
        long remaining = delay - (level.getGameTime() - lastDeath);
        return Math.max(0, remaining);
    }

    private static String deathKey(String structureKey, ResourceLocation typeId) {
        return structureKey + ":" + typeId;
    }

    public static void tick(MinecraftServer server) {
        if (!ModList.get().isLoaded("cataclysm") || !SunkenCityRespawnConfig.isEnabled()) {
            return;
        }

        int interval = SunkenCityRespawnConfig.getTickInterval();
        if (++tickCounter < interval) {
            return;
        }
        tickCounter = 0;
        if (debugLogCooldown > 0) {
            debugLogCooldown--;
        }

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null || overworld.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }

        if (overworld.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(SUNKEN_CITY_KEY).isEmpty()) {
            return;
        }

        Set<String> processedStructures = new HashSet<>();
        for (ServerPlayer player : overworld.players()) {
            if (player.isSpectator()) {
                continue;
            }
            repopulateNearPlayer(overworld, player, processedStructures);
        }
    }

    public static String forceRepopulate(ServerPlayer player) {
        if (!ModList.get().isLoaded("cataclysm")) {
            return "cataclysm not loaded";
        }
        if (!SunkenCityRespawnConfig.isEnabled()) {
            return "sunken-city-respawn disabled in config";
        }

        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD) {
            return "player is not in overworld";
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return "difficulty is peaceful";
        }

        StructureStart start = findSunkenCityNear(level, player);
        if (start == null || !start.isValid()) {
            return "no sunken_city structure found";
        }

        BoundingBox box = start.getBoundingBox();
        BlockPos altar = findAltarOfAbyss(level, box, player.blockPosition());
        if (altar == null) {
            return "no " + SunkenCityRespawnConfig.getAltarBlockId() + " found in loaded sunken_city chunks";
        }

        SpawnResult result = repopulateAroundAltar(level, box, altar, player, true);
        return "altar=" + altar.getX() + "," + altar.getY() + "," + altar.getZ()
                + " spawned=" + result.spawned()
                + " " + result.detail();
    }

    public static String statusNear(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        boolean enabled = SunkenCityRespawnConfig.isEnabled() && ModList.get().isLoaded("cataclysm");
        StructureStart start = findSunkenCityNear(level, player);
        if (start == null || !start.isValid()) {
            return "enabled=" + enabled + " structure=none";
        }

        BoundingBox box = start.getBoundingBox();
        BlockPos altar = findAltarOfAbyss(level, box, player.blockPosition());
        if (altar == null) {
            return "enabled=" + enabled + " structure=yes altar=none";
        }

        AABB localArea = localCountArea(altar, box);
        AABB fullArea = boxToAabb(box);
        int localMax = SunkenCityRespawnConfig.getLocalMaxPerType();
        StringBuilder counts = new StringBuilder();
        for (Map.Entry<ResourceLocation, Integer> target : SunkenCityRespawnConfig.getMobTargets().entrySet()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(target.getKey()).orElse(null);
            int local = type == null ? -1 : level.getEntities(type, localArea, Entity::isAlive).size();
            int full = type == null ? -1 : level.getEntities(type, fullArea, Entity::isAlive).size();
            int softLocal = Math.min(target.getValue(), localMax);
            if (!counts.isEmpty()) {
                counts.append(", ");
            }
            counts.append(target.getKey().getPath())
                    .append(" nearAltar=")
                    .append(local)
                    .append('/')
                    .append(softLocal)
                    .append(" all=")
                    .append(full)
                    .append('/')
                    .append(target.getValue());
        }

        double altarDist = Math.sqrt(player.distanceToSqr(altar.getX() + 0.5D, altar.getY() + 0.5D, altar.getZ() + 0.5D));
        return "enabled=" + enabled
                + " altar=" + altar.getX() + "," + altar.getY() + "," + altar.getZ()
                + " altarDist=" + Mth.floor(altarDist)
                + " ring=" + SunkenCityRespawnConfig.getSpawnMinDistanceFromAltar()
                + "-" + SunkenCityRespawnConfig.getSpawnMaxDistanceFromAltar()
                + " interval=" + SunkenCityRespawnConfig.getTickInterval()
                + " deathDelay=" + SunkenCityRespawnConfig.getRespawnDelayTicks()
                + " mobs=[" + counts + "]";
    }

    private static void repopulateNearPlayer(
            ServerLevel level,
            ServerPlayer player,
            Set<String> processedStructures
    ) {
        StructureStart start = findSunkenCityNear(level, player);
        if (start == null || !start.isValid()) {
            return;
        }

        BoundingBox box = start.getBoundingBox();
        if (!processedStructures.add(structureKey(box))) {
            return;
        }

        BlockPos altar = findAltarOfAbyss(level, box, player.blockPosition());
        if (altar == null) {
            return;
        }

        double altarDist = Math.sqrt(player.distanceToSqr(altar.getX() + 0.5D, altar.getY() + 0.5D, altar.getZ() + 0.5D));
        if (altarDist > SunkenCityRespawnConfig.getPlayerActivationBlocks()) {
            return;
        }

        SpawnResult result = repopulateAroundAltar(level, box, altar, player, false);
        if (result.spawned() > 0 || debugLogCooldown <= 0) {
            LOGGER.info(
                    "Sunken city respawn: player={} altar={} {},{},{} altarDist={} spawned={} {}",
                    player.getGameProfile().getName(),
                    SunkenCityRespawnConfig.getAltarBlockId(),
                    altar.getX(),
                    altar.getY(),
                    altar.getZ(),
                    Mth.floor(altarDist),
                    result.spawned(),
                    result.detail()
            );
            debugLogCooldown = 6;
        }
    }

    private static StructureStart findSunkenCityNear(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();

        StructureStart atPlayer = level.structureManager().getStructureWithPieceAt(
                origin,
                holder -> holder.is(SUNKEN_CITY_KEY)
        );
        if (atPlayer != null && atPlayer.isValid()) {
            return atPlayer;
        }

        Structure structure = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .getHolder(SUNKEN_CITY_KEY)
                .map(Holder::value)
                .orElse(null);
        if (structure != null) {
            StructureStart atBox = level.structureManager().getStructureAt(origin, structure);
            if (atBox != null && atBox.isValid()) {
                return atBox;
            }
        }

        int radius = Math.max(1, SunkenCityRespawnConfig.getChunkScanRadius());
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
                        candidate -> isSunkenCity(level, candidate)
                )) {
                    if (start == null || !start.isValid()) {
                        continue;
                    }
                    double distance = distanceToBox(player.getX(), player.getY(), player.getZ(), start.getBoundingBox());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = start;
                    }
                }
            }
        }

        return best.isValid() ? best : StructureStart.INVALID_START;
    }

    private static boolean isSunkenCity(ServerLevel level, Structure structure) {
        return SUNKEN_CITY_ID.equals(level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure));
    }

    /**
     * Find altar_of_abyss via block entities in loaded chunks intersecting the structure.
     * Prefers the altar closest to {@code near}.
     */
    private static BlockPos findAltarOfAbyss(ServerLevel level, BoundingBox box, BlockPos near) {
        Block altarBlock = BuiltInRegistries.BLOCK.getOptional(SunkenCityRespawnConfig.getAltarBlockId()).orElse(null);
        if (altarBlock == null) {
            return null;
        }

        int minChunkX = box.minX() >> 4;
        int maxChunkX = box.maxX() >> 4;
        int minChunkZ = box.minZ() >> 4;
        int maxChunkZ = box.maxZ() >> 4;

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos pos = blockEntity.getBlockPos();
                    if (!box.isInside(pos) || !blockEntity.getBlockState().is(altarBlock)) {
                        continue;
                    }
                    double dist = near.distSqr(pos);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = pos.immutable();
                    }
                }
            }
        }

        return best;
    }

    private static SpawnResult repopulateAroundAltar(
            ServerLevel level,
            BoundingBox box,
            BlockPos altar,
            ServerPlayer player,
            boolean force
    ) {
        AABB localArea = localCountArea(altar, box);
        AABB structureArea = boxToAabb(box);
        RandomSource random = level.getRandom();
        int maxSpawns = Math.max(1, SunkenCityRespawnConfig.getMaxSpawnsPerCyclePerType());
        if (force) {
            maxSpawns = Math.min(3, Math.max(maxSpawns, 2));
        }
        int attempts = Math.max(32, SunkenCityRespawnConfig.getSpawnPositionAttempts());
        boolean persistent = SunkenCityRespawnConfig.isPersistentRespawns();
        int localMaxPerType = Math.max(1, SunkenCityRespawnConfig.getLocalMaxPerType());

        int spawnedTotal = 0;
        int createNull = 0;
        int cancelledForced = 0;
        int joinFailed = 0;
        int noPos = 0;
        int alreadyFull = 0;
        int onCooldown = 0;
        StringBuilder detail = new StringBuilder("caps=[");

        REFILLING.set(true);
        try {
            for (Map.Entry<ResourceLocation, Integer> target : SunkenCityRespawnConfig.getMobTargets().entrySet()) {
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(target.getKey()).orElse(null);
                if (entityType == null) {
                    appendDetail(detail, target.getKey().getPath() + "=noType");
                    continue;
                }

                int structureCount = level.getEntities(entityType, structureArea, Entity::isAlive).size();
                int localCount = level.getEntities(entityType, localArea, Entity::isAlive).size();
                int structureTarget = target.getValue();
                int localTarget = Math.min(structureTarget, localMaxPerType);
                int missing = Math.min(structureTarget - structureCount, localTarget - localCount);

                long delayLeft = respawnDelayRemaining(level, box, target.getKey());
                appendDetail(
                        detail,
                        target.getKey().getPath()
                                + " L"
                                + localCount
                                + "/"
                                + localTarget
                                + " A"
                                + structureCount
                                + "/"
                                + structureTarget
                                + (delayLeft > 0 ? " cd=" + delayLeft + "t" : "")
                );
                if (missing <= 0) {
                    alreadyFull++;
                    continue;
                }
                if (isRespawnDelayActive(level, box, target.getKey(), force)) {
                    onCooldown++;
                    continue;
                }

                int toSpawn = Math.min(missing, maxSpawns);
                int spawnedThisType = 0;
                for (int i = 0; i < toSpawn; i++) {
                    SpawnAttempt attempt = trySpawnMobAroundAltar(
                            level, entityType, box, altar, player, random, attempts, persistent
                    );
                    switch (attempt) {
                        case OK -> {
                            spawnedTotal++;
                            spawnedThisType++;
                        }
                        case CANCELLED_FORCED -> {
                            spawnedTotal++;
                            spawnedThisType++;
                            cancelledForced++;
                        }
                        case CREATE_NULL -> createNull++;
                        case JOIN_FAILED -> joinFailed++;
                        case NO_POS -> noPos++;
                    }
                }
                detail.append('+').append(spawnedThisType);
            }
        } finally {
            REFILLING.set(false);
        }

        detail.append("] full=")
                .append(alreadyFull)
                .append(" cooldown=")
                .append(onCooldown)
                .append(" noPos=")
                .append(noPos)
                .append(" createNull=")
                .append(createNull)
                .append(" cancelForced=")
                .append(cancelledForced)
                .append(" joinFail=")
                .append(joinFailed);

        return new SpawnResult(spawnedTotal, detail.toString());
    }

    private static void appendDetail(StringBuilder detail, String part) {
        if (!detail.toString().endsWith("[")) {
            detail.append(", ");
        }
        detail.append(part);
    }

    private static SpawnAttempt trySpawnMobAroundAltar(
            ServerLevel level,
            EntityType<?> entityType,
            BoundingBox box,
            BlockPos altar,
            ServerPlayer player,
            RandomSource random,
            int attempts,
            boolean persistent
    ) {
        SpawnAttempt lastFail = SpawnAttempt.NO_POS;
        int noPosCount = 0;
        for (int attempt = 0; attempt < attempts; attempt++) {
            BlockPos spawnPos = randomPosInAltarRing(level, box, altar, player, random);
            if (spawnPos == null) {
                noPosCount++;
                continue;
            }
            SpawnAttempt result = spawnAt(level, entityType, spawnPos, random, persistent);
            if (result == SpawnAttempt.OK || result == SpawnAttempt.CANCELLED_FORCED) {
                return result;
            }
            lastFail = result;
            if (result == SpawnAttempt.CREATE_NULL) {
                return result;
            }
        }
        return noPosCount >= attempts ? SpawnAttempt.NO_POS : lastFail;
    }

    /**
     * Natural ring around the altar: min–max horizontal distance, away from players.
     */
    private static BlockPos randomPosInAltarRing(
            ServerLevel level,
            BoundingBox box,
            BlockPos altar,
            ServerPlayer player,
            RandomSource random
    ) {
        int minDist = Math.max(4, SunkenCityRespawnConfig.getSpawnMinDistanceFromAltar());
        int maxDist = Math.max(minDist, SunkenCityRespawnConfig.getSpawnMaxDistanceFromAltar());
        int minPlayerDist = Math.max(4, SunkenCityRespawnConfig.getMinDistanceFromPlayer());
        double minPlayerDistSq = (double) minPlayerDist * minPlayerDist;

        for (int i = 0; i < 16; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double dist = minDist + random.nextDouble() * (maxDist - minDist);
            int x = altar.getX() + Mth.floor(Math.cos(angle) * dist);
            int z = altar.getZ() + Mth.floor(Math.sin(angle) * dist);

            if (x < box.minX() || x > box.maxX() || z < box.minZ() || z > box.maxZ()) {
                continue;
            }

            int minY = Math.max(box.minY(), altar.getY() - 16);
            int maxY = Math.min(box.maxY(), altar.getY() + 12);
            BlockPos waterHit = null;
            BlockPos airHit = null;

            for (int y = maxY; y >= minY; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!isPassableSpawnPos(level, pos)) {
                    continue;
                }
                if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < minPlayerDistSq) {
                    continue;
                }
                if (tooCloseToAnyPlayer(level, pos, minPlayerDistSq)) {
                    continue;
                }

                if (isWaterColumn(level, pos)) {
                    waterHit = pos;
                    break;
                }
                if (airHit == null) {
                    airHit = pos;
                }
            }

            if (waterHit != null) {
                return waterHit;
            }
            if (airHit != null) {
                return airHit;
            }
        }
        return null;
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

    private static SpawnAttempt spawnAt(
            ServerLevel level,
            EntityType<?> entityType,
            BlockPos spawnPos,
            RandomSource random,
            boolean persistent
    ) {
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
                MobSpawnType.SPAWNER,
                null
        );

        boolean wasCancelled = mob.isSpawnCancelled();
        if (wasCancelled) {
            mob.setSpawnCancelled(false);
        }

        if (mob.isRemoved()) {
            return SpawnAttempt.JOIN_FAILED;
        }

        // Soft natural check — skip only if clearly embedded in solids.
        if (!level.noCollision(mob)) {
            mob.discard();
            return SpawnAttempt.NO_POS;
        }

        if (persistent) {
            mob.setPersistenceRequired();
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

    private static boolean isPassableSpawnPos(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos) || !level.isLoaded(pos.above())) {
            return false;
        }
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    private static boolean isWaterColumn(ServerLevel level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    private static AABB localCountArea(BlockPos altar, BoundingBox box) {
        int radius = Math.max(16, SunkenCityRespawnConfig.getLocalCountRadius());
        return new AABB(altar).inflate(radius).intersect(boxToAabb(box));
    }

    private static AABB boxToAabb(BoundingBox box) {
        return new AABB(
                box.minX(),
                box.minY(),
                box.minZ(),
                box.maxX() + 1.0D,
                box.maxY() + 1.0D,
                box.maxZ() + 1.0D
        );
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
        return box.minX() + ":" + box.minY() + ":" + box.minZ() + ":" + box.maxX() + ":" + box.maxY() + ":" + box.maxZ();
    }

    private record SpawnResult(int spawned, String detail) {
    }

    private enum SpawnAttempt {
        OK,
        CREATE_NULL,
        CANCELLED_FORCED,
        JOIN_FAILED,
        NO_POS
    }
}
