package com.mawlee.cointcore;

import com.mawlee.cointcore.ae.MeUniqueFilterConfig;
import com.mawlee.cointcore.ae.NonStackableItemTagPack;
import com.mawlee.cointcore.chatspy.ChatSpyManager;
import com.mawlee.cointcore.chatspy.ChatSpyService;
import com.mawlee.cointcore.claim.ClaimFlagMigration;
import com.mawlee.cointcore.claim.ClaimFlagService;
import com.mawlee.cointcore.command.ModCommands;
import com.mawlee.cointcore.config.AdminChatConfig;
import com.mawlee.cointcore.config.ChunkLimitConfig;
import com.mawlee.cointcore.config.JoinMessagesConfig;
import com.mawlee.cointcore.config.MobCleanupConfig;
import com.mawlee.cointcore.config.WorldCleanupConfig;
import com.mawlee.cointcore.config.RelpChatPrefixConfig;
import com.mawlee.cointcore.config.VoteConfig;
import com.mawlee.cointcore.placeholder.CointCorePlaceholderRegistry;
import com.mawlee.cointcore.placeholder.TabSparkPlaceholderRegistry;
import com.mawlee.cointcore.spark.SparkMetricsService;
import com.mawlee.cointcore.vote.VoteService;
import com.mawlee.cointcore.config.ServerAutomationConfig;
import com.mawlee.cointcore.config.SparkProfilerConfig;
import com.mawlee.cointcore.server.PeriodicMessageService;
import com.mawlee.cointcore.server.ScheduledRestartService;
import com.mawlee.cointcore.ignore.IgnoreManager;
import com.mawlee.cointcore.join.JoinMessageService;
import com.mawlee.cointcore.message.PrivateMessageService;
import com.mawlee.cointcore.nightvision.NightVisionService;
import com.mawlee.cointcore.privilege.DonorPrivilegeService;
import com.mawlee.cointcore.pvp.PvpModeManager;
import com.mawlee.cointcore.vanish.VanishService;
import com.mojang.logging.LogUtils;
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
            JoinMessagesConfig.load();
            RelpChatPrefixConfig.load();
            MobCleanupConfig.load();
            WorldCleanupConfig.load();
            ChunkLimitConfig.load();
            VoteConfig.load();
            ServerAutomationConfig.load();
            SparkProfilerConfig.load();
            AdminChatConfig.load();
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
        ClaimFlagMigration.migrateLegacySavedData(event.getServer());
        ClaimFlagService.migrateLegacyAllowPvpFlags(event.getServer());
        DonorPrivilegeService.init(event.getServer());
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
            JoinMessageService.sendJoinMessages(player);
        });
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.server.execute(() -> PvpModeManager.loadFromStorage(player));
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PrivateMessageService.onPlayerLeave(player);
            DonorPrivilegeService.onPlayerLeave(player);
        }
    }
}
