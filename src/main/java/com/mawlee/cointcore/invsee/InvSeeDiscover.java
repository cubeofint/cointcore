package com.mawlee.cointcore.invsee;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Discovers optional InvSee targets for command suggestions (not UI tabs).
 */
public final class InvSeeDiscover {
    private static final List<String> ATTACHMENT_PREFIXES = List.of(
            "aether:",
            "irons_spellbooks:",
            "travelersbackpack:"
    );

    private InvSeeDiscover() {
    }

    public static List<String> pocketIds(Player target) {
        if (!ModList.get().isLoaded("pocketstorage")) {
            return List.of();
        }
        try {
            Class<?> integration = Class.forName(
                    "com.mawlee.cointcore.invsee.integrations.PocketStorageInvSeeDiscover"
            );
            @SuppressWarnings("unchecked")
            List<String> ids = (List<String>) integration.getMethod("listIds", Player.class).invoke(null, target);
            return ids;
        } catch (ReflectiveOperationException exception) {
            return List.of();
        }
    }

    public static List<String> backpackKeys(Player target) {
        if (!ModList.get().isLoaded("sophisticatedbackpacks")) {
            return List.of();
        }
        try {
            Class<?> integration = Class.forName(
                    "com.mawlee.cointcore.invsee.integrations.BackpackInvSeeDiscover"
            );
            @SuppressWarnings("unchecked")
            List<String> keys = (List<String>) integration.getMethod("listKeys", Player.class).invoke(null, target);
            return keys;
        } catch (ReflectiveOperationException exception) {
            return List.of();
        }
    }

    public static List<String> attachmentKeys(Player target, HolderLookup.Provider registries) {
        CompoundTag attachments = target.serializeAttachments(registries);
        List<String> keys = new ArrayList<>();
        for (String key : attachments.getAllKeys()) {
            if (shouldShowAttachment(key)) {
                keys.add(key);
            }
        }
        keys.sort(Comparator.naturalOrder());
        return keys;
    }

    private static boolean shouldShowAttachment(String key) {
        for (String prefix : ATTACHMENT_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
