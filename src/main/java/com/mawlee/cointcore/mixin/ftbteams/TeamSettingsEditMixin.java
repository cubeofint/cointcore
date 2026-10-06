package com.mawlee.cointcore.mixin.ftbteams;

import com.mawlee.cointcore.claim.ClaimFlagEditClient;
import dev.ftb.mods.ftbteams.api.property.TeamProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.ftb.mods.ftbteams.client.gui.MyTeamScreen$SettingsButton", remap = false)
public class TeamSettingsEditMixin {
    @Redirect(
            method = "lambda$new$2",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ftb/mods/ftbteams/api/property/TeamProperty;isPlayerEditable()Z"
            ),
            remap = false
    )
    private static boolean cointcore$editClaimFlags(TeamProperty<?> property) {
        return ClaimFlagEditClient.canEdit(property);
    }
}
