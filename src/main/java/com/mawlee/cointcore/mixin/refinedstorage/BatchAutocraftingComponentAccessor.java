package com.mawlee.cointcore.mixin.refinedstorage;

import com.refinedmods.refinedstorage.api.autocrafting.PatternRepositoryImpl;
import com.refinedmods.refinedstorage.api.network.impl.autocrafting.AutocraftingNetworkComponentImpl;
import com.refinedmods.refinedstorage.api.storage.root.RootStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

@Mixin(value = AutocraftingNetworkComponentImpl.class, remap = false)
public interface BatchAutocraftingComponentAccessor {
    @Accessor("patternRepository") PatternRepositoryImpl cointcore$batchPatterns();
    @Accessor("rootStorageProvider") Supplier<RootStorage> cointcore$batchStorage();
    @Accessor("executorService") ExecutorService cointcore$batchExecutor();
}
