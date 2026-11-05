package com.mawlee.cointcore.mixin.minecraft;

import com.mawlee.cointcore.villager.VillagerOfferSaveGuard;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AbstractVillager.class)
public abstract class VillagerOfferSaveGuardMixin {
    @Unique private int cointcore$lastInvalidOffers;

    @ModifyArg(method = "addAdditionalSaveData", at = @At(value = "INVOKE",
            target = "Lcom/mojang/serialization/Codec;encodeStart(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"),
            index = 1, require = 1)
    private Object cointcore$saveValidOffers(Object value) {
        var original = (MerchantOffers) value;
        var filtered = VillagerOfferSaveGuard.forSave(original);
        int invalid = original.size() - filtered.size();
        if (invalid > 0 && invalid != cointcore$lastInvalidOffers) {
            var villager = (AbstractVillager) (Object) this;
            LogUtils.getLogger().warn("Villager {} at {} {} has {} invalid trade(s); omitting those trades from the save snapshot. Valid trades and villager data are retained.",
                    villager.getUUID(), villager.level().dimension().location(), villager.blockPosition(), invalid);
        }
        cointcore$lastInvalidOffers = invalid;
        return filtered;
    }
}
