package com.mawlee.cointcore.keepinventory;

import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

import java.util.ArrayList;
import java.util.List;

public final class KeepInventoryCaptures {
    private static final List<KeepInventoryCaptureProvider> PROVIDERS = buildProviders();

    private KeepInventoryCaptures() {
    }

    public static List<KeepInventoryCaptureProvider> providers() {
        return PROVIDERS;
    }

    private static List<KeepInventoryCaptureProvider> buildProviders() {
        List<KeepInventoryCaptureProvider> providers = new ArrayList<>();
        providers.add(new VanillaKeepInventoryCapture());

        if (isModLoaded("curios")) {
            providers.add(loadProvider("com.mawlee.cointcore.keepinventory.integrations.CuriosKeepInventoryCapture"));
        }
        if (isModLoaded("accessories")) {
            providers.add(loadProvider("com.mawlee.cointcore.keepinventory.integrations.AccessoriesKeepInventoryCapture"));
        }

        providers.add(loadProvider("com.mawlee.cointcore.keepinventory.integrations.AttachmentKeepInventoryCapture"));
        return List.copyOf(providers);
    }

    private static KeepInventoryCaptureProvider loadProvider(String className) {
        try {
            Class<?> type = Class.forName(className);
            Object instance = type.getDeclaredConstructor().newInstance();
            return (KeepInventoryCaptureProvider) instance;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to load keep-inventory provider: " + className, exception);
        }
    }

    private static boolean isModLoaded(String modId) {
        return LoadingModList.get().getMods().stream()
                .map(ModInfo::getModId)
                .anyMatch(modId::equals);
    }
}
