package com.mawlee.cointcore.ftbessentials;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbessentials.commands.impl.kit.KitCommand;
import dev.ftb.mods.ftbessentials.kit.Kit;
import dev.ftb.mods.ftbessentials.kit.KitManager;
import dev.ftb.mods.ftbessentials.util.BlockUtil;
import dev.ftb.mods.ftbessentials.util.InventoryUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Overwrites an existing FTB kit's item list while keeping cooldown and auto-grant.
 * FTB only ships create ({@code overwrite=false}) / delete; update uses {@code addKit(..., true)}.
 */
public final class KitUpdateService {
    private KitUpdateService() {
    }

    public static void updateFromPlayerInv(String kitName, ServerPlayer player, boolean hotbarOnly)
            throws CommandSyntaxException {
        Kit existing = requireExisting(kitName);
        NonNullList<ItemStack> source;
        if (hotbarOnly) {
            source = NonNullList.create();
            for (int i = 0; i < 9; i++) {
                source.add(player.getInventory().items.get(i));
            }
        } else {
            source = player.getInventory().items;
        }
        replaceItems(existing, source);
    }

    public static void updateFromBlockInv(String kitName, ServerPlayer player) throws CommandSyntaxException {
        Kit existing = requireExisting(kitName);
        BlockHitResult hit = BlockUtil.getFocusedBlock(player, 5.5D)
                .orElseThrow(KitCommand.NOT_LOOKING_AT_BLOCK::create);
        List<ItemStack> items = InventoryUtil.getItemsInInventory(
                player.level(),
                hit.getBlockPos(),
                hit.getDirection()
        );
        replaceItems(existing, items);
    }

    private static Kit requireExisting(String kitName) throws CommandSyntaxException {
        return KitManager.getInstance().get(kitName)
                .orElseThrow(() -> KitCommand.NO_SUCH_KIT.create(kitName));
    }

    private static void replaceItems(Kit existing, List<ItemStack> source) throws CommandSyntaxException {
        List<ItemStack> items = source.stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
        if (items.isEmpty()) {
            throw KitCommand.NO_ITEMS_TO_ADD.create();
        }
        KitManager.getInstance().addKit(
                new Kit(existing.getKitName(), items, existing.getCooldown(), existing.isAutoGrant()),
                true
        );
    }
}
