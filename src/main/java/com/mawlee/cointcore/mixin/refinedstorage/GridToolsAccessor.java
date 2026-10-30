package com.mawlee.cointcore.mixin.refinedstorage;

import com.refinedmods.refinedstorage.common.api.grid.Grid;
import com.refinedmods.refinedstorage.common.grid.AbstractGridContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractGridContainerMenu.class, remap = false)
public interface GridToolsAccessor {
    @Accessor("grid") Grid cointcore$grid();
}
