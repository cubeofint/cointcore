package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.invsee.InvSeeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class InvSeeTabIcons {
    private InvSeeTabIcons() {
    }

    static ItemStack icon(InvSeeTab tab) {
        return switch (tab) {
            case INVENTORY -> new ItemStack(Items.CHEST);
            case ENDER -> new ItemStack(Items.ENDER_CHEST);
            case ACCESSORIES -> new ItemStack(Items.GOLDEN_CHESTPLATE);
            case FTB -> new ItemStack(Items.COMPASS);
            case GRAVES -> new ItemStack(Items.SKELETON_SKULL);
            case STATE -> new ItemStack(Items.CLOCK);
        };
    }
}
