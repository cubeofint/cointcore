package com.mawlee.cointcore.privilege;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class FlightItemMatcher {
    private static final Pattern FLIGHT_ITEM_ID = Pattern.compile(
            ".*(?:jetpack|jet_pack|free_runner|free_runners|gravitite|graviton|"
                    + "flight|fly|flying|elytra|hover|wing|wings|levitation|antigravity|"
                    + "gravitational_modulator|cloud_in_a_bottle|angel_ring|creative_flight).*",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> FLIGHT_EFFECT_TOKENS = Set.of(
            "flight",
            "fly",
            "flying",
            "angel_wings",
            "antigravity",
            "airborne",
            "ascension",
            "glide",
            "levitate",
            "levitation"
    );

    private FlightItemMatcher() {
    }

    public static boolean isFlightItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null && FLIGHT_ITEM_ID.matcher(itemId.getPath()).matches();
    }

    public static boolean isFlightEffect(ResourceLocation effectId) {
        if (effectId == null) {
            return false;
        }

        String path = effectId.getPath().toLowerCase(Locale.ROOT);
        for (String token : FLIGHT_EFFECT_TOKENS) {
            if (path.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
