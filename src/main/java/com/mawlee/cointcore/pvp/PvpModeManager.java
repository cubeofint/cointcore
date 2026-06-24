package com.mawlee.cointcore.pvp;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PvpModeManager {
    private static final String PVP_DISABLED_KEY = "cointcore:pvp_disabled";
    private static final long PEACE_COOLDOWN_MS = 10_000L;
    private static final Map<UUID, Boolean> PVP_DISABLED = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_PLAYER_DAMAGE_AT = new ConcurrentHashMap<>();

    private PvpModeManager() {
    }

    public static boolean isPvpEnabled(ServerPlayer player) {
        if (player == null) {
            return true;
        }

        return !PVP_DISABLED.getOrDefault(player.getUUID(), false);
    }

    public static boolean canPlayerVsPlayerFight(ServerPlayer attacker, ServerPlayer target) {
        return isPvpEnabled(attacker) && isPvpEnabled(target);
    }

    public static long getPeaceCooldownRemainingMs(ServerPlayer player) {
        Long lastDamageAt = LAST_PLAYER_DAMAGE_AT.get(player.getUUID());
        if (lastDamageAt == null) {
            return 0L;
        }

        long elapsed = System.currentTimeMillis() - lastDamageAt;
        return Math.max(0L, PEACE_COOLDOWN_MS - elapsed);
    }

    public static void recordPlayerDamage(ServerPlayer victim) {
        LAST_PLAYER_DAMAGE_AT.put(victim.getUUID(), System.currentTimeMillis());
    }

    public static ToggleResult tryToggle(ServerPlayer player) {
        if (!isPvpEnabled(player)) {
            enablePvp(player);
            return ToggleResult.enabled();
        }

        long remainingMs = getPeaceCooldownRemainingMs(player);
        if (remainingMs > 0L) {
            return ToggleResult.peaceRequired(remainingMs);
        }

        disablePvp(player);
        return ToggleResult.disabled();
    }

    private static void enablePvp(ServerPlayer player) {
        PVP_DISABLED.remove(player.getUUID());
        saveToStorage(player, false);
    }

    private static void disablePvp(ServerPlayer player) {
        PVP_DISABLED.put(player.getUUID(), true);
        saveToStorage(player, true);
    }

    public record ToggleResult(ToggleState state, long remainingMs) {
        public enum ToggleState {
            ENABLED,
            DISABLED,
            PEACE_REQUIRED
        }

        public static ToggleResult enabled() {
            return new ToggleResult(ToggleState.ENABLED, 0L);
        }

        public static ToggleResult disabled() {
            return new ToggleResult(ToggleState.DISABLED, 0L);
        }

        public static ToggleResult peaceRequired(long remainingMs) {
            return new ToggleResult(ToggleState.PEACE_REQUIRED, remainingMs);
        }
    }

    public static void loadFromStorage(ServerPlayer player) {
        var data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (data.getBoolean(PVP_DISABLED_KEY)) {
            PVP_DISABLED.put(player.getUUID(), true);
        } else {
            PVP_DISABLED.remove(player.getUUID());
        }
    }

    public static void clearRuntimeState() {
        PVP_DISABLED.clear();
        LAST_PLAYER_DAMAGE_AT.clear();
    }

    public static ServerPlayer resolveAttackingPlayer(DamageSource source) {
        Entity cause = source.getEntity();
        if (cause instanceof ServerPlayer player) {
            return player;
        }

        if (cause instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer owner) {
            return owner;
        }

        Entity direct = source.getDirectEntity();
        if (direct instanceof ServerPlayer player) {
            return player;
        }

        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer owner) {
            return owner;
        }

        return null;
    }

    private static void saveToStorage(ServerPlayer player, boolean disabled) {
        var data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (disabled) {
            data.putBoolean(PVP_DISABLED_KEY, true);
        } else {
            data.remove(PVP_DISABLED_KEY);
        }
    }
}
