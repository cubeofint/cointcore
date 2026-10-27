package com.mawlee.cointcore.refinedstorage.wireless;

import com.refinedmods.refinedstorage.api.network.node.grid.GridExtractMode;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.repository.ResourceRepository;
import com.refinedmods.refinedstorage.api.storage.tracked.TrackedResource;
import com.refinedmods.refinedstorage.common.api.grid.GridScrollMode;
import com.refinedmods.refinedstorage.common.api.grid.strategy.GridExtractionStrategy;
import com.refinedmods.refinedstorage.common.api.grid.strategy.GridScrollingStrategy;
import com.refinedmods.refinedstorage.common.api.grid.view.GridResource;
import com.refinedmods.refinedstorage.common.api.grid.view.GridResourceAttributeKey;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public record RequestedGridResource(ResourceKey key, GridResource delegate) implements GridResource {
    @Override public void render(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0x55449bbc);
        if (key != null) delegate.render(graphics, x, y);
    }
    @Override public List<Component> getTooltip() {
        var lines = new ArrayList<Component>();
        if (key != null) lines.addAll(delegate.getTooltip());
        lines.add(Component.translatable("tooltip.cointcore.orders.row").withStyle(ChatFormatting.AQUA));
        if (key != null) lines.add(Component.translatable("tooltip.cointcore.orders.remove").withStyle(ChatFormatting.GRAY));
        return lines;
    }
    @Override public long getAmount(ResourceRepository<GridResource> repository) { return key == null ? 0 : delegate.getAmount(repository); }
    @Override public String getDisplayedAmount(ResourceRepository<GridResource> repository) {
        return key == null ? "" : getAmount(repository) == 0 ? "…" : delegate.getDisplayedAmount(repository);
    }
    @Override public boolean canExtract(ItemStack stack, ResourceRepository<GridResource> repository) { return key != null && delegate.canExtract(stack, repository); }
    @Override public void onExtract(GridExtractMode mode, boolean cursor, GridExtractionStrategy strategy) { if (key != null) delegate.onExtract(mode, cursor, strategy); }
    @Override public void onScroll(GridScrollMode mode, GridScrollingStrategy strategy) { if (key != null) delegate.onScroll(mode, strategy); }
    @Override public boolean isAutocraftable(ResourceRepository<GridResource> repository) { return false; }
    @Override public ResourceAmount getAutocraftingRequest() { return null; }
    @Override public String getName() { return key == null ? "" : delegate.getName(); }
    @Override public Set<String> getAttribute(GridResourceAttributeKey attribute) { return delegate.getAttribute(attribute); }
    @Override public TrackedResource getTrackedResource(Function<ResourceKey, TrackedResource> lookup) { return delegate.getTrackedResource(lookup); }
    @Override public String getAmountInTooltip(ResourceRepository<GridResource> repository) { return key == null ? "" : delegate.getAmountInTooltip(repository); }
    @Override public boolean belongsToResourceType(ResourceType type) { return delegate.belongsToResourceType(type); }
    @Override public Optional<TooltipComponent> getTooltipImage() { return key == null ? Optional.empty() : delegate.getTooltipImage(); }
    @Override public int getRegistryId() { return delegate.getRegistryId(); }
    @Override public List<ClientTooltipComponent> getExtractionHints(ItemStack stack, ResourceRepository<GridResource> repository) { return key == null ? List.of() : delegate.getExtractionHints(stack, repository); }
    @Override public PlatformResourceKey getResourceForRecipeMods() { return delegate.getResourceForRecipeMods(); }
}
