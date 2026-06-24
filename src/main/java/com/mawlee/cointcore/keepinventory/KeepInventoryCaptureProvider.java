package com.mawlee.cointcore.keepinventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public interface KeepInventoryCaptureProvider {
    String id();

    void capture(ServerPlayer player, CompoundTag root);

    void restore(ServerPlayer player, CompoundTag root);
}
