package com.mawlee.cointcore.quiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One player's quiz: N distinct random questions, answers shuffled anew each time, a deadline per question.
 * The right answer never leaves the server; Minecraft-free, unit-tested.
 */
public final class QuizRound {
    /** Extra time for network delay before a late answer counts as timed out. */
    public static final long GRACE_MS = 1_000L;

    public enum Verdict {
        CORRECT,
        WRONG,
        TIMEOUT,
        /** Answer for another question, or the question was already answered. */
        IGNORED
    }

    public record Shown(int number, int total, QuizCategory category, String question, List<String> answers,
                        int correctIndex) {
    }

    private final List<Shown> questions;
    private final long questionMs;
    private int index = -1;
    private long deadlineMs;
    private boolean awaiting;
    private int correctCount;

    private QuizRound(List<Shown> questions, long questionMs) {
        this.questions = questions;
        this.questionMs = questionMs;
    }

    /** Empty when the pool has fewer than {@code count} questions. */
    public static java.util.Optional<QuizRound> create(List<QuizQuestion> pool, int count, int secondsPerQuestion, Random random) {
        if (count < 1 || pool.size() < count) {
            return java.util.Optional.empty();
        }
        List<QuizQuestion> picked = new ArrayList<>(pool);
        Collections.shuffle(picked, random);
        List<Shown> shown = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            QuizQuestion question = picked.get(i);
            List<String> answers = new ArrayList<>(question.wrong());
            int correctIndex = random.nextInt(answers.size() + 1);
            answers.add(correctIndex, question.correct());
            shown.add(new Shown(i + 1, count, question.category(), question.text(), List.copyOf(answers), correctIndex));
        }
        return java.util.Optional.of(new QuizRound(List.copyOf(shown), Math.max(1, secondsPerQuestion) * 1000L));
    }

    /** Moves to the next question and starts its timer; empty when the round is over. */
    public java.util.Optional<Shown> next(long nowMs) {
        if (index + 1 >= questions.size()) {
            awaiting = false;
            return java.util.Optional.empty();
        }
        index++;
        deadlineMs = nowMs + questionMs;
        awaiting = true;
        return java.util.Optional.of(questions.get(index));
    }

    public Verdict answer(int number, int choice, long nowMs) {
        if (!awaiting || index < 0 || number != questions.get(index).number()) {
            return Verdict.IGNORED;
        }
        awaiting = false;
        if (nowMs > deadlineMs + GRACE_MS) {
            return Verdict.TIMEOUT;
        }
        if (choice == questions.get(index).correctIndex()) {
            correctCount++;
            return Verdict.CORRECT;
        }
        return Verdict.WRONG;
    }

    /** {@link Verdict#TIMEOUT} once the current question ran out without an answer, else {@link Verdict#IGNORED}. */
    public Verdict checkTimeout(long nowMs) {
        if (!awaiting || nowMs <= deadlineMs + GRACE_MS) {
            return Verdict.IGNORED;
        }
        awaiting = false;
        return Verdict.TIMEOUT;
    }

    public Shown current() {
        return index >= 0 ? questions.get(index) : null;
    }

    public long deadlineMs() {
        return deadlineMs;
    }

    public int correctCount() {
        return correctCount;
    }

    public int total() {
        return questions.size();
    }

    public boolean finished() {
        return !awaiting && index + 1 >= questions.size();
    }
}
