package com.mawlee.cointcore.playtime;

import com.mawlee.cointcore.afk.AfkTracker;
import com.mawlee.cointcore.config.PlaytimeRewardConfig;
import com.mawlee.cointcore.config.PlaytimeRewardParse;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.shop.CurrencyMovement;
import com.mawlee.cointcore.shop.CurrencyMovementService;
import com.mawlee.cointcore.shop.CurrencyMovementType;
import com.mawlee.cointcore.shop.GluonWallet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlaytimeRewardService {
    private static final Map<UUID, Long> LAST_ACCRUE_MS = new ConcurrentHashMap<>();

    private PlaytimeRewardService() {
    }

    public static void onPlayerJoin(ServerPlayer player) {
        LAST_ACCRUE_MS.put(player.getUUID(), System.currentTimeMillis());
    }

    public static void onPlayerLeave(ServerPlayer player) {
        accrue(player, true);
        LAST_ACCRUE_MS.remove(player.getUUID());
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        if (!PlaytimeRewardConfig.get().enabled()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                LAST_ACCRUE_MS.put(player.getUUID(), System.currentTimeMillis());
            }
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            accrue(player, false);
        }
    }

    private static void accrue(ServerPlayer player, boolean leaving) {
        PlaytimeRewardParse.Parsed settings = PlaytimeRewardConfig.get();
        long now = System.currentTimeMillis();
        Long last = LAST_ACCRUE_MS.put(player.getUUID(), now);
        if (!settings.enabled()) {
            return;
        }
        if (last == null) {
            return;
        }
        long added = now - last;
        if (added <= 0L) {
            return;
        }
        if (!leaving && AfkTracker.isMarked(player)) {
            return;
        }
        if (leaving && AfkTracker.isMarked(player)) {
            return;
        }
        PlaytimeRewardSavedData data = PlaytimeRewardSavedData.get(player.server);
        PlaytimeRewardRules.Accrual accrual = PlaytimeRewardRules.accrue(
                data.getAccumulatedMs(player.getUUID()),
                added,
                PlaytimeRewardRules.intervalMillis(settings.intervalMinutes())
        );
        data.setAccumulatedMs(player.getUUID(), accrual.remainingMs());
        long credit = PlaytimeRewardRules.creditAmount(accrual.intervals(), settings.amount());
        if (credit <= 0L) {
            return;
        }
        long balance = GluonWallet.add(player.server, player.getUUID(), credit);
        String note = CointCoreMessages.translateKey("ru_ru", CointCoreMessages.PLAYTIME_REWARD_NOTE);
        CurrencyMovementService.record(
                player.server,
                null,
                "cointcore",
                player.getUUID(),
                player.getGameProfile().getName(),
                credit,
                CurrencyMovementType.PLAYTIME_REWARD,
                note,
                List.of(new CurrencyMovement.Delta(player.getUUID(), credit, balance)),
                null
        );
        player.sendSystemMessage(CointCoreMessages.forPlayer(
                player,
                CointCoreMessages.PLAYTIME_REWARD_GRANTED,
                credit,
                balance
        ));
    }
}
