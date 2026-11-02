package com.mawlee.cointcore.refinedstorage.batch;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.mixin.refinedstorage.BatchAutocraftingComponentAccessor;
import com.mawlee.cointcore.refinedstorage.wireless.GridAccess;
import com.refinedmods.refinedstorage.api.autocrafting.calculation.CancellationToken;
import com.refinedmods.refinedstorage.api.autocrafting.preview.Preview;
import com.refinedmods.refinedstorage.api.autocrafting.preview.PreviewType;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskImpl;
import com.refinedmods.refinedstorage.api.network.autocrafting.AutocraftingNetworkComponent;
import com.refinedmods.refinedstorage.api.network.impl.autocrafting.AutocraftingNetworkComponentImpl;
import com.refinedmods.refinedstorage.api.network.impl.autocrafting.TimeoutableCancellationToken;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.common.api.security.SecurityHelper;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.packet.s2c.S2CPackets;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class BatchCraftingPackets {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    public static final int PREVIEW = 0, START = 1, MAX = 2, CANCEL = 3;
    private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();
    private BatchCraftingPackets() { }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(CointCore.MOD_ID).playToServer(Request.TYPE, Request.CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) handle(player, packet);
                }));
    }

    public record Request(int menuId, UUID id, List<ResourceAmount> outputs, ResourceAmount recipeOutput, long multiplier,
                          int action, boolean notifyPlayer) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "batch_crafting_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = new StreamCodec<>() {
            @Override public Request decode(RegistryFriendlyByteBuf buf) {
                int menu = buf.readVarInt(); UUID id = buf.readUUID();
                int count = buf.readVarInt();
                if (count < 1 || count > BatchCraftingPlanner.MAX_REQUESTS) throw new IllegalArgumentException("Invalid batch length");
                var outputs = new ArrayList<ResourceAmount>(count);
                for (int i = 0; i < count; i++) outputs.add(ResourceCodecs.AMOUNT_STREAM_CODEC.decode(buf));
                ResourceAmount recipeOutput = buf.readBoolean() ? ResourceCodecs.AMOUNT_STREAM_CODEC.decode(buf) : null;
                return new Request(menu, id, List.copyOf(outputs), recipeOutput, buf.readVarLong(), buf.readVarInt(), buf.readBoolean());
            }
            @Override public void encode(RegistryFriendlyByteBuf buf, Request value) {
                buf.writeVarInt(value.menuId); buf.writeUUID(value.id); buf.writeVarInt(value.outputs.size());
                value.outputs.forEach(output -> ResourceCodecs.AMOUNT_STREAM_CODEC.encode(buf, output));
                buf.writeBoolean(value.recipeOutput != null);
                if (value.recipeOutput != null) ResourceCodecs.AMOUNT_STREAM_CODEC.encode(buf, value.recipeOutput);
                buf.writeVarLong(value.multiplier); buf.writeVarInt(value.action); buf.writeBoolean(value.notifyPlayer);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static void handle(ServerPlayer player, Request packet) {
        if (packet.action < PREVIEW || packet.action > CANCEL) return;
        var session = SESSIONS.computeIfAbsent(player, ignored -> new Session());
        if (packet.action == CANCEL) {
            if (session.token != null) session.token.cancel();
            session.generation++;
            session.starting = false;
            S2CPackets.sendAutocraftingPreviewCancelResponse(player);
            return;
        }
        var access = GridAccess.from(player, packet.menuId);
        if (access == null || !SecurityHelper.isAllowed(player, BuiltinPermission.AUTOCRAFTING, access.network())) return;
        if (!(access.network().getComponent(AutocraftingNetworkComponent.class) instanceof AutocraftingNetworkComponentImpl component)) return;
        try {
            BatchCraftingPlanner.normalize(packet.outputs, packet.multiplier);
            if (packet.recipeOutput != null) {
                if (packet.recipeOutput.amount() <= 0) throw new IllegalArgumentException("Invalid recipe output");
                Math.multiplyExact(packet.recipeOutput.amount(), packet.multiplier);
            }
        }
        catch (IllegalArgumentException | ArithmeticException invalid) { failure(player, packet); return; }
        if (session.accepted.contains(packet.id)) { failure(player, packet); return; }
        if (session.starting) { failure(player, packet); return; }
        if (session.token != null) session.token.cancel();
        var token = new TimeoutableCancellationToken();
        session.token = token;
        session.starting = packet.action == START;
        int generation = ++session.generation;
        var accessor = (BatchAutocraftingComponentAccessor) component;
        try {
            CompletableFuture.supplyAsync(() -> {
                if (packet.action == MAX) return new Calculation(null, maximum(accessor, packet.outputs, token));
                return new Calculation(BatchCraftingPlanner.calculate(accessor.cointcore$batchPatterns(),
                        accessor.cointcore$batchStorage().get(), packet.outputs, packet.multiplier, packet.action == START, token), 0);
            }, accessor.cointcore$batchExecutor()).whenComplete((result, error) -> player.getServer().execute(() -> {
                if (session.generation != generation) return;
                session.starting = false;
                var current = GridAccess.from(player, packet.menuId);
                if (current == null || current.network() != access.network() || token.isCancelled()
                        || !SecurityHelper.isAllowed(player, BuiltinPermission.AUTOCRAFTING, current.network()) || error != null) {
                    failure(player, packet); return;
                }
                if (packet.action == MAX) {
                    long limit = packet.recipeOutput == null ? Long.MAX_VALUE : Long.MAX_VALUE / packet.recipeOutput.amount();
                    S2CPackets.sendAutocraftingPreviewMaxAmountResponse(player, Math.min(result.maximum, limit));
                } else if (packet.action == PREVIEW) {
                    S2CPackets.sendAutocraftingPreviewResponse(player, packet.id, result.result.preview());
                } else {
                    var plan = result.result.plan();
                    if (plan.isEmpty() || plan.get().patterns().keySet().stream().anyMatch(p -> component.getProviderByPattern(p) == null)) {
                        failure(player, packet); return;
                    }
                    var provider = component.getProviderByPattern(plan.get().rootPattern());
                    if (provider == null) { failure(player, packet); return; }
                    // One ID, one provider and one persistent native task for the entire order.
                    var labelledPlan = BatchCraftingPlanner.withRecipeOutput(plan.get(), packet.recipeOutput, packet.multiplier);
                    var task = new TaskImpl(labelledPlan, new PlayerActor(player), packet.notifyPlayer);
                    provider.addTask(task);
                    LOGGER.debug("Started combined RS task {} for {} outputs: {}", task.getId(), packet.outputs.size(), packet.outputs);
                    if (session.accepted.size() >= 64) session.accepted.clear();
                    session.accepted.add(packet.id);
                    S2CPackets.sendAutocraftingResponse(player, packet.id, true);
                }
            }));
        } catch (RuntimeException rejected) {
            session.starting = false;
            failure(player, packet);
        }
    }

    private static long maximum(BatchAutocraftingComponentAccessor access, List<ResourceAmount> outputs, CancellationToken token) {
        return BatchCraftingPlanner.maxMultiplier(access.cointcore$batchPatterns(),
                access.cointcore$batchStorage().get(), outputs, token);
    }

    private static void failure(ServerPlayer player, Request request) {
        if (request.action == START) S2CPackets.sendAutocraftingResponse(player, request.id, false);
        else if (request.action == MAX) S2CPackets.sendAutocraftingPreviewMaxAmountResponse(player, 0);
        else S2CPackets.sendAutocraftingPreviewResponse(player, request.id, new Preview(PreviewType.NOT_AVAILABLE, List.of(), List.of()));
    }

    private static final class Session {
        private CancellationToken token;
        private int generation;
        private boolean starting;
        private final Set<UUID> accepted = new LinkedHashSet<>();
    }
    private record Calculation(BatchCraftingPlanner.Result result, long maximum) { }
}
