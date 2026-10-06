package com.mawlee.cointcore.mixin.justdirethings;

import com.direwolf20.justdirethings.common.entities.PortalEntity;
import com.direwolf20.justdirethings.setup.Registration;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.config.JdtPortalChunkConfig;
import com.mawlee.cointcore.justdirethings.JdtPortalChunkLoadPolicy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Portal Gun portals keep working as teleporters, but no longer permanently force-load chunks.
 */
@Mixin(value = PortalEntity.class, remap = false)
public abstract class PortalEntityChunkLoadMixin {
    @WrapOperation(
            method = "onAddedToLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/common/world/chunk/TicketController;forceChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;IIZZ)Z"
            ),
            remap = false
    )
    private boolean cointcore$gateForceLoadOnAdd(
            TicketController controller,
            ServerLevel level,
            Entity owner,
            int chunkX,
            int chunkZ,
            boolean add,
            boolean ticking,
            Operation<Boolean> original
    ) {
        if (!add) {
            return original.call(controller, level, owner, chunkX, chunkZ, false, ticking);
        }
        if (!JdtPortalChunkLoadPolicy.shouldForceLoad(
                JdtPortalChunkConfig.isEnabled(),
                cointcore$policyMode(),
                cointcore$ownerOnline(level)
        )) {
            return false;
        }
        return original.call(controller, level, owner, chunkX, chunkZ, true, ticking);
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void cointcore$syncOwnerOnlineTickets(CallbackInfo ci) {
        PortalEntity self = (PortalEntity) (Object) this;
        if (self.level().isClientSide() || !JdtPortalChunkConfig.isEnabled()) {
            return;
        }
        if (JdtPortalChunkConfig.getMode() != JdtPortalChunkConfig.Mode.OWNER_ONLINE) {
            return;
        }
        if (Math.floorMod(self.level().getGameTime(), 40L) != Math.floorMod(self.getId(), 40)) {
            return;
        }
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean ownerOnline = cointcore$ownerOnline(serverLevel);
        var chunkPos = self.chunkPosition();
        Registration.TICKET_CONTROLLER.forceChunk(
                serverLevel,
                self,
                chunkPos.x,
                chunkPos.z,
                ownerOnline,
                false
        );
    }

    private static JdtPortalChunkLoadPolicy.Mode cointcore$policyMode() {
        return switch (JdtPortalChunkConfig.getMode()) {
            case NONE -> JdtPortalChunkLoadPolicy.Mode.NONE;
            case OWNER_ONLINE -> JdtPortalChunkLoadPolicy.Mode.OWNER_ONLINE;
        };
    }

    private boolean cointcore$ownerOnline(ServerLevel level) {
        PortalEntity self = (PortalEntity) (Object) this;
        UUID owner = self.getOwner();
        if (owner == null) {
            return false;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        return JdtPortalChunkLoadPolicy.isOwnerOnline(owner, player != null);
    }
}
