package com.mawlee.cointcore.mixin.naturesaura;

import com.mawlee.cointcore.config.NaturesAuraPerfConfig;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.chunk.effect.PlantBoostEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Caps Plant Boost work per aura tick. NA can request up to ~75 random grow attempts
 * per positive drain spot; that dominates server hangs under high aura.
 */
@Mixin(value = PlantBoostEffect.class, remap = false)
public abstract class PlantBoostEffectMixin {
    @Shadow
    private int amount;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$maybeDisablePlantBoost(
            Level level,
            LevelChunk chunk,
            IAuraChunk auraChunk,
            BlockPos pos,
            Integer spot,
            IAuraChunk.DrainSpot actualSpot,
            CallbackInfo ci
    ) {
        if (!NaturesAuraPerfConfig.isPlantBoostEnabled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lde/ellpeck/naturesaura/chunk/effect/PlantBoostEffect;calcValues(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Ljava/lang/Integer;)Z",
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private void cointcore$capPlantBoostAmount(
            Level level,
            LevelChunk chunk,
            IAuraChunk auraChunk,
            BlockPos pos,
            Integer spot,
            IAuraChunk.DrainSpot actualSpot,
            CallbackInfo ci
    ) {
        int max = NaturesAuraPerfConfig.getPlantBoostMaxAmount();
        if (this.amount > max) {
            this.amount = max;
        }
    }
}
