package com.mawlee.cointcore.command;

import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.twilight.TwilightBossSavedData;
import com.mawlee.cointcore.twilight.TwilightBosses;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * {@code /cointcore tfbosses}: Twilight Forest boss arenas. Arenas are recorded automatically whenever a boss appears;
 * {@code mark} adds one by hand for bosses killed before that (stand on the boss's loot chest).
 */
public final class TwilightBossCommand {
    private TwilightBossCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("tfbosses")
                .requires(TwilightBossCommand::canAdmin)
                .then(Commands.literal("list").executes(TwilightBossCommand::list))
                .then(Commands.literal("respawn").executes(TwilightBossCommand::respawn))
                .then(Commands.literal("mark")
                        .then(Commands.argument("boss", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        TwilightBosses.SPAWNERS.keySet(), builder))
                                .executes(TwilightBossCommand::mark)))
                .then(Commands.literal("remove")
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(TwilightBossCommand::remove)))
                .then(Commands.literal("forget")
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(TwilightBossCommand::forget)));
    }

    private static boolean canAdmin(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.BOSS_RESPAWN)
                    || PermissionService.has(player, CointPermissionNodes.RELOAD);
        }
        return source.hasPermission(2);
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<TwilightBossSavedData.Arena> arenas = TwilightBossSavedData.get(source.getServer()).arenas();
        if (arenas.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Арен боссов Сумеречного леса пока нет."), false);
            return 0;
        }
        for (int i = 0; i < arenas.size(); i++) {
            TwilightBossSavedData.Arena arena = arenas.get(i);
            String line = (i + 1) + ". " + arena.spawner().getPath() + " — " + arena.pos().toShortString()
                    + " (" + arena.dimension().location() + ")"
                    + (arena.aliveCount() > 0 ? " — жив: " + arena.aliveCount() : " — убит, вернётся при рестарте");
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return arenas.size();
    }

    private static int respawn(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String summary = TwilightBosses.respawnAll(source.getServer());
        source.sendSuccess(() -> Component.literal(summary), true);
        return 1;
    }

    private static int mark(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Только для игрока: встань туда, где появлялся босс."));
            return 0;
        }
        String boss = StringArgumentType.getString(context, "boss");
        ResourceLocation spawner = TwilightBosses.SPAWNERS.get(boss);
        if (spawner == null) {
            source.sendFailure(Component.literal("Неизвестный босс: " + boss));
            return 0;
        }
        var pos = player.blockPosition();
        if (!TwilightBossSavedData.get(source.getServer()).mark(player.level().dimension(), pos, spawner)) {
            source.sendFailure(Component.literal("Здесь уже есть арена."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Арена " + boss + " отмечена в " + pos.toShortString()
                + ". Спавнер вернётся при следующем рестарте или по /cointcore tfbosses respawn."), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int index = IntegerArgumentType.getInteger(context, "index");
        TwilightBossSavedData.Arena removed = TwilightBossSavedData.get(source.getServer()).remove(index - 1);
        if (removed == null) {
            source.sendFailure(Component.literal("Нет арены с номером " + index));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Арена " + index + " удалена (спавнер, если стоит, остаётся)."), true);
        return 1;
    }

    private static int forget(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int index = IntegerArgumentType.getInteger(context, "index");
        if (!TwilightBossSavedData.get(source.getServer()).forgetAlive(index - 1)) {
            source.sendFailure(Component.literal("Нет арены с номером " + index));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Арена " + index + ": босс считается убитым."), true);
        return 1;
    }
}
