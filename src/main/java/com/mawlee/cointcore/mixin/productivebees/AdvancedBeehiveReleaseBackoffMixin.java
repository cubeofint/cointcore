package com.mawlee.cointcore.mixin.productivebees;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.productivebees.HiveReleaseBackoff;
import cy.jdkdigital.productivebees.common.block.entity.AdvancedBeehiveBlockEntityAbstract;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Productive Bees 13.13 retries a refused bee release every tick (entity load + sound + events each time): ~2% of
 * the ATM10 server tick when a chunk mob limit keeps refusing them. Only the hive's own tick backs off; breaking the
 * hive or smoking it out still releases at once.
 */
@Mixin(value = AdvancedBeehiveBlockEntityAbstract.class, remap = false)
public abstract class AdvancedBeehiveReleaseBackoffMixin implements HiveReleaseBackoff {
    @Unique
    private long cointcore$releaseRetryAt;

    @Override
    public long cointcore$releaseRetryAt() {
        return cointcore$releaseRetryAt;
    }

    @Override
    public void cointcore$setReleaseRetryAt(long gameTime) {
        cointcore$releaseRetryAt = gameTime;
    }

    @WrapOperation(
            method = "tickBees",
            at = @At(
                    value = "INVOKE",
                    target = "Lcy/jdkdigital/productivebees/common/block/entity/AdvancedBeehiveBlockEntityAbstract;releaseOccupant(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BeehiveBlockEntity$Occupant;Lcy/jdkdigital/productivebees/common/block/entity/AdvancedBeehiveBlockEntityAbstract;Ljava/util/List;Lnet/minecraft/world/level/block/entity/BeehiveBlockEntity$BeeReleaseStatus;)Z"
            ),
            remap = false
    )
    private static boolean cointcore$backOffRefusedRelease(Level level, BlockPos pos, BlockState state,
                                                           BeehiveBlockEntity.Occupant occupant,
                                                           AdvancedBeehiveBlockEntityAbstract hive,
                                                           List<Entity> releasedBees,
                                                           BeehiveBlockEntity.BeeReleaseStatus status,
                                                           Operation<Boolean> original) {
        HiveReleaseBackoff backoff = (HiveReleaseBackoff) hive;
        long now = level.getGameTime();
        if (HiveReleaseBackoff.waiting(backoff, now)) {
            return false;
        }
        boolean released = original.call(level, pos, state, occupant, hive, releasedBees, status);
        HiveReleaseBackoff.recordResult(backoff, now, released);
        return released;
    }
}
