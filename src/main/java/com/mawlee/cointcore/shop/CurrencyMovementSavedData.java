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
    private static final String SENT_UP_TO_KEY = "site_sent_up_to";
    static final int MAX_ENTRIES = 10_000;

    private final Deque<CurrencyMovement> entries = new ArrayDeque<>();
    private long nextId = 1L;
    /** Highest movement id the site confirmed (idempotent on the site, so resending is harmless). */
    private long siteSentUpTo;

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

    public synchronized long siteSentUpTo() {
        return siteSentUpTo;
    }

    public synchronized void markSiteSentUpTo(long id) {
        if (id > siteSentUpTo) {
            siteSentUpTo = id;
            setDirty();
        }
    }

    /** Oldest movements not yet confirmed by the site. */
    public synchronized List<CurrencyMovement> unsent(int limit) {
        List<CurrencyMovement> result = new ArrayList<>();
        for (CurrencyMovement entry : entries) {
            if (entry.id() > siteSentUpTo) {
                result.add(entry);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    private static CurrencyMovementSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        CurrencyMovementSavedData data = new CurrencyMovementSavedData();
        data.nextId = Math.max(1L, tag.getLong(NEXT_ID_KEY));
        data.siteSentUpTo = tag.getLong(SENT_UP_TO_KEY);
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
        tag.putLong(SENT_UP_TO_KEY, siteSentUpTo);
        ListTag list = new ListTag();
        for (CurrencyMovement entry : entries) {
            list.add(entry.save());
        }
        tag.put(ENTRIES_KEY, list);
        return tag;
    }
}
