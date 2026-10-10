package com.mawlee.cointcore.shop;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;
import java.util.Locale;

/**
 * Blocks shulker boxes, filled containers, filled bundles/backpacks, and configured ids/tags.
 */
public final class GlobalMarketBlacklist {
    private GlobalMarketBlacklist() {
    }

    public static boolean forbidden(ItemStack stack, List<String> extraRules) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }
        if (matchesRule(stack, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), "#minecraft:shulker_boxes")) {
            return true;
        }
        if (hasContainerContents(stack) || hasBundleContents(stack) || hasBlockEntityInventory(stack)) {
            return true;
        }
        if (extraRules == null) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String idText = id.toString();
        if (idText.contains("shulker_box")) {
            return true;
        }
        for (String rule : extraRules) {
            if (matchesRule(stack, idText, rule)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesRule(ItemStack stack, String itemId, String rule) {
        if (rule == null || rule.isBlank()) {
            return false;
        }
        String trimmed = rule.trim();
        if (trimmed.startsWith("#")) {
            try {
                TagKey<Item> tag = TagKey.create(
                        BuiltInRegistries.ITEM.key(),
                        ResourceLocation.parse(trimmed.substring(1))
                );
                return stack.is(tag);
            } catch (RuntimeException ignored) {
                return false;
            }
        }
        return itemId.equals(trimmed) || itemId.toLowerCase(Locale.ROOT).equals(trimmed.toLowerCase(Locale.ROOT));
    }

    static boolean hasContainerContents(ItemStack stack) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null || contents.equals(ItemContainerContents.EMPTY)) {
            return false;
        }
        for (ItemStack inner : contents.nonEmptyItems()) {
            if (!inner.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    static boolean hasBundleContents(ItemStack stack) {
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        return contents != null && !contents.isEmpty();
    }

    static boolean hasBlockEntityInventory(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || data.isEmpty()) {
            return false;
        }
        CompoundTag tag = data.copyTag();
        return tag.contains("Items", Tag.TAG_LIST) && !tag.getList("Items", Tag.TAG_COMPOUND).isEmpty();
    }
}
