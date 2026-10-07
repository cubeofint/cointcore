package com.mawlee.cointcore;

import com.mawlee.cointcore.afk.AfkNetwork;
import com.mawlee.cointcore.ae.NonStackableItemTagPack;
import com.mawlee.cointcore.ars.ArsGlyphEvents;
import com.mawlee.cointcore.command.ChatSpyCommand;
import com.mawlee.cointcore.command.CointCoreCommand;
import com.mawlee.cointcore.command.IgnoreCommand;
import com.mawlee.cointcore.command.ModCommands;
import com.mawlee.cointcore.command.NightVisionCommand;
import com.mawlee.cointcore.command.VanishCommand;
import com.mawlee.cointcore.claim.ClaimBufferService;
import com.mawlee.cointcore.claim.ClaimFlagEditNetwork;
import com.mawlee.cointcore.chunklimit.ClaimLimitSync;
import com.mawlee.cointcore.ftb.FtbTeamPropertyRegistration;
import com.mawlee.cointcore.permission.PermissionRegistration;
import com.mawlee.cointcore.seeinvisible.SeeInvisibleNetwork;
import com.mawlee.cointcore.invsee.InvSeeMenus;
import com.mawlee.cointcore.shop.ShopBlocks;
import com.mawlee.cointcore.shop.ShopMenus;
import com.mawlee.cointcore.shop.TraderNetwork;
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
        modEventBus.addListener(SeeInvisibleNetwork::register);
        modEventBus.addListener(ClaimFlagEditNetwork::register);
        modEventBus.addListener(AfkNetwork::register);
        modEventBus.addListener(TraderNetwork::register);
        InvSeeMenus.REGISTER.register(modEventBus);
        ShopBlocks.register(modEventBus);
        ShopMenus.register(modEventBus);

        FtbTeamPropertyRegistration.register();
        ClaimBufferService.register();
        ClaimLimitSync.register();

        NeoForge.EVENT_BUS.addListener(VanishCommand::register);
        NeoForge.EVENT_BUS.addListener(ChatSpyCommand::register);
        NeoForge.EVENT_BUS.addListener(NightVisionCommand::register);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (RegisterCommandsEvent event) -> ModCommands.register(event));
        NeoForge.EVENT_BUS.addListener(IgnoreCommand::register);
        NeoForge.EVENT_BUS.addListener(CointCoreCommand::register);
        NeoForge.EVENT_BUS.addListener(PermissionRegistration::registerNodes);
        NeoForge.EVENT_BUS.addListener(CointCoreEvents::onServerStarting);
        NeoForge.EVENT_BUS.addListener(CointCoreEvents::onServerStarted);
        NeoForge.EVENT_BUS.addListener(CointCoreEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(CointCoreEvents::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(CointCoreEvents::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(CointCoreEvents::onTabListNameFormat);
        NeoForge.EVENT_BUS.addListener(ArsGlyphEvents::onTagsUpdated);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) ->
                {
                    com.mawlee.cointcore.shop.SiteOperationPoller.tick(event.getServer());
                    com.mawlee.cointcore.shop.SiteMovementSender.tick(event.getServer());
                    com.mawlee.cointcore.shop.TraderPriceHistorySampler.tick(event.getServer());
                });
    }
}
