package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.fml.loading.FMLPaths;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CurrencyMovementSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_currency_movements";
    private static final String ENTRIES_KEY = "entries";
    private static final String NEXT_ID_KEY = "next_id";
    private static final String SENT_UP_TO_KEY = "site_sent_up_to";
    private static final String EPOCH_KEY = "epoch";
    static final int MAX_ENTRIES = CurrencyMovementOutbox.MAX_ENTRIES;

    private final CurrencyMovementOutbox outbox = new CurrencyMovementOutbox();

    private CurrencyMovementSavedData() {
        this(true);
    }

    private CurrencyMovementSavedData(boolean freshJournal) {
        if (!freshJournal) {
            return;
        }
        CurrencyMovementSequence.Snapshot sequence = CurrencyMovementIdStore.reconcileWithWorld(1L, 0L);
        outbox.restore(sequence.nextId(), 0L, UUID.randomUUID(), List.of());
        CurrencyMovementIdStore.recordNextId(outbox.nextId());
        setDirty();
    }

    public static CurrencyMovementSavedData get(MinecraftServer server) {
        CurrencyMovementIdStore.bind(
                FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve(CurrencyMovementIdStore.FILE_NAME)
        );
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(CurrencyMovementSavedData::new, CurrencyMovementSavedData::load),
                        DATA_ID
                );
    }

    public synchronized CurrencyMovement append(CurrencyMovement draft) {
        CurrencyMovement stored = outbox.append(draft);
        CurrencyMovementIdStore.recordNextId(outbox.nextId());
        setDirty();
        return stored;
    }

    public synchronized UUID epoch() {
        return outbox.epoch();
    }

    public synchronized List<CurrencyMovement> snapshot() {
        return outbox.snapshot();
    }

    public synchronized long siteSentUpTo() {
        return outbox.siteSentUpTo();
    }

    public synchronized void markSiteSentUpTo(long id) {
        onSiteAccepted(id, id);
    }

    /**
     * Advances the outbox cursor by the ids we actually pushed, and never lets {@code next_id}
     * fall to or below the site's {@code accepted_up_to}.
     */
    public synchronized void onSiteAccepted(long siteAcceptedUpTo, long batchLastId) {
        CurrencyMovementIdStore.recordAcceptedUpTo(siteAcceptedUpTo);
        boolean dirty = outbox.ensureNextIdAtLeast(siteAcceptedUpTo + 1L);
        CurrencyMovementIdStore.recordNextId(outbox.nextId());
        long cursor = Math.min(Math.max(0L, siteAcceptedUpTo), Math.max(0L, batchLastId));
        if (outbox.markSiteSentUpTo(cursor)) {
            dirty = true;
        }
        if (dirty) {
            setDirty();
        }
    }

    /** Oldest server-wallet movements not yet confirmed by the site. */
    public synchronized List<CurrencyMovement> unsent(int limit) {
        return outbox.unsentForSite(limit);
    }

    private static CurrencyMovementSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        CurrencyMovementSavedData data = new CurrencyMovementSavedData(false);
        List<CurrencyMovement> loaded = new ArrayList<>();
        if (tag.contains(ENTRIES_KEY, Tag.TAG_LIST)) {
            ListTag list = tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
            for (Tag entryTag : list) {
                loaded.add(CurrencyMovement.load((CompoundTag) entryTag));
            }
        }
        ParsedEpoch parsed = parseEpoch(tag);
        long worldNext = tag.getLong(NEXT_ID_KEY);
        CurrencyMovementSequence.Snapshot sequence = CurrencyMovementIdStore.reconcileWithWorld(
                worldNext,
                tag.getLong(SENT_UP_TO_KEY)
        );
        data.outbox.restore(sequence.nextId(), tag.getLong(SENT_UP_TO_KEY), parsed.epoch(), loaded);
        CurrencyMovementIdStore.recordNextId(data.outbox.nextId());
        if (parsed.generated() || sequence.nextId() != worldNext) {
            data.setDirty();
        }
        return data;
    }

    private static ParsedEpoch parseEpoch(CompoundTag tag) {
        if (!tag.contains(EPOCH_KEY)) {
            return new ParsedEpoch(UUID.randomUUID(), true);
        }
        try {
            return new ParsedEpoch(UUID.fromString(tag.getString(EPOCH_KEY)), false);
        } catch (IllegalArgumentException exception) {
            return new ParsedEpoch(UUID.randomUUID(), true);
        }
    }

    private record ParsedEpoch(UUID epoch, boolean generated) {
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLong(NEXT_ID_KEY, outbox.nextId());
        tag.putLong(SENT_UP_TO_KEY, outbox.siteSentUpTo());
        tag.putString(EPOCH_KEY, outbox.epoch().toString());
        ListTag list = new ListTag();
        for (CurrencyMovement entry : outbox.snapshot()) {
            list.add(entry.save());
        }
        tag.put(ENTRIES_KEY, list);
        return tag;
    }
}
