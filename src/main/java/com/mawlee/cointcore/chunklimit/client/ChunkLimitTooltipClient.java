package com.mawlee.cointcore.chunklimit.client;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.chunklimit.ChunkLimitTooltipPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Shows the server's placement limits (per chunk / team / player) under item names. */
@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ChunkLimitTooltipClient {
    private static volatile Map<Item, ChunkLimitTooltipPayload.Entry> limits = Map.of();

    private ChunkLimitTooltipClient() {
    }

    public static void accept(ChunkLimitTooltipPayload payload) {
        Map<Item, ChunkLimitTooltipPayload.Entry> map = new IdentityHashMap<>();
        for (ChunkLimitTooltipPayload.Entry entry : payload.entries()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.itemId());
            if (id != null) {
                BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> map.put(item, entry));
            }
        }
        limits = map;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        limits = Map.of();
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ChunkLimitTooltipPayload.Entry entry = limits.get(event.getItemStack().getItem());
        if (entry == null) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        // One group label for every shown scope goes into the header; differing labels go on their own lines.
        String common = commonLabel(entry);
        tooltip.add((common == null || common.isEmpty()
                ? Component.translatable("tooltip.cointcore.limit.header")
                : Component.translatable("tooltip.cointcore.limit.header_shared", common)).withStyle(ChatFormatting.GOLD));
        String perLine = common == null ? null : "";
        line(tooltip, "tooltip.cointcore.limit.chunk", entry.chunk(), perLine != null ? perLine : entry.chunkLabel());
        line(tooltip, "tooltip.cointcore.limit.team", entry.team(), perLine != null ? perLine : entry.teamLabel());
        line(tooltip, "tooltip.cointcore.limit.player", entry.player(), perLine != null ? perLine : entry.playerLabel());
    }

    /** The label shared by all scopes that have a limit, or null when they differ. */
    private static String commonLabel(ChunkLimitTooltipPayload.Entry entry) {
        String common = null;
        int[] limits = {entry.chunk(), entry.team(), entry.player()};
        String[] labels = {entry.chunkLabel(), entry.teamLabel(), entry.playerLabel()};
        for (int i = 0; i < limits.length; i++) {
            if (limits[i] < 0) {
                continue;
            }
            if (common == null) {
                common = labels[i];
            } else if (!common.equals(labels[i])) {
                return null;
            }
        }
        return common == null ? "" : common;
    }

    private static void line(List<Component> tooltip, String key, int limit, String label) {
        if (limit < 0) {
            return;
        }
        MutableComponent text = Component.translatable(key,
                Component.literal(String.valueOf(limit)).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY);
        if (label != null && !label.isEmpty()) {
            text.append(Component.literal(" ")).append(
                    Component.translatable("tooltip.cointcore.limit.shared", label).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(text);
    }
}
