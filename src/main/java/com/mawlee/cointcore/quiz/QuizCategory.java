package com.mawlee.cointcore.quiz;

import java.util.Locale;
import java.util.Optional;

/** Question category; {@link #id()} is what {@code /quiz add} takes and what the questions file stores. */
public enum QuizCategory {
    GENERAL("общие", "general"),
    TECHNICAL("технические", "technical"),
    MAGIC("магические", "magic");

    private final String id;
    private final String alias;

    QuizCategory(String id, String alias) {
        this.id = id;
        this.alias = alias;
    }

    public String id() {
        return id;
    }

    /** Lang key of the label shown in the quiz window and lists. */
    public String langKey() {
        return "gui.cointcore.quiz.category." + alias;
    }

    public static Optional<QuizCategory> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        for (QuizCategory category : values()) {
            if (category.id.equals(value) || category.alias.equals(value) || category.name().toLowerCase(Locale.ROOT).equals(value)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }
}
