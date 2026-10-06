package com.mawlee.cointcore.claim;

import com.mawlee.cointcore.ftb.FtbIntegration;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.util.UUID;

public final class ClaimFlagMigration {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ClaimFlagMigration() {
    }

    public static void migrate(MinecraftServer server) {
        if (!FtbIntegration.isAvailable()) {
            return;
        }

        migrateLegacySavedData(server);
        ClaimFlagService.migrateRemovedFlags(server);
    }

    private static void migrateLegacySavedData(MinecraftServer server) {
        ClaimTeamFlagsSavedData legacy = ClaimTeamFlagsSavedData.get(server);
        if (legacy.isEmpty()) {
            return;
        }

        int migrated = 0;
        for (UUID teamId : legacy.getTeamIds()) {
            if (!legacy.hadNoPlayerDamage(teamId)) {
                continue;
            }
            FtbIntegration.getTeam(teamId).ifPresent(team -> ClaimFlagService.setPvp(server, team, false));
            migrated++;
        }

        legacy.clear();
        LOGGER.info("Migrated {} legacy claim flag entries onto team pvp", migrated);
    }
}
