package com.mawlee.cointcore.command;

import com.mawlee.cointcore.industrialforegoingsouls.SoulSurgeAccelerationClaim;
import com.mawlee.cointcore.industrialforegoingsouls.SoulSurgeClaimTracker;
import com.mawlee.cointcore.industrialforegoingsouls.SoulSurgeDebug;
import com.mawlee.cointcore.industrialforegoingsouls.SoulSurgeFacing;
import com.mawlee.cointcore.industrialforegoingsouls.SoulSurgeSouls;
import com.mawlee.cointcore.industrialforegoingsouls.SoulSurgeTarget;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Admin inspect for Soul Surge facing / per-tick claims / soul supply.
 */
public final class SoulSurgeCommand {
    private SoulSurgeCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("soulsurge")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("debug")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(SoulSurgeCommand::debug)));
    }

    private static int debug(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        BlockState state = level.getBlockState(pos);
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        boolean surge = SoulSurgeTarget.isSoulSurgeId(id != null ? id.toString() : "");

        if (surge) {
            String facing = SoulSurgeTarget.normalizeFacing(SoulSurgeFacing.name(state));
            BlockPos target = SoulSurgeFacing.target(pos, state);
            boolean hasSouls = SoulSurgeSouls.hasSouls(level.getBlockEntity(pos));
            source.sendSuccess(
                    () -> Component.literal(SoulSurgeDebug.surgeLine(
                            pos.getX(), pos.getY(), pos.getZ(),
                            facing,
                            target.getX(), target.getY(), target.getZ(),
                            hasSouls
                    )),
                    false
            );
            return 1;
        }

        Long owner = SoulSurgeClaimTracker.peekOwnerPacked(level, pos);
        if (owner == null) {
            source.sendSuccess(
                    () -> Component.literal(SoulSurgeDebug.noOwnerLine(pos.getX(), pos.getY(), pos.getZ())),
                    false
            );
            return 1;
        }
        boolean active = SoulSurgeClaimTracker.isOwnerActive(level, pos, owner);
        source.sendSuccess(
                () -> Component.literal(SoulSurgeDebug.ownerLine(
                        pos.getX(), pos.getY(), pos.getZ(),
                        SoulSurgeAccelerationClaim.unpackX(owner),
                        SoulSurgeAccelerationClaim.unpackY(owner),
                        SoulSurgeAccelerationClaim.unpackZ(owner),
                        active
                )),
                false
        );
        return 1;
    }
}
