package com.mawlee.cointcore.quiz.client;

import com.mawlee.cointcore.lang.LegacyTextParser;
import com.mawlee.cointcore.quiz.QuizCategory;
import com.mawlee.cointcore.quiz.QuizPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Quiz window: category, question, a timer bar and four answers in a 2×2 grid (click or keys 1–4). After an
 * answer the right one turns green (and a wrong pick red) until the server sends the next question.
 */
public final class QuizScreen extends Screen {
    private static final int PANEL_W = 340;
    private static final int PANEL_H = 214;
    private static final int ANSWER_H = 38;
    private static final int GAP = 6;
    private static final QuizCategory[] CATEGORIES = QuizCategory.values();

    private enum State { WAITING, QUESTION, ANSWERED, REVEAL, FINISHED }

    private State state = State.WAITING;
    private QuizPayloads.Question question;
    private long deadlineMs;
    private long questionMs;
    private int chosen = -1;
    private int correctIndex = -1;
    private QuizPayloads.Finish finish;
    private Button closeButton;

    public QuizScreen() {
        super(Component.translatable("gui.cointcore.quiz.title"));
    }

    void showQuestion(QuizPayloads.Question payload) {
        question = payload;
        questionMs = Math.max(1, payload.seconds()) * 1000L;
        deadlineMs = System.currentTimeMillis() + questionMs;
        chosen = -1;
        correctIndex = -1;
        state = State.QUESTION;
    }

    void showFeedback(QuizPayloads.Feedback payload) {
        if (question == null || payload.number() != question.number()) {
            return;
        }
        chosen = payload.chosen();
        correctIndex = payload.correctIndex();
        state = State.REVEAL;
    }

    void showFinish(QuizPayloads.Finish payload) {
        finish = payload;
        state = State.FINISHED;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        closeButton = null;
        if (state == State.FINISHED) {
            closeButton = addRenderableWidget(Button.builder(Component.translatable("gui.cointcore.quiz.close"), button -> onClose())
                    .bounds(width / 2 - 50, top() + PANEL_H - 30, 100, 20)
                    .build());
        }
    }

    private int left() {
        return (width - PANEL_W) / 2;
    }

    private int top() {
        return (height - PANEL_H) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = left();
        int y = top();
        graphics.fill(x - 1, y - 1, x + PANEL_W + 1, y + PANEL_H + 1, 0xFF6B4FA0);
        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, 0xF0141420);
        graphics.drawCenteredString(font, title, width / 2, y + 8, 0xFFFFAA00);

        if (state == State.FINISHED && finish != null) {
            renderFinish(graphics, x, y);
        } else if (question != null) {
            renderQuestion(graphics, x, y, mouseX, mouseY);
        } else {
            graphics.drawCenteredString(font, Component.translatable("gui.cointcore.quiz.loading"), width / 2, y + PANEL_H / 2, 0xFFAAAAAA);
        }
        if (closeButton != null) {
            closeButton.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderQuestion(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        QuizCategory category = question.category() >= 0 && question.category() < CATEGORIES.length
                ? CATEGORIES[question.category()] : QuizCategory.GENERAL;
        Component header = Component.translatable("gui.cointcore.quiz.header",
                Component.translatable(category.langKey()), question.number(), question.total());
        graphics.drawCenteredString(font, header, width / 2, y + 22, 0xFFAAAAFF);

        int textY = y + 38;
        List<FormattedCharSequence> lines = font.split(LegacyTextParser.parse(question.question()), PANEL_W - 24);
        for (int i = 0; i < Math.min(4, lines.size()); i++) {
            graphics.drawCenteredString(font, lines.get(i), width / 2, textY + i * (font.lineHeight + 1), 0xFFFFFFFF);
        }

        long left = state == State.QUESTION || state == State.ANSWERED
                ? Math.max(0L, deadlineMs - System.currentTimeMillis()) : 0L;
        int barX = x + 12;
        int barY = y + 84;
        int barW = PANEL_W - 24;
        graphics.fill(barX, barY, barX + barW, barY + 4, 0xFF2A2A3A);
        int filled = (int) (barW * (double) left / questionMs);
        graphics.fill(barX, barY, barX + filled, barY + 4, left < 5_000L ? 0xFFFF5555 : 0xFF55FF55);
        if (state == State.QUESTION || state == State.ANSWERED) {
            graphics.drawString(font, (left + 999L) / 1000L + "", barX + barW - 12, barY + 7, 0xFFAAAAAA);
        }

        for (int i = 0; i < question.answers().size(); i++) {
            int[] box = answerBox(i);
            boolean hover = state == State.QUESTION && inside(box, mouseX, mouseY);
            int color = 0xFF2C2C44;
            if (state == State.REVEAL && i == correctIndex) {
                color = 0xFF2E7D32;
            } else if (state == State.REVEAL && i == chosen) {
                color = 0xFFB71C1C;
            } else if (state == State.ANSWERED && i == chosen) {
                color = 0xFF4A4A70;
            } else if (hover) {
                color = 0xFF3D3D60;
            }
            graphics.fill(box[0], box[1], box[0] + box[2], box[1] + box[3], color);
            graphics.renderOutline(box[0], box[1], box[2], box[3], 0xFF6B4FA0);
            List<FormattedCharSequence> answer = font.split(LegacyTextParser.parse((i + 1) + ". " + question.answers().get(i)), box[2] - 8);
            int lineCount = Math.min(2, answer.size());
            int startY = box[1] + (box[3] - lineCount * (font.lineHeight + 1)) / 2 + 1;
            for (int line = 0; line < lineCount; line++) {
                graphics.drawCenteredString(font, answer.get(line), box[0] + box[2] / 2, startY + line * (font.lineHeight + 1), 0xFFFFFFFF);
            }
        }
        if (state == State.REVEAL) {
            boolean right = chosen == correctIndex;
            Component verdict = Component.translatable(chosen < 0 ? "gui.cointcore.quiz.timeout"
                    : right ? "gui.cointcore.quiz.correct" : "gui.cointcore.quiz.wrong");
            graphics.drawCenteredString(font, verdict, width / 2, y + PANEL_H - 16, right ? 0xFF55FF55 : 0xFFFF5555);
        } else {
            graphics.drawCenteredString(font, Component.translatable("gui.cointcore.quiz.hint"), width / 2, y + PANEL_H - 16, 0xFF777788);
        }
    }

    private void renderFinish(GuiGraphics graphics, int x, int y) {
        graphics.drawCenteredString(font, Component.translatable("gui.cointcore.quiz.finished", finish.correct(), finish.total()),
                width / 2, y + 70, 0xFFFFFFFF);
        if (finish.reward() > 0L) {
            graphics.drawCenteredString(font, Component.translatable("gui.cointcore.quiz.reward", finish.reward()),
                    width / 2, y + 90, 0xFF55FF55);
        }
    }

    /** {x, y, w, h} of answer {@code index} in the 2×2 grid. */
    private int[] answerBox(int index) {
        int boxW = (PANEL_W - 24 - GAP) / 2;
        int x = left() + 12 + (index % 2) * (boxW + GAP);
        int y = top() + 100 + (index / 2) * (ANSWER_H + GAP);
        return new int[]{x, y, boxW, ANSWER_H};
    }

    private static boolean inside(int[] box, double mouseX, double mouseY) {
        return mouseX >= box[0] && mouseX < box[0] + box[2] && mouseY >= box[1] && mouseY < box[1] + box[3];
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (state == State.QUESTION && button == 0 && question != null) {
            for (int i = 0; i < question.answers().size(); i++) {
                if (inside(answerBox(i), mouseX, mouseY)) {
                    choose(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (state == State.QUESTION && question != null && keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_4) {
            int index = keyCode - GLFW.GLFW_KEY_1;
            if (index < question.answers().size()) {
                choose(index);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void choose(int index) {
        chosen = index;
        state = State.ANSWERED;
        PacketDistributor.sendToServer(new QuizPayloads.Answer(question.number(), index));
    }

    @Override
    public void onClose() {
        if (state != State.FINISHED && question != null) {
            // Leaving the window ends the round; the server credits what was answered so far.
            PacketDistributor.sendToServer(new QuizPayloads.Answer(question.number(), QuizPayloads.Answer.QUIT));
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
