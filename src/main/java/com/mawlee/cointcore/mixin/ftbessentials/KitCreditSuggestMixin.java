package com.mawlee.cointcore.mixin.ftbessentials;

import com.mawlee.cointcore.kit.KitCreditService;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.ftb.mods.ftbessentials.commands.impl.kit.KitCommand;
import dev.ftb.mods.ftbessentials.kit.Kit;
import dev.ftb.mods.ftbessentials.kit.KitManager;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.concurrent.CompletableFuture;

@Mixin(value = KitCommand.class, remap = false)
public abstract class KitCreditSuggestMixin {
    @Inject(
            method = "suggestKits(Lnet/minecraft/server/level/ServerPlayer;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private static void cointcore$suggestKitsWithCredits(
            @Nullable ServerPlayer player,
            SuggestionsBuilder builder,
            CallbackInfoReturnable<CompletableFuture<Suggestions>> cir
    ) {
        if (player == null) {
            return;
        }

        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (Kit kit : KitManager.getInstance().allKits()) {
            if (kit.playerCanGetKit(player)) {
                merged.add(kit.getKitName());
            }
        }

        KitCreditService.getAllCredits(player.server, player.getUUID()).forEach((kitName, amount) -> {
            if (amount > 0) {
                merged.add(kitName);
            }
        });

        cir.setReturnValue(SharedSuggestionProvider.suggest(merged, builder));
        cir.cancel();
    }
}
