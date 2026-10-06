package com.mawlee.cointcore.chat;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns bare http(s)/www URLs inside chat components into clickable OPEN_URL spans.
 */
public final class ChatLinkFormatter {
    private static final Pattern QUICK_CHECK = Pattern.compile("(?i)https?://|www\\.");
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b((?:https?://|www\\.)[^\\s<]+[^\\s<\\.,:;!?\"'\\]\\)}])"
    );

    private ChatLinkFormatter() {
    }

    public static boolean needsLinkify(Component component) {
        return component != null && QUICK_CHECK.matcher(component.getString()).find();
    }

    public static Component linkify(Component input) {
        if (input == null || !needsLinkify(input)) {
            return input;
        }

        MutableComponent result = Component.empty();
        input.visit((style, text) -> {
            appendLinkified(result, text, style);
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    private static void appendLinkified(MutableComponent out, String text, Style style) {
        if (text.isEmpty()) {
            return;
        }

        if (hasOpenUrl(style)) {
            out.append(Component.literal(text).withStyle(style));
            return;
        }

        Matcher matcher = URL_PATTERN.matcher(text);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                out.append(Component.literal(text.substring(last, matcher.start())).withStyle(style));
            }

            String raw = matcher.group(1);
            String url = normalizeUrl(raw);
            if (isAllowedUrl(url)) {
                Style linkStyle = style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(url)))
                        .withUnderlined(true);
                out.append(Component.literal(raw).withStyle(linkStyle));
            } else {
                out.append(Component.literal(raw).withStyle(style));
            }
            last = matcher.end();
        }

        if (last < text.length()) {
            out.append(Component.literal(text.substring(last)).withStyle(style));
        }
    }

    private static boolean hasOpenUrl(Style style) {
        ClickEvent click = style.getClickEvent();
        return click != null && click.getAction() == ClickEvent.Action.OPEN_URL;
    }

    private static String normalizeUrl(String raw) {
        if (raw.regionMatches(true, 0, "www.", 0, 4)) {
            return "https://" + raw;
        }
        return raw;
    }

    private static boolean isAllowedUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
