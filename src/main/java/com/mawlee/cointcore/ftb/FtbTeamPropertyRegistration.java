package com.mawlee.cointcore.ftb;

import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbteams.api.event.TeamCollectPropertiesEvent;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.function.Consumer;

public final class FtbTeamPropertyRegistration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean registered;

    private FtbTeamPropertyRegistration() {
    }

    public static void register() {
        if (registered || !ModList.get().isLoaded("ftbteams") || !ModList.get().isLoaded("ftbchunks")) {
            return;
        }

        CointCoreFtbProperties.init();
        registerCollectPropertiesHandler(FtbTeamPropertyRegistration::collectProperties);
        registered = true;
    }

    private static void collectProperties(TeamCollectPropertiesEvent event) {
        event.add(CointCoreFtbProperties.DISABLE_HOSTILE_MOB_SPAWN);
        event.add(CointCoreFtbProperties.DISABLE_PLAYER_DAMAGE);
        event.add(CointCoreFtbProperties.PROTECT_MOBS_FROM_OUTSIDERS);
    }

    @SuppressWarnings("unchecked")
    private static void registerCollectPropertiesHandler(Consumer<TeamCollectPropertiesEvent> handler) {
        try {
            Class<?> teamEventClass = Class.forName("dev.ftb.mods.ftbteams.api.event.TeamEvent");
            Object collectEvent = teamEventClass.getField("COLLECT_PROPERTIES").get(null);
            Class<?> eventInterface = Class.forName("dev.architectury.event.Event");
            eventInterface.getMethod("register", Object.class).invoke(collectEvent, handler);
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("Failed to register CointCore FTB team properties", exception);
        }
    }
}
