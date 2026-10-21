package com.mawlee.cointcore.quiz;

import java.util.List;

/** One stored question: the right answer and exactly three wrong ones. */
public record QuizQuestion(int id, QuizCategory category, String text, String correct, List<String> wrong) {
    public static final int WRONG_ANSWERS = 3;

    public QuizQuestion {
        wrong = List.copyOf(wrong);
    }

    public boolean valid() {
        return id > 0 && category != null && notBlank(text) && notBlank(correct)
                && wrong.size() == WRONG_ANSWERS && wrong.stream().allMatch(QuizQuestion::notBlank);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
