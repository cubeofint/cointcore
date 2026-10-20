package com.mawlee.cointcore.command;

import com.mawlee.cointcore.chat.ChatChannel;
import com.mawlee.cointcore.chat.ChatChannelService;
import com.mawlee.cointcore.config.ChatChannelsConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Locale;

/** {@code /chat all|local|l|global|g|trade|t|pm}: which channel the player sees (and types into by default). */
public final class ChatViewCommand {
    private ChatViewCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("chat")
                .requires(source -> source.getEntity() instanceof ServerPlayer && ChatChannelsConfig.isEnabled());
        for (String alias : new String[]{"all", "local", "l", "global", "g", "trade", "t", "pm"}) {
            ChatChannel.View view = ChatChannel.View.parse(alias).orElseThrow();
            root.then(Commands.literal(alias.toLowerCase(Locale.ROOT)).executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                ChatChannelService.setView(player, view);
                return 1;
            }));
        }
        event.getDispatcher().register(root);
    }
}
