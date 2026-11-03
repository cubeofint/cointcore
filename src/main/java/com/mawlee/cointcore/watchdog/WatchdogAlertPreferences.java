package com.mawlee.cointcore.watchdog;

import com.mawlee.cointcore.CointCore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Only event/command handlers touch player NBT; notification threads read the concurrent snapshot. */
@EventBusSubscriber(modid = CointCore.MOD_ID)
public final class WatchdogAlertPreferences {
    private static final String KEY = "cointcore_watchdog_alerts";

    private WatchdogAlertPreferences() { }

    public static void set(ServerPlayer player, boolean enabled) {
        var root = player.getPersistentData();
        var persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(KEY, enabled);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
        WatchdogAlertSubscriptions.set(player.getUUID(), enabled);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WatchdogAlertSubscriptions.set(player.getUUID(),
                    player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(KEY));
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            set(player, event.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(KEY));
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) WatchdogAlertSubscriptions.set(player.getUUID(), false);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { WatchdogAlertSubscriptions.clear(); }
}
