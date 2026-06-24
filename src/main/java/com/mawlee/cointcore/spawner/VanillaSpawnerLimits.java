package com.mawlee.cointcore.spawner;

import net.minecraft.resources.ResourceLocation;

public final class VanillaSpawnerLimits {
    public static final int SPAWN_RANGE = 4;

    public static final ResourceLocation APOTHIC_SPAWN_RANGE_STAT = ResourceLocation.fromNamespaceAndPath(
            "apothic_spawners",
            "spawn_range"
    );

    private VanillaSpawnerLimits() {
    }
}
