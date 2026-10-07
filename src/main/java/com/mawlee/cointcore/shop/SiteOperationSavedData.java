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
 * operation never changes the wallet twice. Value is the final status ("applied" / "failed").
 */
public final class SiteOperationSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_site_operations";
    static final int MAX_ENTRIES = 50_000;

    private final LinkedHashMap<String, String> handled = new LinkedHashMap<>();

    private SiteOperationSavedData() {
    }

    public static SiteOperationSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SiteOperationSavedData::new, SiteOperationSavedData::load), DATA_ID);
    }

    public synchronized String status(String operationId) {
        return handled.get(operationId);
    }

    public synchronized void mark(String operationId, String status) {
        handled.put(operationId, status);
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
            data.handled.put(entry.getString("id"), entry.getString("status"));
        }
        return data;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<String, String> e : handled.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", e.getKey());
            entry.putString("status", e.getValue());
            list.add(entry);
        }
        tag.put("handled", list);
        return tag;
    }
}
