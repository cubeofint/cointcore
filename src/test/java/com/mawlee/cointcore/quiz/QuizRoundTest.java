package com.mawlee.cointcore.quiz;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizRoundTest {
    private static List<QuizQuestion> pool(int size) {
        List<QuizQuestion> questions = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            questions.add(new QuizQuestion(i, QuizCategory.GENERAL, "Q" + i, "R" + i, List.of("W1", "W2", "W3")));
        }
        return questions;
    }

    @Test
    void picksDistinctQuestionsWithShuffledAnswers() {
        QuizRound round = QuizRound.create(pool(20), 5, 15, new Random(1)).orElseThrow();
        Set<String> seen = new HashSet<>();
        Set<Integer> positions = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            QuizRound.Shown shown = round.next(0L).orElseThrow();
            assertTrue(seen.add(shown.question()));
            assertEquals(4, shown.answers().size());
            assertEquals("R" + shown.question().substring(1), shown.answers().get(shown.correctIndex()));
            positions.add(shown.correctIndex());
            round.answer(shown.number(), shown.correctIndex(), 0L);
        }
        assertTrue(round.next(0L).isEmpty());
        assertTrue(round.finished());
        assertEquals(5, round.correctCount());
        assertTrue(positions.size() > 1, "the right answer should not always sit in the same place");
    }

    @Test
    void notEnoughQuestions() {
        assertTrue(QuizRound.create(pool(4), 5, 15, new Random()).isEmpty());
    }

    @Test
    void lateWrongAndDuplicateAnswers() {
        QuizRound round = QuizRound.create(pool(5), 3, 15, new Random(2)).orElseThrow();
        QuizRound.Shown first = round.next(0L).orElseThrow();
        assertEquals(QuizRound.Verdict.IGNORED, round.answer(2, 0, 1_000L));
        assertEquals(QuizRound.Verdict.WRONG, round.answer(1, (first.correctIndex() + 1) % 4, 1_000L));
        assertEquals(QuizRound.Verdict.IGNORED, round.answer(1, first.correctIndex(), 1_000L));

        QuizRound.Shown second = round.next(10_000L).orElseThrow();
        assertEquals(QuizRound.Verdict.TIMEOUT, round.answer(2, second.correctIndex(), 26_001L));

        round.next(30_000L).orElseThrow();
        assertEquals(QuizRound.Verdict.IGNORED, round.checkTimeout(46_000L));
        assertEquals(QuizRound.Verdict.TIMEOUT, round.checkTimeout(46_001L));
        assertEquals(0, round.correctCount());
    }
}
