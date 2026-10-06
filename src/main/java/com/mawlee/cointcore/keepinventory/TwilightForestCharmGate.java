package com.mawlee.cointcore.keepinventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * Detects Twilight Forest Charm of Keeping storage so CointCore keep-inventory does not
 * overwrite TF's {@code TFCharmInventory} restore.
 * <p>
 * Capture runs at {@code LOW} priority so TF's {@code HIGH} handler can relocate items first.
 */
final class TwilightForestCharmGate {
    private static final String PLAYER_PERSISTED = "PlayerPersisted";
    private static final String CHARM_INV_TAG = "TFCharmInventory";
    private static final String CONSUMED_CHARM_TAG = "CharmStack";

    private TwilightForestCharmGate() {
    }

    static boolean shouldDeferToCharm(ServerPlayer player) {
        if (!ModList.get().isLoaded("twilightforest")) {
            return false;
        }
        CompoundTag persisted = player.getPersistentData().getCompound(PLAYER_PERSISTED);
        if (persisted.contains(CONSUMED_CHARM_TAG)) {
            return true;
        }
        if (!persisted.contains(CHARM_INV_TAG, Tag.TAG_LIST)) {
            return false;
        }
        ListTag list = persisted.getList(CHARM_INV_TAG, Tag.TAG_COMPOUND);
        return !list.isEmpty();
    }
}
