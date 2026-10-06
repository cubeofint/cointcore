package com.mawlee.cointcore.mixin.draconicevolution;

import com.brandon3055.draconicevolution.blocks.reactor.ProcessExplosion;
import com.mawlee.cointcore.config.ExplosionTerrainConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * DE 3.1.4.x reactor {@link ProcessExplosion} bypasses vanilla ExplosionEvent.
 * When terrain protection is on: abort the process early and strip block/lava targets.
 * <p>
 * Intentionally targets this class (no client-only refs) instead of {@code TileReactorCore},
 * which embeds client GUI/Minecraft types in common bytecode.
 */
@Mixin(value = ProcessExplosion.class, remap = false)
public abstract class ProcessExplosionMixin {

    @Shadow
    public boolean isDead;

    @Shadow
    protected boolean calculationComplete;

    @Shadow
    protected boolean detonated;

    @Shadow
    public LinkedList<HashSet<Long>> destroyedBlocks;

    @Shadow
    public HashSet<Long> lavaPositions;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void cointcore$abortProtectedTerrainBoom(BlockPos pos, int radius, ServerLevel level, int minimumDelay, CallbackInfo ci) {
        if (!ExplosionTerrainConfig.isEnabled() || level == null) {
            return;
        }
        isDead = true;
        calculationComplete = true;
        detonated = true;
        if (destroyedBlocks != null) {
            destroyedBlocks.clear();
        }
        if (lavaPositions != null) {
            lavaPositions.clear();
        }
        level.removeBlock(pos, false);
    }

    @Inject(method = "updateProcess", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$skipProtectedTerrainProcess(CallbackInfo ci) {
        if (ExplosionTerrainConfig.isEnabled()) {
            isDead = true;
            ci.cancel();
        }
    }

    @Inject(
            method = "detonate",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/brandon3055/draconicevolution/lib/ExplosionHelper;setBlocksForRemoval(Ljava/util/LinkedList;)V"
            ),
            remap = false
    )
    private void cointcore$clearTerrainTargets(CallbackInfoReturnable<Boolean> cir) {
        if (!ExplosionTerrainConfig.isEnabled()) {
            return;
        }
        if (destroyedBlocks != null) {
            destroyedBlocks.clear();
        }
        if (lavaPositions != null) {
            lavaPositions.clear();
        }
    }
}
