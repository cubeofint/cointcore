package com.mawlee.cointcore.mixin.modularbees;

import com.glodblock.github.glodium.network.packet.sync.IActionHolder;
import com.glodblock.github.modularbees.xmod.ae.tileentities.TileMEImport;
import com.mawlee.cointcore.modularbees.ImportAmountDataSlot;
import com.mawlee.cointcore.modularbees.ImportAmountHost;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the import hatch GUI set a per-slot amount: the client sends a ModularBees action, the server applies it,
 * and the amounts reach the client through ordinary container data slots.
 */
@Mixin(targets = "com.glodblock.github.modularbees.xmod.ae.container.ContainerMEImport", remap = false)
public abstract class ContainerMEImportAmountMixin extends AbstractContainerMenu {
    protected ContainerMEImportAmountMixin(@Nullable MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>", at = @At("TAIL"), require = 1)
    private void cointcore$amounts(MenuType<?> type, int id, Inventory inv, TileMEImport tile, CallbackInfo callback) {
        if (!(tile instanceof ImportAmountHost host)) return;
        ((IActionHolder) this).getActionMap().put(ImportAmountHost.SET_ACTION, paras -> {
            Object slot = paras.getSoft(0);
            Object amount = paras.getSoft(1);
            if (slot instanceof Integer s && amount instanceof Integer a && s >= 0 && s < ImportAmountHost.SLOTS && a >= 0) {
                host.cointcore$setAmount(s, a);
            }
        });
        ImportAmountDataSlot.addAll(host, this::addDataSlot);
    }
}
