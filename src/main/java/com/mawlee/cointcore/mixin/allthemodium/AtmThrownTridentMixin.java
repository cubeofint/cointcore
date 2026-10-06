package com.mawlee.cointcore.mixin.allthemodium;

import com.thevortex.allthemodium.entity.ThrownTrident;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Allthemodium's alloy trident sticks in blocks and never returns when monsters are within
 * the chain radius, and on a successful return it {@code discard()}s without giving the item
 * back. Force loyalty-style return once embedded, and restore the item to the owner.
 */
@Mixin(value = ThrownTrident.class, remap = false)
public abstract class AtmThrownTridentMixin extends AbstractArrow {

    @Shadow
    private boolean dealtDamage;

    @Shadow
    private boolean returning;

    @Shadow
    public abstract ItemStack getPickupItem();

    private AtmThrownTridentMixin(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void cointcore$returnWhenEmbedded(CallbackInfo ci) {
        if (this.inGround && this.inGroundTime > 4) {
            this.dealtDamage = true;
            this.returning = true;
        }
        if (this.returning) {
            this.inGround = false;
            this.setNoPhysics(true);
        }
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/thevortex/allthemodium/entity/ThrownTrident;discard()V",
                    ordinal = 0
            )
    )
    private void cointcore$restoreItemOnReturn(CallbackInfo ci) {
        if (!this.returning) {
            return;
        }
        if (this.level().isClientSide) {
            return;
        }
        Entity owner = this.getOwner();
        if (!(owner instanceof Player player) || player.hasInfiniteMaterials()) {
            return;
        }
        ItemStack stack = this.getPickupItem();
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
