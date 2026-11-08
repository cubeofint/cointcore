package com.mawlee.cointcore.refinedstorage.multicrafter;

import com.refinedmods.refinedstorage.api.autocrafting.Pattern;
import com.ultramega.refinedfluidsubstitution.common.fluidsubstitutionpattern.FluidSubstitutionPatternItem;
import com.ultramega.refinedfluidsubstitution.common.fluidsubstitutionpattern.FluidSubstitutionPatternResolver;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.List;
import java.util.Optional;

/**
 * Refined Fluid Substitution patterns, resolved the way that mod does for RS autocrafters: the pattern with fluid
 * ingredients instead of buckets, plus helper patterns the provider must also expose. Only touches the mod's classes
 * when it is installed.
 */
final class FluidSubstitutionCompat {
    private static final boolean LOADED = ModList.get().isLoaded("refinedfluidsubstitution");

    record Resolved(Pattern pattern, List<Pattern> helpers) {
    }

    private FluidSubstitutionCompat() {
    }

    static boolean isSubstitutionPattern(ItemStack stack) {
        return LOADED && Impl.isSubstitutionPattern(stack);
    }

    static Optional<Resolved> resolve(ItemStack stack, Level level) {
        return LOADED ? Impl.resolve(stack, level) : Optional.empty();
    }

    private static final class Impl {
        static boolean isSubstitutionPattern(ItemStack stack) {
            return stack.getItem() instanceof FluidSubstitutionPatternItem;
        }

        static Optional<Resolved> resolve(ItemStack stack, Level level) {
            if (!isSubstitutionPattern(stack)) return Optional.empty();
            return FluidSubstitutionPatternResolver.resolve(stack, level)
                    .map(resolved -> new Resolved(resolved.pattern(), resolved.helperPatterns()));
        }
    }
}
