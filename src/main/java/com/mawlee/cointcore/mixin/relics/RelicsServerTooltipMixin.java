package com.mawlee.cointcore.mixin.relics;

import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Marker on vanilla {@link Item} so {@code CointCoreMixinPlugin} can patch the
 * Relics-merged {@code appendHoverText} handler after apply. Priority {@code 2000}
 * applies after Relics' default-priority {@code ItemMixin}.
 *
 * <p>Injecting into Relics' mixin class is impossible (prepare-time crash).
 * Cancelling {@code Item#appendHoverText} would skip vanilla and other mods.
 */
@Mixin(value = Item.class, priority = 2000)
public abstract class RelicsServerTooltipMixin {
}
