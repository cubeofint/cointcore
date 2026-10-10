package com.mawlee.cointcore.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JEI-like search syntax: plain words (name + tooltip), {@code @mod}, {@code #tooltip},
 * {@code $tag}, {@code ^color}, optional {@code %} tab / {@code &} id, leading {@code -}
 * to negate, {@code |} as OR, and quoted phrases.
 */
public final class MarketSearchQuery {
    private static final String OR = "|";

    private MarketSearchQuery() {
    }

    public enum Kind {
        NAME,
        MOD,
        TOOLTIP,
        TAG,
        COLOR,
        CREATIVE_TAB,
        RESOURCE_ID
    }

    public record Term(Kind kind, String text, boolean negated) {
        public Term {
            kind = kind == null ? Kind.NAME : kind;
            text = text == null ? "" : text.toLowerCase(Locale.ROOT);
        }
    }

    public static boolean matches(String query, MarketSearchTarget target) {
        if (target == null) {
            return false;
        }
        List<List<Term>> groups = parse(query);
        if (groups.isEmpty()) {
            return true;
        }
        for (List<Term> group : groups) {
            if (matchesGroup(group, target)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasNegation(String query) {
        for (List<Term> group : parse(query)) {
            for (Term term : group) {
                if (term.negated()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<List<Term>> parse(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<List<Term>> groups = new ArrayList<>();
        List<Term> current = new ArrayList<>();
        for (String token : tokenize(query)) {
            if (OR.equals(token)) {
                if (!current.isEmpty()) {
                    groups.add(List.copyOf(current));
                    current = new ArrayList<>();
                }
                continue;
            }
            Term term = term(token);
            if (term != null) {
                current.add(term);
            }
        }
        if (!current.isEmpty()) {
            groups.add(List.copyOf(current));
        }
        return List.copyOf(groups);
    }

    private static boolean matchesGroup(List<Term> group, MarketSearchTarget target) {
        for (Term term : group) {
            boolean hit = fieldMatches(term.kind(), term.text(), target);
            if (term.negated() == hit) {
                return false;
            }
        }
        return true;
    }

    private static boolean fieldMatches(Kind kind, String needle, MarketSearchTarget target) {
        if (needle.isEmpty()) {
            return true;
        }
        return switch (kind) {
            case NAME -> contains(target.names(), needle) || contains(target.tooltip(), needle);
            case MOD -> containsLoose(target.modId(), needle) || containsLoose(target.modName(), needle);
            case TOOLTIP -> contains(target.tooltip(), needle);
            case TAG -> matchesAny(target.tags(), needle);
            case COLOR -> contains(target.colors(), needle);
            case CREATIVE_TAB -> containsLoose(target.creativeTabs(), needle);
            case RESOURCE_ID -> contains(target.itemId(), needle);
            default -> {
                Kind unused = kind;
                throw new IllegalStateException(unused.name());
            }
        };
    }

    private static boolean matchesAny(List<String> values, String needle) {
        for (String value : values) {
            if (contains(value, needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.contains(needle);
    }

    private static boolean containsLoose(String haystack, String needle) {
        if (contains(haystack, needle)) {
            return true;
        }
        if (haystack == null || needle == null) {
            return false;
        }
        return haystack.replace(" ", "").contains(needle.replace(" ", ""));
    }

    private static Term term(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        boolean negated = false;
        String raw = token;
        if (raw.charAt(0) == '-' && raw.length() > 1) {
            negated = true;
            raw = raw.substring(1);
        }
        Kind kind = Kind.NAME;
        if (!raw.isEmpty() && isPrefix(raw.charAt(0))) {
            kind = kindOf(raw.charAt(0));
            raw = raw.substring(1);
        }
        if (raw.isEmpty()) {
            return null;
        }
        return new Term(kind, raw.toLowerCase(Locale.ROOT), negated);
    }

    private static boolean isPrefix(char prefix) {
        return prefix == '@' || prefix == '#' || prefix == '$' || prefix == '^' || prefix == '%' || prefix == '&';
    }

    private static Kind kindOf(char prefix) {
        return switch (prefix) {
            case '@' -> Kind.MOD;
            case '#' -> Kind.TOOLTIP;
            case '$' -> Kind.TAG;
            case '^' -> Kind.COLOR;
            case '%' -> Kind.CREATIVE_TAB;
            case '&' -> Kind.RESOURCE_ID;
            default -> Kind.NAME;
        };
    }

    private static List<String> tokenize(String query) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < query.length(); index++) {
            char ch = query.charAt(index);
            if (quoted) {
                if (ch == '"') {
                    quoted = false;
                    flush(current, tokens);
                } else {
                    current.append(ch);
                }
                continue;
            }
            if (ch == '"') {
                flush(current, tokens);
                quoted = true;
                continue;
            }
            if (ch == '|') {
                flush(current, tokens);
                tokens.add(OR);
                continue;
            }
            if (Character.isWhitespace(ch)) {
                flush(current, tokens);
                continue;
            }
            current.append(ch);
        }
        flush(current, tokens);
        return tokens;
    }

    private static void flush(StringBuilder current, List<String> tokens) {
        if (current.isEmpty()) {
            return;
        }
        tokens.add(current.toString());
        current.setLength(0);
    }
}
