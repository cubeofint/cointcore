package com.mawlee.cointcore.mixin.create;

import com.mawlee.cointcore.claim.ClaimGuard;
import com.mawlee.cointcore.claim.ClaimGuardContext;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(MinecraftServer.class)
public abstract class CreateClaimBootstrapMixin {
    @Unique
    private static boolean cointcore$movementChecksRegistered;

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void cointcore$registerMovementChecks(CallbackInfo ci) {
        if (cointcore$movementChecksRegistered || !ClaimGuard.isAvailable()) {
            return;
        }

        cointcore$movementChecksRegistered = true;
        BlockMovementChecks.registerMovementAllowedCheck((state, level, pos) -> {
            Optional<UUID> actorId = ClaimGuardContext.currentActor();
            if (actorId.isEmpty()) {
                return BlockMovementChecks.CheckResult.PASS;
            }

            Entity actor = ClaimGuard.resolveEntity(level, actorId.get()).orElse(null);
            if (actor == null) {
                return BlockMovementChecks.CheckResult.FAIL;
            }

            return ClaimGuard.canEdit(actor, level, pos)
                    ? BlockMovementChecks.CheckResult.SUCCESS
                    : BlockMovementChecks.CheckResult.FAIL;
        });
    }
}
