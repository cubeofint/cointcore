package com.mawlee.cointcore.mixin.justdirethings;

import com.direwolf20.justdirethings.common.items.datacomponents.JustDireDataComponents;
import com.direwolf20.justdirethings.common.items.interfaces.AbilityMethods;
import com.direwolf20.justdirethings.util.MiscTools;
import com.mawlee.cointcore.justdirethings.PolymorphBossGuard;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Random polymorph only accepts the wand's vanilla lists. Targeted polymorph
 * replaces any mob and can roll a stored boss. Refuse both directions.
 */
@Mixin(value = AbilityMethods.class, remap = false)
public abstract class PolymorphBossDenyMixin {
    private static final double LOOK_RANGE = 4.0D;

    @Inject(method = "polymorphRandom", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$blockBossRandom(
            Level level,
            Player player,
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (refuseLookedAtBoss(level, player, null)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "polymorphTarget", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$blockBossTarget(
            Level level,
            Player player,
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (refuseLookedAtBoss(level, player, storedType(stack))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getRandomMobTypeByCategory", at = @At("RETURN"), cancellable = true, remap = false)
    private static void cointcore$dropBossRoll(
            Level level,
            boolean peaceful,
            CallbackInfoReturnable<EntityType<?>> cir
    ) {
        if (PolymorphBossGuard.isBoss(cir.getReturnValue())) {
            cir.setReturnValue(null);
        }
    }

    private static boolean refuseLookedAtBoss(Level level, Player player, EntityType<?> replacement) {
        if (level.isClientSide()) {
            return false;
        }
        Entity looked = MiscTools.getEntityLookedAt(player, LOOK_RANGE);
        if (looked == null) {
            return false;
        }
        if (!PolymorphBossGuard.isBoss(looked.getType()) && !PolymorphBossGuard.isBoss(replacement)) {
            return false;
        }
        player.displayClientMessage(Component.translatable("justdirethings.invalidpolymorphentity"), true);
        return true;
    }

    private static EntityType<?> storedType(ItemStack stack) {
        DataComponentType<String> component = JustDireDataComponents.ENTITIYTYPE.get();
        if (!stack.has(component)) {
            return null;
        }
        String id = stack.get(component);
        if (id == null || id.isEmpty()) {
            return null;
        }
        return EntityType.byString(id).orElse(null);
    }
}
