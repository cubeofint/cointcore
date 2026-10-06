package com.mawlee.cointcore.claim;

import com.mawlee.cointcore.config.ClaimBufferConfig;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * Enforces a Chebyshev gap of N free chunks between claims of different FTB teams.
 * Registers via reflection so Architectury need not be on the compile classpath
 * ({@code ftb-chunks} is {@code transitive = false}).
 */
public final class ClaimBufferService {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String PROBLEM_KEY = "message.cointcore.claim.buffer";

    private static boolean registered;

    private ClaimBufferService() {
    }

    public static void register() {
        if (registered || !ModList.get().isLoaded("ftbchunks")) {
            return;
        }
        try {
            Class<?> eventClass = Class.forName("dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent");
            Object beforeClaimEvent = eventClass.getField("BEFORE_CLAIM").get(null);
            Class<?> beforeInterface = Class.forName("dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent$Before");
            Class<?> architecturyEvent = Class.forName("dev.architectury.event.Event");

            Object handler = Proxy.newProxyInstance(
                    beforeInterface.getClassLoader(),
                    new Class<?>[]{beforeInterface},
                    (proxy, method, args) -> {
                        if ("before".equals(method.getName()) && args != null && args.length == 2) {
                            return beforeClaim((CommandSourceStack) args[0], (ClaimedChunk) args[1]);
                        }
                        return switch (method.getName()) {
                            case "toString" -> "ClaimBufferBeforeHandler";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> Boolean.valueOf(proxy == args[0]);
                            default -> passResult();
                        };
                    }
            );

            architecturyEvent.getMethod("register", Object.class).invoke(beforeClaimEvent, handler);
            registered = true;
            LOGGER.info("Registered FTB Chunks claim buffer (BEFORE_CLAIM)");
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("Failed to register FTB Chunks claim buffer", exception);
        }
    }

    private static Object beforeClaim(CommandSourceStack source, ClaimedChunk claimedChunk) {
        try {
            ClaimBufferConfig.Settings settings = ClaimBufferConfig.get();
            if (!settings.enabled() || settings.freeChunks() <= 0) {
                return passResult();
            }
            if (!FTBChunksAPI.api().isManagerLoaded()) {
                return passResult();
            }
            if (canBypass(source)) {
                return passResult();
            }

            UUID claimingTeamId = claimedChunk.getTeamData().getTeam().getId();
            ChunkDimPos pos = claimedChunk.getPos();
            ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
            int radius = settings.freeChunks();

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    ClaimedChunk other = manager.getChunk(pos.offset(dx, dz));
                    if (other == null) {
                        continue;
                    }
                    UUID otherTeamId = other.getTeamData().getTeam().getId();
                    if (!claimingTeamId.equals(otherTeamId)) {
                        return interruptFalse(ClaimResult.customProblem(PROBLEM_KEY));
                    }
                }
            }

            return passResult();
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("Claim buffer check failed", exception);
            try {
                return passResult();
            } catch (ReflectiveOperationException passFailed) {
                LOGGER.error("Failed to build CompoundEventResult.pass()", passFailed);
                return null;
            }
        }
    }

    private static boolean canBypass(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return source.hasPermission(Commands.LEVEL_GAMEMASTERS);
        }
        return PermissionService.has(player, CointPermissionNodes.CLAIM_BUFFER_BYPASS);
    }

    private static Object passResult() throws ReflectiveOperationException {
        Class<?> compound = Class.forName("dev.architectury.event.CompoundEventResult");
        Method pass = compound.getMethod("pass");
        return pass.invoke(null);
    }

    private static Object interruptFalse(ClaimResult result) throws ReflectiveOperationException {
        Class<?> compound = Class.forName("dev.architectury.event.CompoundEventResult");
        Method interrupt = compound.getMethod("interruptFalse", Object.class);
        return interrupt.invoke(null, result);
    }
}
