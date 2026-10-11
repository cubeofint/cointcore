package com.mawlee.cointcore.industrialforegoingsouls;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reads IF Souls 1.10.7 surge soul state without a compile-time dependency.
 * {@code tickingTime} is leftover paid soul duration; {@code getNetwork().getSoulAmount()}
 * is the pipe network tank. {@link #hasSouls} is the pre-refill
 * {@link SoulSurgeAcceleration#canWorkThisTick(int, int)} test.
 */
public final class SoulSurgeSouls {
    private SoulSurgeSouls() {
    }

    public static boolean hasSouls(Object surgeBlockEntity) {
        return SoulSurgeAcceleration.canWorkThisTick(
                tickingTime(surgeBlockEntity),
                networkSoulAmount(surgeBlockEntity)
        );
    }

    public static int tickingTime(Object surgeBlockEntity) {
        if (surgeBlockEntity == null) {
            return 0;
        }
        try {
            Field field = findField(surgeBlockEntity.getClass(), "tickingTime");
            if (field == null) {
                return 0;
            }
            field.setAccessible(true);
            return field.getInt(surgeBlockEntity);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    public static int networkSoulAmount(Object surgeBlockEntity) {
        if (surgeBlockEntity == null) {
            return 0;
        }
        try {
            Method getNetwork = surgeBlockEntity.getClass().getMethod("getNetwork");
            Object network = getNetwork.invoke(surgeBlockEntity);
            if (network == null) {
                return 0;
            }
            Object amount = network.getClass().getMethod("getSoulAmount").invoke(network);
            return amount instanceof Integer value ? value : 0;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
