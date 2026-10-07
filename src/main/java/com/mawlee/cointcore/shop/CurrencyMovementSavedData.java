package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public final class CurrencyMovementSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_currency_movements";
    private static final String ENTRIES_KEY = "entries";
    private static final String NEXT_ID_KEY = "next_id";
    private static final String SENT_UP_TO_KEY = "site_sent_up_to";
    static final int MAX_ENTRIES = CurrencyMovementOutbox.MAX_ENTRIES;

    private final CurrencyMovementOutbox outbox = new CurrencyMovementOutbox();

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
        CurrencyMovement stored = outbox.append(draft);
        setDirty();
        return stored;
    }

    public synchronized List<CurrencyMovement> snapshot() {
        return outbox.snapshot();
    }

    public synchronized long siteSentUpTo() {
        return outbox.siteSentUpTo();
    }

    public synchronized void markSiteSentUpTo(long id) {
        if (outbox.markSiteSentUpTo(id)) {
            setDirty();
        }
    }

    /** Oldest server-wallet movements not yet confirmed by the site. */
    public synchronized List<CurrencyMovement> unsent(int limit) {
        return outbox.unsentForSite(limit);
    }

    private static CurrencyMovementSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        CurrencyMovementSavedData data = new CurrencyMovementSavedData();
        List<CurrencyMovement> loaded = new ArrayList<>();
        if (tag.contains(ENTRIES_KEY, Tag.TAG_LIST)) {
            ListTag list = tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
            for (Tag entryTag : list) {
                loaded.add(CurrencyMovement.load((CompoundTag) entryTag));
            }
        }
        data.outbox.restore(tag.getLong(NEXT_ID_KEY), tag.getLong(SENT_UP_TO_KEY), loaded);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLong(NEXT_ID_KEY, outbox.nextId());
        tag.putLong(SENT_UP_TO_KEY, outbox.siteSentUpTo());
        ListTag list = new ListTag();
        for (CurrencyMovement entry : outbox.snapshot()) {
            list.add(entry.save());
        }
        tag.put(ENTRIES_KEY, list);
        return tag;
    }
}
