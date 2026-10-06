package com.mawlee.cointcore.invsee.integrations;

import com.flanks255.pocketstorage.util.PSUtils;
import com.mawlee.cointcore.invsee.InvSeePlayerStacks;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PocketStorageInvSeeDiscover {
    private PocketStorageInvSeeDiscover() {
    }

    public static List<String> listIds(Player target) {
        Set<UUID> seen = new LinkedHashSet<>();
        for (InvSeePlayerStacks.LocatedStack located : InvSeePlayerStacks.collect(target, stack -> !stack.isEmpty())) {
            PSUtils.getUUID(located.stack()).ifPresent(seen::add);
        }
        List<String> ids = new ArrayList<>(seen.size());
        for (UUID uuid : seen) {
            ids.add(uuid.toString());
        }
        return ids;
    }
}
