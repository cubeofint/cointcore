package com.mawlee.cointcore.keepinventory;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KeepInventoryService {
    private static final Map<UUID, KeepInventorySnapshot> PENDING = new ConcurrentHashMap<>();
    private static final Set<UUID> BLOCKED_DROPS = ConcurrentHashMap.newKeySet();

    private KeepInventoryService() {
    }

    public static boolean canKeepInventory(ServerPlayer player) {
        return PermissionService.has(player, CointPermissionNodes.KEEP_INVENTORY);
    }

    public static boolean hasPending(ServerPlayer player) {
        return PENDING.containsKey(player.getUUID());
    }

    public static boolean shouldBlockDeathDrops(ServerPlayer player) {
        return BLOCKED_DROPS.contains(player.getUUID());
    }

    public static void capture(ServerPlayer player) {
        if (!canKeepInventory(player)) {
            return;
        }

        CompoundTag data = new CompoundTag();
        for (KeepInventoryCaptureProvider provider : KeepInventoryCaptures.providers()) {
            provider.capture(player, data);
        }

        UUID playerId = player.getUUID();
        PENDING.put(playerId, new KeepInventorySnapshot(data));
        BLOCKED_DROPS.add(playerId);
    }

    public static void restore(ServerPlayer player) {
        KeepInventorySnapshot snapshot = PENDING.remove(player.getUUID());
        BLOCKED_DROPS.remove(player.getUUID());
        if (snapshot == null) {
            return;
        }

        for (KeepInventoryCaptureProvider provider : KeepInventoryCaptures.providers()) {
            provider.restore(player, snapshot.data());
        }

        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
        player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.KEEP_INVENTORY_RESTORED));
    }

    public static void clear(ServerPlayer player) {
        UUID playerId = player.getUUID();
        PENDING.remove(playerId);
        BLOCKED_DROPS.remove(playerId);
    }

    public static void clearRuntimeState() {
        PENDING.clear();
        BLOCKED_DROPS.clear();
    }
}
