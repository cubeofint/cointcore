package com.mawlee.cointcore.keepinventory.integrations;

import com.mawlee.cointcore.keepinventory.KeepInventoryCaptureProvider;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;

public final class AccessoriesKeepInventoryCapture implements KeepInventoryCaptureProvider {
    public static final String ID = "accessories";
    private static final String CONTAINERS = "Containers";
    private static final String COSMETIC_SUFFIX = "_cosmetic";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void capture(ServerPlayer player, CompoundTag root) {
        AccessoriesCapability capability = AccessoriesCapability.getOptionally(player).orElse(null);
        if (capability == null) {
            return;
        }

        HolderLookup.Provider registries = player.registryAccess();
        CompoundTag containers = new CompoundTag();
        capability.getContainers().forEach((slotName, container) -> {
            containers.put(slotName, container.getAccessories().createTag(registries));
            containers.put(slotName + COSMETIC_SUFFIX, container.getCosmeticAccessories().createTag(registries));
        });

        CompoundTag section = new CompoundTag();
        section.put(CONTAINERS, containers);
        root.put(ID, section);
    }

    @Override
    public void restore(ServerPlayer player, CompoundTag root) {
        if (!root.contains(ID)) {
            return;
        }

        AccessoriesCapability capability = AccessoriesCapability.getOptionally(player).orElse(null);
        if (capability == null) {
            return;
        }

        CompoundTag containers = root.getCompound(ID).getCompound(CONTAINERS);
        if (containers.isEmpty()) {
            return;
        }

        HolderLookup.Provider registries = player.registryAccess();
        capability.updateContainers();
        for (var entry : capability.getContainers().entrySet()) {
            String slotName = entry.getKey();
            AccessoriesContainer container = entry.getValue();
            restoreContainer(container.getAccessories(), containers, slotName, registries);
            restoreContainer(container.getCosmeticAccessories(), containers, slotName + COSMETIC_SUFFIX, registries);
            container.markChanged();
        }
    }

    private static void restoreContainer(
            ExpandedSimpleContainer container,
            CompoundTag containers,
            String key,
            HolderLookup.Provider registries
    ) {
        if (!containers.contains(key, ListTag.TAG_LIST)) {
            return;
        }

        container.fromTag(containers.getList(key, ListTag.TAG_COMPOUND), registries);
    }
}
