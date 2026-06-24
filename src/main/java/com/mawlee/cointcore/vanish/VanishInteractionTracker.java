package com.mawlee.cointcore.vanish;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VanishInteractionTracker {
    private static final Pair<BlockPos, UUID> EMPTY_INTERACTION = Pair.of(null, null);
    private static final Map<UUID, Pair<BlockPos, UUID>> INTERACTIONS = new ConcurrentHashMap<>();
    private static Pair<Packet<?>, UUID> packetOrigin;

    private VanishInteractionTracker() {
    }

    public static void trackVanished(ServerPlayer player, boolean vanished) {
        if (vanished) {
            INTERACTIONS.put(player.getUUID(), EMPTY_INTERACTION);
        } else {
            INTERACTIONS.remove(player.getUUID());
        }
        cleanupOffline(player.server.getPlayerList());
    }

    public static void updateBlockInteraction(ServerPlayer player, BlockHitResult hitResult) {
        updateBlockInteraction(player, hitResult.getBlockPos());
    }

    public static void updateBlockInteraction(ServerPlayer player, BlockPos blockPos) {
        if (!VanishManager.isVanished(player)) {
            return;
        }

        UUID uuid = player.getUUID();
        Pair<BlockPos, UUID> previous = INTERACTIONS.getOrDefault(uuid, EMPTY_INTERACTION);
        INTERACTIONS.put(uuid, Pair.of(blockPos, previous.getSecond()));
    }

    public static void updateEntityInteraction(ServerPlayer player, Entity entity) {
        if (!VanishManager.isVanished(player)) {
            return;
        }

        INTERACTIONS.put(player.getUUID(), Pair.of(entity.blockPosition(), entity.getUUID()));
    }

    public static void clearInteraction(ServerPlayer player) {
        if (VanishManager.isVanished(player)) {
            INTERACTIONS.put(player.getUUID(), EMPTY_INTERACTION);
        }
    }

    public static void rememberBroadcastSource(Packet<?> packet, Player source) {
        packetOrigin = Pair.of(packet, source.getUUID());
    }

    public static Player resolveBroadcastSource(Packet<?> packet, PlayerList playerList) {
        if (packetOrigin != null && packetOrigin.getFirst() == packet) {
            return playerList.getPlayer(packetOrigin.getSecond());
        }
        return null;
    }

    public static Player findHiddenCause(Player directSource, Level level, Vec3 origin, ServerPlayer receiver) {
        if (directSource instanceof ServerPlayer player && VanishVisibility.isHiddenFrom(receiver, player)) {
            return player;
        }

        Player interacted = findInteractedPlayer(level, origin, receiver);
        if (interacted != null) {
            return interacted;
        }

        Player atPosition = findVanishedAt(level, origin, receiver);
        if (atPosition != null) {
            return atPosition;
        }

        return findProjectileOwnerAt(level, origin, receiver);
    }

    public static ServerPlayer findHiddenCauseForBlock(Level level, BlockPos blockPos, ServerPlayer receiver) {
        for (Map.Entry<UUID, Pair<BlockPos, UUID>> entry : INTERACTIONS.entrySet()) {
            ServerPlayer player = receiver.server.getPlayerList().getPlayer(entry.getKey());
            Pair<BlockPos, UUID> interaction = entry.getValue();
            if (player == null || !hasBlockInteraction(interaction) || !VanishVisibility.isHiddenFrom(receiver, player)) {
                continue;
            }
            if (matchesBlockInteraction(level, blockPos, interaction.getFirst())) {
                return player;
            }
        }
        return null;
    }

    public static Player findHiddenCause(Player directSource, Level level, Entity originEntity, ServerPlayer receiver) {
        if (directSource instanceof ServerPlayer player && VanishVisibility.isHiddenFrom(receiver, player)) {
            return player;
        }

        if (originEntity == null) {
            return null;
        }

        Player interacted = findInteractedEntity(level, originEntity, receiver);
        if (interacted != null) {
            return interacted;
        }

        Player atPosition = findVanishedAt(level, originEntity.position(), receiver);
        if (atPosition != null) {
            return atPosition;
        }

        if (originEntity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer owner
                && VanishVisibility.isHiddenFrom(receiver, owner)) {
            return owner;
        }

        return findVanishedVehicle(originEntity, receiver);
    }

    private static Player findInteractedPlayer(Level level, Vec3 pos, ServerPlayer receiver) {
        for (Map.Entry<UUID, Pair<BlockPos, UUID>> entry : INTERACTIONS.entrySet()) {
            ServerPlayer player = receiver.server.getPlayerList().getPlayer(entry.getKey());
            Pair<BlockPos, UUID> interaction = entry.getValue();
            if (player == null || !hasBlockInteraction(interaction) || !VanishVisibility.isHiddenFrom(receiver, player)) {
                continue;
            }
            if (matchesBlockInteraction(level, BlockPos.containing(pos.x, pos.y, pos.z), interaction.getFirst())) {
                return player;
            }
        }
        return null;
    }

    private static Player findInteractedEntity(Level level, Entity entity, ServerPlayer receiver) {
        for (Map.Entry<UUID, Pair<BlockPos, UUID>> entry : INTERACTIONS.entrySet()) {
            ServerPlayer player = receiver.server.getPlayerList().getPlayer(entry.getKey());
            Pair<BlockPos, UUID> interaction = entry.getValue();
            if (player == null || interaction.getSecond() == null || !VanishVisibility.isHiddenFrom(receiver, player)) {
                continue;
            }
            if (entity.getUUID().equals(interaction.getSecond())) {
                return player;
            }
        }
        return null;
    }

    private static Player findVanishedAt(Level level, Vec3 pos, ServerPlayer receiver) {
        VoxelShape shape = Shapes.block().move(pos.x - 0.5D, pos.y - 0.5D, pos.z - 0.5D);
        for (UUID uuid : INTERACTIONS.keySet()) {
            ServerPlayer player = receiver.server.getPlayerList().getPlayer(uuid);
            if (player == null || player.level() != level || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR
                    || !VanishVisibility.isHiddenFrom(receiver, player)) {
                continue;
            }
            if (Shapes.joinIsNotEmpty(shape, Shapes.create(player.getBoundingBox()), BooleanOp.AND)) {
                return player;
            }
        }
        return null;
    }

    private static Player findProjectileOwnerAt(Level level, Vec3 pos, ServerPlayer receiver) {
        AABB area = AABB.ofSize(pos, 1.0D, 1.0D, 1.0D);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, area, projectile -> true);
        if (projectiles.isEmpty()) {
            return null;
        }

        for (UUID uuid : INTERACTIONS.keySet()) {
            ServerPlayer player = receiver.server.getPlayerList().getPlayer(uuid);
            if (player == null || player.level() != level || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR
                    || !VanishVisibility.isHiddenFrom(receiver, player)) {
                continue;
            }
            boolean ownsProjectile = projectiles.stream().anyMatch(projectile -> player.equals(projectile.getOwner()));
            if (ownsProjectile) {
                return player;
            }
        }
        return null;
    }

    private static Player findVanishedVehicle(Entity entity, ServerPlayer receiver) {
        for (UUID uuid : INTERACTIONS.keySet()) {
            ServerPlayer player = receiver.server.getPlayerList().getPlayer(uuid);
            if (player == null || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR
                    || !VanishVisibility.isHiddenFrom(receiver, player)) {
                continue;
            }
            Entity vehicle = player.getVehicle();
            if (vehicle != null && vehicle.equals(entity)) {
                return player;
            }
        }
        return null;
    }

    private static boolean hasBlockInteraction(Pair<BlockPos, UUID> interaction) {
        return interaction != null && interaction.getFirst() != null;
    }

    private static boolean matchesBlockInteraction(Level level, BlockPos eventPos, BlockPos interactPos) {
        if (interactPos == null) {
            return false;
        }

        if (eventPos.equals(interactPos)) {
            return true;
        }

        BlockState state = level.getBlockState(interactPos);
        if (state.getBlock() instanceof ChestBlock) {
            BlockPos connected = interactPos.relative(ChestBlock.getConnectedDirection(state));
            return eventPos.equals(connected);
        }

        return false;
    }

    public static void clear() {
        INTERACTIONS.clear();
        packetOrigin = null;
    }

    private static void cleanupOffline(PlayerList playerList) {
        List<UUID> offline = new ArrayList<>();
        for (UUID uuid : INTERACTIONS.keySet()) {
            if (playerList.getPlayer(uuid) == null) {
                offline.add(uuid);
            }
        }
        offline.forEach(INTERACTIONS::remove);
    }
}
