package com.mawlee.cointcore.claim;

import com.mawlee.cointcore.config.BossClaimGuardConfig;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

/**
 * Rejects FTB Chunks claims on End dragon/crystal chunks and Cataclysm boss dungeon chunks.
 * Registers via the same reflective Architectury path as {@link ClaimBufferService}.
 */
public final class BossClaimGuardService {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String PROBLEM_KEY = "message.cointcore.claim.boss_arena";

    private static boolean registered;
    private static volatile Field portalLocationField;
    private static volatile Field originField;
    private static volatile boolean dragonFieldsResolved;

    private BossClaimGuardService() {
    }

    public static void register() {
        if (registered || !ModList.get().isLoaded("ftbchunks")) {
            return;
        }
        try {
            Class<?> eventClass = Class.forName("dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent");
            Object beforeClaimEvent = eventClass.getField("BEFORE_CLAIM").get(null);
            Class<?> beforeInterface = Class.forName("dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent$Before");
            Class<?> architecturyEvent = Class.forName("dev.architectury.event.Event");

            Object handler = Proxy.newProxyInstance(
                    beforeInterface.getClassLoader(),
                    new Class<?>[]{beforeInterface},
                    (proxy, method, args) -> {
                        if ("before".equals(method.getName()) && args != null && args.length == 2) {
                            return beforeClaim((CommandSourceStack) args[0], (ClaimedChunk) args[1]);
                        }
                        return switch (method.getName()) {
                            case "toString" -> "BossClaimGuardBeforeHandler";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> Boolean.valueOf(proxy == args[0]);
                            default -> passResult();
                        };
                    }
            );

            architecturyEvent.getMethod("register", Object.class).invoke(beforeClaimEvent, handler);
            registered = true;
            LOGGER.info("Registered FTB Chunks boss claim guard (BEFORE_CLAIM)");
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("Failed to register FTB Chunks boss claim guard", exception);
        }
    }

    private static Object beforeClaim(CommandSourceStack source, ClaimedChunk claimedChunk) {
        try {
            BossClaimGuardConfig.Settings settings = BossClaimGuardConfig.get();
            if (!settings.enabled()) {
                return passResult();
            }
            if (canBypass(source)) {
                return passResult();
            }

            MinecraftServer server = source.getServer();
            if (server == null) {
                return passResult();
            }

            ChunkDimPos pos = claimedChunk.getPos();
            ServerLevel level = server.getLevel(pos.dimension());
            if (level == null) {
                return passResult();
            }

            ChunkPos chunkPos = new ChunkPos(pos.x(), pos.z());
            if (settings.endDragon() && isEndDragonProtectedChunk(level, chunkPos, settings.endPortalChunkRadius())) {
                return interruptFalse(ClaimResult.customProblem(PROBLEM_KEY));
            }
            if (settings.cataclysmBossStructures()
                    && isCataclysmBossStructureChunk(level, chunkPos, settings.structures())) {
                return interruptFalse(ClaimResult.customProblem(PROBLEM_KEY));
            }

            return passResult();
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("Boss claim guard check failed", exception);
            try {
                return passResult();
            } catch (ReflectiveOperationException passFailed) {
                LOGGER.error("Failed to build CompoundEventResult.pass()", passFailed);
                return null;
            }
        }
    }

    static boolean isEndDragonProtectedChunk(ServerLevel level, ChunkPos chunkPos, int portalChunkRadius) {
        if (!level.dimension().equals(Level.END)) {
            return false;
        }

        for (SpikeFeature.EndSpike spike : SpikeFeature.getSpikesForLevel(level)) {
            if (BossClaimGeometry.chunkIntersectsDisk(
                    chunkPos.x,
                    chunkPos.z,
                    spike.getCenterX(),
                    spike.getCenterZ(),
                    spike.getRadius()
            )) {
                return true;
            }
        }

        BlockPos anchor = dragonFightAnchor(level);
        int originChunkX = blockToChunk(anchor.getX());
        int originChunkZ = blockToChunk(anchor.getZ());
        return BossClaimGeometry.chunkWithinChebyshev(
                chunkPos.x,
                chunkPos.z,
                originChunkX,
                originChunkZ,
                portalChunkRadius
        );
    }

    private static int blockToChunk(int block) {
        return block >> 4;
    }

    private static BlockPos dragonFightAnchor(ServerLevel level) {
        EndDragonFight fight = level.getDragonFight();
        if (fight == null) {
            return BlockPos.ZERO;
        }
        resolveDragonFields();
        BlockPos portal = readBlockPos(fight, portalLocationField);
        if (portal != null) {
            return portal;
        }
        BlockPos origin = readBlockPos(fight, originField);
        return origin != null ? origin : BlockPos.ZERO;
    }

    private static void resolveDragonFields() {
        if (dragonFieldsResolved) {
            return;
        }
        synchronized (BossClaimGuardService.class) {
            if (dragonFieldsResolved) {
                return;
            }
            portalLocationField = findField(EndDragonFight.class, "portalLocation");
            originField = findField(EndDragonFight.class, "origin");
            dragonFieldsResolved = true;
        }
    }

    private static Field findField(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("EndDragonFight.{} is unavailable; using End island origin fallback", name);
            return null;
        }
    }

    private static BlockPos readBlockPos(EndDragonFight fight, Field field) {
        if (field == null) {
            return null;
        }
        try {
            Object value = field.get(fight);
            return value instanceof BlockPos pos ? pos : null;
        } catch (IllegalAccessException exception) {
            return null;
        }
    }

    static boolean isCataclysmBossStructureChunk(
            ServerLevel level,
            ChunkPos chunkPos,
            Set<ResourceLocation> structures
    ) {
        if (structures.isEmpty() || !ModList.get().isLoaded("cataclysm")) {
            return false;
        }
        for (StructureStart start : level.structureManager().startsForStructure(
                chunkPos,
                structure -> tracksStructure(level, structure, structures)
        )) {
            if (start != null && start.isValid()) {
                return true;
            }
        }
        return false;
    }

    private static boolean tracksStructure(
            ServerLevel level,
            Structure structure,
            Set<ResourceLocation> structures
    ) {
        ResourceLocation id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
        return id != null && structures.contains(id);
    }

    private static boolean canBypass(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return source.hasPermission(Commands.LEVEL_GAMEMASTERS);
        }
        return PermissionService.has(player, CointPermissionNodes.CLAIM_BOSS_ARENA_BYPASS);
    }

    private static Object passResult() throws ReflectiveOperationException {
        Class<?> compound = Class.forName("dev.architectury.event.CompoundEventResult");
        Method pass = compound.getMethod("pass");
        return pass.invoke(null);
    }

    private static Object interruptFalse(ClaimResult result) throws ReflectiveOperationException {
        Class<?> compound = Class.forName("dev.architectury.event.CompoundEventResult");
        Method interrupt = compound.getMethod("interruptFalse", Object.class);
        return interrupt.invoke(null, result);
    }
}
