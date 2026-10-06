package com.mawlee.cointcore.mixin.evilcraft;

import com.mojang.logging.LogUtils;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.fluids.FluidStack;
import org.cyclops.cyclopscore.network.PacketCodec;
import org.cyclops.evilcraft.network.packet.UpdateWorldSharedTankClientCachePacket;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hardens evilcraft:update_world_shared_tank_client_cache against FluidStack
 * IndexOutOfBoundsException on decode (components / truncated payload).
 * <p>
 * Wire order must match Cyclops PacketCodec: @CodecField names are sorted
 * alphabetically, so fluidStack is written/read before tankID.
 * Encode strips data components (fluid + amount only). Decode swallows corrupt
 * FluidStack payloads and falls back to EMPTY instead of disconnecting.
 */
@Mixin(value = PacketCodec.class, remap = false)
public abstract class SharedTankPacketCodecMixin {
    @Unique
    private static final Logger COINTCORE$LOGGER = LogUtils.getLogger();

    @Inject(method = "encode", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$encodeSharedTank(RegistryFriendlyByteBuf buf, CallbackInfo ci) {
        if (!((Object) this instanceof UpdateWorldSharedTankClientCachePacket)) {
            return;
        }

        ci.cancel();
        UpdateWorldSharedTankPacketAccessor packet = (UpdateWorldSharedTankPacketAccessor) this;
        // Alphabetical CodecField order: fluidStack, then tankID.
        FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, cointcore$sanitizeFluid(packet.cointcore$getFluidStack()));
        String tankID = packet.cointcore$getTankID();
        buf.writeUtf(tankID == null ? "" : tankID);
    }

    @Inject(method = "decode", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$decodeSharedTank(RegistryFriendlyByteBuf buf, CallbackInfo ci) {
        if (!((Object) this instanceof UpdateWorldSharedTankClientCachePacket)) {
            return;
        }

        ci.cancel();
        UpdateWorldSharedTankPacketAccessor packet = (UpdateWorldSharedTankPacketAccessor) this;

        try {
            packet.cointcore$setFluidStack(FluidStack.OPTIONAL_STREAM_CODEC.decode(buf));
        } catch (IndexOutOfBoundsException | DecoderException | IllegalArgumentException | IllegalStateException exception) {
            COINTCORE$LOGGER.warn(
                    "Failed to decode evilcraft shared tank FluidStack; using EMPTY (readable={})",
                    buf.readableBytes(),
                    exception
            );
            buf.readerIndex(buf.writerIndex());
            packet.cointcore$setFluidStack(FluidStack.EMPTY);
            packet.cointcore$setTankID("");
            return;
        }

        try {
            packet.cointcore$setTankID(buf.readUtf(32767));
        } catch (IndexOutOfBoundsException | DecoderException | IllegalArgumentException | IllegalStateException exception) {
            COINTCORE$LOGGER.warn(
                    "Failed to decode evilcraft shared tank tankID; discarding id (readable={})",
                    buf.readableBytes(),
                    exception
            );
            buf.readerIndex(buf.writerIndex());
            packet.cointcore$setTankID("");
        }
    }

    @Unique
    private static FluidStack cointcore$sanitizeFluid(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        // Drop components: they are the usual source of client/server patch skew.
        return new FluidStack(stack.getFluidHolder(), stack.getAmount());
    }
}
