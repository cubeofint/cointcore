package com.mawlee.cointcore.keepinventory.integrations;

import com.mawlee.cointcore.keepinventory.KeepInventoryCaptureProvider;
import com.mawlee.cointcore.mixin.accessor.AttachmentHolderAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class AttachmentKeepInventoryCapture implements KeepInventoryCaptureProvider {
    public static final String ID = "attachments";

    private static final List<String> ATTACHMENT_PREFIXES = List.of(
            "aether:",
            "irons_spellbooks:",
            "sophisticatedbackpacks:",
            "travelersbackpack:"
    );

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void capture(ServerPlayer player, CompoundTag root) {
        HolderLookup.Provider registries = player.registryAccess();
        CompoundTag allAttachments = player.serializeAttachments(registries);
        CompoundTag filtered = new CompoundTag();

        for (String key : allAttachments.getAllKeys()) {
            if (shouldCaptureAttachment(key)) {
                filtered.put(key, allAttachments.get(key));
            }
        }

        if (!filtered.isEmpty()) {
            root.put(ID, filtered);
        }
    }

    @Override
    public void restore(ServerPlayer player, CompoundTag root) {
        if (!root.contains(ID)) {
            return;
        }

        CompoundTag saved = root.getCompound(ID);
        if (saved.isEmpty()) {
            return;
        }

        HolderLookup.Provider registries = player.registryAccess();
        CompoundTag merged = player.serializeAttachments(registries);
        for (String key : saved.getAllKeys()) {
            merged.put(key, saved.get(key));
        }
        ((AttachmentHolderAccess) player).cointcore$deserializeAttachments(registries, merged);
    }

    private static boolean shouldCaptureAttachment(String key) {
        for (String prefix : ATTACHMENT_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
