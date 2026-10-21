package com.mawlee.cointcore.command;

import com.mawlee.cointcore.config.QuizConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.quiz.QuizCategory;
import com.mawlee.cointcore.quiz.QuizQuestion;
import com.mawlee.cointcore.quiz.QuizService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * {@code /quiz join|decline} for players; {@code /quiz add|list|remove|start|test} for staff
 * ({@code cointcore.quiz.manage}). Adding: {@code /quiz add <категория> вопрос | правильный | неверный | неверный | неверный}.
 */
public final class QuizCommand {
    private QuizCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> add = Commands.literal("add").requires(QuizCommand::canManage)
                .executes(context -> addUsage(context.getSource()));
        LiteralArgumentBuilder<CommandSourceStack> list = Commands.literal("list").requires(QuizCommand::canManage)
                .executes(context -> list(context.getSource(), null));
        for (QuizCategory category : QuizCategory.values()) {
            add.then(Commands.literal(category.id())
                    .executes(context -> addUsage(context.getSource()))
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(context -> add(context, category))));
            list.then(Commands.literal(category.id()).executes(context -> list(context.getSource(), category)));
        }
        CommandRegistration.replaceRootLiteral(dispatcher, "quiz", Commands.literal("quiz")
                .then(Commands.literal("join").requires(QuizCommand::isPlayer).executes(QuizCommand::join))
                .then(Commands.literal("decline").requires(QuizCommand::isPlayer).executes(context -> 1))
                .then(add)
                .then(list)
                .then(Commands.literal("remove").requires(QuizCommand::canManage)
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        QuizConfig.questions().stream().map(question -> String.valueOf(question.id())),
                                        builder))
                                .executes(QuizCommand::remove)))
                .then(Commands.literal("start").requires(QuizCommand::canManage).executes(QuizCommand::start))
                .then(Commands.literal("test").requires(source -> isPlayer(source) && canManage(source))
                        .executes(QuizCommand::test)));
    }

    private static boolean isPlayer(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer;
    }

    private static boolean canManage(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.QUIZ_MANAGE);
        }
        return source.hasPermission(2);
    }

    private static int join(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return report(context.getSource(), QuizService.join(context.getSource().getPlayerOrException(), false));
    }

    private static int test(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return report(context.getSource(), QuizService.join(context.getSource().getPlayerOrException(), true));
    }

    private static int report(CommandSourceStack source, QuizService.JoinResult result) {
        String key = switch (result) {
            case STARTED -> null;
            case NO_MOD -> CointCoreMessages.QUIZ_NO_MOD;
            case NO_INVITE -> CointCoreMessages.QUIZ_NO_INVITE;
            case ALREADY_PLAYED -> CointCoreMessages.QUIZ_ALREADY_PLAYED;
            case IN_PROGRESS -> CointCoreMessages.QUIZ_IN_PROGRESS;
            case NOT_ENOUGH_QUESTIONS -> CointCoreMessages.QUIZ_NOT_ENOUGH;
        };
        if (key == null) {
            return 1;
        }
        source.sendFailure(CointCoreMessages.forSource(source, key, QuizConfig.get().questionsPerRound()));
        return 0;
    }

    private static int start(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (QuizConfig.questions().size() < QuizConfig.get().questionsPerRound()) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.QUIZ_NOT_ENOUGH,
                    QuizConfig.get().questionsPerRound()));
            return 0;
        }
        int sent = QuizService.invite(source.getServer());
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.QUIZ_STARTED, sent), true);
        return sent;
    }

    private static int addUsage(CommandSourceStack source) {
        source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.QUIZ_ADD_USAGE));
        return 0;
    }

    private static int add(CommandContext<CommandSourceStack> context, QuizCategory category) {
        CommandSourceStack source = context.getSource();
        List<String> parts = Arrays.stream(StringArgumentType.getString(context, "text").split("\\|"))
                .map(String::trim)
                .toList();
        if (parts.size() != 2 + QuizQuestion.WRONG_ANSWERS || parts.stream().anyMatch(String::isEmpty)) {
            return addUsage(source);
        }
        Optional<QuizQuestion> added = QuizConfig.add(category, parts.get(0), parts.get(1), parts.subList(2, parts.size()));
        if (added.isEmpty()) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.QUIZ_SAVE_FAILED));
            return 0;
        }
        QuizQuestion question = added.get();
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.QUIZ_ADDED, question.id(),
                category.id(), QuizConfig.questions().size()), true);
        return question.id();
    }

    private static int list(CommandSourceStack source, QuizCategory filter) {
        List<QuizQuestion> questions = QuizConfig.questions().stream()
                .filter(question -> filter == null || question.category() == filter)
                .toList();
        if (questions.isEmpty()) {
            source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.QUIZ_LIST_EMPTY), false);
            return 0;
        }
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.QUIZ_LIST_HEADER, questions.size()), false);
        for (QuizQuestion question : questions) {
            MutableComponent line = Component.empty()
                    .append(CointCoreMessages.forSource(source, CointCoreMessages.QUIZ_LIST_ENTRY, question.id(),
                            question.category().id()))
                    .append(Component.literal(question.text() + " "))
                    .append(Component.literal("✔ " + question.correct()).withStyle(style -> style.withColor(0x55FF55)))
                    .append(Component.literal(" ✘ " + String.join(" / ", question.wrong())).withStyle(style -> style.withColor(0xAAAAAA)))
                    .append(Component.literal(" "))
                    .append(CointCoreMessages.forSource(source, CointCoreMessages.QUIZ_LIST_REMOVE).copy()
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/quiz remove " + question.id()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            CointCoreMessages.forSource(source, CointCoreMessages.QUIZ_LIST_REMOVE_HOVER, question.id())))));
            source.sendSuccess(() -> line, false);
        }
        return questions.size();
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int id = IntegerArgumentType.getInteger(context, "id");
        if (!QuizConfig.remove(id)) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.QUIZ_NOT_FOUND, id));
            return 0;
        }
        source.sendSuccess(() -> CointCoreMessages.forSource(source, CointCoreMessages.QUIZ_REMOVED, id), true);
        return 1;
    }
}
