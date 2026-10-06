package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class CurrencyMovementSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_currency_movements";
    private static final String ENTRIES_KEY = "entries";
    private static final String NEXT_ID_KEY = "next_id";
    static final int MAX_ENTRIES = 10_000;

    private final Deque<CurrencyMovement> entries = new ArrayDeque<>();
    private long nextId = 1L;

    private CurrencyMovementSavedData() {
    }

    public static CurrencyMovementSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(CurrencyMovementSavedData::new, CurrencyMovementSavedData::load),
                        DATA_ID
                );
    }

    public synchronized CurrencyMovement append(CurrencyMovement draft) {
        CurrencyMovement stored = new CurrencyMovement(
                nextId++,
                draft.timestampMs(),
                draft.fromId(),
                draft.fromName(),
                draft.toId(),
                draft.toName(),
                draft.amount(),
                draft.type(),
                draft.note()
        );
        entries.addLast(stored);
        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
        setDirty();
        return stored;
    }

    public synchronized List<CurrencyMovement> snapshot() {
        return new ArrayList<>(entries);
    }

    private static CurrencyMovementSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        CurrencyMovementSavedData data = new CurrencyMovementSavedData();
        data.nextId = Math.max(1L, tag.getLong(NEXT_ID_KEY));
        if (!tag.contains(ENTRIES_KEY, Tag.TAG_LIST)) {
            return data;
        }
        ListTag list = tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        for (Tag entryTag : list) {
            data.entries.addLast(CurrencyMovement.load((CompoundTag) entryTag));
        }
        while (data.entries.size() > MAX_ENTRIES) {
            data.entries.removeFirst();
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLong(NEXT_ID_KEY, nextId);
        ListTag list = new ListTag();
        for (CurrencyMovement entry : entries) {
            list.add(entry.save());
        }
        tag.put(ENTRIES_KEY, list);
        return tag;
    }
}
