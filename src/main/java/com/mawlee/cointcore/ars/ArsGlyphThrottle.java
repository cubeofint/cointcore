package com.mawlee.cointcore.ars;

import com.mawlee.cointcore.config.ArsGlyphPerfConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-caster resolve cooldowns for Toss / Wololo spam (Orbit, turrets, etc.).
 */
public final class ArsGlyphThrottle {
    private static final Map<UUID, Long> TOSS_LAST = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> WOLOLO_LAST = new ConcurrentHashMap<>();
    private static final int MAX_ENTRIES = 4096;

    private ArsGlyphThrottle() {
    }

    public static boolean tryToss(LivingEntity shooter, Level level) {
        return tryCooldown(shooter, level, ArsGlyphPerfConfig.getTossCooldownTicks(), TOSS_LAST);
    }

    public static boolean tryWololo(LivingEntity shooter, Level level) {
        return tryCooldown(shooter, level, ArsGlyphPerfConfig.getWololoCooldownTicks(), WOLOLO_LAST);
    }

    public static void clear() {
        TOSS_LAST.clear();
        WOLOLO_LAST.clear();
    }

    private static boolean tryCooldown(
            LivingEntity shooter,
            Level level,
            int cooldownTicks,
            Map<UUID, Long> map
    ) {
        if (!ArsGlyphPerfConfig.isEnabled() || cooldownTicks <= 0 || shooter == null) {
            return true;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return true;
        }
        UUID id = shooter.getUUID();
        long now = serverLevel.getGameTime();
        Long last = map.get(id);
        if (last != null && now - last < cooldownTicks) {
            return false;
        }
        if (map.size() >= MAX_ENTRIES) {
            map.clear();
        }
        map.put(id, now);
        return true;
    }
}
