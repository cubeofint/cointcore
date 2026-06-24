package com.mawlee.cointcore.mixin.apothicspawners;

import com.mawlee.cointcore.spawner.VanillaSpawnerLimits;
import dev.shadowsoffire.apothic_spawners.block.ApothSpawnerTile;
import dev.shadowsoffire.apothic_spawners.modifiers.StatModifier;
import dev.shadowsoffire.apothic_spawners.stats.SpawnerStat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StatModifier.class, remap = false)
public abstract class StatModifierMixin {
    @Shadow
    @Final
    private SpawnerStat<?> stat;

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$blockSpawnRangeUpgrade(ApothSpawnerTile tile, CallbackInfoReturnable<Boolean> cir) {
        if (VanillaSpawnerLimits.APOTHIC_SPAWN_RANGE_STAT.equals(this.stat.getId())) {
            cir.setReturnValue(false);
        }
    }
}
