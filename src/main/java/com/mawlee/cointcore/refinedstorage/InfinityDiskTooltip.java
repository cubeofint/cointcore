package com.mawlee.cointcore.refinedstorage;

import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Common-side tooltip data; rendering remains in the client setup. */
public record InfinityDiskTooltip(ResourceAmount resource) implements TooltipComponent {
}
