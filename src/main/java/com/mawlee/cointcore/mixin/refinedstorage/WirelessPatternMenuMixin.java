package com.mawlee.cointcore.mixin.refinedstorage;

import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternIntegration;
import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternMenu;
import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternSession;
import com.mawlee.cointcore.refinedstorage.wireless.NetworkPatternSlot;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.refinedmods.refinedstorage.common.support.containermenu.ValidatedSlot;
import com.refinedmods.refinedstorage.common.support.containermenu.TransferManager;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import java.util.function.Predicate;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridContainerMenu;
import com.refinedmods.refinedstorage.common.grid.AbstractGridContainerMenu;
import com.refinedmods.refinedstorage.common.grid.GridData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ClickType;
import com.mawlee.cointcore.refinedstorage.wireless.NetworkPatternCountPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PatternGridContainerMenu.class, remap = false)
public abstract class WirelessPatternMenuMixin extends AbstractGridContainerMenu implements WirelessPatternMenu {
    @Unique private SlotReference cointcore$reference;
    @Unique private WirelessPatternSession cointcore$session;
    @Unique private long cointcore$availablePatterns;

    @Override public long cointcore$availablePatterns() {
        return cointcore$session == null ? cointcore$availablePatterns : cointcore$session.availablePatterns();
    }

    protected WirelessPatternMenuMixin(MenuType<? extends AbstractGridContainerMenu> type, int id,
                                      Inventory inventory, GridData data) {
        super(type, id, inventory, data);
    }

    @Override
    public void cointcore$initializeWirelessPatterns(SlotReference reference, WirelessPatternSession session) {
        cointcore$reference = reference;
        cointcore$session = session;
        for (int part = 0; part < 4; part++) {
            final int shift = part * 16;
            addDataSlot(new NetworkPatternCountPart(this::cointcore$availablePatterns,
                    value -> cointcore$availablePatterns = value, shift));
        }
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
        // Also reject creative clone clicks, which do not consult Slot.mayPickup.
        if (cointcore$reference != null && slot >= 0 && slot < slots.size()
                && slots.get(slot) instanceof NetworkPatternSlot) return;
        if (cointcore$session == null || cointcore$session.canUse()) super.clicked(slot, button, type, player);
    }

    @Inject(method = "createPattern", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$validatePatternCreation(CallbackInfo callback) {
        if (cointcore$session != null) {
            if (!cointcore$session.canUse()) callback.cancel();
            else cointcore$session.refreshPatternPreview();
        }
    }

    @Inject(method = "createPattern", at = @At("RETURN"), require = 1)
    private void cointcore$refreshAfterEncoding(CallbackInfo callback) {
        if (cointcore$session != null) cointcore$session.refreshPatternPreview();
    }

    @WrapOperation(method = "addPatternSlots", at = @At(value = "NEW",
            target = "com/refinedmods/refinedstorage/common/support/containermenu/ValidatedSlot"), require = 1)
    private ValidatedSlot cointcore$networkPatternPreview(Container container, int slot, int x, int y,
                                                         Predicate<ItemStack> filter, Operation<ValidatedSlot> original) {
        if (cointcore$reference == null) return original.call(container, slot, x, y, filter);
        return new NetworkPatternSlot(container, slot, x, y);
    }

    @WrapOperation(method = "addPatternSlots", at = @At(value = "INVOKE",
            target = "Lcom/refinedmods/refinedstorage/common/support/containermenu/TransferManager;addBiTransfer(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;)V"), require = 1)
    private void cointcore$disablePreviewTransfers(TransferManager manager, Container inventory, Container input,
                                                  Operation<Void> original) {
        if (cointcore$reference == null) original.call(manager, inventory, input);
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
