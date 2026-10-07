package com.mawlee.cointcore.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import com.google.gson.JsonElement;

/**
 * Safety net for dedicated servers whose JVM stays alive after the server stopped
 * (non-daemon threads from other mods). After {@code delaySeconds} it logs the
 * remaining non-daemon threads with stacks and, if enabled, calls Runtime.halt.
 * Config: config/cointcore/shutdown-guard.json.
 */
@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class ShutdownHaltGuard {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile boolean armed;

    private ShutdownHaltGuard() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerStopped(ServerStoppedEvent event) {
        if (armed) {
            return;
        }
        Settings settings = loadSettings();
        if (!settings.enabled) {
            return;
        }
        armed = true;
        Thread guard = new Thread(() -> run(settings), "cointcore-shutdown-guard");
        guard.setDaemon(true);
        guard.start();
    }

    private static void run(Settings settings) {
        interruptKnownThreads(settings);
        try {
            Thread.sleep(Math.max(1, settings.delaySeconds) * 1000L);
        } catch (InterruptedException ignored) {
            return;
        }
        StringBuilder dump = new StringBuilder();
        int count = 0;
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread thread = entry.getKey();
            if (thread.isDaemon() || !thread.isAlive() || thread == Thread.currentThread()) {
                continue;
            }
            count++;
            dump.append('\n').append('"').append(thread.getName()).append("\" id=").append(thread.threadId())
                    .append(' ').append(thread.getState());
            for (StackTraceElement frame : entry.getValue()) {
                dump.append("\n    at ").append(frame);
            }
        }
        LOGGER.warn("JVM still alive {}s after server stopped; {} non-daemon threads:{}", settings.delaySeconds, count, dump);
        if (settings.haltIfStuck) {
            LOGGER.warn("Halting JVM (shutdown-guard haltIfStuck=true)");
            Runtime.getRuntime().halt(0);
        }
    }

    private static void interruptKnownThreads(Settings settings) {
        List<String> regexes = new ArrayList<>(settings.interruptThreadPatterns);
        if (settings.interruptPoolThreads) {
            regexes.add("pool-\\d+-thread-\\d+");
        }
        List<Pattern> patterns = new ArrayList<>();
        for (String regex : regexes) {
            try {
                patterns.add(Pattern.compile(regex));
            } catch (Exception exception) {
                LOGGER.warn("shutdown-guard: bad regex {}", regex);
            }
        }
        List<Thread> targets = new ArrayList<>();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.isDaemon() || !thread.isAlive() || thread == Thread.currentThread()) {
                continue;
            }
            for (Pattern pattern : patterns) {
                if (pattern.matcher(thread.getName()).matches()) {
                    targets.add(thread);
                    break;
                }
            }
        }
        if (targets.isEmpty()) {
            LOGGER.info("shutdown-guard: no known non-daemon threads to interrupt");
            return;
        }
        List<String> names = new ArrayList<>();
        for (Thread thread : targets) {
            names.add(thread.getName());
            try {
                thread.interrupt();
            } catch (Throwable throwable) {
                LOGGER.warn("shutdown-guard: interrupt failed for {}", thread.getName(), throwable);
            }
        }
        LOGGER.info("shutdown-guard: interrupted {} threads: {}", names.size(), names);
        try {
            Thread.sleep(Math.max(0, settings.gracePeriodSeconds) * 1000L);
        } catch (InterruptedException ignored) {
            return;
        }
        List<String> alive = new ArrayList<>();
        for (Thread thread : targets) {
            if (thread.isAlive()) {
                alive.add(thread.getName() + " " + thread.getState());
            }
        }
        LOGGER.info("shutdown-guard: still alive after {}s grace: {}", settings.gracePeriodSeconds, alive);
    }

    private static Settings loadSettings() {
        Settings settings = new Settings();
        Path path = FMLPaths.CONFIGDIR.get().resolve("cointcore").resolve("shutdown-guard.json");
        try {
            if (Files.exists(path)) {
                JsonObject json = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
                if (json != null) {
                    if (json.has("enabled")) settings.enabled = json.get("enabled").getAsBoolean();
                    if (json.has("delaySeconds")) settings.delaySeconds = json.get("delaySeconds").getAsInt();
                    if (json.has("haltIfStuck")) settings.haltIfStuck = json.get("haltIfStuck").getAsBoolean();
                    if (json.has("gracePeriodSeconds")) settings.gracePeriodSeconds = json.get("gracePeriodSeconds").getAsInt();
                    if (json.has("interruptPoolThreads")) settings.interruptPoolThreads = json.get("interruptPoolThreads").getAsBoolean();
                    if (json.has("interruptThreadPatterns") && json.get("interruptThreadPatterns").isJsonArray()) {
                        settings.interruptThreadPatterns = new ArrayList<>();
                        for (JsonElement element : json.getAsJsonArray("interruptThreadPatterns")) {
                            settings.interruptThreadPatterns.add(element.getAsString());
                        }
                    }
                }
            } else {
                Files.createDirectories(path.getParent());
                Files.writeString(path, GSON.toJson(settings), StandardCharsets.UTF_8);
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to read {}, using defaults", path, exception);
        }
        return settings;
    }

    private static final class Settings {
        boolean enabled = true;
        int delaySeconds = 30;
        boolean haltIfStuck = true;
        int gracePeriodSeconds = 5;
        boolean interruptPoolThreads = false;
        List<String> interruptThreadPatterns = new ArrayList<>(List.of(
                "^TAB .*", "luckperms-scheduler.*", "WorldEdit Session Manager", "nioEventLoopGroup-.*"));
    }
}
