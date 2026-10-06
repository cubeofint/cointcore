package com.mawlee.cointcore.watchdog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maps Java class names to mod ids via longest package-prefix match.
 */
public final class ClassToModMapper {
    private final List<PrefixRule> rules;

    public ClassToModMapper(Map<String, String> packagePrefixToModId) {
        List<PrefixRule> built = new ArrayList<>();
        if (packagePrefixToModId != null) {
            for (Map.Entry<String, String> entry : packagePrefixToModId.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue().isBlank()) {
                    continue;
                }
                String prefix = normalizePrefix(entry.getKey());
                if (!prefix.isEmpty()) {
                    built.add(new PrefixRule(prefix, entry.getValue().trim().toLowerCase(Locale.ROOT)));
                }
            }
        }
        built.sort(Comparator.comparingInt((PrefixRule rule) -> rule.prefix().length()).reversed()
                .thenComparing(PrefixRule::prefix));
        this.rules = List.copyOf(built);
    }

    public static ClassToModMapper empty() {
        return new ClassToModMapper(Map.of());
    }

    public static ClassToModMapper fromPackageMap(Map<String, String> packagePrefixToModId) {
        return new ClassToModMapper(packagePrefixToModId);
    }

    public String resolve(String className) {
        if (className == null || className.isBlank()) {
            return "unknown";
        }
        String normalized = className.replace('/', '.').trim();
        if (normalized.startsWith("net.minecraft.") || normalized.startsWith("com.mojang.")) {
            return "minecraft";
        }
        if (normalized.startsWith("net.neoforged.") || normalized.startsWith("net.minecraftforge.")) {
            return "neoforge";
        }
        for (PrefixRule rule : rules) {
            if (normalized.equals(rule.prefix()) || normalized.startsWith(rule.prefix() + ".")) {
                return rule.modId();
            }
        }
        return guessFromPackage(normalized);
    }

    public Map<String, String> snapshotRules() {
        Map<String, String> map = new HashMap<>();
        for (PrefixRule rule : rules) {
            map.put(rule.prefix(), rule.modId());
        }
        return Map.copyOf(map);
    }

    private static String guessFromPackage(String className) {
        String[] parts = className.split("\\.");
        if (parts.length >= 2) {
            // common patterns: com.<mod>, <mod>.*, dev.<mod>, org.<mod>
            if (("com".equals(parts[0]) || "dev".equals(parts[0]) || "org".equals(parts[0]) || "io".equals(parts[0]))
                    && parts.length >= 2) {
                return parts[1].toLowerCase(Locale.ROOT);
            }
            return parts[0].toLowerCase(Locale.ROOT);
        }
        return "unknown";
    }

    private static String normalizePrefix(String prefix) {
        String value = prefix.replace('/', '.').trim();
        while (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private record PrefixRule(String prefix, String modId) {
    }
}
