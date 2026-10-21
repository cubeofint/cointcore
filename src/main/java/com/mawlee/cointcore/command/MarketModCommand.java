package com.mawlee.cointcore.command;

import com.mawlee.cointcore.config.MarketMinPricesConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.mute.MuteService;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.shop.GlobalMarketListing;
import com.mawlee.cointcore.shop.GlobalMarketService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Market moderation (permission {@code cointcore.market.moderate}):
 * <ul>
 *     <li>{@code /marketmod list <player>} — the player's active lots with a clickable [снять];</li>
 *     <li>{@code /marketmod remove <lot> [reason]} — removes a lot, its stock goes to the seller's returns;</li>
 *     <li>{@code /marketmod exportprices} — writes a min-price template from what is on sale now.</li>
 * </ul>
 */
public final class MarketModCommand {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MarketModCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandRegistration.replaceRootLiteral(dispatcher, "marketmod", Commands.literal("marketmod")
                .requires(MarketModCommand::canModerate)
                .then(Commands.literal("list")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        GlobalMarketService.activeSellerNames(context.getSource().getServer()), builder))
                                .executes(MarketModCommand::list)))
                .then(Commands.literal("remove")
                        .then(Commands.argument("lot", StringArgumentType.word())
                                .executes(context -> remove(context, ""))
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(context -> remove(context,
                                                StringArgumentType.getString(context, "reason"))))))
                .then(Commands.literal("exportprices")
                        .executes(MarketModCommand::exportPrices)));
    }

    private static boolean canModerate(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.MARKET_MODERATE);
        }
        return source.hasPermission(2);
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        String name = StringArgumentType.getString(context, "player");
        Optional<UUID> sellerId = MuteService.resolvePlayerId(server, name);
        if (sellerId.isEmpty()) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.MARKETMOD_PLAYER_NOT_FOUND, name));
            return 0;
        }
        List<GlobalMarketListing> lots = GlobalMarketService.sellerListings(server, sellerId.get());
        if (lots.isEmpty()) {
            source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.MARKETMOD_LIST_EMPTY, name), false);
            return 0;
        }
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.MARKETMOD_LIST_HEADER, name, lots.size()), false);
        for (GlobalMarketListing lot : lots) {
            String id = lot.id().toString();
            MutableComponent line = Component.empty()
                    .append(CointCoreMessages.forSource(source, CointCoreMessages.MARKETMOD_LIST_ENTRY,
                            lot.countPerDeal(), lot.stack().getHoverName().getString(), lot.pricePerDeal(),
                            lot.dealsLeft()))
                    .append(Component.literal(" "))
                    .append(CointCoreMessages.forSource(source, CointCoreMessages.MARKETMOD_LIST_REMOVE).copy()
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                            "/marketmod remove " + id + " "))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            CointCoreMessages.forSource(source, CointCoreMessages.MARKETMOD_LIST_REMOVE_HOVER)))));
            source.sendSuccess(() -> line, false);
        }
        return lots.size();
    }

    private static int remove(CommandContext<CommandSourceStack> context, String reason) {
        CommandSourceStack source = context.getSource();
        String raw = StringArgumentType.getString(context, "lot");
        UUID id;
        try {
            id = UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.MARKETMOD_NOT_FOUND));
            return 0;
        }
        GlobalMarketListing lot = GlobalMarketService.moderatorRemove(source.getServer(), id);
        if (lot == null) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.MARKETMOD_NOT_FOUND));
            return 0;
        }
        String item = lot.stack().getHoverName().getString();
        String cleanReason = reason == null ? "" : reason.trim();
        LOGGER.info("MARKET-MOD {} removed lot {} of {}: {}x {} for {} ({} items returned){}",
                source.getTextName(), lot.id(), lot.sellerName(), lot.countPerDeal(), item, lot.pricePerDeal(),
                lot.remainingStock(), cleanReason.isEmpty() ? "" : ", reason: " + cleanReason);
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.MARKETMOD_REMOVED,
                item, lot.sellerName()), true);

        ServerPlayer seller = source.getServer().getPlayerList().getPlayer(lot.sellerId());
        if (seller != null) {
            seller.sendSystemMessage(CointCoreMessages.forPlayer(seller, CointCoreMessages.MARKETMOD_SELLER_NOTICE, item,
                    cleanReason.isEmpty()
                            ? CointCoreMessages.forPlayer(seller, CointCoreMessages.MARKETMOD_NO_REASON).getString()
                            : cleanReason));
        }
        return 1;
    }

    private static int exportPrices(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Map<String, Long> prices = GlobalMarketService.currentPriceTemplate(source.getServer());
        try {
            Path path = MarketMinPricesConfig.writeTemplate(prices);
            source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.MARKETMOD_EXPORTED,
                    prices.size(), path.getFileName().toString()), false);
            return prices.size();
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to write market min price template", exception);
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.MARKETMOD_EXPORT_FAILED));
            return 0;
        }
    }
}
