package com.mawlee.cointcore.kit;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbessentials.commands.impl.kit.KitCommand;
import dev.ftb.mods.ftbessentials.kit.Kit;
import dev.ftb.mods.ftbessentials.kit.KitManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class KitCreditService {
    private KitCreditService() {
    }

    public static Optional<Integer> tryHandleSelfClaim(
            CommandSourceStack source,
            String kitName,
            Collection<ServerPlayer> players
    ) throws CommandSyntaxException {
        if (players.size() != 1 || !(source.getEntity() instanceof ServerPlayer player) || !players.contains(player)) {
            return Optional.empty();
        }

        if (source.hasPermission(Commands.LEVEL_GAMEMASTERS)) {
            return Optional.empty();
        }

        int before = getCredits(source.getServer(), player.getUUID(), kitName);
        if (before <= 0) {
            return Optional.empty();
        }

        Kit kit = KitManager.getInstance().get(kitName).orElseThrow(() -> KitCommand.NO_SUCH_KIT.create(kitName));
        int remaining = takeCredits(source.getServer(), player.getUUID(), kitName, 1);
        if (remaining >= before) {
            return Optional.empty();
        }

        giveKitItems(kit, player);
        return Optional.of(remaining);
    }

    public static int getCredits(MinecraftServer server, UUID playerId, String kitName) {
        return KitCreditSavedData.get(server).getCredits(playerId, kitName);
    }

    public static int addCredits(MinecraftServer server, UUID playerId, String kitName, int amount) {
        return KitCreditSavedData.get(server).addCredits(playerId, kitName, amount);
    }

    public static int setCredits(MinecraftServer server, UUID playerId, String kitName, int amount) {
        return KitCreditSavedData.get(server).setCredits(playerId, kitName, amount);
    }

    public static int takeCredits(MinecraftServer server, UUID playerId, String kitName, int amount) {
        return KitCreditSavedData.get(server).takeCredits(playerId, kitName, amount);
    }

    public static Map<String, Integer> getAllCredits(MinecraftServer server, UUID playerId) {
        return KitCreditSavedData.get(server).getAllCredits(playerId);
    }

    public static void ensureKitExists(String kitName) throws CommandSyntaxException {
        if (KitManager.getInstance().get(kitName).isEmpty()) {
            throw KitCommand.NO_SUCH_KIT.create(kitName);
        }
    }

    public static Component formatKitName(String kitName) {
        return Component.literal(kitName);
    }

    private static void giveKitItems(Kit kit, ServerPlayer player) {
        for (ItemStack stack : kit.getItems()) {
            ItemStack copy = stack.copy();
            if (!player.getInventory().add(copy)) {
                ItemEntity itemEntity = player.drop(copy, false);
                if (itemEntity != null) {
                    itemEntity.setNoPickUpDelay();
                    itemEntity.setTarget(player.getUUID());
                }
            }
        }
    }
}
