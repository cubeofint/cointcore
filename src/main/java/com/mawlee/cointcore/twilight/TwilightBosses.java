package com.mawlee.cointcore.twilight;

import com.mawlee.cointcore.config.TwilightBossRespawnConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Twilight Forest bosses come from a spawner block that removes itself once a player walks up. Every boss remembers
 * that spot ({@code getRestrictionPoint}) and its spawner block ({@code getBossSpawner}); TF itself puts the spawner
 * back there in peaceful. This records those spots and, on server start, puts the spawner back wherever every boss
 * from it is dead, so the boss spawns again when a player comes. Reads TF through reflection: no compile dependency.
 */
public final class TwilightBosses {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NAMESPACE = "twilightforest";
    /** Boss name (as typed in commands) → its spawner block, for arenas marked by hand. */
    public static final Map<String, ResourceLocation> SPAWNERS = Map.of(
            "naga", tf("naga_boss_spawner"),
            "lich", tf("lich_boss_spawner"),
            "minoshroom", tf("minoshroom_boss_spawner"),
            "hydra", tf("hydra_boss_spawner"),
            "knight_phantom", tf("knight_phantom_boss_spawner"),
            "ur_ghast", tf("ur_ghast_boss_spawner"),
            "alpha_yeti", tf("alpha_yeti_boss_spawner"),
            "snow_queen", tf("snow_queen_boss_spawner")
    );

    private record Accessors(Method home, Method spawner) {
    }

    private static final Map<Class<?>, Optional<Accessors>> ACCESSORS = new ConcurrentHashMap<>();

    private TwilightBosses() {
    }

    private static ResourceLocation tf(String path) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
    }

    /** The arena of a TF boss: where its spawner stood and which spawner block it was. */
    record Home(GlobalPos pos, ResourceLocation spawner) {
    }

    static Optional<Home> homeOf(Entity entity) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (type == null || !NAMESPACE.equals(type.getNamespace())) {
            return Optional.empty();
        }
        Optional<Accessors> accessors = ACCESSORS.computeIfAbsent(entity.getClass(), TwilightBosses::findAccessors);
        if (accessors.isEmpty()) {
            return Optional.empty();
        }
        try {
            Object home = accessors.get().home().invoke(entity);
            Object spawner = accessors.get().spawner().invoke(entity);
            if (!(home instanceof GlobalPos pos) || !(spawner instanceof Block block)) {
                return Optional.empty();
            }
            ResourceLocation spawnerId = BuiltInRegistries.BLOCK.getKey(block);
            if (block == Blocks.AIR || spawnerId == null) {
                return Optional.empty();
            }
            return Optional.of(new Home(pos, spawnerId));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Accessors> findAccessors(Class<?> type) {
        try {
            Method home = type.getMethod("getRestrictionPoint");
            Method spawner = type.getMethod("getBossSpawner");
            if (GlobalPos.class.isAssignableFrom(home.getReturnType()) && Block.class.isAssignableFrom(spawner.getReturnType())) {
                return Optional.of(new Accessors(home, spawner));
            }
        } catch (NoSuchMethodException ignored) {
            // Not a TF boss (minions, parts, ordinary mobs).
        }
        return Optional.empty();
    }

    public enum Result { PLACED, ALREADY_THERE, ALIVE, BLOCKED, NOT_GENERATED, UNKNOWN }

    /** Puts spawners back for every arena whose bosses are all dead. Returns a one-line summary. */
    public static String respawnAll(MinecraftServer server) {
        TwilightBossSavedData data = TwilightBossSavedData.get(server);
        int placed = 0;
        int alreadyThere = 0;
        int alive = 0;
        int blocked = 0;
        int skipped = 0;
        for (TwilightBossSavedData.Arena arena : data.arenas()) {
            switch (respawn(server, arena)) {
                case PLACED -> placed++;
                case ALREADY_THERE -> alreadyThere++;
                case ALIVE -> alive++;
                case BLOCKED -> blocked++;
                case NOT_GENERATED, UNKNOWN -> skipped++;
            }
        }
        String summary = "Twilight bosses: " + placed + " spawners put back, " + alreadyThere + " already waiting, "
                + alive + " bosses still alive, " + blocked + " blocked, " + skipped + " skipped";
        LOGGER.info(summary);
        return summary;
    }

    static Result respawn(MinecraftServer server, TwilightBossSavedData.Arena arena) {
        if (arena.aliveCount() > 0) {
            return Result.ALIVE;
        }
        ServerLevel level = server.getLevel(arena.dimension());
        Block spawner = BuiltInRegistries.BLOCK.get(arena.spawner());
        if (level == null || spawner == Blocks.AIR) {
            return Result.UNKNOWN;
        }
        BlockPos pos = arena.pos();
        if (!isGenerated(level, new ChunkPos(pos))) {
            return Result.NOT_GENERATED;
        }
        level.getChunk(pos); // generated (checked above), so this only loads it
        BlockState current = level.getBlockState(pos);
        if (current.is(spawner)) {
            return Result.ALREADY_THERE;
        }
        if (!isFree(current)) {
            // TF drops the loot chest on the spot when the floor below can't take it: never break it, go above.
            BlockPos above = level.getBlockEntity(pos) instanceof Container ? freeAbove(level, pos) : null;
            if (above == null) {
                LOGGER.warn("Twilight boss arena at {} {} {} in {}: {} stands where the spawner goes, not restoring",
                        pos.getX(), pos.getY(), pos.getZ(), arena.dimension().location(), current.getBlock());
                return Result.BLOCKED;
            }
            TwilightBossSavedData.get(server).move(arena, above);
            pos = above;
        }
        level.setBlockAndUpdate(pos, spawner.defaultBlockState());
        return Result.PLACED;
    }

    private static boolean isFree(BlockState state) {
        return state.isAir() || state.canBeReplaced() || TwilightBossRespawnConfig.isReplaceBlocks();
    }

    private static BlockPos freeAbove(ServerLevel level, BlockPos pos) {
        for (int dy = 1; dy <= 3; dy++) {
            BlockPos candidate = pos.above(dy);
            BlockState state = level.getBlockState(candidate);
            if (state.isAir() || state.canBeReplaced()) {
                return candidate;
            }
            if (!(level.getBlockEntity(candidate) instanceof Container)) {
                return null;
            }
        }
        return null;
    }

    /** True for chunks that exist and are fully generated; never triggers generation. */
    private static boolean isGenerated(ServerLevel level, ChunkPos chunk) {
        if (level.hasChunk(chunk.x, chunk.z)) {
            return true;
        }
        Optional<CompoundTag> tag = level.getChunkSource().chunkMap.read(chunk).join();
        return tag.map(nbt -> nbt.getString("Status").endsWith("full")).orElse(false);
    }
}
