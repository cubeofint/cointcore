package com.mawlee.cointcore.chat;

/**
 * Picks the channel for a typed chat line (no Minecraft types, unit-tested). An explicit prefix
 * ({@code !} global, {@code $} trade) wins; otherwise the sender's current view decides, local by default.
 */
public final class ChatRouting {
    private ChatRouting() {
    }

    /** {@code channel == null} means nothing to send ({@link #error} says why). */
    public record Route(ChatChannel channel, String text, String error) {
        static Route of(ChatChannel channel, String text) {
            return new Route(channel, text, null);
        }

        static Route fail(String error) {
            return new Route(null, "", error);
        }
    }

    public static final String EMPTY_AFTER_PREFIX = "empty_after_prefix";
    public static final String PRIVATE_VIEW = "private_view";

    public static Route route(String raw, String globalChar, String tradeChar, ChatChannel.View senderView) {
        String text = raw == null ? "" : raw;
        if (startsWith(text, globalChar)) {
            return stripped(ChatChannel.GLOBAL, text, globalChar);
        }
        if (startsWith(text, tradeChar)) {
            return stripped(ChatChannel.TRADE, text, tradeChar);
        }
        return switch (senderView == null ? ChatChannel.View.ALL : senderView) {
            case GLOBAL -> Route.of(ChatChannel.GLOBAL, text);
            case TRADE -> Route.of(ChatChannel.TRADE, text);
            case PRIVATE -> Route.fail(PRIVATE_VIEW);
            default -> Route.of(ChatChannel.LOCAL, text);
        };
    }

    private static boolean startsWith(String text, String prefix) {
        return prefix != null && !prefix.isEmpty() && text.startsWith(prefix);
    }

    private static Route stripped(ChatChannel channel, String text, String prefix) {
        String body = text.substring(prefix.length()).trim();
        return body.isEmpty() ? Route.fail(EMPTY_AFTER_PREFIX) : Route.of(channel, body);
    }
}
