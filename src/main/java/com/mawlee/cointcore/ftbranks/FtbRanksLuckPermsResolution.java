package com.mawlee.cointcore.ftbranks;

import java.util.Optional;

/**
 * Isolated FTB Ranks → LuckPerms fallback order. No Minecraft types, so it can be unit-tested.
 *
 * <p>Order: explicit FTB Ranks value, then LuckPerms, then the original FTB Ranks fallback
 * (vanilla OP for {@code command.*}, mod config defaults for meta).
 */
public final class FtbRanksLuckPermsResolution {
    private FtbRanksLuckPermsResolution() {
    }

    public enum Source {
        FTB_RANKS,
        LUCKPERMS,
        FALLBACK
    }

    public sealed interface ParsedValue {
        record Bool(boolean value) implements ParsedValue {
        }

        record Num(Number value) implements ParsedValue {
        }

        record Str(String value) implements ParsedValue {
        }
    }

    public record Outcome(Source source, Optional<ParsedValue> luckPermsValue) {
        public static Outcome ftbRanks() {
            return new Outcome(Source.FTB_RANKS, Optional.empty());
        }

        public static Outcome luckPerms(ParsedValue value) {
            return new Outcome(Source.LUCKPERMS, Optional.of(value));
        }

        public static Outcome fallback() {
            return new Outcome(Source.FALLBACK, Optional.empty());
        }
    }

    public interface LuckPermsQuery {
        Optional<Boolean> permissionTristate(String node);

        Optional<String> meta(String node);
    }

    public static Outcome resolve(
            boolean bridgeEnabled,
            boolean luckPermsAvailable,
            boolean ftbRanksHasExplicitValue,
            String node,
            LuckPermsQuery query
    ) {
        if (!bridgeEnabled) {
            return ftbRanksHasExplicitValue ? Outcome.ftbRanks() : Outcome.fallback();
        }
        if (ftbRanksHasExplicitValue) {
            return Outcome.ftbRanks();
        }
        if (!luckPermsAvailable || query == null || node == null || node.isBlank()) {
            return Outcome.fallback();
        }

        Optional<ParsedValue> fromLuckPerms = lookupLuckPerms(node, query);
        return fromLuckPerms.map(Outcome::luckPerms).orElseGet(Outcome::fallback);
    }

    public static Optional<ParsedValue> lookupLuckPerms(String node, LuckPermsQuery query) {
        if (isCommandNode(node)) {
            Optional<ParsedValue> permission = query.permissionTristate(node).map(ParsedValue.Bool::new);
            if (permission.isPresent()) {
                return permission;
            }
            return parseMeta(query.meta(node).orElse(null));
        }

        Optional<ParsedValue> meta = parseMeta(query.meta(node).orElse(null));
        if (meta.isPresent()) {
            return meta;
        }
        return query.permissionTristate(node).map(ParsedValue.Bool::new);
    }

    public static Optional<ParsedValue> parseMeta(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        String value = raw.trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return Optional.of(new ParsedValue.Str(value.substring(1, value.length() - 1)));
        }
        if ("true".equalsIgnoreCase(value)) {
            return Optional.of(new ParsedValue.Bool(true));
        }
        if ("false".equalsIgnoreCase(value)) {
            return Optional.of(new ParsedValue.Bool(false));
        }

        try {
            if (looksLikeFloatingPoint(value)) {
                return Optional.of(new ParsedValue.Num(Double.valueOf(value)));
            }
            long parsed = Long.parseLong(value);
            if (parsed >= Integer.MIN_VALUE && parsed <= Integer.MAX_VALUE) {
                return Optional.of(new ParsedValue.Num((int) parsed));
            }
            return Optional.of(new ParsedValue.Num(parsed));
        } catch (NumberFormatException ignored) {
            return Optional.of(new ParsedValue.Str(value));
        }
    }

    public static boolean isCommandNode(String node) {
        return node != null && node.startsWith("command.");
    }

    private static boolean looksLikeFloatingPoint(String value) {
        return value.indexOf('.') >= 0 || value.indexOf('e') >= 0 || value.indexOf('E') >= 0;
    }
}
