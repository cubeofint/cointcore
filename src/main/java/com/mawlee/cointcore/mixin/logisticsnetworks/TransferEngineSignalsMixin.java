package com.mawlee.cointcore.mixin.logisticsnetworks;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.config.LogisticsNetworksBudgetConfig;
import com.mawlee.cointcore.logisticsnetworks.PassSignalCache;
import me.almana.logisticsnetworks.data.ChannelMode;
import me.almana.logisticsnetworks.entity.LogisticsNodeEntity;
import me.almana.logisticsnetworks.logic.TransferEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(value = TransferEngine.class, remap = false)
public abstract class TransferEngineSignalsMixin {
    @Unique
    private static final ThreadLocal<PassSignalCache<ServerLevel>> cointcore$signals = new ThreadLocal<>();

    @WrapMethod(method = "buildSignalCache", require = 1)
    private static Map<UUID, Integer> cointcore$prepareSignals(List<LogisticsNodeEntity> nodes,
                                                               Operation<Map<UUID, Integer>> original) {
        if (!LogisticsNetworksBudgetConfig.isOptimizeSignals()) return original.call(nodes);
        // Native Logistics code discards every signal when there are no enabled exporters.
        // Decide that before reading the world, rather than after reading all nine channels on every node.
        boolean exporter = false;
        for (var node : nodes) {
            if (!node.isValidNode()) continue;
            for (int i = 0; i < LogisticsNodeEntity.CHANNEL_COUNT; i++) {
                var channel = node.getChannel(i);
                if (channel != null && channel.isEnabled() && channel.getMode() == ChannelMode.EXPORT) {
                    exporter = true;
                    break;
                }
            }
            if (exporter) break;
        }
        if (!exporter) return Collections.emptyMap();
        var previous = cointcore$signals.get();
        cointcore$signals.set(new PassSignalCache<>());
        try {
            return original.call(nodes);
        } finally {
            if (previous == null) cointcore$signals.remove();
            else cointcore$signals.set(previous);
        }
    }

    @WrapOperation(method = "buildSignalCache", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getBestNeighborSignal(Lnet/minecraft/core/BlockPos;)I"), require = 1)
    private static int cointcore$shareSignalForAttachedBlock(ServerLevel level, BlockPos pos, Operation<Integer> original) {
        var cache = cointcore$signals.get();
        return cache == null ? original.call(level, pos)
                : cache.get(level, pos.asLong(), () -> original.call(level, pos));
    }
}
