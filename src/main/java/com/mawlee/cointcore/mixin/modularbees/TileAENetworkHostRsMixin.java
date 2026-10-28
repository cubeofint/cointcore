package com.mawlee.cointcore.mixin.modularbees;

import appeng.api.networking.IManagedGridNode;
import appeng.api.storage.MEStorage;
import com.mawlee.cointcore.modularbees.RsHatchConnection;
import com.mawlee.cointcore.modularbees.RsHatchHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.glodblock.github.modularbees.xmod.ae.tileentities.TileAENetworkHost", remap = false)
public abstract class TileAENetworkHostRsMixin implements RsHatchHost {
    @Shadow protected abstract Direction getFacing();
    @Shadow public abstract IManagedGridNode getMainNode();
    @Unique private RsHatchConnection cointcore$rsConnection;

    @Inject(method = "<init>", at = @At("TAIL"), require = 1)
    private void cointcore$createRsNode(BlockEntityType<?> type, BlockPos pos, BlockState state, CallbackInfo callback) {
        cointcore$rsConnection = new RsHatchConnection((BlockEntity) (Object) this, this::getFacing, this::getMainNode);
    }

    @Override public RsHatchConnection cointcore$rsHatch() { return cointcore$rsConnection; }

    @Inject(method = "getMEStorage", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$routeToRs(CallbackInfoReturnable<MEStorage> callback) {
        if (cointcore$rsConnection != null && cointcore$rsConnection.isPreferred())
            callback.setReturnValue(cointcore$rsConnection.storage());
    }

    @Inject(method = "onReady", at = @At("TAIL"), require = 1)
    private void cointcore$initializeRs(CallbackInfo callback) { cointcore$rsConnection.initialize(); }

    @Inject(method = {"onChunkUnloaded", "setRemoved"}, at = @At("HEAD"), require = 1)
    private void cointcore$removeRs(CallbackInfo callback) {
        if (cointcore$rsConnection != null) cointcore$rsConnection.remove();
    }

    @Inject(method = "onGridConnectableSidesChanged", at = @At("TAIL"), require = 1)
    private void cointcore$rotateRs(CallbackInfo callback) { cointcore$rsConnection.update(); }
}
