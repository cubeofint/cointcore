package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;

/** Multiblock crafter (RebornStorage-style) tuning; section {@code multicrafter} of storage-perf.json. */
public final class MultiCrafterConfig {
    private static int stepsPerCpu = 4;
    private static int maxStepsPerTick = 1024;
    private static int maxSize = 16;
    private static long storageEnergy = 8;
    private static long cpuEnergy = 4;

    private MultiCrafterConfig() {
    }

    /** Pattern iterations per tick each CPU block adds (a fully upgraded RS autocrafter manages 2.5). */
    public static int getStepsPerCpu() { return stepsPerCpu; }
    /** Upper bound on iterations per tick for one pattern, however many CPUs there are. */
    public static int getMaxStepsPerTick() { return maxStepsPerTick; }
    /** Largest edge length of the structure, in blocks (3 is the smallest). */
    public static int getMaxSize() { return maxSize; }
    /** RS energy per tick of each pattern storage block (patterns add their own usage on top). */
    public static long getStorageEnergy() { return storageEnergy; }
    /** RS energy per tick of each CPU, charged to the structure's storage blocks. */
    public static long getCpuEnergy() { return cpuEnergy; }

    static Data defaultFileData() {
        return new Data();
    }

    static void applySection(Data data) {
        Data value = data == null ? defaultFileData() : data;
        stepsPerCpu = Math.max(1, Math.min(4096, value.stepsPerCpu));
        maxStepsPerTick = Math.max(1, Math.min(1_000_000, value.maxStepsPerTick));
        maxSize = Math.max(3, Math.min(32, value.maxSize));
        storageEnergy = Math.max(0, value.storageEnergy);
        cpuEnergy = Math.max(0, value.cpuEnergy);
    }

    static final class Data {
        @SerializedName("steps_per_cpu") int stepsPerCpu = 4;
        @SerializedName("max_steps_per_tick") int maxStepsPerTick = 1024;
        @SerializedName("max_size") int maxSize = 16;
        @SerializedName("storage_energy") long storageEnergy = 8;
        @SerializedName("cpu_energy") long cpuEnergy = 4;
    }
}
