package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.quiz.QuizCategory;
import com.mawlee.cointcore.quiz.QuizQuestion;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Quiz settings ({@code config/cointcore/quiz.json}) and questions ({@code config/cointcore/quiz-questions.json}).
 * Questions are managed with {@code /quiz add|list|remove}; hand edits need {@code /cointcore reload}.
 */
public final class QuizConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static Settings settings = Settings.defaults();
    private static List<QuizQuestion> questions = List.of();

    private QuizConfig() {
    }

    public static Settings get() {
        return settings;
    }

    /** Valid questions sorted by id. */
    public static List<QuizQuestion> questions() {
        return questions;
    }

    public static void load() {
        Settings loadedSettings = loadSettings(false);
        settings = loadedSettings != null ? loadedSettings : Settings.defaults();
        List<QuizQuestion> loadedQuestions = loadQuestions(false);
        questions = loadedQuestions != null ? loadedQuestions : List.of();
    }

    public static boolean reload() {
        Settings loadedSettings = loadSettings(true);
        List<QuizQuestion> loadedQuestions = loadQuestions(true);
        if (loadedSettings == null || loadedQuestions == null) {
            return false;
        }
        settings = loadedSettings;
        questions = loadedQuestions;
        LOGGER.info("Reloaded quiz ({} questions, enabled={})", questions.size(), settings.enabled());
        return true;
    }

    public static Optional<QuizQuestion> add(QuizCategory category, String text, String correct, List<String> wrong) {
        int id = questions.stream().mapToInt(QuizQuestion::id).max().orElse(0) + 1;
        QuizQuestion question = new QuizQuestion(id, category, text, correct, wrong);
        if (!question.valid()) {
            return Optional.empty();
        }
        List<QuizQuestion> updated = new ArrayList<>(questions);
        updated.add(question);
        if (!saveQuestions(updated)) {
            return Optional.empty();
        }
        questions = List.copyOf(updated);
        return Optional.of(question);
    }

    public static boolean remove(int id) {
        List<QuizQuestion> updated = new ArrayList<>(questions);
        if (!updated.removeIf(question -> question.id() == id) || !saveQuestions(updated)) {
            return false;
        }
        questions = List.copyOf(updated);
        return true;
    }

    private static Settings loadSettings(boolean reloading) {
        Path path = dir().resolve("quiz.json");
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                write(new SettingsData(), path);
                LOGGER.info("Created default quiz config at {}", path);
                return parse(new SettingsData());
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                SettingsData data = GSON.fromJson(reader, SettingsData.class);
                return parse(data != null ? data : new SettingsData());
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load quiz config from {}", path, exception);
            return reloading ? null : Settings.defaults();
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading quiz config from {}", path, exception);
            return reloading ? null : Settings.defaults();
        }
    }

    private static List<QuizQuestion> loadQuestions(boolean reloading) {
        Path path = questionsPath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                write(new QuestionsData(), path);
                return List.of();
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                QuestionsData data = GSON.fromJson(reader, QuestionsData.class);
                return parseQuestions(data != null ? data : new QuestionsData());
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load quiz questions from {}", path, exception);
            return reloading ? null : List.of();
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading quiz questions from {}", path, exception);
            return reloading ? null : List.of();
        }
    }

    private static Settings parse(SettingsData data) {
        return new Settings(
                data.enabled,
                Math.max(5, Math.min(24 * 60, data.intervalMinutes)),
                Math.max(1, Math.min(120, data.inviteValidMinutes)),
                Math.max(1, Math.min(20, data.questionsPerRound)),
                Math.max(5, Math.min(120, data.secondsPerQuestion)),
                Math.max(0L, data.rewardPerCorrect),
                Math.max(0L, data.rewardAllCorrectBonus),
                Math.max(1, data.minOnlinePlayers)
        );
    }

    private static List<QuizQuestion> parseQuestions(QuestionsData data) {
        List<QuizQuestion> parsed = new ArrayList<>();
        if (data.questions != null) {
            for (QuestionData raw : data.questions) {
                if (raw == null) {
                    continue;
                }
                Optional<QuizCategory> category = QuizCategory.parse(raw.category);
                QuizQuestion question = new QuizQuestion(raw.id, category.orElse(null), raw.question, raw.correct,
                        raw.wrong == null ? List.of() : raw.wrong);
                if (!question.valid() || parsed.stream().anyMatch(other -> other.id() == raw.id)) {
                    LOGGER.warn("Quiz question #{} is invalid or duplicated (needs category, question, correct and 3 wrong); skipped", raw.id);
                    continue;
                }
                parsed.add(question);
            }
        }
        parsed.sort(Comparator.comparingInt(QuizQuestion::id));
        return List.copyOf(parsed);
    }

    private static boolean saveQuestions(List<QuizQuestion> list) {
        QuestionsData data = new QuestionsData();
        for (QuizQuestion question : list) {
            QuestionData raw = new QuestionData();
            raw.id = question.id();
            raw.category = question.category().id();
            raw.question = question.text();
            raw.correct = question.correct();
            raw.wrong = new ArrayList<>(question.wrong());
            data.questions.add(raw);
        }
        try {
            write(data, questionsPath());
            return true;
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to save quiz questions to {}", questionsPath(), exception);
            return false;
        }
    }

    private static void write(Object data, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
        Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
    }

    private static Path dir() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID);
    }

    private static Path questionsPath() {
        return dir().resolve("quiz-questions.json");
    }

    public record Settings(
            boolean enabled,
            int intervalMinutes,
            int inviteValidMinutes,
            int questionsPerRound,
            int secondsPerQuestion,
            long rewardPerCorrect,
            long rewardAllCorrectBonus,
            int minOnlinePlayers
    ) {
        static Settings defaults() {
            return parse(new SettingsData());
        }
    }

    private static final class SettingsData {
        @SerializedName("enabled")
        boolean enabled = true;

        /** How often everyone online gets the "Принять участие?" invite. */
        @SerializedName("interval_minutes")
        int intervalMinutes = 120;

        /** How long the invite's [Да] button works. */
        @SerializedName("invite_valid_minutes")
        int inviteValidMinutes = 10;

        @SerializedName("questions_per_round")
        int questionsPerRound = 5;

        @SerializedName("seconds_per_question")
        int secondsPerQuestion = 15;

        /** Gluons for each right answer. */
        @SerializedName("reward_per_correct")
        long rewardPerCorrect = 0L;

        /** Extra gluons when every answer of the round is right. */
        @SerializedName("reward_all_correct_bonus")
        long rewardAllCorrectBonus = 0L;

        @SerializedName("min_online_players")
        int minOnlinePlayers = 1;
    }

    private static final class QuestionsData {
        @SerializedName("questions")
        List<QuestionData> questions = new ArrayList<>();
    }

    private static final class QuestionData {
        @SerializedName("id")
        int id;

        /** общие / технические / магические */
        @SerializedName("category")
        String category;

        @SerializedName("question")
        String question;

        @SerializedName("correct")
        String correct;

        /** Exactly three wrong answers. */
        @SerializedName("wrong")
        List<String> wrong;
    }
}
