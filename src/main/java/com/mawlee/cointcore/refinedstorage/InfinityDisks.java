package com.mawlee.cointcore.refinedstorage;

import com.mawlee.cointcore.CointCore;
import com.google.gson.JsonParser;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InfinityDisks {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CointCore.MOD_ID);
    private static final List<Disk> DISKS = loadDisks();

    public record Disk(String name, ResourceLocation resource, boolean fluid,
                       DeferredItem<InfinityDiskItem> item, InfinityDiskStorageType type) {
    }

    private InfinityDisks() {
    }

    public static List<Disk> all() {
        return DISKS;
    }

    private static List<Disk> loadDisks() {
        List<Disk> disks = new ArrayList<>();
        try (var reader = new InputStreamReader(Objects.requireNonNull(
                InfinityDisks.class.getResourceAsStream("/cointcore/infinity_disks.json")), StandardCharsets.UTF_8)) {
            for (var element : JsonParser.parseReader(reader).getAsJsonArray()) {
                var entry = element.getAsJsonObject();
                String name = entry.get("name").getAsString();
                var resource = ResourceLocation.parse(entry.get("resource").getAsString());
                boolean fluid = entry.get("fluid").getAsBoolean();
                int color = Integer.parseInt(entry.get("color").getAsString().substring(1), 16);
                var type = new InfinityDiskStorageType(() -> resolveResource(resource, fluid),
                        () -> fluid ? RefinedStorageApi.INSTANCE.getFluidStorageType()
                                : RefinedStorageApi.INSTANCE.getItemStorageType());
                var item = ITEMS.register("infinity_" + name + "_disk",
                        () -> new InfinityDiskItem(type::createSource, type::isAvailable, color));
                disks.add(new Disk(name, resource, fluid, item, type));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to register CointCore infinity disks", exception);
        }
        return List.copyOf(disks);
    }

    private static ResourceKey resolveResource(ResourceLocation id, boolean fluid) {
        // A small dev mod set may omit a material's mod. Never resolve a missing item as air.
        if (fluid) {
            return BuiltInRegistries.FLUID.getOptional(id).map(FluidResource::new).orElse(null);
        }
        return BuiltInRegistries.ITEM.getOptional(id).map(ItemResource::new).orElse(null);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(InfinityDisks::setup);
        bus.addListener(InfinityDisks::creativeTab);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var registry = RefinedStorageApi.INSTANCE.getStorageTypeRegistry();
            for (var disk : DISKS) {
                registry.register(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "infinity_" + disk.name()), disk.type());
            }
        });
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            DISKS.forEach(disk -> event.accept(disk.item()));
        }
    }
}
