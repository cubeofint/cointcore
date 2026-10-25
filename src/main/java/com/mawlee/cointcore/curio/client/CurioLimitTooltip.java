package com.mawlee.cointcore.curio.client;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.config.CurioLimitConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Tells players that a curio is limited ("Можно носить только 1 одновременно"), so a refused slot is not a mystery.
 * Uses the built-in defaults of {@link CurioLimitConfig}: the server file is not synced to clients.
 */
@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CurioLimitTooltip {
    private CurioLimitTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        int limit = CurioLimitConfig.limitFor(event.getItemStack().getItem());
        if (limit >= 0) {
            event.getToolTip().add(Component.translatable("tooltip.cointcore.curio_limit",
                    Component.literal(String.valueOf(limit)).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GOLD));
        }
    }
}
