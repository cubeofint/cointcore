package com.mawlee.cointcore.ae;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;

public final class MeFilterTags {
    public static final String TAG_ID = "non_stackable";
    public static final ResourceLocation TAG_LOCATION = ResourceLocation.fromNamespaceAndPath(
            com.mawlee.cointcore.CointCore.MOD_ID,
            TAG_ID
    );
    public static final TagKey<Item> NON_STACKABLE = TagKey.create(Registries.ITEM, TAG_LOCATION);

    private MeFilterTags() {
    }
}
