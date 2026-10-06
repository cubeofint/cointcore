package com.mawlee.cointcore.mixin.enderio;

import com.enderio.enderio.api.conduits.network.ConduitNetwork;
import com.enderio.enderio.content.conduits.type.item.ItemConduitTicker;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.config.EnderIoItemConduitConfig;
import com.mawlee.cointcore.enderio.ItemConduitNetworkThrottle;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Soft-throttles pathological EnderIO item conduit networks (huge unstackable inventories).
 * Normal small networks keep moving items; large/idle ones run less often and scan fewer slots.
 */
@Mixin(value = ItemConduitTicker.class, remap = false)
public abstract class ItemConduitTickerMixin {
    @Unique
    private transient ItemConduitNetworkThrottle.State cointcore$state;

    @Unique
    private transient long cointcore$fingerprint;

    @Unique
    private transient boolean cointcore$moved;

    @Unique
    private transient int cointcore$slotWindow;

    @Inject(method = "tickNetwork", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$throttleNetwork(
            ServerLevel level,
            ConduitNetwork network,
            List<? extends Holder<?>> tickableConduits,
            CallbackInfo ci
    ) {
        if (!EnderIoItemConduitConfig.isEnabled()) {
            return;
        }

        int id = System.identityHashCode(network);
        ItemConduitNetworkThrottle.State state = ItemConduitNetworkThrottle.stateFor(id);
        long fingerprint = cointcore$networkFingerprint(network);
        var decision = ItemConduitNetworkThrottle.shouldRun(
                state,
                level.getGameTime(),
                EnderIoItemConduitConfig.getTickInterval(),
                EnderIoItemConduitConfig.getIdleBackoffTicks(),
                fingerprint,
                EnderIoItemConduitConfig.isSkipUnchangedInventories()
        );
        if (decision != ItemConduitNetworkThrottle.Decision.RUN) {
            ci.cancel();
            return;
        }

        this.cointcore$state = state;
        this.cointcore$fingerprint = fingerprint;
        this.cointcore$moved = false;
        this.cointcore$slotWindow = -1;
    }

    @Inject(method = "tickNetwork", at = @At("RETURN"), remap = false)
    private void cointcore$afterNetwork(
            ServerLevel level,
            ConduitNetwork network,
            List<? extends Holder<?>> tickableConduits,
            CallbackInfo ci
    ) {
        if (!EnderIoItemConduitConfig.isEnabled() || this.cointcore$state == null) {
            return;
        }
        ItemConduitNetworkThrottle.onPassComplete(
                this.cointcore$state,
                level.getGameTime(),
                this.cointcore$moved,
                EnderIoItemConduitConfig.getTickInterval(),
                EnderIoItemConduitConfig.getIdleBackoffTicks(),
                this.cointcore$fingerprint
        );
        this.cointcore$state = null;
    }

    @WrapOperation(
            method = "tickChannel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/items/IItemHandler;getSlots()I"
            ),
            remap = false
    )
    private int cointcore$capSlots(IItemHandler handler, Operation<Integer> original) {
        int slots = original.call(handler);
        if (!EnderIoItemConduitConfig.isEnabled() || this.cointcore$state == null) {
            return slots;
        }
        // First getSlots in a channel pass sets the window; subsequent isEmpty scans stay capped.
        if (this.cointcore$slotWindow < 0) {
            this.cointcore$slotWindow = ItemConduitNetworkThrottle.resolveSlotWindow(
                    this.cointcore$state,
                    slots,
                    EnderIoItemConduitConfig.getMaxSlotsPerPass()
            );
        }
        return Math.min(slots, Math.max(0, this.cointcore$slotWindow));
    }

    @WrapOperation(
            method = "tickChannel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/items/IItemHandler;extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;"
            ),
            remap = false
    )
    private ItemStack cointcore$trackExtract(
            IItemHandler handler,
            int slot,
            int amount,
            boolean simulate,
            Operation<ItemStack> original
    ) {
        ItemStack result = original.call(handler, slot, amount, simulate);
        if (!simulate && !result.isEmpty() && EnderIoItemConduitConfig.isEnabled()) {
            this.cointcore$moved = true;
        }
        return result;
    }

    @Unique
    private static long cointcore$networkFingerprint(ConduitNetwork network) {
        // Identity + channel count is enough to detect network rebuilds; slot sampling happens
        // via subsequent extract attempts when the throttle allows a pass.
        int channels = 0;
        try {
            channels = network.allChannels().size();
        } catch (RuntimeException ignored) {
            // Defensive: conduit API must not crash the ticker.
        }
        return ItemConduitNetworkThrottle.fingerprint(
                channels,
                System.identityHashCode(network),
                0L,
                0L
        );
    }
}
