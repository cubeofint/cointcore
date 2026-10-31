package com.mawlee.cointcore.refinedstorage.wireless;

import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

/** Optional blank types are resolved by registry ID; the addon is never a required dependency. */
public final class NetworkPatternBlanks {
    public static final String STATE_KEY = "cointcore_blank_pattern";
    private static final List<String> IDS = List.of("refinedstorage:pattern", "refinedfluidsubstitution:fluid_substitution_pattern");
    private NetworkPatternBlanks() { }

    public static List<ItemResource> types() {
        var result = new ArrayList<ItemResource>();
        for (String id : IDS) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
            if (item != Items.AIR) result.add(new ItemResource(item));
        }
        return List.copyOf(result);
    }
    public static ItemResource read(String id) {
        return types().stream().filter(resource -> BuiltInRegistries.ITEM.getKey(resource.item()).toString().equals(id))
                .findFirst().orElseGet(() -> types().getFirst());
    }
    public static String id(ItemResource blank) { return BuiltInRegistries.ITEM.getKey(blank.item()).toString(); }
    public static ItemStack output(com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridBlockEntity grid) {
        return ((NetworkPatternOutput) grid).cointcore$outputPattern();
    }
    public static ItemResource choose(ItemResource current, ItemStack carried, boolean backwards) {
        var types = types();
        if (!carried.isEmpty()) return types.stream().filter(resource -> resource.item() == carried.getItem()).findFirst().orElse(current);
        int index = types.indexOf(current);
        return types.get(Math.floorMod(index + (backwards ? -1 : 1), types.size()));
    }
    public static ItemResource forOutput(ItemResource selected, ItemStack output) {
        if (output.isEmpty()) return selected;
        return types().stream().filter(resource -> resource.item() == output.getItem()).findFirst().orElse(selected);
    }
    public static ItemStack result(ItemStack built, ItemStack previous, ItemResource selected) {
        return built.transmuteCopy(forOutput(selected, previous).item());
    }
}
