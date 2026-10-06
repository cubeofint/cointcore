package com.mawlee.cointcore.mixin.ftbteams;

import com.mawlee.cointcore.claim.ClaimFlagEditAccess;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.net.UpdatePropertiesRequestMessage;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UpdatePropertiesRequestMessage.class, remap = false)
public class UpdatePropertiesRequestMixin {
    @Inject(method = "lambda$handle$1", at = @At("HEAD"), remap = false)
    private static void cointcore$keepUnauthorizedFlags(
            ServerPlayer player,
            UpdatePropertiesRequestMessage message,
            Team team,
            CallbackInfo callback
    ) {
        ClaimFlagEditAccess.revertUnauthorized(player, team, message.properties());
    }
}
