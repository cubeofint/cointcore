package com.mawlee.cointcore.ftbranks;

import com.mawlee.cointcore.config.FtbRanksLuckPermsBridgeConfig;
import com.mawlee.cointcore.ftb.ChunkBonusService;
import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbranks.api.PermissionValue;
import dev.ftb.mods.ftbranks.impl.permission.BooleanPermissionValue;
import dev.ftb.mods.ftbranks.impl.permission.NumberPermissionValue;
import dev.ftb.mods.ftbranks.impl.permission.StringPermissionValue;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

/**
 * Runtime glue for the FTB Ranks mixin: consults LuckPerms only when FTB Ranks has no
 * explicit node, the config switch is on, and this thread is not already inside a lookup.
 */
public final class FtbRanksLuckPermsBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadLocal<Boolean> QUERYING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private FtbRanksLuckPermsBridge() {
    }

    public static void init(MinecraftServer server) {
        LuckPermsIntegration.registerUserDataRecalculateListener(playerId -> {
            if (!FtbRanksLuckPermsBridgeConfig.isEnabled()) {
                return;
            }
            server.execute(() -> refreshAfterLuckPermsChange(server, playerId));
        });
    }

    public static PermissionValue afterFtbLookup(ServerPlayer player, String node, PermissionValue ftbValue) {
        boolean ftbExplicit = ftbValue != null && !ftbValue.isEmpty();
        boolean enabled = FtbRanksLuckPermsBridgeConfig.isEnabled();
        boolean luckPerms = LuckPermsIntegration.isAvailable();

        if (!enabled || QUERYING.get()) {
            log(player, node, ftbExplicit ? FtbRanksLuckPermsResolution.Source.FTB_RANKS : FtbRanksLuckPermsResolution.Source.FALLBACK);
            return ftbValue;
        }

        FtbRanksLuckPermsResolution.Outcome outcome = FtbRanksLuckPermsResolution.resolve(
                true,
                luckPerms,
                ftbExplicit,
                node,
                queryFor(player)
        );

        log(player, node, outcome.source());
        if (outcome.source() != FtbRanksLuckPermsResolution.Source.LUCKPERMS) {
            return ftbValue;
        }

        return outcome.luckPermsValue().map(FtbRanksLuckPermsBridge::toPermissionValue).orElse(ftbValue);
    }

    private static FtbRanksLuckPermsResolution.LuckPermsQuery queryFor(ServerPlayer player) {
        UUID playerId = player.getUUID();
        return new FtbRanksLuckPermsResolution.LuckPermsQuery() {
            @Override
            public Optional<Boolean> permissionTristate(String node) {
                return withQueryGuard(() -> LuckPermsIntegration.permissionTristate(playerId, node));
            }

            @Override
            public Optional<String> meta(String node) {
                return withQueryGuard(() -> LuckPermsIntegration.getMetaValue(playerId, node));
            }
        };
    }

    private static <T> T withQueryGuard(java.util.function.Supplier<T> supplier) {
        QUERYING.set(Boolean.TRUE);
        try {
            return supplier.get();
        } finally {
            QUERYING.set(Boolean.FALSE);
        }
    }

    private static PermissionValue toPermissionValue(FtbRanksLuckPermsResolution.ParsedValue value) {
        return switch (value) {
            case FtbRanksLuckPermsResolution.ParsedValue.Bool bool -> BooleanPermissionValue.of(bool.value());
            case FtbRanksLuckPermsResolution.ParsedValue.Num num -> NumberPermissionValue.of(num.value());
            case FtbRanksLuckPermsResolution.ParsedValue.Str str -> StringPermissionValue.of(str.value());
        };
    }

    private static void refreshAfterLuckPermsChange(MinecraftServer server, UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return;
        }
        ChunkBonusService.refreshPlayerLimits(player);
    }

    private static void log(ServerPlayer player, String node, FtbRanksLuckPermsResolution.Source source) {
        if (!FtbRanksLuckPermsBridgeConfig.isDebug()) {
            return;
        }
        String sourceName = switch (source) {
            case FTB_RANKS -> "FTB Ranks";
            case LUCKPERMS -> "LuckPerms";
            case FALLBACK -> "fallback";
        };
        LOGGER.info(
                "FTB Ranks LuckPerms bridge: node={} player={} source={}",
                node,
                player != null ? player.getGameProfile().getName() : "?",
                sourceName
        );
    }
}
