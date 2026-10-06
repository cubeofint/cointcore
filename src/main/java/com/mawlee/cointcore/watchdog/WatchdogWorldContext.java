package com.mawlee.cointcore.watchdog;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Soft lookups for owner / force-load / type ids. FTB Chunks is optional (reflection).
 */
public final class WatchdogWorldContext {
    private static final Object FTB_LOCK = new Object();
    private static boolean ftbResolved;
    private static Method ftbApi;
    private static Method ftbManagerLoaded;
    private static Method ftbGetManager;
    private static Method ftbGetChunk;
    private static Method claimedGetTeamData;
    private static Method claimedIsForceLoaded;
    private static Method claimedIsActuallyForceLoaded;
    private static Method teamDataGetTeam;
    private static Method teamGetName;
    private static Method teamGetOwner;
    private static Method teamGetShortName;
    private static Method componentGetString;
    private static Class<?> chunkDimPosClass;

    private WatchdogWorldContext() {
    }

    public static String dimensionId(ServerLevel level) {
        if (level == null) {
            return "unknown";
        }
        ResourceLocation key = level.dimension().location();
        return key == null ? "unknown" : key.toString();
    }

    public static String blockEntityTypeId(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return "unknown";
        }
        ResourceLocation key = BlockEntityType.getKey(blockEntity.getType());
        return key == null ? "unknown" : key.toString();
    }

    public static String entityTypeId(Entity entity) {
        if (entity == null) {
            return "unknown";
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key == null ? "unknown" : key.toString();
    }

    public static String owningMod(String typeId) {
        if (typeId == null || typeId.isBlank()) {
            return "unknown";
        }
        int colon = typeId.indexOf(':');
        if (colon > 0) {
            return typeId.substring(0, colon).toLowerCase(Locale.ROOT);
        }
        return "unknown";
    }

    public static WatchdogReportFormatter.ChunkContext describeChunk(ServerLevel level, int chunkX, int chunkZ) {
        if (level == null) {
            return new WatchdogReportFormatter.ChunkContext("-", "unknown");
        }
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        long packed = chunkPos.toLong();
        boolean vanillaForced = level.getForcedChunks().contains(packed);

        FtbChunkInfo ftb = lookupFtb(level, chunkPos);
        String claim = ftb.claimOwner().isBlank() ? "-" : ftb.claimOwner();
        String force;
        if (ftb.forceLoaded() && vanillaForced) {
            force = "ftbchunks+vanilla";
        } else if (ftb.forceLoaded()) {
            force = "ftbchunks";
        } else if (vanillaForced) {
            force = "vanilla";
        } else if (level.getChunkSource().hasChunk(chunkX, chunkZ)) {
            force = "loaded";
        } else {
            force = "no";
        }
        return new WatchdogReportFormatter.ChunkContext(claim, force);
    }

    private static FtbChunkInfo lookupFtb(ServerLevel level, ChunkPos chunkPos) {
        if (!ModList.get().isLoaded("ftbchunks")) {
            return FtbChunkInfo.EMPTY;
        }
        resolveFtb();
        if (ftbApi == null || chunkDimPosClass == null) {
            return FtbChunkInfo.EMPTY;
        }
        try {
            Object api = ftbApi.invoke(null);
            if (api == null) {
                return FtbChunkInfo.EMPTY;
            }
            if (ftbManagerLoaded != null && Boolean.FALSE.equals(ftbManagerLoaded.invoke(api))) {
                return FtbChunkInfo.EMPTY;
            }
            Object manager = ftbGetManager.invoke(api);
            if (manager == null) {
                return FtbChunkInfo.EMPTY;
            }
            Object dimPos = chunkDimPosClass.getConstructor(
                    net.minecraft.resources.ResourceKey.class,
                    ChunkPos.class
            ).newInstance(level.dimension(), chunkPos);
            Object claimed = ftbGetChunk.invoke(manager, dimPos);
            if (claimed == null) {
                return FtbChunkInfo.EMPTY;
            }
            boolean forced = false;
            if (claimedIsForceLoaded != null) {
                forced = Boolean.TRUE.equals(claimedIsForceLoaded.invoke(claimed));
            }
            if (!forced && claimedIsActuallyForceLoaded != null) {
                forced = Boolean.TRUE.equals(claimedIsActuallyForceLoaded.invoke(claimed));
            }
            String owner = "";
            Object teamData = claimedGetTeamData == null ? null : claimedGetTeamData.invoke(claimed);
            Object team = teamData == null || teamDataGetTeam == null ? null : teamDataGetTeam.invoke(teamData);
            if (team != null) {
                Object nameComponent = teamGetName == null ? null : teamGetName.invoke(team);
                if (nameComponent != null && componentGetString != null) {
                    Object name = componentGetString.invoke(nameComponent);
                    if (name != null) {
                        owner = name.toString();
                    }
                }
                if (owner.isBlank() && teamGetShortName != null) {
                    Object shortName = teamGetShortName.invoke(team);
                    owner = shortName == null ? "" : shortName.toString();
                }
                if (owner.isBlank() && teamGetOwner != null) {
                    Object uuid = teamGetOwner.invoke(team);
                    owner = uuid == null ? "" : uuid.toString();
                }
            }
            return new FtbChunkInfo(owner, forced);
        } catch (Throwable ignored) {
            return FtbChunkInfo.EMPTY;
        }
    }

    private static void resolveFtb() {
        if (ftbResolved) {
            return;
        }
        synchronized (FTB_LOCK) {
            if (ftbResolved) {
                return;
            }
            try {
                Class<?> apiClass = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
                ftbApi = apiClass.getMethod("api");
                Class<?> apiIface = ftbApi.getReturnType();
                try {
                    ftbManagerLoaded = apiIface.getMethod("isManagerLoaded");
                } catch (NoSuchMethodException ignored) {
                    ftbManagerLoaded = null;
                }
                ftbGetManager = apiIface.getMethod("getManager");
                Class<?> managerClass = ftbGetManager.getReturnType();
                chunkDimPosClass = Class.forName("dev.ftb.mods.ftblibrary.math.ChunkDimPos");
                ftbGetChunk = managerClass.getMethod("getChunk", chunkDimPosClass);
                Class<?> claimedClass = Class.forName("dev.ftb.mods.ftbchunks.api.ClaimedChunk");
                claimedGetTeamData = claimedClass.getMethod("getTeamData");
                claimedIsForceLoaded = claimedClass.getMethod("isForceLoaded");
                try {
                    claimedIsActuallyForceLoaded = claimedClass.getMethod("isActuallyForceLoaded");
                } catch (NoSuchMethodException ignored) {
                    claimedIsActuallyForceLoaded = null;
                }
                Class<?> teamDataClass = Class.forName("dev.ftb.mods.ftbchunks.api.ChunkTeamData");
                teamDataGetTeam = teamDataClass.getMethod("getTeam");
                Class<?> teamClass = Class.forName("dev.ftb.mods.ftbteams.api.Team");
                teamGetName = teamClass.getMethod("getName");
                teamGetOwner = teamClass.getMethod("getOwner");
                teamGetShortName = teamClass.getMethod("getShortName");
                componentGetString = net.minecraft.network.chat.Component.class.getMethod("getString");
            } catch (Throwable ignored) {
                ftbApi = null;
            }
            ftbResolved = true;
        }
    }

    private record FtbChunkInfo(String claimOwner, boolean forceLoaded) {
        private static final FtbChunkInfo EMPTY = new FtbChunkInfo("", false);
    }
}
