package com.mawlee.cointcore.quiz.client;

import com.mawlee.cointcore.quiz.QuizPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Routes quiz payloads to the open {@link QuizScreen}, opening it for the first question. */
public final class QuizClient {
    private QuizClient() {
    }

    public static void onQuestion(QuizPayloads.Question payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof QuizScreen screen) {
            screen.showQuestion(payload);
        } else {
            QuizScreen screen = new QuizScreen();
            mc.setScreen(screen);
            screen.showQuestion(payload);
        }
    }

    public static void onFeedback(QuizPayloads.Feedback payload) {
        if (Minecraft.getInstance().screen instanceof QuizScreen screen) {
            screen.showFeedback(payload);
        }
    }

    public static void onFinish(QuizPayloads.Finish payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof QuizScreen screen) {
            screen.showFinish(payload);
        } else if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("gui.cointcore.quiz.result_chat",
                    payload.correct(), payload.total(), payload.reward()), false);
        }
    }
}
