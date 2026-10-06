package com.mawlee.cointcore;

import com.mawlee.cointcore.ae.MeUniqueFilterConfig;
import com.mawlee.cointcore.ae.NonStackableItemTagPack;
import com.mawlee.cointcore.chatspy.ChatSpyManager;
import com.mawlee.cointcore.chatspy.ChatSpyService;
import com.mawlee.cointcore.claim.ClaimFlagEditSync;
import com.mawlee.cointcore.claim.ClaimFlagMigration;
import com.mawlee.cointcore.command.ModCommands;
import com.mawlee.cointcore.afk.AfkListMarker;
import com.mawlee.cointcore.afk.AfkTracker;
import com.mawlee.cointcore.config.AfkConfig;
import com.mawlee.cointcore.config.ChatDiscordRelayConfig;
import com.mawlee.cointcore.config.ArsPerfConfigs;
import com.mawlee.cointcore.config.CataclysmRespawnConfigs;
import com.mawlee.cointcore.config.ChatConfigs;
import com.mawlee.cointcore.config.ClaimsConfigs;
import com.mawlee.cointcore.config.ChunkLimitConfig;
import com.mawlee.cointcore.config.CleanupConfigs;
import com.mawlee.cointcore.config.NaturalSpawnConfig;
import com.mawlee.cointcore.config.RelpChatPrefixConfig;
import com.mawlee.cointcore.config.VoteConfig;
import com.mawlee.cointcore.placeholder.CointCorePlaceholderRegistry;
import com.mawlee.cointcore.placeholder.TabSparkPlaceholderRegistry;
import com.mawlee.cointcore.spark.SparkMetricsService;
import com.mawlee.cointcore.vote.VoteService;
import com.mawlee.cointcore.config.ServerAutomationConfig;
import com.mawlee.cointcore.config.SoulSurgePerfConfig;
import com.mawlee.cointcore.config.SoulSurgeDenyConfig;
import com.mawlee.cointcore.config.TickAccelerationDenyConfig;
import com.mawlee.cointcore.config.SpawnerPerfConfig;
import com.mawlee.cointcore.config.ItemPerfConfig;
import com.mawlee.cointcore.config.StoragePerfConfigs;
import com.mawlee.cointcore.config.TickThrottleConfigs;
import com.mawlee.cointcore.config.ExplosionTerrainConfig;
import com.mawlee.cointcore.config.SpawnerByproductConfig;
import com.mawlee.cointcore.config.SparkProfilerConfig;
import com.mawlee.cointcore.config.DimensionWipeConfig;
import com.mawlee.cointcore.config.StarterKitConfig;
import com.mawlee.cointcore.kit.StarterKitService;
import com.mawlee.cointcore.server.PeriodicMessageService;
import com.mawlee.cointcore.server.ScheduledRestartService;
import com.mawlee.cointcore.ignore.IgnoreManager;
import com.mawlee.cointcore.join.JoinMessageService;
import com.mawlee.cointcore.message.PrivateMessageService;
import com.mawlee.cointcore.nightvision.NightVisionService;
import com.mawlee.cointcore.ftb.ChunkBonusService;
import com.mawlee.cointcore.flux.FluxAdminAccess;
import com.mawlee.cointcore.privilege.DonorPrivilegeService;
import com.mawlee.cointcore.pvp.PvpModeManager;
import com.mawlee.cointcore.seeinvisible.SeeInvisibleService;
import com.mawlee.cointcore.vanish.VanishListMarker;
import com.mawlee.cointcore.vanish.VanishManager;
import com.mawlee.cointcore.vanish.VanishService;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

public final class CointCoreEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CointCoreEvents() {
    }

    public static void onServerStarting(ServerStartingEvent event) {
        try {
            CointCoreRuntimeCleanup.warmupAtServerStart();
            ChatConfigs.load();
            RelpChatPrefixConfig.load();
            AfkConfig.load();
            ChatDiscordRelayConfig.load();
            NaturalSpawnConfig.load();
            CleanupConfigs.load();
            CataclysmRespawnConfigs.load();
            ChunkLimitConfig.load();
            ClaimsConfigs.load();
            VoteConfig.load();
            ServerAutomationConfig.load();
            SparkProfilerConfig.load();
            TickThrottleConfigs.load();
            SoulSurgePerfConfig.load();
            SoulSurgeDenyConfig.load();
            TickAccelerationDenyConfig.load();
            SpawnerPerfConfig.load();
            ItemPerfConfig.load();
            ArsPerfConfigs.load();
            StoragePerfConfigs.load();
            ExplosionTerrainConfig.load();
            SpawnerByproductConfig.load();
            DimensionWipeConfig.load();
            StarterKitConfig.load();
            MeUniqueFilterConfig.load();
            NonStackableItemTagPack.ensureGenerated();
            SparkMetricsService.bind();
            TabSparkPlaceholderRegistry.hookIfAvailable();
            CointCorePlaceholderRegistry.registerIfAvailable();
        } catch (RuntimeException exception) {
            LOGGER.error("CointCore config failed to load during server startup", exception);
        }

        VoteService.applySleepPercentage(event.getServer());

        ModCommands.register(event.getServer().getCommands().getDispatcher());
        ClaimFlagMigration.migrate(event.getServer());
        DonorPrivilegeService.init(event.getServer());
        FluxAdminAccess.init(event.getServer());
        SeeInvisibleService.init(event.getServer());
        ClaimFlagEditSync.init(event.getServer());
        ChunkBonusService.init(event.getServer());
        ChatSpyManager.loadFromSavedData(event.getServer());
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        player.server.execute(() -> {
            IgnoreManager.loadFromStorage(player);
            PvpModeManager.loadFromStorage(player);
            VanishService.onPlayerJoin(player);
            ChatSpyService.onPlayerJoin(player);
            NightVisionService.onPlayerJoin(player);
            DonorPrivilegeService.onPlayerJoin(player);
            FluxAdminAccess.syncCapability(player);
            SeeInvisibleService.sync(player);
            ClaimFlagEditSync.sync(player);
            ChunkBonusService.onPlayerJoin(player);
            JoinMessageService.sendJoinMessages(player);
            StarterKitService.onPlayerJoin(player);
        });
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.server.execute(() -> {
                PvpModeManager.loadFromStorage(player);
                SeeInvisibleService.sync(player);
            });
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PrivateMessageService.onPlayerLeave(player);
            DonorPrivilegeService.onPlayerLeave(player);
        }
    }

    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        boolean vanished = VanishManager.isVanished(player);
        boolean afk = AfkTracker.isMarked(player);
        if (!vanished && !afk) {
            return;
        }

        Component current = event.getDisplayName();
        if (current == null) {
            current = player.getDisplayName();
        }
        if (vanished) {
            current = VanishListMarker.append(current);
        }
        if (afk) {
            current = AfkListMarker.append(current);
        }
        event.setDisplayName(current);
    }
}
