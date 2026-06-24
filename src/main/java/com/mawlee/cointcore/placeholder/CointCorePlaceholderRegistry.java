package com.mawlee.cointcore.placeholder;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.spark.SparkMetricsService;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Registers {@code %cointcore:...%} placeholders via Text Placeholder API when that mod is installed.
 */
public final class CointCorePlaceholderRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PLACEHOLDER_API_MOD_ID = "placeholder-api";

    private CointCorePlaceholderRegistry() {
    }

    public static void registerIfAvailable() {
        if (!ModList.get().isLoaded(PLACEHOLDER_API_MOD_ID)) {
            LOGGER.info(
                    "Text Placeholder API ({}) is not loaded; install it to use %{}:tps% placeholders in tab configs",
                    PLACEHOLDER_API_MOD_ID,
                    CointCore.MOD_ID
            );
            return;
        }

        try {
            registerPlaceholder("tps", CointCorePlaceholderRegistry::resolveTps);
            registerPlaceholder("tps_colored", CointCorePlaceholderRegistry::resolveColoredTps);
            registerPlaceholder("mspt", CointCorePlaceholderRegistry::resolveMsptMean);
            registerPlaceholder("mspt_max", CointCorePlaceholderRegistry::resolveMsptMax);
            registerPlaceholder("mspt_p95", CointCorePlaceholderRegistry::resolveMsptP95);
            registerPlaceholder("spark", CointCorePlaceholderRegistry::resolveSparkAlias);
            LOGGER.info("Registered CointCore Spark TPS placeholders (%{}:tps%, %{}:mspt%, ...)", CointCore.MOD_ID, CointCore.MOD_ID);
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("Failed to register CointCore placeholders", exception);
        }
    }

    private static Object resolveTps(String argument) {
        return resolveNumeric(argument, "1m", SparkMetricsService::resolveTps, SparkMetricsService::formatTps);
    }

    private static Object resolveColoredTps(String argument) {
        OptionalDouble value = SparkMetricsService.resolveTps(normalizeWindow(argument, "1m"));
        if (value.isEmpty()) {
            return invalid("Unknown TPS window: " + argument);
        }
        return value(value.getAsDouble(), SparkMetricsService.coloredTps(value.getAsDouble()));
    }

    private static Object resolveMsptMean(String argument) {
        return resolveNumeric(argument, "1m", SparkMetricsService::resolveMsptMean, SparkMetricsService::formatMspt);
    }

    private static Object resolveMsptMax(String argument) {
        return resolveNumeric(argument, "1m", SparkMetricsService::resolveMsptMax, SparkMetricsService::formatMspt);
    }

    private static Object resolveMsptP95(String argument) {
        return resolveNumeric(argument, "1m", SparkMetricsService::resolveMsptPercentile95, SparkMetricsService::formatMspt);
    }

    private static Object resolveSparkAlias(String argument) {
        if (argument == null || argument.isBlank()) {
            return resolveTps("1m");
        }

        String normalized = argument.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("tps")) {
            return resolveTps(normalized.length() > 3 ? normalized.substring(3).trim() : "1m");
        }
        if (normalized.startsWith("mspt")) {
            String suffix = normalized.length() > 4 ? normalized.substring(4).trim() : "1m";
            if (suffix.startsWith("_max")) {
                return resolveMsptMax(suffix.substring(4).trim());
            }
            if (suffix.startsWith("_p95")) {
                return resolveMsptP95(suffix.substring(4).trim());
            }
            return resolveMsptMean(suffix);
        }

        return resolveTps(normalized);
    }

    private static Object resolveNumeric(
            String argument,
            String defaultWindow,
            java.util.function.Function<String, OptionalDouble> resolver,
            java.util.function.DoubleFunction<String> formatter
    ) {
        OptionalDouble value = resolver.apply(normalizeWindow(argument, defaultWindow));
        if (value.isEmpty()) {
            return invalid("Unknown window: " + argument);
        }
        return value(value.getAsDouble(), formatter.apply(value.getAsDouble()));
    }

    private static String normalizeWindow(String argument, String fallback) {
        if (argument == null || argument.isBlank()) {
            return fallback;
        }
        return argument.trim().toLowerCase(Locale.ROOT);
    }

    private static void registerPlaceholder(String path, PlaceholderResolver resolver) throws ReflectiveOperationException {
        Class<?> handlerClass = Class.forName("eu.pb4.placeholders.api.PlaceholderHandler");
        Class<?> placeholdersClass = Class.forName("eu.pb4.placeholders.api.Placeholders");

        Object handler = Proxy.newProxyInstance(
                handlerClass.getClassLoader(),
                new Class<?>[]{handlerClass},
                placeholderHandler(resolver)
        );

        Method register = placeholdersClass.getMethod("register", ResourceLocation.class, handlerClass);
        register.invoke(null, ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, path), handler);
    }

    private static InvocationHandler placeholderHandler(PlaceholderResolver resolver) {
        return (proxy, method, args) -> {
            if (!"onPlaceholderRequest".equals(method.getName())) {
                return null;
            }
            String argument = args.length > 1 && args[1] instanceof String text ? text : null;
            return resolver.resolve(argument);
        };
    }

    @FunctionalInterface
    private interface PlaceholderResolver {
        Object resolve(String argument);
    }

    private static Object value(double numeric, String text) {
        try {
            Class<?> resultClass = Class.forName("eu.pb4.placeholders.api.PlaceholderResult");
            try {
                return resultClass.getMethod("value", String.class).invoke(null, text);
            } catch (NoSuchMethodException ignored) {
                return resultClass.getMethod("value", double.class).invoke(null, numeric);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to build placeholder result", exception);
        }
    }

    private static Object invalid(String message) {
        try {
            Class<?> resultClass = Class.forName("eu.pb4.placeholders.api.PlaceholderResult");
            return resultClass.getMethod("invalid", String.class).invoke(null, message);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to build invalid placeholder result", exception);
        }
    }
}
