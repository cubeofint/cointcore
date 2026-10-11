package com.mawlee.cointcore.speedrunner;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Optional;
import java.util.UUID;

public final class SpeedrunnerSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_speedrunner";

    private UUID holderId;
    private String holderName = "";
    private int ticks = -1;
    private long craftedAtMs;

    private SpeedrunnerSavedData() {
    }

    public static SpeedrunnerSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SpeedrunnerSavedData::new, SpeedrunnerSavedData::load),
                DATA_ID
        );
    }

    public synchronized Optional<Record> record() {
        if (holderId == null || ticks < 0) {
            return Optional.empty();
        }
        return Optional.of(new Record(holderId, holderName, ticks, craftedAtMs));
    }

    public synchronized boolean isBetter(int candidateTicks) {
        return holderId == null || ticks < 0 || candidateTicks < ticks;
    }

    public synchronized void set(UUID playerId, String name, int playTicks, long craftedAtMs) {
        this.holderId = playerId;
        this.holderName = name == null ? "" : name;
        this.ticks = Math.max(0, playTicks);
        this.craftedAtMs = craftedAtMs;
        setDirty();
    }

    public synchronized void clear() {
        holderId = null;
        holderName = "";
        ticks = -1;
        craftedAtMs = 0L;
        setDirty();
    }

    private static SpeedrunnerSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        SpeedrunnerSavedData data = new SpeedrunnerSavedData();
        if (tag.hasUUID("holder")) {
            data.holderId = tag.getUUID("holder");
            data.holderName = tag.getString("name");
            data.ticks = tag.getInt("ticks");
            data.craftedAtMs = tag.getLong("date");
        }
        return data;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        if (holderId != null && ticks >= 0) {
            tag.putUUID("holder", holderId);
            tag.putString("name", holderName);
            tag.putInt("ticks", ticks);
            tag.putLong("date", craftedAtMs);
        }
        return tag;
    }

    public record Record(UUID holderId, String holderName, int ticks, long craftedAtMs) {
    }
}
