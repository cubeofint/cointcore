package com.mawlee.cointcore.ftb;

import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;

import java.util.Optional;
import java.util.UUID;

public final class FtbIntegration {
    private FtbIntegration() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded("ftbchunks")
                && ModList.get().isLoaded("ftbteams")
                && FTBChunksAPI.api().isManagerLoaded()
                && FTBTeamsAPI.api().isManagerLoaded();
    }

    public static Optional<ChunkTeamData> getClaimTeamData(ServerLevel level, BlockPos pos) {
        if (!isAvailable()) {
            return Optional.empty();
        }

        ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
        ClaimedChunk chunk = manager.getChunk(new ChunkDimPos(level.dimension(), new ChunkPos(pos)));
        if (chunk == null) {
            return Optional.empty();
        }

        return Optional.of(chunk.getTeamData());
    }

    public static Optional<Team> resolveTeam(MinecraftServer server, String target) {
        if (!isAvailable()) {
            return Optional.empty();
        }

        TeamManager teamManager = FTBTeamsAPI.api().getManager();
        Optional<Team> byName = teamManager.getTeamByName(target);
        if (byName.isPresent()) {
            return byName;
        }

        ServerPlayer online = server.getPlayerList().getPlayerByName(target);
        if (online != null) {
            return teamManager.getTeamForPlayer(online);
        }

        return Optional.empty();
    }

    public static Optional<Team> getTeam(UUID teamId) {
        if (!isAvailable()) {
            return Optional.empty();
        }

        return FTBTeamsAPI.api().getManager().getTeamByID(teamId);
    }

    public static boolean hasBypassProtection(ServerPlayer player) {
        if (!isAvailable()) {
            return false;
        }

        return FTBChunksAPI.api().getManager().getBypassProtection(player.getUUID());
    }

    public static boolean shouldPreventLivingEntityInteraction(ServerPlayer player, LivingEntity target) {
        if (!isAvailable() || !(target.level() instanceof ServerLevel)) {
            return false;
        }

        ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
        return manager.shouldPreventInteraction(
                player,
                InteractionHand.MAIN_HAND,
                target.blockPosition(),
                Protection.INTERACT_ENTITY,
                target
        );
    }

    public static boolean shouldPreventBlockInteraction(
            Entity actor,
            Level level,
            BlockPos pos,
            Protection protection
    ) {
        if (!isAvailable() || !(level instanceof ServerLevel) || actor == null) {
            return false;
        }

        if (actor instanceof ServerPlayer player && hasBypassProtection(player)) {
            return false;
        }

        ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
        return manager.shouldPreventInteraction(
                actor,
                InteractionHand.MAIN_HAND,
                pos,
                protection,
                null
        );
    }

    public static boolean canEditBlock(Entity actor, Level level, BlockPos pos) {
        return !shouldPreventBlockInteraction(actor, level, pos, Protection.EDIT_BLOCK);
    }

    public static boolean canInteractBlock(Entity actor, Level level, BlockPos pos) {
        return !shouldPreventBlockInteraction(actor, level, pos, Protection.INTERACT_BLOCK);
    }

    public static boolean canEditBox(Entity actor, Level level, AABB box) {
        if (!isAvailable() || actor == null) {
            return true;
        }

        int minX = (int) Math.floor(box.minX);
        int minY = (int) Math.floor(box.minY);
        int minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.floor(box.maxX);
        int maxY = (int) Math.floor(box.maxY);
        int maxZ = (int) Math.floor(box.maxZ);

        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            if (!canEditBlock(actor, level, pos)) {
                return false;
            }
        }

        return true;
    }
}
