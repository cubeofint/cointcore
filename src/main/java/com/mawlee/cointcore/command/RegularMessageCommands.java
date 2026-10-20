package com.mawlee.cointcore.command;

import com.mawlee.cointcore.config.RegularMessagesConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.reminder.RegularMessageService;
import com.mawlee.cointcore.reminder.RegularSchedule;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

/**
 * {@code /addreg <частота> <текст>}, {@code /reglist}, {@code /delreg <номер>} — regular chat reminders
 * (permission {@code cointcore.reminders.manage}).
 */
public final class RegularMessageCommands {
    private static final List<String> SCHEDULE_EXAMPLES = List.of("1/д", "2/д", "4/д", "+30м", "+1ч", "+2ч30м");

    private RegularMessageCommands() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandRegistration.replaceRootLiteral(dispatcher, "addreg", Commands.literal("addreg")
                .requires(RegularMessageCommands::canManage)
                .executes(context -> usage(context.getSource()))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests((context, builder) -> builder.getRemaining().contains(" ")
                                ? builder.buildFuture()
                                : SharedSuggestionProvider.suggest(SCHEDULE_EXAMPLES, builder))
                        .executes(RegularMessageCommands::add)));
        CommandRegistration.replaceRootLiteral(dispatcher, "reglist", Commands.literal("reglist")
                .requires(RegularMessageCommands::canManage)
                .executes(context -> list(context.getSource())));
        CommandRegistration.replaceRootLiteral(dispatcher, "delreg", Commands.literal("delreg")
                .requires(RegularMessageCommands::canManage)
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                RegularMessagesConfig.entries().stream().map(entry -> String.valueOf(entry.id())),
                                builder))
                        .executes(RegularMessageCommands::delete)));
    }

    private static boolean canManage(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.REMINDERS_MANAGE);
        }
        return source.hasPermission(2);
    }

    private static int usage(CommandSourceStack source) {
        source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.REG_USAGE));
        return 0;
    }

    private static int add(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String args = StringArgumentType.getString(context, "args").trim();
        int split = args.indexOf(' ');
        if (split <= 0 || args.substring(split + 1).isBlank()) {
            return usage(source);
        }
        String scheduleToken = args.substring(0, split);
        String text = args.substring(split + 1).trim();
        Optional<RegularSchedule> schedule = RegularSchedule.parse(scheduleToken);
        if (schedule.isEmpty()) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.REG_BAD_SCHEDULE, scheduleToken));
            return 0;
        }
        Optional<RegularMessagesConfig.Entry> added = RegularMessagesConfig.add(schedule.get(), text, source.getTextName());
        if (added.isEmpty()) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.REG_SAVE_FAILED));
            return 0;
        }
        RegularMessagesConfig.Entry entry = added.get();
        String next = RegularMessageService.describeNext(entry.schedule())
                .orElseGet(() -> plain(source, CointCoreMessages.REG_NEXT_DONE));
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.REG_ADDED,
                entry.id(), entry.schedule().describe(), next), true);
        source.sendSuccess(() -> RegularMessageService.render(entry.text()), false);
        return entry.id();
    }

    private static int list(CommandSourceStack source) {
        List<RegularMessagesConfig.Entry> entries = RegularMessagesConfig.entries();
        if (entries.isEmpty()) {
            source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.REG_LIST_EMPTY), false);
            return 0;
        }
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.REG_LIST_HEADER, entries.size()), false);
        if (!RegularMessagesConfig.get().enabled()) {
            source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.REG_LIST_DISABLED), false);
        }
        for (RegularMessagesConfig.Entry entry : entries) {
            String next = RegularMessageService.describeNext(entry.schedule())
                    .orElseGet(() -> plain(source, CointCoreMessages.REG_NEXT_DONE));
            MutableComponent line = Component.empty()
                    .append(CointCoreMessages.forSource(source, CointCoreMessages.REG_LIST_ENTRY,
                            entry.id(), entry.schedule().describe(), next))
                    .append(RegularMessageService.render(entry.text()))
                    .append(Component.literal(" "))
                    .append(CointCoreMessages.forSource(source, CointCoreMessages.REG_LIST_DELETE).copy()
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/delreg " + entry.id()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            CointCoreMessages.forSource(source, CointCoreMessages.REG_LIST_DELETE_HOVER, entry.id())))));
            source.sendSuccess(() -> line, false);
        }
        return entries.size();
    }

    private static int delete(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int id = IntegerArgumentType.getInteger(context, "id");
        if (RegularMessagesConfig.find(id).isEmpty()) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.REG_NOT_FOUND, id));
            return 0;
        }
        if (!RegularMessagesConfig.remove(id)) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.REG_SAVE_FAILED));
            return 0;
        }
        RegularMessageService.forget(id);
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.REG_DELETED, id), true);
        return 1;
    }

    private static String plain(CommandSourceStack source, String key) {
        return CointCoreMessages.forSource(source, key).getString();
    }
}
