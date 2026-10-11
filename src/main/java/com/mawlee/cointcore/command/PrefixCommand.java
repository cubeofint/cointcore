package com.mawlee.cointcore.command;

import com.mawlee.cointcore.config.PrefixConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.lang.LegacyTextParser;
import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import com.mawlee.cointcore.luckperms.LuckPermsUsers;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.prefix.PrefixGui;
import com.mawlee.cointcore.prefix.PrefixOption;
import com.mawlee.cointcore.prefix.PrefixSelectionRules;
import com.mawlee.cointcore.prefix.PrefixSelectionService;
import com.mawlee.cointcore.prefix.PrefixSnapshot;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class PrefixCommand {
    private PrefixCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("prefix")
                        .requires(PrefixCommand::canUse)
                        .executes(PrefixCommand::list)
                        .then(Commands.literal("auto").executes(PrefixCommand::auto))
                        .then(Commands.literal("gui").executes(PrefixCommand::gui))
                        .then(Commands.literal("set")
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                suggestGroups(context.getSource()),
                                                builder
                                        ))
                                        .executes(PrefixCommand::set)))
        );
    }

    public static void notifyDecision(ServerPlayer player, PrefixSelectionRules.Decision decision) {
        CommandSourceStack source = player.createCommandSourceStack();
        source.sendSuccess(() -> messageFor(player, decision), false);
    }

    private static boolean canUse(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player
                && PermissionService.has(player, CointPermissionNodes.PREFIX);
    }

    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!ready(player)) {
            return 0;
        }
        LuckPermsUsers.loadSnapshot(player.getUUID()).whenComplete((snapshot, error) ->
                player.server.execute(() -> sendList(player, snapshot, error)));
        return Command.SINGLE_SUCCESS;
    }

    private static void sendList(ServerPlayer player, PrefixSnapshot snapshot, Throwable error) {
        if (error != null || snapshot == null) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_UNAVAILABLE));
            return;
        }
        player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_HEADER));
        player.sendSystemMessage(currentLine(player, snapshot));
        player.sendSystemMessage(autoLine(player));
        for (PrefixOption option : snapshot.available()) {
            player.sendSystemMessage(optionLine(player, option, option.group().equals(snapshot.selected())));
        }
        if (PrefixConfig.get().guiEnabled()) {
            player.sendSystemMessage(guiLine(player));
        }
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!ready(player)) {
            return 0;
        }
        String group = StringArgumentType.getString(context, "group");
        PrefixSelectionService.select(player.server, player.getUUID(), group, decision ->
                context.getSource().sendSuccess(() -> messageFor(player, decision), false));
        return Command.SINGLE_SUCCESS;
    }

    private static int auto(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!ready(player)) {
            return 0;
        }
        PrefixSelectionService.selectAuto(player.server, player.getUUID(), decision ->
                context.getSource().sendSuccess(() -> messageFor(player, decision), false));
        return Command.SINGLE_SUCCESS;
    }

    private static int gui(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!ready(player)) {
            return 0;
        }
        if (!PrefixConfig.get().guiEnabled()) {
            context.getSource().sendFailure(CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_GUI_DISABLED));
            return 0;
        }
        LuckPermsUsers.loadSnapshot(player.getUUID()).whenComplete((snapshot, error) -> player.server.execute(() -> {
            if (error != null || snapshot == null) {
                player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_UNAVAILABLE));
                return;
            }
            PrefixGui.open(player, snapshot);
        }));
        return Command.SINGLE_SUCCESS;
    }

    private static boolean ready(ServerPlayer player) {
        if (!PrefixConfig.get().enabled() || !LuckPermsIntegration.isAvailable()) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_UNAVAILABLE));
            return false;
        }
        return true;
    }

    private static Component messageFor(ServerPlayer player, PrefixSelectionRules.Decision decision) {
        return switch (decision.kind()) {
            case APPLIED -> CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_SET, decision.selectedGroup());
            case AUTO -> CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_AUTO);
            case UNAVAILABLE -> CointCoreMessages.forPlayer(
                    player,
                    CointCoreMessages.PREFIX_NOT_AVAILABLE,
                    decision.selectedGroup() == null ? "" : decision.selectedGroup()
            );
            case UNCHANGED -> CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_UNCHANGED);
        };
    }

    private static Component currentLine(ServerPlayer player, PrefixSnapshot snapshot) {
        if (snapshot.selected() == null) {
            return CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_CURRENT_AUTO);
        }
        PrefixOption option = snapshot.option(snapshot.selected());
        String preview = option == null ? snapshot.selected() : option.prefixRaw();
        return CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_CURRENT, snapshot.selected())
                .copy()
                .append(LegacyTextParser.parse(" " + preview));
    }

    private static Component autoLine(ServerPlayer player) {
        MutableComponent line = CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_ENTRY_AUTO).copy();
        return line.withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/prefix auto"))
                .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_HOVER_AUTO)
                )));
    }

    private static Component optionLine(ServerPlayer player, PrefixOption option, boolean selected) {
        MutableComponent prefix = LegacyTextParser.parse(option.prefixRaw()).copy();
        MutableComponent line = CointCoreMessages.forPlayer(
                player,
                selected ? CointCoreMessages.PREFIX_ENTRY_SELECTED : CointCoreMessages.PREFIX_ENTRY,
                option.group(),
                option.category()
        ).copy();
        Component hover = CointCoreMessages.forPlayer(
                player,
                CointCoreMessages.PREFIX_HOVER_SET,
                option.group()
        ).copy().append(Component.literal(" ")).append(prefix).append(Component.literal(player.getGameProfile().getName()));
        return prefix.append(Component.literal(" ")).append(line).withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/prefix set " + option.group()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
    }

    private static Component guiLine(ServerPlayer player) {
        return CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_OPEN_GUI).copy()
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/prefix gui"))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_HOVER_GUI)
                        )));
    }

    private static List<String> suggestGroups(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return List.of();
        }
        return LuckPermsUsers.snapshotIfLoaded(player.getUUID())
                .map(snapshot -> {
                    List<String> groups = new ArrayList<>();
                    for (PrefixOption option : snapshot.available()) {
                        groups.add(option.group());
                    }
                    return groups;
                })
                .orElse(List.of());
    }
}
