package com.mawlee.cointcore.server;

import com.mawlee.cointcore.config.ServerAutomationConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.server.MinecraftServer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ScheduledRestartService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<String> SENT_MARKERS = ConcurrentHashMap.newKeySet();

    private ScheduledRestartService() {
    }

    public static void tick(MinecraftServer server) {
        ServerAutomationConfig.ScheduledRestartSettings settings = ServerAutomationConfig.getScheduledRestart();
        if (!settings.enabled()) {
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(settings.zoneId());
        LocalDate today = now.toLocalDate();
        LocalTime currentMinute = now.toLocalTime().withSecond(0).withNano(0);

        // Minute-scoped warnings only need the first seconds of each minute.
        boolean warningWindow = now.getSecond() <= 1;

        for (LocalTime restartTime : settings.times()) {
            LocalTime normalizedHaltTime = restartTime.withSecond(0).withNano(0);
            String restartTimeLabel = normalizedHaltTime.format(TIME_FORMAT);
            ZonedDateTime haltAt = ZonedDateTime.of(today, normalizedHaltTime, settings.zoneId());

            if (warningWindow) {
                for (int minutesBefore : settings.warningsMinutesBefore()) {
                    LocalTime warningTime = normalizedHaltTime.minusMinutes(minutesBefore);
                    if (!currentMinute.equals(warningTime)) {
                        continue;
                    }

                    String marker = today + ":warn:" + restartTimeLabel + ":" + minutesBefore;
                    if (SENT_MARKERS.add(marker)) {
                        broadcastWarning(server, minutesBefore, restartTimeLabel);
                    }
                }
            }

            long secondsUntilHalt = ChronoUnit.SECONDS.between(now, haltAt);
            if (secondsUntilHalt < 0 || secondsUntilHalt > settings.restartDelaySeconds()) {
                continue;
            }

            String marker = today + ":restart:" + restartTimeLabel;
            if (!SENT_MARKERS.add(marker)) {
                continue;
            }

            // Configured time is the halt moment; countdown fills the preceding delay window.
            if (secondsUntilHalt <= 1) {
                ServerRestartService.executeImmediateRestart(server);
            } else {
                ServerRestartService.scheduleRestart(
                        server,
                        (int) secondsUntilHalt,
                        CointCoreMessages.SERVER_RESTART_SCHEDULED,
                        restartTimeLabel,
                        (int) secondsUntilHalt
                );
            }
        }
    }

    public static void resetRuntimeState() {
        SENT_MARKERS.clear();
    }

    private static void broadcastWarning(MinecraftServer server, int minutesBefore, String restartTimeLabel) {
        server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(
                CointCoreMessages.forPlayer(player, CointCoreMessages.SERVER_RESTART_WARNING, restartTimeLabel, minutesBefore)
        ));
    }
}
