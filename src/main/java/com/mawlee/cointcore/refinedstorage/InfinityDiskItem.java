package com.mawlee.cointcore.refinedstorage;

import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageContainerItem;
import com.refinedmods.refinedstorage.common.api.storage.StorageInfo;
import com.refinedmods.refinedstorage.common.api.storage.StorageRepository;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;

public final class InfinityDiskItem extends Item implements StorageContainerItem {
    private final Supplier<InfinityResourceStorage> storage;
    private final BooleanSupplier available;
    private final int color;

    public InfinityDiskItem(Supplier<InfinityResourceStorage> storage, BooleanSupplier available, int color) {
        super(new Properties().stacksTo(1));
        this.storage = storage;
        this.available = available;
        this.color = color;
    }

    public int color() {
        return color;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (!available.getAsBoolean()) {
            return Optional.empty();
        }
        return Optional.of(new InfinityDiskTooltip(storage.get().getAll().iterator().next()));
    }

    @Override
    public Optional<SerializableStorage> resolve(StorageRepository repository, ItemStack stack) {
        return available.getAsBoolean() ? Optional.of(storage.get()) : Optional.empty();
    }

    @Override
    public Optional<StorageInfo> getInfo(StorageRepository repository, ItemStack stack) {
        return available.getAsBoolean() ? Optional.of(StorageInfo.of(storage.get())) : Optional.empty();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.cointcore.infinity_disk.source").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.cointcore.infinity_disk.drive").withStyle(ChatFormatting.GRAY));
        if (!available.getAsBoolean()) {
            tooltip.add(Component.translatable("tooltip.cointcore.infinity_disk.unavailable").withStyle(ChatFormatting.RED));
        }
    }
}
