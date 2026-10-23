package com.mawlee.cointcore.command;

import com.mawlee.cointcore.ghost.GhostHuntSavedData;
import com.mawlee.cointcore.ghost.GhostHuntService;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.mute.MuteService;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code /ghost} — own progress for everyone; {@code add [name] | remove [id] | list | top | stats <player> |
 * reset confirm} for staff ({@code cointcore.ghost.manage}). {@code add} / {@code remove} act on what you look at.
 */
public final class GhostCommand {
    private static final int TOP = 15;
    private static final java.time.format.DateTimeFormatter TIME_FORMAT =
            java.time.format.DateTimeFormatter.ofPattern("dd.MM HH:mm:ss");

    private GhostCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandRegistration.replaceRootLiteral(dispatcher, "ghost", Commands.literal("ghost")
                .requires(source -> source.getEntity() instanceof ServerPlayer || source.hasPermission(2))
                .executes(GhostCommand::progress)
                .then(Commands.literal("add").requires(GhostCommand::canManagePlayer)
                        .executes(context -> add(context, ""))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> add(context, StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("remove").requires(GhostCommand::canManage)
                        .executes(GhostCommand::removeLookedAt)
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        GhostHuntSavedData.get(context.getSource().getServer()).all().stream()
                                                .map(ghost -> String.valueOf(ghost.id())), builder))
                                .executes(GhostCommand::removeById)))
                .then(Commands.literal("list").requires(GhostCommand::canManage).executes(GhostCommand::list))
                .then(Commands.literal("top").requires(GhostCommand::canManage).executes(GhostCommand::top))
                .then(Commands.literal("stats").requires(GhostCommand::canManage)
                        .then(Commands.argument("player", StringArgumentType.word()).executes(GhostCommand::stats)))
                .then(Commands.literal("reset").requires(GhostCommand::canManage)
                        .then(Commands.literal("confirm").executes(GhostCommand::reset))));
    }

    private static boolean canManage(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.GHOST_MANAGE);
        }
        return source.hasPermission(2);
    }

    private static boolean canManagePlayer(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer && canManage(source);
    }

    private static int progress(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        GhostHuntSavedData data = GhostHuntSavedData.get(player.server);
        int found = data.foundCount(player.getUUID());
        context.getSource().sendSuccess(() -> CointCoreMessages.forPlayer(player, CointCoreMessages.GHOST_PROGRESS,
                found, data.total()), false);
        return found;
    }

    private static int add(CommandContext<CommandSourceStack> context, String label) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Optional<GhostHuntSavedData.Ghost> ghost = GhostHuntService.addLookedAt(player, label.trim());
        if (ghost.isEmpty()) {
            context.getSource().sendFailure(CointCoreMessages.forPlayer(player, CointCoreMessages.GHOST_ADD_FAILED));
            return 0;
        }
        int total = GhostHuntSavedData.get(player.server).total();
        context.getSource().sendSuccess(() -> CointCoreMessages.forPlayer(player, CointCoreMessages.GHOST_ADDED,
                ghost.get().id(), kindLabel(context.getSource(), ghost.get()), total), true);
        return ghost.get().id();
    }

    private static int removeLookedAt(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Optional<GhostHuntSavedData.Ghost> ghost = GhostHuntService.lookedAtGhost(player);
        if (ghost.isEmpty()) {
            context.getSource().sendFailure(CointCoreMessages.forPlayer(player, CointCoreMessages.GHOST_NOT_LOOKING));
            return 0;
        }
        return remove(context.getSource(), ghost.get().id());
    }

    private static int removeById(CommandContext<CommandSourceStack> context) {
        return remove(context.getSource(), IntegerArgumentType.getInteger(context, "id"));
    }

    private static int remove(CommandSourceStack source, int id) {
        if (!GhostHuntSavedData.get(source.getServer()).remove(id)) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.GHOST_NOT_FOUND, id));
            return 0;
        }
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.GHOST_REMOVED, id), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<GhostHuntSavedData.Ghost> ghosts = List.copyOf(GhostHuntSavedData.get(source.getServer()).all());
        if (ghosts.isEmpty()) {
            source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.GHOST_LIST_EMPTY), false);
            return 0;
        }
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.GHOST_LIST_HEADER, ghosts.size()), false);
        for (GhostHuntSavedData.Ghost ghost : ghosts) {
            BlockPos pos = BlockPos.of(ghost.pos());
            String tp = "/execute in " + ghost.dimension() + " run tp @s " + pos.getX() + " " + (pos.getY() + 1) + " " + pos.getZ();
            MutableComponent line = Component.empty()
                    .append(CointCoreMessages.forSource(source, CointCoreMessages.GHOST_LIST_ENTRY, ghost.id(),
                            kindLabel(source, ghost), ghost.label().isEmpty() ? "-" : ghost.label(),
                            ghost.dimension(), pos.getX(), pos.getY(), pos.getZ()))
                    .append(Component.literal(" "))
                    .append(CointCoreMessages.forSource(source, CointCoreMessages.GHOST_LIST_TP).copy()
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tp))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tp)))));
            source.sendSuccess(() -> line, false);
        }
        return ghosts.size();
    }

    private static int top(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        GhostHuntSavedData data = GhostHuntSavedData.get(source.getServer());
        List<Map.Entry<UUID, Integer>> ranking = data.ranking();
        if (ranking.isEmpty()) {
            source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.GHOST_TOP_EMPTY), false);
            return 0;
        }
        int total = data.total();
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.GHOST_TOP_HEADER, total), false);
        for (int i = 0; i < Math.min(TOP, ranking.size()); i++) {
            Map.Entry<UUID, Integer> row = ranking.get(i);
            int place = i + 1;
            String name = data.nameOf(row.getKey());
            long at = data.lastFindAt(row.getKey());
            String when = at <= 0L ? "—" : java.time.Instant.ofEpochMilli(at)
                    .atZone(com.mawlee.cointcore.config.RegularMessagesConfig.get().zone())
                    .format(TIME_FORMAT);
            source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.GHOST_TOP_ENTRY,
                    place, name, row.getValue(), total, when), false);
        }
        return ranking.size();
    }

    private static int stats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "player");
        Optional<UUID> playerId = MuteService.resolvePlayerId(source.getServer(), name);
        if (playerId.isEmpty()) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.MARKETMOD_PLAYER_NOT_FOUND, name));
            return 0;
        }
        GhostHuntSavedData data = GhostHuntSavedData.get(source.getServer());
        int found = data.foundCount(playerId.get());
        StringBuilder ids = new StringBuilder();
        for (GhostHuntSavedData.Ghost ghost : data.all()) {
            if (data.hasFound(playerId.get(), ghost.id())) {
                ids.append(ids.isEmpty() ? "" : ", ").append('#').append(ghost.id());
            }
        }
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.GHOST_STATS, name, found,
                data.total(), ids.isEmpty() ? "-" : ids.toString()), false);
        return found;
    }

    private static int reset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        GhostHuntSavedData.get(source.getServer()).resetFinds();
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.GHOST_RESET), true);
        return 1;
    }

    private static String kindLabel(CommandSourceStack source, GhostHuntSavedData.Ghost ghost) {
        return CointCoreMessages.forSource(source, ghost.kind() == GhostHuntSavedData.Kind.ENTITY
                ? CointCoreMessages.GHOST_KIND_ENTITY : CointCoreMessages.GHOST_KIND_BLOCK).getString();
    }
}
