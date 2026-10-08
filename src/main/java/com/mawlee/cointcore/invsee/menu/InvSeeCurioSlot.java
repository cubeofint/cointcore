package com.mawlee.cointcore.invsee.menu;

import com.mawlee.cointcore.invsee.InvSeeClientDispatchers;
import com.mawlee.cointcore.invsee.InvSeeCurioSlotMeta;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.InventoryMenu;

public final class InvSeeCurioSlot extends InvSeeBoundSlot {
    private InvSeeCurioSlotMeta meta;

    public InvSeeCurioSlot(
            Container placeholder,
            int index,
            int x,
            int y,
            boolean clientSide,
            InvSeeBaseMenu menu,
            int contentIndex
    ) {
        super(placeholder, index, x, y, clientSide, menu, contentIndex);
    }

    public void applyMeta(InvSeeCurioSlotMeta meta) {
        this.meta = meta != null && meta.present() ? meta : null;
    }

    public void clearMeta() {
        this.meta = null;
    }

    public boolean hasMeta() {
        return meta != null;
    }

    public boolean cosmetic() {
        return meta != null && meta.cosmetic();
    }

    public InvSeeCurioSlotMeta meta() {
        return meta;
    }

    public Component slotTypeName() {
        if (meta == null) {
            return Component.empty();
        }
        return Component.translatable("curios.identifier." + meta.identifier());
    }

    public Component slotTypeLine() {
        Component type = slotTypeName();
        if (meta != null && meta.cosmetic()) {
            return Component.translatable("gui.cointcore.invsee.curios.slot_line_cosmetic", type);
        }
        return Component.translatable("gui.cointcore.invsee.curios.slot_line", type);
    }

    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        if (meta == null) {
            return null;
        }
        ResourceLocation icon = InvSeeClientDispatchers.curiosIcon(meta.identifier());
        if (icon == null) {
            return null;
        }
        return Pair.of(InventoryMenu.BLOCK_ATLAS, icon);
    }
}
