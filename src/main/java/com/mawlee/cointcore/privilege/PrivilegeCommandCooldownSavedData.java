package com.mawlee.cointcore.privilege;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class PrivilegeCommandCooldownSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_privilege_command_cooldowns";
    private static final String PLAYERS_KEY = "players";

    private final Map<UUID, EnumMap<PrivilegeCommandId, Long>> readyAtByPlayer = new HashMap<>();

    private PrivilegeCommandCooldownSavedData() {
    }

    public static PrivilegeCommandCooldownSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(
                                PrivilegeCommandCooldownSavedData::new,
                                PrivilegeCommandCooldownSavedData::load
                        ),
                        DATA_ID
                );
    }

    public synchronized long readyAt(UUID playerId, PrivilegeCommandId commandId) {
        EnumMap<PrivilegeCommandId, Long> byCommand = readyAtByPlayer.get(playerId);
        if (byCommand == null) {
            return 0L;
        }
        return byCommand.getOrDefault(commandId, 0L);
    }

    public synchronized void setReadyAt(UUID playerId, PrivilegeCommandId commandId, long readyAtEpochMs) {
        if (readyAtEpochMs <= 0L) {
            EnumMap<PrivilegeCommandId, Long> byCommand = readyAtByPlayer.get(playerId);
            if (byCommand != null) {
                byCommand.remove(commandId);
                if (byCommand.isEmpty()) {
                    readyAtByPlayer.remove(playerId);
                }
                setDirty();
            }
            return;
        }
        readyAtByPlayer
                .computeIfAbsent(playerId, ignored -> new EnumMap<>(PrivilegeCommandId.class))
                .put(commandId, readyAtEpochMs);
        setDirty();
    }

    private static PrivilegeCommandCooldownSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        PrivilegeCommandCooldownSavedData data = new PrivilegeCommandCooldownSavedData();
        CompoundTag players = tag.getCompound(PLAYERS_KEY);
        for (String rawId : players.getAllKeys()) {
            UUID playerId;
            try {
                playerId = UUID.fromString(rawId);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            CompoundTag commands = players.getCompound(rawId);
            EnumMap<PrivilegeCommandId, Long> byCommand = new EnumMap<>(PrivilegeCommandId.class);
            for (String key : commands.getAllKeys()) {
                PrivilegeCommandId commandId = PrivilegeCommandId.fromKey(key);
                if (commandId == null) {
                    continue;
                }
                long readyAt = commands.getLong(key);
                if (readyAt > 0L) {
                    byCommand.put(commandId, readyAt);
                }
            }
            if (!byCommand.isEmpty()) {
                data.readyAtByPlayer.put(playerId, byCommand);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        long now = System.currentTimeMillis();
        CompoundTag players = new CompoundTag();
        Iterator<Map.Entry<UUID, EnumMap<PrivilegeCommandId, Long>>> playerIterator =
                readyAtByPlayer.entrySet().iterator();
        while (playerIterator.hasNext()) {
            Map.Entry<UUID, EnumMap<PrivilegeCommandId, Long>> playerEntry = playerIterator.next();
            CompoundTag commands = new CompoundTag();
            Iterator<Map.Entry<PrivilegeCommandId, Long>> commandIterator = playerEntry.getValue().entrySet().iterator();
            while (commandIterator.hasNext()) {
                Map.Entry<PrivilegeCommandId, Long> commandEntry = commandIterator.next();
                if (commandEntry.getValue() <= now) {
                    commandIterator.remove();
                    continue;
                }
                commands.putLong(commandEntry.getKey().key(), commandEntry.getValue());
            }
            if (playerEntry.getValue().isEmpty()) {
                playerIterator.remove();
                continue;
            }
            players.put(playerEntry.getKey().toString(), commands);
        }
        tag.put(PLAYERS_KEY, players);
        return tag;
    }
}
