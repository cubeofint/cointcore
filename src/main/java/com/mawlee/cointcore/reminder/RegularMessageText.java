package com.mawlee.cointcore.reminder;

import com.mawlee.cointcore.lang.MiniMessageLegacy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the text typed in {@code /addreg} into legacy codes for {@code LegacyTextParser}: {@code &} codes as is
 * ({@code &l} bold, {@code &o} italic, {@code &n} underline, colours), {@code &#RRGGBB} hex colours and the
 * MiniMessage subset of {@link MiniMessageLegacy}. Minecraft-free, unit-tested.
 */
public final class RegularMessageText {
    private static final Pattern AMPERSAND_HEX = Pattern.compile("&#([0-9a-fA-F]{6})");

    private RegularMessageText() {
    }

    public static String toLegacy(String raw) {
        String text = MiniMessageLegacy.toLegacy(raw);
        Matcher matcher = AMPERSAND_HEX.matcher(text);
        StringBuilder out = new StringBuilder(text.length() + 16);
        while (matcher.find()) {
            StringBuilder code = new StringBuilder("§x");
            for (char c : matcher.group(1).toLowerCase().toCharArray()) {
                code.append('§').append(c);
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(code.toString()));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
