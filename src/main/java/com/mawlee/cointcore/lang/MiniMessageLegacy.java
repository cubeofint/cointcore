package com.mawlee.cointcore.lang;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts the MiniMessage subset used in LuckPerms prefixes ({@code <#00AA00>[Mod]<reset>}, {@code <white>},
 * {@code <bold>}, {@code <gradient:#a:#b>}) to legacy {@code §} codes for {@link LegacyTextParser}. No Minecraft
 * types, unit-tested. Closing tags reset formatting (exact nesting is not needed for prefixes); a gradient keeps
 * its first colour; unknown tags are left as text.
 */
public final class MiniMessageLegacy {
    private static final Pattern TAG = Pattern.compile("<(/?)([^<>\\s]+)>");

    private static final Map<String, Character> COLORS = Map.ofEntries(
            Map.entry("black", '0'), Map.entry("dark_blue", '1'), Map.entry("dark_green", '2'),
            Map.entry("dark_aqua", '3'), Map.entry("dark_red", '4'), Map.entry("dark_purple", '5'),
            Map.entry("gold", '6'), Map.entry("gray", '7'), Map.entry("grey", '7'),
            Map.entry("dark_gray", '8'), Map.entry("dark_grey", '8'), Map.entry("blue", '9'),
            Map.entry("green", 'a'), Map.entry("aqua", 'b'), Map.entry("red", 'c'),
            Map.entry("light_purple", 'd'), Map.entry("yellow", 'e'), Map.entry("white", 'f')
    );

    private static final Map<String, Character> DECORATIONS = Map.ofEntries(
            Map.entry("bold", 'l'), Map.entry("b", 'l'),
            Map.entry("italic", 'o'), Map.entry("i", 'o'), Map.entry("em", 'o'),
            Map.entry("underlined", 'n'), Map.entry("u", 'n'),
            Map.entry("strikethrough", 'm'), Map.entry("st", 'm'),
            Map.entry("obfuscated", 'k'), Map.entry("obf", 'k')
    );

    private MiniMessageLegacy() {
    }

    public static String toLegacy(String input) {
        if (input == null || input.indexOf('<') < 0) {
            return input == null ? "" : input;
        }
        Matcher matcher = TAG.matcher(input);
        StringBuilder out = new StringBuilder(input.length());
        int last = 0;
        while (matcher.find()) {
            out.append(input, last, matcher.start());
            String code = matcher.group(1).isEmpty() ? openTag(matcher.group(2)) : "§r";
            out.append(code != null ? code : matcher.group());
            last = matcher.end();
        }
        out.append(input, last, input.length());
        return out.toString();
    }

    /** Legacy code for an opening tag, or {@code null} if the tag is not understood. */
    private static String openTag(String rawTag) {
        String tag = rawTag.toLowerCase(Locale.ROOT);
        if (tag.equals("reset") || tag.equals("r")) {
            return "§r";
        }
        String colorArg = tag;
        if (tag.startsWith("color:") || tag.startsWith("colour:") || tag.startsWith("c:")) {
            colorArg = tag.substring(tag.indexOf(':') + 1);
        } else if (tag.startsWith("gradient:")) {
            String[] parts = tag.split(":");
            colorArg = parts.length > 1 ? parts[1] : "";
        }
        if (colorArg.startsWith("#")) {
            return hex(colorArg.substring(1));
        }
        Character named = COLORS.get(colorArg);
        if (named != null) {
            return "§" + named;
        }
        Character decoration = DECORATIONS.get(tag);
        return decoration != null ? "§" + decoration : null;
    }

    private static String hex(String digits) {
        if (digits.length() != 6 || !digits.chars().allMatch(c -> Character.digit(c, 16) >= 0)) {
            return null;
        }
        StringBuilder code = new StringBuilder("§x");
        for (char c : digits.toCharArray()) {
            code.append('§').append(c);
        }
        return code.toString();
    }
}
