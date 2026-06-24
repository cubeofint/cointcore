package com.mawlee.cointcore.command;

import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.vote.VoteService;
import com.mawlee.cointcore.vote.VoteType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class VoteCommand {
    private VoteCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        register(dispatcher, "voteday", VoteType.DAY, CointPermissionNodes.VOTE_DAY);
        register(dispatcher, "voteclearweather", VoteType.CLEAR_WEATHER, CointPermissionNodes.VOTE_CLEAR_WEATHER);
        register(dispatcher, "votesun", VoteType.CLEAR_WEATHER, CointPermissionNodes.VOTE_CLEAR_WEATHER);
    }

    private static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            String name,
            VoteType type,
            net.neoforged.neoforge.server.permission.nodes.PermissionNode<Boolean> permission
    ) {
        dispatcher.register(
                Commands.literal(name)
                        .requires(source -> canUse(source, permission))
                        .executes(context -> vote(context, type))
        );
    }

    private static boolean canUse(
            CommandSourceStack source,
            net.neoforged.neoforge.server.permission.nodes.PermissionNode<Boolean> permission
    ) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, permission);
        }
        return false;
    }

    private static int vote(CommandContext<CommandSourceStack> context, VoteType type) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return VoteService.castVote(type, player);
    }
}
