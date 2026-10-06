package com.mawlee.cointcore.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ModCommands {
    private ModCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        PrivateMessageCommand.applyMessageCommands(dispatcher);
        PrivateMessageCommand.applyReplyCommand(dispatcher);
        MuteCommand.apply(dispatcher);
        BanCommand.apply(dispatcher);
        TplCommand.apply(dispatcher);
        WarnCommand.apply(dispatcher);
        PunishmentsCommand.apply(dispatcher);
        TurnPvpCommand.apply(dispatcher);
        UnbanCommand.apply(dispatcher);
        InvSeeCommand.apply(dispatcher);
        BalanceCommand.apply(dispatcher);
        PayCommand.apply(dispatcher);
        VoteCommand.apply(dispatcher);
        AdminChatCommand.apply(dispatcher);
    }
}
