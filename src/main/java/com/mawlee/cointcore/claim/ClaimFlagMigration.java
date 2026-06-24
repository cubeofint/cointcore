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

    public static void migrateLegacySavedData(MinecraftServer server) {
        if (!FtbIntegration.isAvailable()) {
            return;
        }

        ClaimTeamFlagsSavedData legacy = ClaimTeamFlagsSavedData.get(server);
        if (legacy.isEmpty()) {
            return;
        }

        int migrated = 0;
        for (UUID teamId : legacy.getTeamIds()) {
            ClaimTeamFlags flags = legacy.getOrDefault(teamId);
            FtbIntegration.getTeam(teamId).ifPresent(team -> {
                if (flags.disablePlayerDamage()) {
                    ClaimFlagService.setDisablePlayerDamage(server, team, true);
                }
                if (flags.disableHostileMobSpawn()) {
                    ClaimFlagService.setDisableHostileMobSpawn(server, team, true);
                }
                if (flags.protectMobsFromOutsiders()) {
                    ClaimFlagService.setProtectMobsFromOutsiders(server, team, true);
                }
            });
            migrated++;
        }

        legacy.clear();
        LOGGER.info("Migrated {} CointCore claim flag entries into FTB team properties", migrated);
    }
}
