package com.mawlee.cointcore.speedrunner;

import com.mawlee.cointcore.config.PrefixConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import com.mawlee.cointcore.luckperms.LuckPermsUsers;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

public final class SpeedrunnerService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private SpeedrunnerService() {
    }

    public static boolean isRecordItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return PrefixConfig.get().speedrunnerItem().equals(id.toString());
    }

    public static int playTimeTicks(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM, Stats.PLAY_TIME);
    }

    public static void onCrafted(ServerPlayer player, ItemStack result) {
        if (!PrefixConfig.get().speedrunnerEnabled() || !isRecordItem(result)) {
            return;
        }
        tryClaim(player.server, player.getUUID(), player.getGameProfile().getName(), playTimeTicks(player), true, false);
    }

    public static Optional<SpeedrunnerSavedData.Record> info(MinecraftServer server) {
        return SpeedrunnerSavedData.get(server).record();
    }

    public static void reset(MinecraftServer server, Runnable after) {
        SpeedrunnerSavedData data = SpeedrunnerSavedData.get(server);
        Optional<SpeedrunnerSavedData.Record> previous = data.record();
        data.clear();
        if (previous.isEmpty() || !LuckPermsIntegration.isAvailable()) {
            after.run();
            return;
        }
        LuckPermsUsers.removeGroup(previous.get().holderId(), PrefixConfig.get().speedrunnerGroup())
                .whenComplete((ignored, error) -> server.execute(() -> {
                    if (error != null) {
                        LOGGER.warn("Failed to remove speedrunner group from {}", previous.get().holderId(), error);
                    }
                    after.run();
                }));
    }

    public static void adminSet(MinecraftServer server, UUID playerId, String name, int ticks, Runnable after) {
        tryClaim(server, playerId, name, ticks, false, true);
        server.execute(after);
    }

    static void tryClaim(
            MinecraftServer server,
            UUID playerId,
            String name,
            int ticks,
            boolean broadcast,
            boolean force
    ) {
        if (!PrefixConfig.get().speedrunnerEnabled() && !force) {
            return;
        }
        SpeedrunnerSavedData data = SpeedrunnerSavedData.get(server);
        Optional<SpeedrunnerSavedData.Record> previous = data.record();
        if (!force) {
            if (previous.isPresent() && previous.get().holderId().equals(playerId) && !data.isBetter(ticks)) {
                return;
            }
            if (!data.isBetter(ticks) && previous.isPresent() && !previous.get().holderId().equals(playerId)) {
                return;
            }
        }
        UUID previousId = previous.map(SpeedrunnerSavedData.Record::holderId).orElse(null);
        data.set(playerId, name, ticks, System.currentTimeMillis());
        if (LuckPermsIntegration.isAvailable()) {
            String group = PrefixConfig.get().speedrunnerGroup();
            if (previousId != null && !previousId.equals(playerId)) {
                LuckPermsUsers.removeGroup(previousId, group).whenComplete((ignored, error) -> {
                    if (error != null) {
                        LOGGER.warn("Failed to remove speedrunner group from previous holder {}", previousId, error);
                    }
                });
            }
            LuckPermsUsers.addGroup(playerId, group).whenComplete((ignored, error) -> {
                if (error != null) {
                    LOGGER.warn("Failed to grant speedrunner group to {}", playerId, error);
                }
            });
        }
        LOGGER.info("Speedrunner record: {} ({}) in {} ticks", name, playerId, ticks);
        if (broadcast) {
            broadcast(server, name, ticks);
        }
    }

    private static void broadcast(MinecraftServer server, String name, int ticks) {
        String time = SpeedrunnerTime.formatPlayTime(ticks);
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            online.sendSystemMessage(CointCoreMessages.forPlayer(
                    online,
                    CointCoreMessages.SPEEDRUNNER_BROADCAST,
                    name,
                    time
            ));
        }
    }

}
