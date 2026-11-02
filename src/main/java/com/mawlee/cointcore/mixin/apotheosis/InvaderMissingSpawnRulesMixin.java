package com.mawlee.cointcore.mixin.apotheosis;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.util.Optional;

/** Explicit gateway summons can create invaders in dimensions without natural invader spawn rules. */
@Pseudo
@Mixin(targets = "dev.shadowsoffire.apotheosis.mobs.types.Invader", remap = false)
public abstract class InvaderMissingSpawnRulesMixin {
    @WrapOperation(method = "initBoss", at = @At(value = "INVOKE",
            target = "Ldev/shadowsoffire/apotheosis/mobs/InvaderSpawnRules;cursed()Ljava/util/Optional;"), require = 1)
    private Optional<Boolean> cointcore$defaultCurseWithoutRules(@Coerce Object rules,
                                                                Operation<Optional<Boolean>> original) {
        // initBoss already falls back to AdventureConfig.curseBossItems for an empty override.
        // Do not add natural-spawn rules or change any configured dimension overrides.
        return rules == null ? Optional.empty() : original.call(rules);
    }
}
