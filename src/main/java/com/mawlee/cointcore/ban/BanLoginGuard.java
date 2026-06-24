package com.mawlee.cointcore.ban;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import org.slf4j.Logger;

import java.util.Date;
import java.util.UUID;

public final class BanLoginGuard {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FALLBACK_REASON = "Banned by an operator";

    private BanLoginGuard() {
    }

    public static UserBanListEntry resolveEntry(UserBanList bans, GameProfile profile) {
        UserBanListEntry direct = bans.get(profile);
        if (direct != null) {
            return direct;
        }

        UUID profileId = profile.getId();
        if (profileId == null) {
            return null;
        }

        UserBanListEntry byIdOnly = bans.get(new GameProfile(profileId, ""));
        if (byIdOnly != null) {
            return byIdOnly;
        }

        for (String key : bans.getUserList()) {
            UserBanListEntry candidate = lookupByKey(bans, key, profile);
            if (candidate != null) {
                return candidate;
            }
        }

        return null;
    }

    public static void purgeInconsistentBan(UserBanList bans, GameProfile profile) {
        boolean removed = false;
        String[] keys = bans.getUserList().clone();

        for (String key : keys) {
            if (matchesProfileKey(key, profile)) {
                removed |= removeIfPresent(bans, profileFromKey(key, profile));
            }
        }

        removed |= removeIfPresent(bans, profile);
        if (profile.getId() != null) {
            removed |= removeIfPresent(bans, new GameProfile(profile.getId(), ""));
            removed |= removeIfPresent(bans, new GameProfile(profile.getId(), profile.getName()));
        }

        if (removed) {
            LOGGER.warn(
                    "Removed inconsistent ban list state for {} ({}) during login",
                    profile.getName(),
                    profile.getId()
            );
        }
    }

    public static String safeReason(UserBanListEntry entry) {
        if (entry == null) {
            return FALLBACK_REASON;
        }

        String reason = entry.getReason();
        return reason == null || reason.isBlank() ? FALLBACK_REASON : reason;
    }

    public static Date safeExpires(UserBanListEntry entry) {
        return entry == null ? null : entry.getExpires();
    }

    private static UserBanListEntry lookupByKey(UserBanList bans, String key, GameProfile profile) {
        if (!matchesProfileKey(key, profile)) {
            return null;
        }
        return bans.get(profileFromKey(key, profile));
    }

    private static boolean matchesProfileKey(String key, GameProfile profile) {
        if (key == null || key.isBlank()) {
            return false;
        }

        UUID profileId = profile.getId();
        if (profileId != null) {
            try {
                if (profileId.equals(UUID.fromString(key))) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                // Legacy name-based key.
            }
        }

        String profileName = profile.getName();
        return profileName != null && profileName.equalsIgnoreCase(key);
    }

    private static GameProfile profileFromKey(String key, GameProfile profile) {
        try {
            UUID uuid = UUID.fromString(key);
            return new GameProfile(uuid, profile.getName());
        } catch (IllegalArgumentException ignored) {
            return new GameProfile(profile.getId(), key);
        }
    }

    private static boolean removeIfPresent(UserBanList bans, GameProfile profile) {
        if (bans.get(profile) == null && !bans.isBanned(profile)) {
            return false;
        }
        bans.remove(profile);
        return true;
    }
}
