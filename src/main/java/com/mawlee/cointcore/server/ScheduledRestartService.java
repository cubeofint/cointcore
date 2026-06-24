package com.mawlee.cointcore.server;

import com.mawlee.cointcore.config.ServerAutomationConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.server.MinecraftServer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
        if (now.getSecond() > 1) {
            return;
        }

        LocalDate today = now.toLocalDate();
        LocalTime current = now.toLocalTime().withSecond(0).withNano(0);

        for (LocalTime restartTime : settings.times()) {
            LocalTime normalizedRestartTime = restartTime.withSecond(0).withNano(0);
            String restartTimeLabel = normalizedRestartTime.format(TIME_FORMAT);

            for (int minutesBefore : settings.warningsMinutesBefore()) {
                LocalTime warningTime = normalizedRestartTime.minusMinutes(minutesBefore);
                if (!current.equals(warningTime)) {
                    continue;
                }

                String marker = today + ":warn:" + restartTimeLabel + ":" + minutesBefore;
                if (SENT_MARKERS.add(marker)) {
                    broadcastWarning(server, minutesBefore, restartTimeLabel);
                }
            }

            if (current.equals(normalizedRestartTime)) {
                String marker = today + ":restart:" + restartTimeLabel;
                if (SENT_MARKERS.add(marker)) {
                    ServerRestartService.scheduleRestart(
                            server,
                            settings.restartDelaySeconds(),
                            CointCoreMessages.SERVER_RESTART_SCHEDULED,
                            restartTimeLabel,
                            settings.restartDelaySeconds()
                    );
                }
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
