package com.mawlee.cointcore.mixin.refinedstorage;

import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternIntegration;
import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternMenu;
import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternSession;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridContainerMenu;
import com.refinedmods.refinedstorage.common.grid.AbstractGridContainerMenu;
import com.refinedmods.refinedstorage.common.grid.GridData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PatternGridContainerMenu.class, remap = false)
public abstract class WirelessPatternMenuMixin extends AbstractGridContainerMenu implements WirelessPatternMenu {
    @Unique private SlotReference cointcore$reference;
    @Unique private WirelessPatternSession cointcore$session;

    protected WirelessPatternMenuMixin(MenuType<? extends AbstractGridContainerMenu> type, int id,
                                      Inventory inventory, GridData data) {
        super(type, id, inventory, data);
    }

    @Override
    public void cointcore$initializeWirelessPatterns(SlotReference reference, WirelessPatternSession session) {
        cointcore$reference = reference;
        cointcore$session = session;
        disabledSlot = reference;
        // Rebuild inventory slots after installing the device lock, including offhand/Curios references.
        resized(0, 0, 0);
    }

    @Override
    public SlotReference cointcore$patternGridReference() {
        return cointcore$reference;
    }

    @Override
    public MenuType<?> getType() {
        return cointcore$reference == null ? super.getType() : WirelessPatternIntegration.MENU.get();
    }

    @Override
    public boolean stillValid(Player player) {
        return cointcore$session == null ? super.stillValid(player) : cointcore$session.canUse();
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (cointcore$session == null || cointcore$session.canUse()) super.clicked(slot, button, type, player);
    }

    @Inject(method = "createPattern", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$validatePatternCreation(CallbackInfo callback) {
        if (cointcore$session != null && !cointcore$session.canUse()) callback.cancel();
    }

    @Override
    public void broadcastChanges() {
        if (cointcore$session != null) cointcore$session.refresh();
        super.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        if (cointcore$session != null) cointcore$session.save();
        super.removed(player);
    }
}
