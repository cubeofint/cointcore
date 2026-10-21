package com.mawlee.cointcore.quiz;

import com.mawlee.cointcore.config.QuizConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.shop.CurrencyMovement;
import com.mawlee.cointcore.shop.CurrencyMovementService;
import com.mawlee.cointcore.shop.CurrencyMovementType;
import com.mawlee.cointcore.shop.GluonWallet;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Quiz rounds: every {@code interval_minutes} players with cointcore on the client get a clickable invite; [Да]
 * runs {@code /quiz join} and opens the quiz window. Each player plays alone; timing and the right answers stay
 * on the server.
 */
public final class QuizService {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** How long the window shows the right answer before the next question. */
    private static final long REVEAL_MS = 2_000L;
    private static final Random RANDOM = new Random();

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Set<UUID> PLAYED_INVITE = new HashSet<>();
    private static long nextInviteAtMs;
    private static long inviteExpiresAtMs;

    private QuizService() {
    }

    public enum JoinResult {
        STARTED,
        NO_MOD,
        NO_INVITE,
        ALREADY_PLAYED,
        IN_PROGRESS,
        NOT_ENOUGH_QUESTIONS
    }

    private static final class Session {
        final QuizRound round;
        long revealUntilMs;

        Session(QuizRound round) {
            this.round = round;
        }
    }

    /** Called once a second. */
    public static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        QuizConfig.Settings settings = QuizConfig.get();
        if (nextInviteAtMs == 0L) {
            nextInviteAtMs = now + settings.intervalMinutes() * 60_000L;
        }
        if (settings.enabled() && now >= nextInviteAtMs) {
            nextInviteAtMs = now + settings.intervalMinutes() * 60_000L;
            if (server.getPlayerList().getPlayerCount() >= settings.minOnlinePlayers()) {
                invite(server);
            }
        }

        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Session session = entry.getValue();
            if (player == null) {
                it.remove();
                continue;
            }
            if (session.revealUntilMs > 0L) {
                if (now >= session.revealUntilMs) {
                    session.revealUntilMs = 0L;
                    if (!sendNext(player, session, now)) {
                        it.remove();
                        finish(player, session.round);
                    }
                }
                continue;
            }
            if (session.round.checkTimeout(now) == QuizRound.Verdict.TIMEOUT) {
                reveal(player, session, QuizPayloads.Answer.QUIT, false, now);
            }
        }
    }

    /** Sends the invite now. Returns how many players got it (only clients with cointcore can play). */
    public static int invite(MinecraftServer server) {
        QuizConfig.Settings settings = QuizConfig.get();
        if (QuizConfig.questions().size() < settings.questionsPerRound()) {
            LOGGER.warn("Quiz invite skipped: {} questions, need at least {}", QuizConfig.questions().size(),
                    settings.questionsPerRound());
            return 0;
        }
        PLAYED_INVITE.clear();
        inviteExpiresAtMs = System.currentTimeMillis() + settings.inviteValidMinutes() * 60_000L;
        int sent = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (hasMod(player)) {
                player.sendSystemMessage(inviteMessage(player));
                sent++;
            }
        }
        LOGGER.info("Quiz invite sent to {} players", sent);
        return sent;
    }

    /** {@code /quiz join}; {@code ignoreInvite} for staff testing with {@code /quiz test}. */
    public static JoinResult join(ServerPlayer player, boolean ignoreInvite) {
        if (!hasMod(player)) {
            return JoinResult.NO_MOD;
        }
        if (SESSIONS.containsKey(player.getUUID())) {
            return JoinResult.IN_PROGRESS;
        }
        long now = System.currentTimeMillis();
        if (!ignoreInvite) {
            if (now > inviteExpiresAtMs) {
                return JoinResult.NO_INVITE;
            }
            if (PLAYED_INVITE.contains(player.getUUID())) {
                return JoinResult.ALREADY_PLAYED;
            }
        }
        QuizConfig.Settings settings = QuizConfig.get();
        Optional<QuizRound> round = QuizRound.create(QuizConfig.questions(), settings.questionsPerRound(),
                settings.secondsPerQuestion(), RANDOM);
        if (round.isEmpty()) {
            return JoinResult.NOT_ENOUGH_QUESTIONS;
        }
        if (!ignoreInvite) {
            PLAYED_INVITE.add(player.getUUID());
        }
        Session session = new Session(round.get());
        SESSIONS.put(player.getUUID(), session);
        sendNext(player, session, now);
        return JoinResult.STARTED;
    }

    public static void onAnswer(ServerPlayer player, int number, int choice) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (choice == QuizPayloads.Answer.QUIT) {
            SESSIONS.remove(player.getUUID());
            finish(player, session.round);
            return;
        }
        if (session.revealUntilMs > 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        QuizRound.Verdict verdict = session.round.answer(number, choice, now);
        if (verdict == QuizRound.Verdict.IGNORED) {
            return;
        }
        reveal(player, session, verdict == QuizRound.Verdict.TIMEOUT ? QuizPayloads.Answer.QUIT : choice,
                verdict == QuizRound.Verdict.CORRECT, now);
    }

    public static void onLogout(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    public static void resetRuntimeState() {
        SESSIONS.clear();
        PLAYED_INVITE.clear();
        nextInviteAtMs = 0L;
        inviteExpiresAtMs = 0L;
    }

    private static boolean sendNext(ServerPlayer player, Session session, long now) {
        Optional<QuizRound.Shown> next = session.round.next(now);
        if (next.isEmpty()) {
            return false;
        }
        QuizRound.Shown shown = next.get();
        PacketDistributor.sendToPlayer(player, new QuizPayloads.Question(shown.number(), shown.total(),
                shown.category().ordinal(), shown.question(), shown.answers(), QuizConfig.get().secondsPerQuestion()));
        return true;
    }

    private static void reveal(ServerPlayer player, Session session, int chosen, boolean correct, long now) {
        QuizRound.Shown shown = session.round.current();
        PacketDistributor.sendToPlayer(player, new QuizPayloads.Feedback(shown.number(), chosen, shown.correctIndex(), correct));
        session.revealUntilMs = now + REVEAL_MS;
    }

    private static void finish(ServerPlayer player, QuizRound round) {
        QuizConfig.Settings settings = QuizConfig.get();
        int correct = round.correctCount();
        long reward = correct * settings.rewardPerCorrect()
                + (correct == round.total() ? settings.rewardAllCorrectBonus() : 0L);
        if (reward > 0L) {
            long balance = GluonWallet.add(player.server, player.getUUID(), reward);
            CurrencyMovementService.record(
                    player.server,
                    null,
                    "cointcore",
                    player.getUUID(),
                    player.getGameProfile().getName(),
                    reward,
                    CurrencyMovementType.ADMIN_ADD,
                    CointCoreMessages.translateKey("ru_ru", CointCoreMessages.QUIZ_REWARD_NOTE),
                    List.of(new CurrencyMovement.Delta(player.getUUID(), reward, balance)),
                    null
            );
        }
        LOGGER.info("Quiz: {} answered {}/{} (reward {})", player.getGameProfile().getName(), correct, round.total(), reward);
        PacketDistributor.sendToPlayer(player, new QuizPayloads.Finish(correct, round.total(), reward));
    }

    private static boolean hasMod(ServerPlayer player) {
        return player.connection != null && player.connection.hasChannel(QuizPayloads.Question.TYPE);
    }

    private static Component inviteMessage(ServerPlayer player) {
        MutableComponent yes = CointCoreMessages.forPlayer(player, CointCoreMessages.QUIZ_INVITE_YES).copy()
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/quiz join"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                CointCoreMessages.forPlayer(player, CointCoreMessages.QUIZ_INVITE_YES_HOVER))));
        MutableComponent no = CointCoreMessages.forPlayer(player, CointCoreMessages.QUIZ_INVITE_NO).copy()
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/quiz decline")));
        return Component.empty()
                .append(CointCoreMessages.forPlayer(player, CointCoreMessages.QUIZ_INVITE))
                .append(yes)
                .append(Component.literal(" "))
                .append(no);
    }
}
