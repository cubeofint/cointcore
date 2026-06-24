package com.mawlee.cointcore.placeholder;

import com.mawlee.cointcore.spark.SparkMetricsService;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.OptionalDouble;
import java.util.function.Supplier;

/**
 * Registers Spark TPS/MSPT placeholders into NEZNAMY TAB ({@code %cointcore_tps_1m%}, etc.).
 */
public final class TabSparkPlaceholderRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAB_MOD_ID = "tab";
    private static final int REFRESH_MS = 1000;

    private TabSparkPlaceholderRegistry() {
    }

    public static void hookIfAvailable() {
        if (!ModList.get().isLoaded(TAB_MOD_ID)) {
            LOGGER.info("TAB is not loaded; CointCore tab TPS placeholders were not registered");
            return;
        }

        try {
            subscribeTabLoadEvent();
            registerPlaceholders();
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("Failed to register CointCore placeholders with TAB", exception);
        }
    }

    private static void subscribeTabLoadEvent() throws ReflectiveOperationException {
        Class<?> tabApiClass = Class.forName("me.neznamy.tab.api.TabAPI");
        Object tabApi = tabApiClass.getMethod("getInstance").invoke(null);
        Object eventBus = tabApiClass.getMethod("getEventBus").invoke(tabApi);

        Class<?> tabLoadEventClass = Class.forName("me.neznamy.tab.api.event.plugin.TabLoadEvent");
        Class<?> eventHandlerClass = Class.forName("me.neznamy.tab.api.event.EventHandler");

        Object handler = Proxy.newProxyInstance(
                eventHandlerClass.getClassLoader(),
                new Class<?>[]{eventHandlerClass},
                (proxy, method, args) -> {
                    if ("handle".equals(method.getName())) {
                        try {
                            registerPlaceholders();
                        } catch (ReflectiveOperationException exception) {
                            LOGGER.warn("Failed to re-register CointCore TAB placeholders after TAB reload", exception);
                        }
                    }
                    return null;
                }
        );

        Method register = eventBus.getClass().getMethod("register", Class.class, eventHandlerClass);
        register.invoke(eventBus, tabLoadEventClass, handler);
    }

    private static void registerPlaceholders() throws ReflectiveOperationException {
        Class<?> tabApiClass = Class.forName("me.neznamy.tab.api.TabAPI");
        Object tabApi = tabApiClass.getMethod("getInstance").invoke(null);
        Object placeholderManager = tabApiClass.getMethod("getPlaceholderManager").invoke(tabApi);

        Method register = placeholderManager.getClass().getMethod(
                "registerServerPlaceholder",
                String.class,
                int.class,
                Supplier.class
        );
        Method unregister = findUnregisterMethod(placeholderManager.getClass());

        registerServer(unregister, register, placeholderManager, "%cointcore_tps%", () -> formatTps("1m"));
        registerServer(unregister, register, placeholderManager, "%cointcore_tps_5s%", () -> formatTps("5s"));
        registerServer(unregister, register, placeholderManager, "%cointcore_tps_10s%", () -> formatTps("10s"));
        registerServer(unregister, register, placeholderManager, "%cointcore_tps_1m%", () -> formatTps("1m"));
        registerServer(unregister, register, placeholderManager, "%cointcore_tps_5m%", () -> formatTps("5m"));
        registerServer(unregister, register, placeholderManager, "%cointcore_tps_15m%", () -> formatTps("15m"));
        registerServer(unregister, register, placeholderManager, "%cointcore_tps_colored%", () -> coloredTps("1m"));
        registerServer(unregister, register, placeholderManager, "%cointcore_tps_colored_1m%", () -> coloredTps("1m"));
        registerServer(unregister, register, placeholderManager, "%cointcore_mspt%", () -> formatMspt("1m"));
        registerServer(unregister, register, placeholderManager, "%cointcore_mspt_1m%", () -> formatMspt("1m"));
        registerServer(unregister, register, placeholderManager, "%cointcore_mspt_5m%", () -> formatMspt("5m"));
        registerServer(unregister, register, placeholderManager, "%cointcore_mspt_max_1m%", () -> formatMsptMax("1m"));
        registerServer(unregister, register, placeholderManager, "%cointcore_mspt_max_5m%", () -> formatMsptMax("5m"));
        registerServer(unregister, register, placeholderManager, "%cointcore_mspt_p95_1m%", () -> formatMsptP95("1m"));
        registerServer(unregister, register, placeholderManager, "%cointcore_mspt_p95_5m%", () -> formatMsptP95("5m"));

        LOGGER.info("Registered CointCore Spark TPS placeholders with TAB");
    }

    private static Method findUnregisterMethod(Class<?> managerClass) {
        try {
            return managerClass.getMethod("unregisterPlaceholder", String.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static void registerServer(
            Method unregister,
            Method register,
            Object placeholderManager,
            String identifier,
            Supplier<String> supplier
    ) throws ReflectiveOperationException {
        if (unregister != null) {
            try {
                unregister.invoke(placeholderManager, identifier);
            } catch (ReflectiveOperationException ignored) {
                // Placeholder was not registered yet.
            }
        }
        register.invoke(placeholderManager, identifier, REFRESH_MS, supplier);
    }

    private static String formatTps(String window) {
        OptionalDouble value = SparkMetricsService.resolveTps(window);
        if (value.isEmpty()) {
            return "N/A";
        }
        return SparkMetricsService.formatTps(value.getAsDouble());
    }

    private static String coloredTps(String window) {
        OptionalDouble value = SparkMetricsService.resolveTps(window);
        if (value.isEmpty()) {
            return "N/A";
        }
        return SparkMetricsService.coloredTps(value.getAsDouble());
    }

    private static String formatMspt(String window) {
        OptionalDouble value = SparkMetricsService.resolveMsptMean(window);
        if (value.isEmpty()) {
            return "N/A";
        }
        return SparkMetricsService.formatMspt(value.getAsDouble());
    }

    private static String formatMsptMax(String window) {
        OptionalDouble value = SparkMetricsService.resolveMsptMax(window);
        if (value.isEmpty()) {
            return "N/A";
        }
        return SparkMetricsService.formatMspt(value.getAsDouble());
    }

    private static String formatMsptP95(String window) {
        OptionalDouble value = SparkMetricsService.resolveMsptPercentile95(window);
        if (value.isEmpty()) {
            return "N/A";
        }
        return SparkMetricsService.formatMspt(value.getAsDouble());
    }
}
