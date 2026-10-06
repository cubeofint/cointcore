package com.mawlee.cointcore.mixin.arsunification;

import com.mawlee.cointcore.config.ArsUnificationPerfConfig;
import com.mojang.logging.LogUtils;
import dev.qther.ars_unification.ArsUnification;
import dev.qther.ars_unification.Config;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ars Unification 1.2.18 runs {@code processRecipes()} on every {@code OnDatapackSyncEvent},
 * including each player login (~0.5s on the server thread). Skip after the first successful
 * build; still rebuild when syncing all players ({@code /reload}, player == null).
 */
@Mixin(value = ArsUnification.class, remap = false)
public abstract class DatapackSyncRecipeMixin {
    @Unique
    private static final Logger COINTCORE$LOGGER = LogUtils.getLogger();

    @Unique
    private static boolean cointcore$recipesProcessed;

    @Inject(method = "onDatapackSync", at = @At("HEAD"), cancellable = true)
    private void cointcore$skipPerPlayerRecipeRebuild(OnDatapackSyncEvent event, CallbackInfo ci) {
        if (!ArsUnificationPerfConfig.isSkipPerPlayerSync()) {
            return;
        }

        if (event.getPlayer() == null) {
            cointcore$recipesProcessed = false;
            return;
        }

        if (cointcore$recipesProcessed) {
            ci.cancel();
        }
    }

    @Inject(method = "processRecipes", at = @At("RETURN"))
    private static void cointcore$markRecipesProcessed(MinecraftServer server, CallbackInfo ci) {
        if (Config.SPEC.isLoaded()) {
            if (!cointcore$recipesProcessed) {
                COINTCORE$LOGGER.info("Ars Unification recipes processed once; skipping further per-player rebuilds");
            }
            cointcore$recipesProcessed = true;
        }
    }
}
