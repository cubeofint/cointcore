package com.mawlee.cointcore.prefix;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.lang.LegacyTextParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

public final class PrefixGui {
    private PrefixGui() {
    }

    public static void open(ServerPlayer player, PrefixSnapshot snapshot) {
        SimpleContainer container = new SimpleContainer(27);
        List<String> slotGroups = new ArrayList<>();
        slotGroups.add(null);
        container.setItem(0, autoItem(player, snapshot));

        int slot = 1;
        for (PrefixOption option : snapshot.available()) {
            if (slot >= 27) {
                break;
            }
            slotGroups.add(option.group());
            container.setItem(slot, prefixItem(player, option, option.group().equals(snapshot.selected())));
            slot++;
        }

        player.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new PrefixMenu(id, inventory, container, player, slotGroups),
                CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_GUI_TITLE)
        ));
    }

    private static ItemStack autoItem(ServerPlayer player, PrefixSnapshot snapshot) {
        ItemStack stack = new ItemStack(snapshot.selected() == null ? Items.LIME_DYE : Items.STRUCTURE_VOID);
        stack.set(DataComponents.CUSTOM_NAME, CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_GUI_AUTO));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_GUI_AUTO_LORE)
        )));
        return stack;
    }

    private static ItemStack prefixItem(ServerPlayer player, PrefixOption option, boolean selected) {
        ItemStack stack = new ItemStack(selected ? Items.NAME_TAG : Items.PAPER);
        Component title = LegacyTextParser.parse(option.prefixRaw()).copy()
                .append(Component.literal(" " + option.group()));
        stack.set(DataComponents.CUSTOM_NAME, title);
        List<Component> lore = new ArrayList<>();
        lore.add(CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_GUI_CATEGORY, option.category()));
        if (selected) {
            lore.add(CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_GUI_CURRENT));
        }
        lore.add(CointCoreMessages.forPlayer(player, CointCoreMessages.PREFIX_GUI_CLICK, option.group()));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }
}
