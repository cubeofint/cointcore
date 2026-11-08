package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;

/** Refined Storage autocrafting speed fixes; section {@code rs_autocrafting} of storage-perf.json. */
public final class RsAutocraftingConfig {
    private static boolean providerSpeed = true;
    private static int externalIterationsPerStep = 64;

    private RsAutocraftingConfig() {
    }

    /**
     * A task's sub-patterns that live in another autocrafter use that autocrafter's speed (upgrades, Cable Tiers tier,
     * multiblock CPUs). Stock RS falls back to one step per tick for them.
     */
    public static boolean isProviderSpeed() { return providerSpeed; }

    /** Most processing iterations pushed into a machine per step, while it keeps accepting them (1 = stock RS). */
    public static int getExternalIterationsPerStep() { return externalIterationsPerStep; }

    static Data defaultFileData() {
        return new Data();
    }

    static void applySection(Data data) {
        Data value = data == null ? defaultFileData() : data;
        providerSpeed = value.providerSpeed;
        externalIterationsPerStep = Math.max(1, Math.min(4096, value.externalIterationsPerStep));
    }

    static final class Data {
        @SerializedName("provider_speed") boolean providerSpeed = true;
        @SerializedName("external_iterations_per_step") int externalIterationsPerStep = 64;
    }
}
