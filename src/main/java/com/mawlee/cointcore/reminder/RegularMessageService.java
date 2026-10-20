package com.mawlee.cointcore.reminder;

import com.mawlee.cointcore.chat.ChatLinkFormatter;
import com.mawlee.cointcore.config.RegularMessagesConfig;
import com.mawlee.cointcore.lang.LegacyTextParser;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/** Sends {@link RegularMessagesConfig} reminders to everyone online; ticked once a second. */
public final class RegularMessageService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    /** Created on the first tick after start; its start time anchors "+1ч" schedules. */
    private static RegularPlanner planner;

    private RegularMessageService() {
    }

    public static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        if (planner == null) {
            planner = new RegularPlanner(now);
        }
        RegularMessagesConfig.Settings settings = RegularMessagesConfig.get();
        if (!settings.enabled() || server.getPlayerList().getPlayerCount() < settings.minOnlinePlayers()) {
            return;
        }
        List<RegularMessagesConfig.Entry> entries = RegularMessagesConfig.entries();
        List<RegularPlanner.Item> items = entries.stream()
                .map(entry -> new RegularPlanner.Item(entry.id(), entry.schedule()))
                .toList();
        Optional<RegularPlanner.Item> due = planner.poll(items, now, settings.zone(), settings.minGapMinutes() * 60_000L);
        if (due.isEmpty()) {
            return;
        }
        RegularMessagesConfig.find(due.get().id()).ifPresent(entry -> {
            Component message = render(entry.text());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(message);
            }
            LOGGER.info("Sent regular message #{} ({})", entry.id(), entry.schedule().describe());
        });
    }

    public static Component render(String text) {
        return ChatLinkFormatter.linkify(LegacyTextParser.parse(RegularMessageText.toLegacy(text)));
    }

    public static void forget(int id) {
        if (planner != null) {
            planner.forget(id);
        }
    }

    /** Planned time of the next send (without the shift for coinciding messages), or empty if none is left. */
    public static Optional<String> describeNext(RegularSchedule schedule) {
        RegularMessagesConfig.Settings settings = RegularMessagesConfig.get();
        long now = System.currentTimeMillis();
        long start = planner != null ? planner.startMs() : now;
        long next = schedule.nextSlot(now, start, settings.zone());
        if (next == RegularSchedule.NONE) {
            return Optional.empty();
        }
        ZonedDateTime at = Instant.ofEpochMilli(next).atZone(settings.zone());
        boolean today = at.toLocalDate().equals(Instant.ofEpochMilli(now).atZone(settings.zone()).toLocalDate());
        return Optional.of(at.format(today ? TIME : DATE_TIME));
    }

    public static void resetRuntimeState() {
        planner = null;
    }
}
