package com.mawlee.cointcore.chat;

import java.util.Locale;
import java.util.Optional;

/** Chat channels a message belongs to. {@link View#ALL} shows every channel. */
public enum ChatChannel {
    LOCAL,
    GLOBAL,
    TRADE,
    PRIVATE;

    /** Lang key of the chat notice shown when switching to {@code view}. */
    public static String viewNoticeKey(View view) {
        return "message.cointcore.chat.view." + view.name().toLowerCase(Locale.ROOT);
    }

    public enum View {
        ALL,
        LOCAL,
        GLOBAL,
        TRADE,
        PRIVATE;

        public boolean shows(ChatChannel channel) {
            return this == ALL || name().equals(channel.name());
        }

        /** {@code all|l|local|g|global|t|trade|pm} (case-insensitive). */
        public static Optional<View> parse(String raw) {
            if (raw == null) {
                return Optional.empty();
            }
            return switch (raw.toLowerCase(Locale.ROOT)) {
                case "all", "a" -> Optional.of(ALL);
                case "local", "l" -> Optional.of(LOCAL);
                case "global", "g" -> Optional.of(GLOBAL);
                case "trade", "t" -> Optional.of(TRADE);
                case "pm", "private", "p" -> Optional.of(PRIVATE);
                default -> Optional.empty();
            };
        }
    }
}
