package com.mawlee.cointcore.lang;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class LegacyTextParser {
    private LegacyTextParser() {
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        String text = input.replace('&', '§');
        MutableComponent result = Component.empty();
        StringBuilder segment = new StringBuilder();
        Style style = Style.EMPTY;

        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current != '§' || index + 1 >= text.length()) {
                segment.append(current);
                continue;
            }

            if (text.charAt(index + 1) == 'x' && index + 13 < text.length()) {
                TextColor hexColor = parseLegacyHexColor(text, index);
                if (hexColor != null) {
                    appendSegment(result, segment, style);
                    style = style.withColor(hexColor);
                    index += 13;
                    continue;
                }
            }

            ChatFormatting format = ChatFormatting.getByCode(text.charAt(index + 1));
            if (format != null) {
                appendSegment(result, segment, style);
                style = applyFormatting(style, format);
                index++;
                continue;
            }

            segment.append(current);
        }

        appendSegment(result, segment, style);
        return result;
    }

    private static void appendSegment(MutableComponent result, StringBuilder segment, Style style) {
        if (segment.isEmpty()) {
            return;
        }
        result.append(Component.literal(segment.toString()).withStyle(style));
        segment.setLength(0);
    }

    private static Style applyFormatting(Style style, ChatFormatting format) {
        if (format == ChatFormatting.RESET) {
            return Style.EMPTY;
        }

        if (format.isColor()) {
            TextColor color = TextColor.fromLegacyFormat(format);
            return color != null ? style.withColor(color) : style;
        }

        return switch (format) {
            case OBFUSCATED -> style.withObfuscated(true);
            case BOLD -> style.withBold(true);
            case STRIKETHROUGH -> style.withStrikethrough(true);
            case UNDERLINE -> style.withUnderlined(true);
            case ITALIC -> style.withItalic(true);
            default -> style;
        };
    }

    private static TextColor parseLegacyHexColor(String text, int sectionIndex) {
        StringBuilder hex = new StringBuilder(6);
        for (int offset = 0; offset < 12; offset += 2) {
            if (text.charAt(sectionIndex + 2 + offset) != '§') {
                return null;
            }
            hex.append(text.charAt(sectionIndex + 3 + offset));
        }

        try {
            return TextColor.fromRgb(Integer.parseInt(hex.toString(), 16));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
