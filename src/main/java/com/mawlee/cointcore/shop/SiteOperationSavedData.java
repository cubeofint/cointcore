package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Remembers site operation ids already handled on this server, so a lost ack or a re-delivered
 * operation never changes the wallet twice. Value is the final status ("applied" / "failed")
 * and the in-game {@code balance_after} used for idempotent acks.
 */
public final class SiteOperationSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_site_operations";
    static final int MAX_ENTRIES = 50_000;

    private final LinkedHashMap<String, Outcome> handled = new LinkedHashMap<>();

    private SiteOperationSavedData() {
    }

    public static SiteOperationSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SiteOperationSavedData::new, SiteOperationSavedData::load), DATA_ID);
    }

    public synchronized String status(String operationId) {
        Outcome outcome = handled.get(operationId);
        return outcome == null ? null : outcome.status();
    }

    public synchronized Long balanceAfter(String operationId) {
        Outcome outcome = handled.get(operationId);
        return outcome == null ? null : outcome.balanceAfter();
    }

    public synchronized Outcome outcome(String operationId) {
        return handled.get(operationId);
    }

    public synchronized void mark(String operationId, String status) {
        mark(operationId, status, null);
    }

    public synchronized void mark(String operationId, String status, Long balanceAfter) {
        handled.put(operationId, new Outcome(status, balanceAfter));
        while (handled.size() > MAX_ENTRIES) {
            handled.remove(handled.keySet().iterator().next());
        }
        setDirty();
    }

    private static SiteOperationSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        SiteOperationSavedData data = new SiteOperationSavedData();
        ListTag list = tag.getList("handled", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            Long balance = entry.contains("balance_after") ? entry.getLong("balance_after") : null;
            data.handled.put(entry.getString("id"), new Outcome(entry.getString("status"), balance));
        }
        return data;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<String, Outcome> e : handled.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", e.getKey());
            entry.putString("status", e.getValue().status());
            if (e.getValue().balanceAfter() != null) {
                entry.putLong("balance_after", e.getValue().balanceAfter());
            }
            list.add(entry);
        }
        tag.put("handled", list);
        return tag;
    }

    public record Outcome(String status, Long balanceAfter) {
    }
}
