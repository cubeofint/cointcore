package com.mawlee.cointcore.mixin.forbiddenarcanus;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mawlee.cointcore.config.ForbiddenArcanusPerfConfig;
import com.mawlee.cointcore.spark.PerfTickCache;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;

/**
 * FA injects into {@code LivingEntity.hasEffect} and scans every equipment slot for
 * {@code GRANTS_EFFECTS}. That cannot be targeted as a foreign mixin handler (not visible
 * at inject-prepare), and the scan is not inlined into {@code hasEffect}.
 * <p>
 * {@link WrapMethod} wraps the whole method: when we skip, we answer from the active-effects
 * map and never call {@code original}, so FA's inject does not run.
 */
@Mixin(LivingEntity.class)
public abstract class HasEffectPreventMixin {
    @Shadow
    @Final
    private Map<Holder<MobEffect>, MobEffectInstance> activeEffects;

    @Unique
    private static long cointcore$cacheTick = Long.MIN_VALUE;

    @Unique
    private static final Map<Long, Boolean> COINTCORE$TICK_CACHE = new HashMap<>();

    @WrapMethod(method = "hasEffect")
    private boolean cointcore$wrapHasEffect(Holder<MobEffect> effect, Operation<Boolean> original) {
        if (!ForbiddenArcanusPerfConfig.isEnabled()) {
            return original.call(effect);
        }

        LivingEntity self = (LivingEntity) (Object) this;
        Level level = self.level();
        if (level.isClientSide) {
            return original.call(effect);
        }

        boolean player = self instanceof Player;

        // Hot path: mobs never pay FA equipment scan.
        if (ForbiddenArcanusPerfConfig.isSkipNonPlayers() && !player) {
            return this.activeEffects.containsKey(effect);
        }

        // High MSPT: skip FA scan for non-players always; for players when configured (prod default).
        if (PerfTickCache.isMsptAtLeast(ForbiddenArcanusPerfConfig.getMsptSkipThreshold())) {
            if (!player || ForbiddenArcanusPerfConfig.isSkipPlayersWhenMsptHigh()) {
                return this.activeEffects.containsKey(effect);
            }
        }

        if (ForbiddenArcanusPerfConfig.isTickCachePlayers() && player) {
            long tick = level.getGameTime();
            if (tick != cointcore$cacheTick) {
                COINTCORE$TICK_CACHE.clear();
                cointcore$cacheTick = tick;
            }

            long key = cointcore$cacheKey(self, effect);
            Boolean cached = COINTCORE$TICK_CACHE.get(key);
            if (cached != null) {
                return cached;
            }

            boolean result = original.call(effect);
            COINTCORE$TICK_CACHE.put(key, result);
            return result;
        }

        return original.call(effect);
    }

    @Unique
    private static long cointcore$cacheKey(LivingEntity entity, Holder<MobEffect> effect) {
        int effectId = BuiltInRegistries.MOB_EFFECT.getId(effect.value());
        return ((long) entity.getId() << 32) | (effectId & 0xffffffffL);
    }
}
