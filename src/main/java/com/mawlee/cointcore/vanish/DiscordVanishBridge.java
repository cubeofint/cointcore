package com.mawlee.cointcore.vanish;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Hides vanished players from Discord Chat Mod's pinned online status.
 */
public final class DiscordVanishBridge {
    private static final String DISCORD_MOD_ID = "discord_chat_mod";
    private static final String STATUS_CONTROLLER =
            "com.denisnumb.discord_chat_mod.discord.ServerStatusController";

    private static Method updateStatusMethod;
    private static boolean updateStatusResolved;

    private DiscordVanishBridge() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded(DISCORD_MOD_ID);
    }

    public static int visiblePlayerCount(MinecraftServer server) {
        if (server == null || server.getPlayerList() == null) {
            return 0;
        }

        int visible = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!VanishManager.isVanished(player)) {
                visible++;
            }
        }
        return visible;
    }

    public static String[] visiblePlayerNames(MinecraftServer server) {
        if (server == null || server.getPlayerList() == null) {
            return new String[0];
        }

        List<String> names = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!VanishManager.isVanished(player)) {
                names.add(player.getGameProfile().getName());
            }
        }
        return names.toArray(String[]::new);
    }

    public static void refreshStatus() {
        if (!isAvailable()) {
            return;
        }

        Method method = resolveUpdateStatusMethod();
        if (method == null) {
            return;
        }

        try {
            method.invoke(null);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Method resolveUpdateStatusMethod() {
        if (updateStatusResolved) {
            return updateStatusMethod;
        }

        updateStatusResolved = true;
        try {
            updateStatusMethod = Class.forName(STATUS_CONTROLLER)
                    .getMethod("updateServerStatusWithDelay");
        } catch (ReflectiveOperationException ignored) {
            updateStatusMethod = null;
        }
        return updateStatusMethod;
    }
}
