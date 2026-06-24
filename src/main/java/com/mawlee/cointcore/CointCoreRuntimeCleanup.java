package com.mawlee.cointcore;

import com.mawlee.cointcore.chatspy.ChatSpyManager;
import com.mawlee.cointcore.chatspy.ChatSpyTracker;
import com.mawlee.cointcore.ignore.IgnoreManager;
import com.mawlee.cointcore.message.PrivateMessageTargets;
import com.mawlee.cointcore.nightvision.NightVisionManager;
import com.mawlee.cointcore.pvp.PvpModeManager;
import com.mawlee.cointcore.server.ServerRestartService;
import com.mawlee.cointcore.vanish.VanishManager;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class CointCoreRuntimeCleanup {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String[] PRELOAD_CLASSES = {
            "com.mawlee.cointcore.vanish.VanishManager",
            "com.mawlee.cointcore.nightvision.NightVisionManager",
            "com.mawlee.cointcore.message.PrivateMessageTargets",
            "com.mawlee.cointcore.chatspy.ChatSpyManager",
            "com.mawlee.cointcore.chatspy.ChatSpyTracker",
            "com.mawlee.cointcore.ignore.IgnoreManager",
            "com.mawlee.cointcore.pvp.PvpModeManager",
            "com.mawlee.cointcore.server.ServerRestartService",
            "com.mawlee.cointcore.server.ScheduledRestartService",
            "com.mawlee.cointcore.server.PeriodicMessageService",
            "com.mawlee.cointcore.server.ServerAutomationEvents",
            "com.mawlee.cointcore.teleport.TplService",
            "com.mawlee.cointcore.teleport.OfflinePlayerPosition",
            "com.mawlee.cointcore.mute.MuteService",
            "com.mawlee.cointcore.ban.BanService",
            "com.mawlee.cointcore.punishment.WarnService",
            "com.mawlee.cointcore.punishment.PunishmentHistory",
            "com.mawlee.cointcore.spark.SparkMetricsService",
    };

    private CointCoreRuntimeCleanup() {
    }

    public static void warmupAtServerStart() {
        ClassLoader classLoader = CointCoreRuntimeCleanup.class.getClassLoader();
        for (String className : PRELOAD_CLASSES) {
            preloadClass(className, classLoader);
        }
    }

    public static void onServerStopped() {
        runQuietly(VanishManager::clearRuntimeState);
        runQuietly(NightVisionManager::clearRuntimeState);
        runQuietly(PrivateMessageTargets::clearRuntimeState);
        runQuietly(ChatSpyTracker::clearRuntimeState);
        runQuietly(ChatSpyManager::clearRuntimeState);
        runQuietly(IgnoreManager::clearRuntimeState);
        runQuietly(PvpModeManager::clearRuntimeState);
        runQuietly(ServerRestartService::cancelPendingRestart);
    }

    private static void preloadClass(String className, ClassLoader classLoader) {
        try {
            Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException exception) {
            LOGGER.error("Failed to preload required CointCore class {}", className, exception);
        } catch (LinkageError exception) {
            LOGGER.error("Failed to initialize required CointCore class {}", className, exception);
        }
    }

    private static void runQuietly(Runnable action) {
        try {
            action.run();
        } catch (Throwable exception) {
            LOGGER.warn("CointCore runtime cleanup step failed", exception);
        }
    }
}
