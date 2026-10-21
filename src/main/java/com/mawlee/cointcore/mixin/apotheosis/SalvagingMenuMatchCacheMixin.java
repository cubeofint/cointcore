package com.mawlee.cointcore.mixin.apotheosis;

import com.mawlee.cointcore.apotheosis.SalvageMatchCache;
import com.mawlee.cointcore.config.ApotheosisSalvageCacheConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Soft mixin: only applied when {@code apotheosis} is loaded. Serves {@code SalvagingMenu.findMatch(Level, ItemStack)}
 * from {@link SalvageMatchCache} on the server so pipes offering items to a Salvaging Table every tick stop scanning
 * every salvaging recipe each time.
 *
 * <p>Signature verified against Apotheosis-1.21.1-8.9.0.
 */
@Pseudo
@Mixin(targets = "dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingMenu", remap = false)
public abstract class SalvagingMenuMatchCacheMixin {
    @Inject(method = "findMatch", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$cachedMatch(Level level, ItemStack stack, CallbackInfoReturnable<List<?>> cir) {
        if (!ApotheosisSalvageCacheConfig.isEnabled() || level == null || level.isClientSide() || stack.isEmpty()) {
            return;
        }
        List<?> cached = SalvageMatchCache.get(level.getRecipeManager(), stack);
        if (cached != null) {
            cir.setReturnValue(new ArrayList<>(cached));
        }
    }

    @Inject(method = "findMatch", at = @At("RETURN"), remap = false)
    private static void cointcore$storeMatch(Level level, ItemStack stack, CallbackInfoReturnable<List<?>> cir) {
        if (!ApotheosisSalvageCacheConfig.isEnabled() || level == null || level.isClientSide() || stack.isEmpty()) {
            return;
        }
        SalvageMatchCache.put(level.getRecipeManager(), stack, cir.getReturnValue());
    }
}
