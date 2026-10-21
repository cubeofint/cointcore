package com.mawlee.cointcore.quiz;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.quiz.client.QuizClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/** Quiz window traffic. The right answer is only sent after the player answered (in {@link Feedback}). */
public final class QuizPayloads {
    private static final int MAX_TEXT = 1024;

    private QuizPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CointCore.MOD_ID).optional();
        registrar.playToClient(Question.TYPE, Question.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> QuizClient.onQuestion(payload)));
        registrar.playToClient(Feedback.TYPE, Feedback.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> QuizClient.onFeedback(payload)));
        registrar.playToClient(Finish.TYPE, Finish.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> QuizClient.onFinish(payload)));
        registrar.playToServer(Answer.TYPE, Answer.STREAM_CODEC, Answer::handleServer);
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, path));
    }

    /** Server → client: show question {@code number} of {@code total}; the timer runs {@code seconds}. */
    public record Question(int number, int total, int category, String question, List<String> answers, int seconds)
            implements CustomPacketPayload {
        public static final Type<Question> TYPE = payloadType("quiz_question");
        public static final StreamCodec<RegistryFriendlyByteBuf, Question> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeVarInt(payload.number);
                    buffer.writeVarInt(payload.total);
                    buffer.writeVarInt(payload.category);
                    buffer.writeUtf(payload.question, MAX_TEXT);
                    buffer.writeVarInt(payload.answers.size());
                    for (String answer : payload.answers) {
                        buffer.writeUtf(answer, MAX_TEXT);
                    }
                    buffer.writeVarInt(payload.seconds);
                },
                buffer -> {
                    int number = buffer.readVarInt();
                    int total = buffer.readVarInt();
                    int category = buffer.readVarInt();
                    String question = buffer.readUtf(MAX_TEXT);
                    int size = Math.min(8, buffer.readVarInt());
                    List<String> answers = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        answers.add(buffer.readUtf(MAX_TEXT));
                    }
                    return new Question(number, total, category, question, List.copyOf(answers), buffer.readVarInt());
                }
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server → client: result of question {@code number}; {@code chosen} is -1 on timeout. */
    public record Feedback(int number, int chosen, int correctIndex, boolean correct) implements CustomPacketPayload {
        public static final Type<Feedback> TYPE = payloadType("quiz_feedback");
        public static final StreamCodec<RegistryFriendlyByteBuf, Feedback> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Feedback::number,
                ByteBufCodecs.VAR_INT, Feedback::chosen,
                ByteBufCodecs.VAR_INT, Feedback::correctIndex,
                ByteBufCodecs.BOOL, Feedback::correct,
                Feedback::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server → client: the round is over. */
    public record Finish(int correct, int total, long reward) implements CustomPacketPayload {
        public static final Type<Finish> TYPE = payloadType("quiz_finish");
        public static final StreamCodec<RegistryFriendlyByteBuf, Finish> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Finish::correct,
                ByteBufCodecs.VAR_INT, Finish::total,
                ByteBufCodecs.VAR_LONG, Finish::reward,
                Finish::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client → server: answer {@code choice} (0–3) to question {@code number}; {@link #QUIT} leaves the round. */
    public record Answer(int number, int choice) implements CustomPacketPayload {
        public static final int QUIT = -1;
        public static final Type<Answer> TYPE = payloadType("quiz_answer");
        public static final StreamCodec<RegistryFriendlyByteBuf, Answer> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Answer::number,
                ByteBufCodecs.VAR_INT, Answer::choice,
                Answer::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handleServer(Answer payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    QuizService.onAnswer(player, payload.number(), payload.choice());
                }
            });
        }
    }
}
