package com.mawlee.cointcore;

import com.mawlee.cointcore.ae.NonStackableItemTagPack;
import com.mawlee.cointcore.command.ChatSpyCommand;
import com.mawlee.cointcore.command.CointCoreCommand;
import com.mawlee.cointcore.command.IgnoreCommand;
import com.mawlee.cointcore.command.ModCommands;
import com.mawlee.cointcore.command.NightVisionCommand;
import com.mawlee.cointcore.command.VanishCommand;
import com.mawlee.cointcore.ftb.FtbTeamPropertyRegistration;
import com.mawlee.cointcore.permission.PermissionRegistration;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(value = CointCore.MOD_ID)
public class CointCore {
    public static final String MOD_ID = "cointcore";

    public CointCore(IEventBus modEventBus) {
        modEventBus.addListener(NonStackableItemTagPack::registerPackFinder);

        FtbTeamPropertyRegistration.register();

        NeoForge.EVENT_BUS.addListener(VanishCommand::register);
        NeoForge.EVENT_BUS.addListener(ChatSpyCommand::register);
        NeoForge.EVENT_BUS.addListener(NightVisionCommand::register);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (RegisterCommandsEvent event) -> ModCommands.register(event));
        NeoForge.EVENT_BUS.addListener(IgnoreCommand::register);
        NeoForge.EVENT_BUS.addListener(CointCoreCommand::register);
        NeoForge.EVENT_BUS.addListener(PermissionRegistration::registerNodes);
        NeoForge.EVENT_BUS.addListener(CointCoreEvents::onServerStarting);
        NeoForge.EVENT_BUS.addListener(CointCoreEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(CointCoreEvents::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(CointCoreEvents::onPlayerLoggedOut);
    }
}
