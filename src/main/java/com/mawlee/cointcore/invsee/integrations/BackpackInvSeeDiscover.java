package com.mawlee.cointcore.invsee.integrations;

import com.mawlee.cointcore.invsee.InvSeePlayerStacks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;

import java.util.ArrayList;
import java.util.List;

public final class BackpackInvSeeDiscover {
    private BackpackInvSeeDiscover() {
    }

    public static List<String> listKeys(Player target) {
        List<String> keys = new ArrayList<>();
        for (InvSeePlayerStacks.LocatedStack located : InvSeePlayerStacks.collect(target, BackpackInvSeeDiscover::isBackpack)) {
            keys.add(InvSeePlayerStacks.encodeLocation(located));
        }
        return keys;
    }

    private static boolean isBackpack(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BackpackItem;
    }
}
