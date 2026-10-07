package com.mawlee.cointcore.invsee;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InvSeeStateCollector {
    private InvSeeStateCollector() {
    }

    public static List<String> collect(InvSeeTarget target) {
        Player player = target.getPlayer();
        if (player == null) {
            return InvSeeInfoLines.playerState(
                    target.displayName(),
                    false,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    "-",
                    0,
                    0,
                    0,
                    "-",
                    List.of()
            );
        }

        String gameMode = "-";
        if (player instanceof ServerPlayer serverPlayer) {
            GameType type = serverPlayer.gameMode.getGameModeForPlayer();
            gameMode = type.getName();
        }

        List<String> effects = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            String id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString();
            effects.add(id + " amp=" + effect.getAmplifier() + " t=" + effect.getDuration());
        }

        ResourceKey<Level> dimension = player.level().dimension();
        return InvSeeInfoLines.playerState(
                target.displayName(),
                !target.isOffline(),
                player.getHealth(),
                player.getMaxHealth(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel(),
                player.experienceLevel,
                player.experienceProgress,
                player.totalExperience,
                dimension.location().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                gameMode,
                effects
        );
    }

    public static String lastDeath(Player player) {
        if (player == null) {
            return "";
        }
        Optional<GlobalPos> death = player.getLastDeathLocation();
        if (death.isEmpty()) {
            return "";
        }
        GlobalPos pos = death.get();
        return pos.dimension().location() + " "
                + pos.pos().getX() + " " + pos.pos().getY() + " " + pos.pos().getZ();
    }
}
