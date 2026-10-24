package com.mawlee.cointcore.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mawlee.cointcore.config.EntityPerfConfig;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * {@code ChunkMap.move(ServerPlayer)} runs on every movement packet and re-checks every tracked entity of the level
 * against that player. Skip that loop (the chunk-tracking update after it still runs) until the player moved
 * {@code min_move_blocks} or {@code max_ticks} passed since the last full pass. Entities that cross a section are
 * re-checked for all players every tick by {@code ChunkMap.tick}, so only far-edge visibility can lag by about a second.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapTrackingThrottleMixin {
    @Unique
    private final Map<ServerPlayer, double[]> cointcore$lastFullPass = new WeakHashMap<>();

    @WrapOperation(
            method = "move(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;values()Lit/unimi/dsi/fastutil/objects/ObjectCollection;")
    )
    private ObjectCollection<?> cointcore$throttleTrackingPass(Int2ObjectMap<?> entityMap, Operation<ObjectCollection<?>> original,
                                                              @Local(argsOnly = true) ServerPlayer player) {
        if (!EntityPerfConfig.trackingEnabled()) {
            return original.call(entityMap);
        }
        double now = player.serverLevel().getGameTime();
        double[] last = cointcore$lastFullPass.get(player);
        if (last != null) {
            double dx = player.getX() - last[0];
            double dy = player.getY() - last[1];
            double dz = player.getZ() - last[2];
            if (dx * dx + dy * dy + dz * dz < EntityPerfConfig.trackingMinMoveSq()
                    && now - last[3] < EntityPerfConfig.trackingMaxTicks()
                    && now >= last[3]) {
                return ObjectLists.emptyList();
            }
        }
        cointcore$lastFullPass.put(player, new double[]{player.getX(), player.getY(), player.getZ(), now});
        return original.call(entityMap);
    }
}
