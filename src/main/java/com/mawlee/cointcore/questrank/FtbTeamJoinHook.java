package com.mawlee.cointcore.questrank;

import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.PlayerJoinedPartyTeamEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.function.Consumer;

/**
 * Instant party-join hook compiled against the FTB Teams API.
 *
 * <p>{@code TeamEvent.PLAYER_JOINED_PARTY} is Architectury
 * {@code Event<Consumer<PlayerJoinedPartyTeamEvent>>} ({@code createConsumerLoop}).
 * The listener is a real {@code Consumer} method reference — the event's interface type.
 * Do not use {@code java.lang.reflect.Proxy}: Architectury {@code EventActor} buses
 * (FTB Quests {@code ObjectCompletedEvent}) ClassCastException on a proxy (#49).
 *
 * <p>Register is invoked reflectively so Architectury need not be on the compile
 * classpath (same pattern as {@link com.mawlee.cointcore.ftb.FtbTeamPropertyRegistration}).
 * The handler only enqueues {@code server.execute(syncTeam)}.
 */
public final class FtbTeamJoinHook {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean registered;

    private FtbTeamJoinHook() {
    }

    public static void register() {
        if (registered || !ModList.get().isLoaded("ftbteams")) {
            return;
        }
        try {
            Class<?> teamEventClass = Class.forName("dev.ftb.mods.ftbteams.api.event.TeamEvent");
            Object bus = teamEventClass.getField("PLAYER_JOINED_PARTY").get(null);
            Class<?> eventClass = Class.forName("dev.architectury.event.Event");
            Consumer<PlayerJoinedPartyTeamEvent> listener = FtbTeamJoinHook::onPlayerJoinedParty;
            eventClass.getMethod("register", Object.class).invoke(bus, listener);
            registered = true;
            LOGGER.info("Quest-rank FTB Teams PLAYER_JOINED_PARTY hook registered");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            LOGGER.info("Quest-rank FTB Teams join hook skipped: {}", exception.toString());
        }
    }

    private static void onPlayerJoinedParty(PlayerJoinedPartyTeamEvent event) {
        if (event == null) {
            return;
        }
        ServerPlayer player = event.getPlayer();
        Team team = event.getTeam();
        if (player == null || team == null) {
            return;
        }
        MinecraftServer server = player.server;
        if (server == null) {
            return;
        }
        server.execute(() -> QuestRankService.syncTeam(server, team, QuestRankTeamSyncLogic.REASON_TEAM_JOIN));
    }
}
