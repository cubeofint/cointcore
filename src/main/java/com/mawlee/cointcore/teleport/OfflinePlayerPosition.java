package com.mawlee.cointcore.teleport;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record OfflinePlayerPosition(
        ResourceKey<Level> dimension,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {
}
