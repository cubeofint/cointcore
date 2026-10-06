package com.mawlee.cointcore.justdirethings;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * Bosses the polymorphic wand must neither copy nor replace.
 * {@code c:bosses} is the pack-wide set. The wand's own deny tag still covers
 * the wither, the ender dragon, and the warden.
 */
public final class PolymorphBossGuard {
    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("c", "bosses")
    );
    private static final TagKey<EntityType<?>> WAND_DENY = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("justdirethings", "polymorphic_target_deny")
    );

    private PolymorphBossGuard() {
    }

    public static boolean isBoss(EntityType<?> type) {
        return type != null && (type.is(BOSSES) || type.is(WAND_DENY));
    }
}
