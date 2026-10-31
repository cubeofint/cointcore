package com.mawlee.cointcore.mixin.refinedstorage;

import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternIntegration;
import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternMenu;
import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternSession;
import com.mawlee.cointcore.refinedstorage.wireless.NetworkPatternSlot;
import com.mawlee.cointcore.refinedstorage.wireless.BlockPatternSession;
import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternBackend;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridBlockEntity;
import net.minecraft.server.level.ServerPlayer;
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PatternGridContainerMenu.class, remap = false)
public abstract class WirelessPatternMenuMixin extends AbstractGridContainerMenu implements WirelessPatternMenu {
    @Unique private SlotReference cointcore$reference;
    @Unique private WirelessPatternSession cointcore$session;
    @Unique private long cointcore$availablePatterns;
    @Unique private BlockPatternSession cointcore$blockSession;
    @Shadow private PatternGridBlockEntity patternGrid;
    @Shadow @Final @Mutable private Container patternInput;
    @Shadow @Final private Container patternOutput;

    @Override public long cointcore$availablePatterns() {
        if (cointcore$blockSession != null) return cointcore$blockSession.available();
        return cointcore$session == null ? cointcore$availablePatterns : cointcore$session.availablePatterns();
    }

    protected WirelessPatternMenuMixin(MenuType<? extends AbstractGridContainerMenu> type, int id,
                                      Inventory inventory, GridData data) {
        super(type, id, inventory, data);
    }

    @Inject(method = {
            "<init>(ILnet/minecraft/world/entity/player/Inventory;Lcom/refinedmods/refinedstorage/common/autocrafting/patterngrid/PatternGridData;)V",
            "<init>(ILnet/minecraft/world/entity/player/Inventory;Lcom/refinedmods/refinedstorage/common/autocrafting/patterngrid/PatternGridBlockEntity;)V"
    }, at = @At("RETURN"), require = 2)
    private void cointcore$initializeNetworkPatterns(CallbackInfo callback) {
        for (int part = 0; part < 4; part++) {
            final int shift = part * 16;
            addDataSlot(new NetworkPatternCountPart(this::cointcore$availablePatterns,
                    value -> cointcore$availablePatterns = value, shift));
        }
        if (patternGrid != null && !(patternGrid instanceof WirelessPatternBackend)
                && playerInventory.player instanceof ServerPlayer player) {
            cointcore$blockSession = new BlockPatternSession(patternGrid, player);
            patternInput = cointcore$blockSession.preview();
            resized(0, 0, 0);
        }
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
        // Also reject creative clone clicks, which do not consult Slot.mayPickup.
        if (slot >= 0 && slot < slots.size() && slots.get(slot) instanceof NetworkPatternSlot) {
            if (player instanceof ServerPlayer && type == ClickType.PICKUP && (button == 0 || button == 1)) {
                if (cointcore$session != null) cointcore$session.selectBlank(getCarried(), button == 1);
                if (cointcore$blockSession != null) cointcore$blockSession.select(getCarried(), button == 1);
                broadcastChanges();
            }
            return;
        }
        if (cointcore$session == null || cointcore$session.canUse()) super.clicked(slot, button, type, player);
    }

    @Inject(method = "createPattern", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$validatePatternCreation(CallbackInfo callback) {
        if (cointcore$session != null) {
            if (!cointcore$session.canUse()) callback.cancel();
            else cointcore$session.refreshPatternPreview();
        }
    }

    @Inject(method = "canCreatePattern", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$requireAvailableBlank(CallbackInfoReturnable<Boolean> callback) {
        if (cointcore$availablePatterns() <= 0 && patternOutput.isEmpty()) callback.setReturnValue(false);
    }

    @Inject(method = "createPattern", at = @At("RETURN"), require = 1)
    private void cointcore$refreshAfterEncoding(CallbackInfo callback) {
        if (cointcore$session != null) cointcore$session.refreshPatternPreview();
    }

    @WrapOperation(method = "createPattern", at = @At(value = "INVOKE",
            target = "Lcom/refinedmods/refinedstorage/common/autocrafting/patterngrid/PatternGridBlockEntity;createPattern()V"), require = 1)
    private void cointcore$encodeFromBlockNetwork(PatternGridBlockEntity grid, Operation<Void> original) {
        if (cointcore$blockSession == null) original.call(grid);
        else cointcore$blockSession.encode(() -> original.call(grid));
    }

    @WrapOperation(method = "addPatternSlots", at = @At(value = "NEW",
            target = "com/refinedmods/refinedstorage/common/support/containermenu/ValidatedSlot"), require = 1)
    private ValidatedSlot cointcore$networkPatternPreview(Container container, int slot, int x, int y,
                                                         Predicate<ItemStack> filter, Operation<ValidatedSlot> original) {
        return new NetworkPatternSlot(container, slot, x, y);
    }

    @WrapOperation(method = "addPatternSlots", at = @At(value = "INVOKE",
            target = "Lcom/refinedmods/refinedstorage/common/support/containermenu/TransferManager;addBiTransfer(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;)V"), require = 1)
    private void cointcore$disablePreviewTransfers(TransferManager manager, Container inventory, Container input,
                                                  Operation<Void> original) {
        // Both wired and wireless inputs are read-only network previews.
    }

    @Override
    public void broadcastChanges() {
        if (cointcore$session != null) cointcore$session.refresh();
        if (cointcore$blockSession != null) cointcore$blockSession.refresh();
        super.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        if (cointcore$session != null) cointcore$session.save();
        super.removed(player);
    }
}
