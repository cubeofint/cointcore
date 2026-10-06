package com.mawlee.cointcore.config;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.shop.TraderOffer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponentPatch;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Static trader listings: {@code config/cointcore/trader_offers.json}.
 * Buyers pay price + ceil(commission). Sellers receive price − ceil(commission).
 */
public final class TraderOffersConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Loaded loaded = Loaded.empty();

    private TraderOffersConfig() {
    }

    public static double commissionPercent() {
        return loaded.commissionPercent;
    }

    public static List<TraderOffer> offers() {
        return loaded.offers;
    }

    public static void load() {
        apply(loadFromDisk(false));
    }

    public static boolean reload() {
        Loaded next = loadFromDisk(true);
        if (next == null) {
            return false;
        }
        apply(next);
        return true;
    }

    private static void apply(Loaded next) {
        loaded = next != null ? next : Loaded.empty();
    }

    private static Loaded loadFromDisk(boolean reloading) {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = FileData.defaults();
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    ConfigMergeSupport.GSON.toJson(defaults, writer);
                }
                LOGGER.info("Created default trader offers at {}", path);
                return parse(defaults);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = ConfigMergeSupport.GSON.fromJson(reader, FileData.class);
                Loaded parsed = parse(data != null ? data : FileData.defaults());
                LOGGER.info("Loaded {} trader offers (commission={}%)", parsed.offers.size(), parsed.commissionPercent);
                if (reloading) {
                    LOGGER.info("Reloaded trader offers from {}", path);
                }
                return parsed;
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load trader offers from {}", path, exception);
            return reloading ? null : Loaded.empty();
        }
    }

    static Loaded parse(FileData data) {
        double percent = data.commissionPercent != null && !Double.isNaN(data.commissionPercent)
                ? Math.max(0.0d, data.commissionPercent)
                : 2.5d;
        List<TraderOffer> offers = new ArrayList<>();
        if (data.offers != null) {
            for (OfferData entry : data.offers) {
                toOffer(entry, percent).ifPresent(offers::add);
            }
        }
        return new Loaded(percent, List.copyOf(offers));
    }

    private static Optional<TraderOffer> toOffer(OfferData entry, double commissionPercent) {
        if (entry == null || !entry.enabled || entry.item == null || entry.item.isBlank()) {
            return Optional.empty();
        }
        ResourceLocation itemId;
        try {
            itemId = ResourceLocation.parse(entry.item);
        } catch (RuntimeException exception) {
            LOGGER.warn("Skipping trader offer with invalid item id {}", entry.item);
            return Optional.empty();
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR) {
            LOGGER.warn("Skipping trader offer {} — unknown item {}", entry.id, entry.item);
            return Optional.empty();
        }
        int count = entry.count != null ? Math.max(1, Math.min(item.getDefaultMaxStackSize(), entry.count)) : 1;
        ItemStack stack = new ItemStack(item, count);
        if (entry.components != null && !entry.components.isJsonNull()) {
            DataComponentPatch.CODEC.parse(JsonOps.INSTANCE, entry.components).resultOrPartial(LOGGER::warn)
                    .ifPresent(stack::applyComponents);
        }
        String id = entry.id != null && !entry.id.isBlank() ? entry.id : itemId.toString();
        long buy = entry.buyPrice != null ? entry.buyPrice : 0L;
        long sell = entry.sellPrice != null ? entry.sellPrice : 0L;
        if (buy <= 0L && sell <= 0L) {
            return Optional.empty();
        }
        return Optional.of(TraderOffer.of(id, stack, count, buy, sell, commissionPercent));
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("trader_offers.json");
    }

    record Loaded(double commissionPercent, List<TraderOffer> offers) {
        static Loaded empty() {
            return new Loaded(2.5d, List.of());
        }
    }

    static final class FileData {
        @SerializedName("commission_percent")
        Double commissionPercent;
        List<OfferData> offers;

        static FileData defaults() {
            FileData data = new FileData();
            data.commissionPercent = 2.5d;
            data.offers = List.of(
                    offer("diamond", "minecraft:diamond", 1, 100L, 80L),
                    offer("emerald", "minecraft:emerald", 1, 80L, 64L),
                    offer("iron_ingot", "minecraft:iron_ingot", 16, 24L, 16L),
                    offer("gold_ingot", "minecraft:gold_ingot", 8, 40L, 28L),
                    offer("oak_log", "minecraft:oak_log", 32, 12L, 8L),
                    offer("coal", "minecraft:coal", 32, 10L, 6L)
            );
            return data;
        }
    }

    static final class OfferData {
        String id;
        String item;
        Integer count;
        @SerializedName("buy_price")
        Long buyPrice;
        @SerializedName("sell_price")
        Long sellPrice;
        boolean enabled = true;
        JsonElement components;
    }

    private static OfferData offer(String id, String item, int count, long buy, long sell) {
        OfferData data = new OfferData();
        data.id = id;
        data.item = item;
        data.count = count;
        data.buyPrice = buy;
        data.sellPrice = sell;
        data.enabled = true;
        return data;
    }
}
