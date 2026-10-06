package com.mawlee.cointcore.mixin.ftbessentials;

import com.mawlee.cointcore.ftbessentials.KitUpdateService;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.ftb.mods.ftbessentials.commands.impl.kit.KitCommand;
import dev.ftb.mods.ftbessentials.kit.Kit;
import dev.ftb.mods.ftbessentials.kit.KitManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Adds {@code /ftb_kit update_from_*} — rewrite kit items without delete/recreate
 * (preserves cooldown + auto_grant).
 */
@Mixin(value = KitCommand.class, remap = false)
public abstract class KitCommandUpdateMixin {
    @Inject(method = "register", at = @At("RETURN"), remap = false)
    private void cointcore$appendUpdateCommands(
            CallbackInfoReturnable<List<LiteralArgumentBuilder<CommandSourceStack>>> cir
    ) {
        List<LiteralArgumentBuilder<CommandSourceStack>> registered = cir.getReturnValue();
        if (registered == null || registered.isEmpty()) {
            return;
        }

        LiteralArgumentBuilder<CommandSourceStack> root = registered.getFirst();
        root.then(Commands.literal("update_from_player_inv")
                .then(nameArg().executes(ctx -> updateFromPlayer(ctx, false))));
        root.then(Commands.literal("update_from_player_hotbar")
                .then(nameArg().executes(ctx -> updateFromPlayer(ctx, true))));
        root.then(Commands.literal("update_from_block_inv")
                .then(nameArg().executes(KitCommandUpdateMixin::updateFromBlock)));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> nameArg() {
        return Commands.argument("name", StringArgumentType.word())
                .suggests((ctx, builder) -> suggestExistingKits(builder));
    }

    private static CompletableFuture<Suggestions> suggestExistingKits(SuggestionsBuilder builder) {
        List<String> names = KitManager.getInstance().allKits().stream()
                .map(Kit::getKitName)
                .sorted()
                .toList();
        return SharedSuggestionProvider.suggest(names, builder);
    }

    private static int updateFromPlayer(CommandContext<CommandSourceStack> ctx, boolean hotbarOnly)
            throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");
        ServerPlayer player = source.getPlayerOrException();
        KitUpdateService.updateFromPlayerInv(name, player, hotbarOnly);
        source.sendSuccess(
                () -> CointCoreMessages.forSource(source, CointCoreMessages.KIT_UPDATED, name),
                true
        );
        return 1;
    }

    private static int updateFromBlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");
        ServerPlayer player = source.getPlayerOrException();
        KitUpdateService.updateFromBlockInv(name, player);
        source.sendSuccess(
                () -> CointCoreMessages.forSource(source, CointCoreMessages.KIT_UPDATED, name),
                true
        );
        return 1;
    }
}
