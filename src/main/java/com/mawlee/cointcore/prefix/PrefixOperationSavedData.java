package com.mawlee.cointcore.prefix;

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
 * Idempotency store for site {@code prefix_select} operations ({@code op_id}).
 */
public final class PrefixOperationSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_prefix_operations";
    static final int MAX_ENTRIES = 50_000;

    private final LinkedHashMap<String, Outcome> handled = new LinkedHashMap<>();

    private PrefixOperationSavedData() {
    }

    public static PrefixOperationSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PrefixOperationSavedData::new, PrefixOperationSavedData::load),
                DATA_ID
        );
    }

    public synchronized Outcome outcome(String operationId) {
        return handled.get(operationId);
    }

    public synchronized void mark(String operationId, String status, String reason, String selected) {
        handled.put(operationId, new Outcome(status, reason, selected));
        while (handled.size() > MAX_ENTRIES) {
            handled.remove(handled.keySet().iterator().next());
        }
        setDirty();
    }

    private static PrefixOperationSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        PrefixOperationSavedData data = new PrefixOperationSavedData();
        ListTag list = tag.getList("handled", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.handled.put(
                    entry.getString("id"),
                    new Outcome(
                            entry.getString("status"),
                            entry.contains("reason") ? entry.getString("reason") : "",
                            entry.contains("selected") ? entry.getString("selected") : null
                    )
            );
        }
        return data;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<String, Outcome> entry : handled.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putString("id", entry.getKey());
            row.putString("status", entry.getValue().status());
            if (entry.getValue().reason() != null && !entry.getValue().reason().isBlank()) {
                row.putString("reason", entry.getValue().reason());
            }
            if (entry.getValue().selected() != null) {
                row.putString("selected", entry.getValue().selected());
            }
            list.add(row);
        }
        tag.put("handled", list);
        return tag;
    }

    public record Outcome(String status, String reason, String selected) {
    }
}
