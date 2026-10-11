package com.mawlee.cointcore.kit;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
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
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class KitCreditService {
    private static final Logger LOGGER = LogUtils.getLogger();

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

    /**
     * AzLink reflection API. Stable signature: {@code (UUID, String, int)}.
     * Credits never expire. Safe from any thread: off-thread calls are scheduled
     * onto the server thread and return {@link KitCreditGrant#SCHEDULED}.
     *
     * @return {@code applied}, {@code scheduled}, {@code no_server}, or {@code invalid}
     */
    public static String addCredits(UUID uuid, String kit, int qty) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        boolean sameThread = server != null && server.isSameThread();
        KitCreditGrant.Decision decision = KitCreditGrant.decide(uuid, kit, qty, server != null, sameThread);
        if (decision.schedule()) {
            server.execute(() -> applyBridgeGrant(server, uuid, kit, qty));
            return decision.status();
        }
        if (decision.applyNow()) {
            applyBridgeGrant(server, uuid, kit, qty);
        }
        return decision.status();
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

    public static Map<String, Integer> clearAll(MinecraftServer server, UUID playerId) {
        return KitCreditSavedData.get(server).clearAll(playerId);
    }

    public static int clearKit(MinecraftServer server, UUID playerId, String kitName) {
        return KitCreditSavedData.get(server).clearKit(playerId, kitName);
    }

    public static void ensureKitExists(String kitName) throws CommandSyntaxException {
        if (KitManager.getInstance().get(kitName).isEmpty()) {
            throw KitCommand.NO_SUCH_KIT.create(kitName);
        }
    }

    public static Component formatKitName(String kitName) {
        return Component.literal(kitName);
    }

    private static void applyBridgeGrant(MinecraftServer server, UUID uuid, String kit, int qty) {
        int total = KitCreditSavedData.get(server).addCredits(uuid, kit, qty);
        LOGGER.info("AzLink granted {} use(s) of kit {} to {} (total {})", qty, kit, uuid, total);
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
