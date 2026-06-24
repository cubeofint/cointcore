package com.mawlee.cointcore.command;

import com.mawlee.cointcore.chat.AdminChatService;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class AdminChatCommand {
    private static final List<String> ALIASES = List.of("a", "adminchat", "ac");

    private AdminChatCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String alias : ALIASES) {
            CommandRegistration.replaceRootLiteral(dispatcher, alias, adminChatCommand(alias));
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> adminChatCommand(String name) {
        return Commands.literal(name)
                .requires(AdminChatCommand::canUse)
                .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(AdminChatCommand::sendMessage));
    }

    private static boolean canUse(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player
                && PermissionService.has(player, CointPermissionNodes.ADMIN_CHAT);
    }

    private static int sendMessage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        String text = StringArgumentType.getString(context, "text");
        return AdminChatService.send(sender, text) ? 1 : 0;
    }
}
