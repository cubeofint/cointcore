package com.mawlee.cointcore.kit;

import com.mawlee.cointcore.config.StarterKitConfig;
import com.mawlee.cointcore.ftb.FtbEssentialsIntegration;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbessentials.kit.Kit;
import dev.ftb.mods.ftbessentials.kit.KitManager;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * First-join grant + reclaim for a configured FTB Essentials kit.
 * Cooldown is owned by FTB ({@link FTBEPlayerData}); auto_grant is forced off.
 */
public final class StarterKitService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private StarterKitService() {
    }

    public static boolean isAvailable() {
        return FtbEssentialsIntegration.isAvailable();
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (!isAvailable()) {
            return;
        }
        StarterKitConfig.Settings settings = StarterKitConfig.get();
        if (!settings.enabled() || !settings.firstJoinEnabled()) {
            return;
        }

        StarterKitSavedData data = StarterKitSavedData.get(player.server);
        if (data.hasReceivedFirstJoin(player.getUUID())) {
            return;
        }

        Optional<Kit> kit = KitManager.getInstance().get(settings.kitName());
        if (kit.isEmpty() || kit.get().getItems().isEmpty()) {
            LOGGER.debug("Starter kit '{}' missing or empty; skip first-join for {}", settings.kitName(), player.getGameProfile().getName());
            return;
        }

        try {
            ensureAutoGrantOff(kit.get());
            KitManager.getInstance().giveKitToPlayer(settings.kitName(), player);
            data.markFirstJoinReceived(player.getUUID());
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.STARTER_KIT_FIRST_JOIN, settings.kitName()));
        } catch (CommandSyntaxException exception) {
            LOGGER.warn(
                    "Failed to grant starter kit '{}' to {} on first join: {}",
                    settings.kitName(),
                    player.getGameProfile().getName(),
                    exception.getRawMessage().getString()
            );
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error granting starter kit on first join to {}", player.getGameProfile().getName(), exception);
        }
    }

    public static ClaimResult claim(ServerPlayer player) {
        if (!isAvailable()) {
            return ClaimResult.fail(CointCoreMessages.STARTER_KIT_FTB_MISSING);
        }
        StarterKitConfig.Settings settings = StarterKitConfig.get();
        if (!settings.enabled()) {
            return ClaimResult.fail(CointCoreMessages.STARTER_KIT_DISABLED);
        }

        Optional<Kit> kit = KitManager.getInstance().get(settings.kitName());
        if (kit.isEmpty() || kit.get().getItems().isEmpty()) {
            return ClaimResult.fail(CointCoreMessages.STARTER_KIT_MISSING, settings.kitName());
        }

        try {
            ensureAutoGrantOff(kit.get());
            KitManager.getInstance().giveKitToPlayer(settings.kitName(), player);
            StarterKitSavedData.get(player.server).markFirstJoinReceived(player.getUUID());
            return ClaimResult.ok(settings.kitName());
        } catch (CommandSyntaxException exception) {
            return ClaimResult.failRaw(exception.getRawMessage().getString());
        }
    }

    public static SetFromInvResult setFromInventory(ServerPlayer admin) {
        if (!isAvailable()) {
            return SetFromInvResult.fail(CointCoreMessages.STARTER_KIT_FTB_MISSING);
        }

        StarterKitConfig.Settings settings = StarterKitConfig.get();
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : admin.getInventory().items) {
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
        if (items.isEmpty()) {
            return SetFromInvResult.fail(CointCoreMessages.STARTER_KIT_EMPTY_INV);
        }

        try {
            Kit kit = new Kit(settings.kitName(), items, settings.cooldownSeconds(), false);
            KitManager.getInstance().addKit(kit, true);
            return SetFromInvResult.ok(settings.kitName(), items.size(), settings.cooldownSeconds());
        } catch (CommandSyntaxException exception) {
            return SetFromInvResult.failRaw(exception.getRawMessage().getString());
        }
    }

    public static boolean syncCooldownFromConfig() {
        if (!isAvailable()) {
            return false;
        }
        StarterKitConfig.Settings settings = StarterKitConfig.get();
        Optional<Kit> existing = KitManager.getInstance().get(settings.kitName());
        if (existing.isEmpty()) {
            return false;
        }
        try {
            Kit updated = existing.get().withCooldown(settings.cooldownSeconds()).withAutoGrant(false);
            KitManager.getInstance().addKit(updated, true);
            return true;
        } catch (CommandSyntaxException exception) {
            LOGGER.warn("Failed to sync starter kit cooldown: {}", exception.getRawMessage().getString());
            return false;
        }
    }

    public static String statusSummary(ServerPlayer viewer) {
        StarterKitConfig.Settings settings = StarterKitConfig.get();
        if (!isAvailable()) {
            return "ftbessentials=missing";
        }
        Optional<Kit> kit = KitManager.getInstance().get(settings.kitName());
        boolean firstJoin = StarterKitSavedData.get(viewer.server).hasReceivedFirstJoin(viewer.getUUID());
        long kitCd = kit.map(Kit::getCooldown).orElse(-1L);
        int itemCount = kit.map(value -> value.getItems().size()).orElse(0);
        return "enabled=" + settings.enabled()
                + ", kit=" + settings.kitName()
                + ", items=" + itemCount
                + ", configCooldown=" + settings.cooldownSeconds() + "s"
                + ", kitCooldown=" + kitCd + "s"
                + ", firstJoinDone=" + firstJoin
                + ", firstJoinEnabled=" + settings.firstJoinEnabled();
    }

    private static void ensureAutoGrantOff(Kit kit) throws CommandSyntaxException {
        if (!kit.isAutoGrant()) {
            return;
        }
        KitManager.getInstance().addKit(kit.withAutoGrant(false), true);
        LOGGER.info("Disabled FTB auto_grant on starter kit '{}' (CointCore owns first-join)", kit.getKitName());
    }

    public record ClaimResult(boolean success, String messageKey, Object[] args, String rawMessage) {
        public static ClaimResult ok(String kitName) {
            return new ClaimResult(true, CointCoreMessages.STARTER_KIT_CLAIMED, new Object[]{kitName}, null);
        }

        public static ClaimResult fail(String messageKey, Object... args) {
            return new ClaimResult(false, messageKey, args, null);
        }

        public static ClaimResult failRaw(String raw) {
            return new ClaimResult(false, null, new Object[0], raw);
        }
    }

    public record SetFromInvResult(boolean success, String messageKey, Object[] args, String rawMessage) {
        public static SetFromInvResult ok(String kitName, int itemCount, long cooldownSeconds) {
            return new SetFromInvResult(
                    true,
                    CointCoreMessages.STARTER_KIT_SET_FROM_INV,
                    new Object[]{kitName, itemCount, cooldownSeconds},
                    null
            );
        }

        public static SetFromInvResult fail(String messageKey, Object... args) {
            return new SetFromInvResult(false, messageKey, args, null);
        }

        public static SetFromInvResult failRaw(String raw) {
            return new SetFromInvResult(false, null, new Object[0], raw);
        }
    }
}
