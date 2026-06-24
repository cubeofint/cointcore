package com.mawlee.cointcore.vanish;

import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

public final class VanishService {
    private VanishService() {
    }

    public static boolean toggle(ServerPlayer player) {
        boolean vanished = !VanishManager.isVanished(player);
        setVanished(player, vanished);
        return vanished;
    }

    public static boolean toggleMobs(ServerPlayer player) {
        boolean hiddenFromMobs = !VanishManager.isHiddenFromMobs(player);
        VanishManager.setHiddenFromMobs(player, hiddenFromMobs);
        applyMobTargetState(player);
        player.sendSystemMessage(CointCoreMessages.forPlayer(player,
                hiddenFromMobs ? CointCoreMessages.VANISH_MOBS_DISABLED : CointCoreMessages.VANISH_MOBS_ENABLED));
        return !hiddenFromMobs;
    }

    public static void setVanished(ServerPlayer player, boolean vanished) {
        VanishManager.setVanished(player, vanished);
        applyState(player, vanished);
        applyMobTargetState(player);
        VanishServerStatus.invalidate(player.server);
        player.sendSystemMessage(CointCoreMessages.forPlayer(player,
                vanished ? CointCoreMessages.VANISH_ENABLED : CointCoreMessages.VANISH_DISABLED));
    }

    public static void onPlayerJoin(ServerPlayer joining) {
        VanishManager.loadFromStorage(joining);

        PlayerList playerList = joining.server.getPlayerList();

        if (VanishManager.isVanished(joining)) {
            applyState(joining, true);
            applyMobTargetState(joining);
            VanishServerStatus.invalidate(joining.server);
        }

        for (ServerPlayer online : playerList.getPlayers()) {
            if (online == joining) {
                continue;
            }

            if (VanishManager.isVanished(online)) {
                hideFrom(online, joining);
            } else if (VanishManager.isVanished(joining)) {
                hideFrom(joining, online);
            }
        }
    }

    public static void applyMobTargetState(ServerPlayer player) {
        if (!VanishManager.isVanished(player)) {
            return;
        }

        if (VanishManager.canMobsTarget(player)) {
            return;
        }

        clearMobTargets(player);
    }

    private static void clearMobTargets(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(64.0D))) {
            if (entity instanceof Mob mob && player.equals(mob.getTarget())) {
                mob.setTarget(null);
            }
        }
    }

    private static void applyState(ServerPlayer player, boolean vanished) {
        player.setInvisible(vanished);
        if (vanished) {
            player.removeEffect(MobEffects.GLOWING);
            player.setGlowingTag(false);
        }
        player.refreshTabListName();

        ServerLevel level = player.serverLevel();
        PlayerList playerList = player.server.getPlayerList();

        if (vanished) {
            for (ServerPlayer other : playerList.getPlayers()) {
                if (other != player) {
                    hideFrom(player, other);
                }
            }
        } else {
            for (ServerPlayer other : playerList.getPlayers()) {
                if (other != player) {
                    showTo(player, other);
                }
            }
        }

        refreshEntityTracking(player, level);
    }

    public static void hideFrom(ServerPlayer vanished, ServerPlayer viewer) {
        if (!VanishVisibility.isHiddenFrom(viewer, vanished)) {
            return;
        }

        viewer.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(vanished.getUUID())));
        viewer.connection.send(new ClientboundRemoveEntitiesPacket(vanished.getId()));
    }

    public static void showTo(ServerPlayer player, ServerPlayer viewer) {
        if (VanishManager.shouldHideFrom(player, viewer)) {
            return;
        }

        viewer.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player)));
        refreshEntityTracking(player, player.serverLevel());
    }

    private static void refreshEntityTracking(ServerPlayer player, ServerLevel level) {
        var chunkMap = level.getChunkSource().chunkMap;
        if (chunkMap.entityMap.containsKey(player.getId())) {
            chunkMap.entityMap.remove(player.getId());
            level.getChunkSource().addEntity(player);
        }
    }
}
