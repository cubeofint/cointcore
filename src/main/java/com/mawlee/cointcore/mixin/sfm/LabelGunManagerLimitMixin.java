package com.mawlee.cointcore.mixin.sfm;

import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.label.LabelGunManagerPushOrPullAction;
import ca.teamdman.sfm.common.label.LabelPositionHolder;
import ca.teamdman.sfm.common.net.ServerboundLabelGunUsePacket;
import ca.teamdman.sfm.common.registry.registration.SFMDataComponents;
import com.mawlee.cointcore.config.SfmPerfConfig;
import com.mawlee.cointcore.sfm.BoundBlockLimit;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LabelGunManagerPushOrPullAction.class, remap = false)
public abstract class LabelGunManagerLimitMixin {
    @Shadow @Final private Player player;
    @Shadow @Final private ManagerBlockEntity manager;
    @Shadow @Final private LabelPositionHolder gunLabels;
    @Shadow @Final private ServerboundLabelGunUsePacket msg;

    @Inject(method = "run", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$checkBoundBlocksBeforePush(CallbackInfo callback) {
        int limit = SfmPerfConfig.getMaxBoundBlocksPerManager();
        if (limit <= 0 || player.level().isClientSide || msg.isPullModifierActive()) return;
        var disk = manager.getDisk();
        if (disk == null || disk.isEmpty()) return;
        var existing = disk.getOrDefault(SFMDataComponents.LABEL_POSITION_HOLDER.get(), LabelPositionHolder.empty());
        if (!BoundBlockLimit.allowsReplacement(existing.labels().values(), gunLabels.labels().values(), limit)) {
            player.displayClientMessage(Component.translatable("message.cointcore.sfm.block_limit_rejected", limit), false);
            callback.cancel();
        }
    }
}
