package com.mawlee.cointcore.prefix;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Last accepted catalog hash and the bulk-report cursor.
 */
public final class PrefixSyncSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_prefix_sync";

    private String lastCatalogHash = "";
    private String reportAllCursor = "";
    private boolean reportAllRunning;

    private PrefixSyncSavedData() {
    }

    public static PrefixSyncSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PrefixSyncSavedData::new, PrefixSyncSavedData::load),
                DATA_ID
        );
    }

    public synchronized String lastCatalogHash() {
        return lastCatalogHash;
    }

    public synchronized void acceptCatalogHash(String hash) {
        lastCatalogHash = hash == null ? "" : hash;
        setDirty();
    }

    public synchronized String reportAllCursor() {
        return reportAllCursor;
    }

    public synchronized boolean reportAllRunning() {
        return reportAllRunning;
    }

    public synchronized void startReportAll() {
        reportAllRunning = true;
        reportAllCursor = "";
        setDirty();
    }

    public synchronized void resumeReportAll() {
        reportAllRunning = true;
        setDirty();
    }

    public synchronized void advanceReportAll(String cursor) {
        reportAllCursor = cursor == null ? "" : cursor;
        reportAllRunning = true;
        setDirty();
    }

    public synchronized void finishReportAll() {
        reportAllCursor = "";
        reportAllRunning = false;
        setDirty();
    }

    private static PrefixSyncSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        PrefixSyncSavedData data = new PrefixSyncSavedData();
        data.lastCatalogHash = tag.getString("last_catalog_hash");
        data.reportAllCursor = tag.getString("report_all_cursor");
        data.reportAllRunning = tag.getBoolean("report_all_running");
        return data;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putString("last_catalog_hash", lastCatalogHash == null ? "" : lastCatalogHash);
        tag.putString("report_all_cursor", reportAllCursor == null ? "" : reportAllCursor);
        tag.putBoolean("report_all_running", reportAllRunning);
        return tag;
    }
}
