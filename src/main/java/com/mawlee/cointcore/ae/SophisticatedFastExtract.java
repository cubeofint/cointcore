package com.mawlee.cointcore.ae;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Extraction from Sophisticated Storage inventories through their own slot index
 * ({@code ITrackedContentsItemHandler#extractItem(ItemStack, boolean)}) instead of AE2 walking every slot.
 * The tracked handler applies the storage's output-filter upgrades itself. Sophisticated Core is not a compile
 * dependency: everything goes through cached reflection, and any surprise turns the fast path off.
 *
 * <p>Verified against sophisticatedcore-1.21.1-1.5.12.
 */
public final class SophisticatedFastExtract {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PACKAGE = "net.p3pp3rf1y.sophisticatedcore.inventory.";

    private static volatile boolean available;
    private static Class<?> cachedFailedClass;
    private static Field wrappedGetterField;
    private static Class<?> trackedClass;
    private static Method extractByStack;
    private static Class<?> inventoryHandlerClass;
    private static Method getSlotTracker;
    private static Class<?> noopTrackerClass;
    private static Class<?> filteredClass;
    private static Field filteredInnerField;

    static {
        try {
            cachedFailedClass = Class.forName(PACKAGE + "CachedFailedInsertInventoryHandler");
            wrappedGetterField = cachedFailedClass.getDeclaredField("wrappedHandlerGetter");
            wrappedGetterField.setAccessible(true);
            trackedClass = Class.forName(PACKAGE + "ITrackedContentsItemHandler");
            extractByStack = Class.forName(PACKAGE + "IItemHandlerSimpleExtractor")
                    .getMethod("extractItem", ItemStack.class, boolean.class);
            inventoryHandlerClass = Class.forName(PACKAGE + "InventoryHandler");
            getSlotTracker = inventoryHandlerClass.getMethod("getSlotTracker");
            noopTrackerClass = Class.forName(PACKAGE + "ISlotTracker$Noop");
            filteredClass = Class.forName(PACKAGE + "FilteredItemHandler");
            filteredInnerField = filteredClass.getDeclaredField("inventoryHandler");
            filteredInnerField.setAccessible(true);
            available = true;
        } catch (ClassNotFoundException exception) {
            available = false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            available = false;
            LOGGER.warn("Sophisticated Storage fast AE2 extraction disabled (API changed?)", exception);
        }
    }

    private SophisticatedFastExtract() {
    }

    /** The Sophisticated handler to extract from by stack, or null when AE2 must walk the slots itself. */
    public static Object resolve(IItemHandler handler) {
        if (!available || handler == null) {
            return null;
        }
        try {
            Object target = handler;
            if (cachedFailedClass.isInstance(target)) {
                target = ((Supplier<?>) wrappedGetterField.get(target)).get();
            }
            if (!trackedClass.isInstance(target)) {
                return null;
            }
            Object inner = target;
            while (filteredClass.isInstance(inner)) {
                inner = filteredInnerField.get(inner);
            }
            // Without a real slot tracker the by-stack extract finds nothing; keep AE2's slot walk then.
            if (!inventoryHandlerClass.isInstance(inner) || noopTrackerClass.isInstance(getSlotTracker.invoke(inner))) {
                return null;
            }
            return target;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            disable(exception);
            return null;
        }
    }

    /** Extracts up to {@code amount} of {@code stack}'s item and components; returns how many came out, or -1 on failure. */
    public static int extract(Object tracked, ItemStack stack, int amount, boolean simulate) {
        try {
            ItemStack request = stack.copyWithCount(amount);
            ItemStack extracted = (ItemStack) extractByStack.invoke(tracked, request, simulate);
            return extracted == null ? 0 : Math.min(amount, extracted.getCount());
        } catch (ReflectiveOperationException | RuntimeException exception) {
            disable(exception);
            return -1;
        }
    }

    private static void disable(Throwable exception) {
        if (available) {
            available = false;
            LOGGER.warn("Sophisticated Storage fast AE2 extraction disabled after an error", exception);
        }
    }
}
