package com.mawlee.cointcore.ae;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;

public final class MeFilterTags {
    public static final String TAG_ID = "non_stackable";
    /** Short path so AE2 search can fit {@code #cointcore:ns_heavy} / {@code #ns_heavy}. */
    public static final String MERGED_TAG_ID = "ns_heavy";
    /** Legacy path kept only for old datapacks / search aliases. */
    public static final String MERGED_TAG_ID_LEGACY = "non_stackable_merged";

    public static final ResourceLocation TAG_LOCATION = ResourceLocation.fromNamespaceAndPath(
            com.mawlee.cointcore.CointCore.MOD_ID,
            TAG_ID
    );
    public static final ResourceLocation MERGED_TAG_LOCATION = ResourceLocation.fromNamespaceAndPath(
            com.mawlee.cointcore.CointCore.MOD_ID,
            MERGED_TAG_ID
    );

    public static final TagKey<Item> NON_STACKABLE = TagKey.create(Registries.ITEM, TAG_LOCATION);
    public static final TagKey<Item> NON_STACKABLE_MERGED = TagKey.create(Registries.ITEM, MERGED_TAG_LOCATION);

    private MeFilterTags() {
    }
}
