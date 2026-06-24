package com.mawlee.cointcore.server;

import com.mawlee.cointcore.config.ServerAutomationConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.lang.LegacyTextParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public final class ServerRestartService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile Timer activeTimer;
    private static volatile boolean restartShutdownActive;
    private static volatile boolean restartSaveStarted;

    private ServerRestartService() {
    }

    public static void scheduleRestart(MinecraftServer server, int delaySeconds, String messageKey, Object... args) {
        cancelPendingRestart();

        ServerAutomationConfig.RestartCountdownSettings countdown = ServerAutomationConfig.getRestartCountdown();
        int effectiveDelay = Math.max(1, delaySeconds);
        int kickSecondsBefore = countdown.kickPlayersSecondsBefore();

        beginRestartShutdown();

        Timer timer = new Timer("CointCore-RestartCountdown", true);
        activeTimer = timer;

        for (int secondsRemaining : countdown.warningSecondsBefore()) {
            if (secondsRemaining <= 0 || secondsRemaining > effectiveDelay) {
                continue;
            }

            long scheduleAt = (effectiveDelay - secondsRemaining) * 1000L;
            timer.schedule(createWarningTask(server, countdown, secondsRemaining), scheduleAt);
        }

        long kickAt = Math.max(0L, (effectiveDelay - kickSecondsBefore) * 1000L);
        timer.schedule(createKickTask(server), kickAt);
        timer.schedule(createHaltTask(server), effectiveDelay * 1000L);

        broadcastFinalMessage(server, messageKey, args);

        if (countdown.showTitleOnFirstWarning() && !countdown.titleText().isBlank()) {
            runTitleCommand(server, countdown.titleText());
        }
    }

    public static void executeImmediateRestart(MinecraftServer server) {
        cancelPendingRestart();
        beginRestartShutdown();
        kickAllPlayers(server);
        trySaveWhenReady(server);
        server.execute(() -> completeRestartAfterKick(server));
    }

    public static void tick(MinecraftServer server) {
        if (restartShutdownActive) {
            trySaveWhenReady(server);
        }
    }

    public static boolean cancelPendingRestart() {
        Timer timer = activeTimer;
        if (timer != null) {
            timer.cancel();
            activeTimer = null;
            clearRestartShutdown();
            return true;
        }

        if (restartShutdownActive) {
            clearRestartShutdown();
            return true;
        }

        return false;
    }

    private static TimerTask createWarningTask(
            MinecraftServer server,
            ServerAutomationConfig.RestartCountdownSettings countdown,
            int secondsRemaining
    ) {
        return new TimerTask() {
            @Override
            public void run() {
                server.execute(() -> broadcastCountdownWarning(server, countdown.warningText(), secondsRemaining));
            }
        };
    }

    private static TimerTask createKickTask(MinecraftServer server) {
        return new TimerTask() {
            @Override
            public void run() {
                server.execute(() -> {
                    kickAllPlayers(server);
                    trySaveWhenReady(server);
                });
            }
        };
    }

    private static TimerTask createHaltTask(MinecraftServer server) {
        return new TimerTask() {
            @Override
            public void run() {
                server.execute(() -> finalizeRestart(server));
            }
        };
    }

    private static void finalizeRestart(MinecraftServer server) {
        if (!server.getPlayerList().getPlayers().isEmpty()) {
            kickAllPlayers(server);
        }

        completeRestartAfterKick(server);
    }

    private static void completeRestartAfterKick(MinecraftServer server) {
        if (!server.getPlayerList().getPlayers().isEmpty()) {
            server.execute(() -> completeRestartAfterKick(server));
            return;
        }

        trySaveWhenReady(server);

        activeTimer = null;
        clearRestartShutdown();
        server.halt(false);
    }

    private static void kickAllPlayers(MinecraftServer server) {
        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        for (ServerPlayer player : players) {
            player.connection.disconnect(CointCoreMessages.forPlayer(player, CointCoreMessages.SERVER_RESTART_KICK));
        }
    }

    private static void trySaveWhenReady(MinecraftServer server) {
        if (!restartShutdownActive || restartSaveStarted) {
            return;
        }

        if (!server.getPlayerList().getPlayers().isEmpty()) {
            return;
        }

        restartSaveStarted = true;
        runSaveCommand(server);
    }

    private static void beginRestartShutdown() {
        restartShutdownActive = true;
        restartSaveStarted = false;
    }

    private static void clearRestartShutdown() {
        restartShutdownActive = false;
        restartSaveStarted = false;
    }

    private static void broadcastFinalMessage(MinecraftServer server, String messageKey, Object... args) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, messageKey, args));
        }
    }

    private static void broadcastCountdownWarning(MinecraftServer server, String template, int secondsRemaining) {
        Component message = buildCountdownMessage(template, secondsRemaining);
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    static Component buildCountdownMessage(String template, int secondsRemaining) {
        String text = template == null ? "" : template.replace("%", String.valueOf(secondsRemaining));
        Component inner = LegacyTextParser.parse(text);
        return Component.literal("[===").append(inner).append(Component.literal("===]"));
    }

    private static void runTitleCommand(MinecraftServer server, String titleText) {
        String escaped = titleText.replace("\\", "\\\\").replace("\"", "\\\"");
        String command = "title @a title {\"text\":\"" + escaped + "\"}";
        try {
            server.getCommands().getDispatcher().execute(command, server.createCommandSourceStack().withSuppressedOutput());
        } catch (CommandSyntaxException exception) {
            LOGGER.warn("Failed to run restart title command", exception);
        }
    }

    private static void runSaveCommand(MinecraftServer server) {
        try {
            server.getCommands().getDispatcher().execute("save-all", server.createCommandSourceStack().withSuppressedOutput());
            LOGGER.info("Executed save-all after players left the server before restart");
        } catch (CommandSyntaxException exception) {
            LOGGER.warn("Failed to run save-all before restart", exception);
        }
    }

}
