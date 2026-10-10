package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.shop.MarketSearchTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds a {@link MarketSearchTarget} from the actual listing stack.
 */
public final class MarketSearchTargets {
    private MarketSearchTargets() {
    }

    public static MarketSearchTarget from(ItemStack stack, String listingModId) {
        if (stack == null || stack.isEmpty()) {
            return MarketSearchTarget.of("", "", listingModId, "", List.of(), "", "");
        }
        String itemId = stack.getItemHolder().getRegisteredName();
        String namespace = namespace(itemId);
        String modId = listingModId == null || listingModId.isBlank() ? namespace : listingModId;
        String modName = modName(modId);
        String hover = stack.getHoverName().getString();
        String baseName = stack.getItem().getName(stack).getString();
        List<String> tooltipLines = tooltip(stack);
        List<String> tags = new ArrayList<>();
        stack.getTags().forEach(tag -> {
            tags.add(tag.location().toString());
            tags.add(tag.location().getPath());
        });
        return new MarketSearchTarget(
                hover + "\n" + baseName,
                String.join("\n", tooltipLines),
                modId,
                modName,
                tags,
                colors(stack, hover, tooltipLines),
                itemId,
                modName
        );
    }

    private static String namespace(String itemId) {
        int colon = itemId.indexOf(':');
        return colon < 0 ? itemId : itemId.substring(0, colon);
    }

    private static String modName(String modId) {
        if (modId == null || modId.isBlank()) {
            return "";
        }
        return ModList.get()
                .getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
    }

    private static List<String> tooltip(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return List.of();
        }
        List<Component> lines = stack.getTooltipLines(
                Item.TooltipContext.of(minecraft.level),
                minecraft.player,
                TooltipFlag.Default.NORMAL
        );
        List<String> text = new ArrayList<>(lines.size());
        for (Component line : lines) {
            text.add(line.getString());
        }
        return text;
    }

    private static String colors(ItemStack stack, String hover, List<String> tooltipLines) {
        StringBuilder out = new StringBuilder();
        if (stack.getItem() instanceof DyeItem dye) {
            append(out, dye.getDyeColor().getName());
        }
        DyeColor base = stack.get(DataComponents.BASE_COLOR);
        if (base != null) {
            append(out, base.getName());
        }
        DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
        if (dyed != null) {
            append(out, nearestDye(dyed.rgb()));
        }
        String haystack = (hover + "\n" + String.join("\n", tooltipLines)).toLowerCase(Locale.ROOT);
        for (DyeColor color : DyeColor.values()) {
            String name = color.getName();
            if (haystack.contains(name)) {
                append(out, name);
            }
        }
        return out.toString();
    }

    private static String nearestDye(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        DyeColor best = DyeColor.WHITE;
        int bestDist = Integer.MAX_VALUE;
        for (DyeColor color : DyeColor.values()) {
            int packed = color.getTextureDiffuseColor();
            int dr = red - ((packed >> 16) & 0xFF);
            int dg = green - ((packed >> 8) & 0xFF);
            int db = blue - (packed & 0xFF);
            int dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                best = color;
            }
        }
        return best.getName();
    }

    private static void append(StringBuilder out, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!out.isEmpty()) {
            out.append(' ');
        }
        out.append(value);
    }
}
