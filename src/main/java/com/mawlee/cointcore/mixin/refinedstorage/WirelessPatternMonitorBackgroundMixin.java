package com.mawlee.cointcore.mixin.refinedstorage;

import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternClient;
import com.refinedmods.refinedstorage.common.autocrafting.monitor.AutocraftingMonitorScreen;
import com.refinedmods.refinedstorage.common.autocrafting.monitor.AbstractAutocraftingMonitorContainerMenu;
import com.refinedmods.refinedstorage.common.support.AbstractBaseScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AutocraftingMonitorScreen.class, remap = false)
public abstract class WirelessPatternMonitorBackgroundMixin
        extends AbstractBaseScreen<AbstractAutocraftingMonitorContainerMenu> {
    protected WirelessPatternMonitorBackgroundMixin(AbstractAutocraftingMonitorContainerMenu menu,
                                                    Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    // Empty/inactive monitors return early; TAIL would only cover the populated monitor.
    @Inject(method = "renderBg", at = @At("RETURN"), require = 1)
    private void cointcore$drawWirelessTabs(GuiGraphics graphics, float partialTick, int x, int y, CallbackInfo callback) {
        // Universal Grid synchronizes the monitor's device slot after screen initialization.
        WirelessPatternClient.ensureTabs(this, this::addRenderableWidget);
        WirelessPatternClient.renderTabBackgrounds(this, graphics);
    }
}
