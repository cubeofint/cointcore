package com.mawlee.cointcore.industrialforegoingsouls;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reads IF Souls 1.10.7 surge soul supply without a compile-time dependency.
 * <p>
 * The surge has no local soul tank. {@code tickingTime} is leftover paid
 * duration after a consume. Fresh souls live on {@code SoulNetwork} (Titanium
 * {@code NetworkManager} graph): {@code getNetwork().getSoulAmount()}, or,
 * on pre-1.10.2 builds, on connected soul laser bases via
 * {@code getSoulLaserDrills()}. {@link #hasSouls} is the pre-refill
 * {@link SoulSurgeAcceleration#canWorkThisTick(int, int)} test.
 */
public final class SoulSurgeSouls {
    private static final String TICKING_TIME = "tickingTime";
    private static final String GET_NETWORK = "getNetwork";
    private static final String GET_SOUL_AMOUNT = "getSoulAmount";
    private static final String GET_SOULS = "getSouls";
    private static final String SOUL_AMOUNT_FIELD = "soulAmount";
    private static final String GET_SOUL_LASER_DRILLS = "getSoulLaserDrills";
    private static final String GET_LEVEL = "getLevel";
    private static final String GET_POS = "getPos";
    private static final String GET_BLOCK_POS = "getBlockPos";
    private static final String GET_BLOCK_ENTITY = "getBlockEntity";
    private static final String GET_ELEMENT = "getElement";
    private static final String NETWORK_MANAGER = "com.hrznstudio.titanium.block_network.NetworkManager";

    private SoulSurgeSouls() {
    }

    public static boolean hasSouls(Object surgeBlockEntity) {
        return SoulSurgeAcceleration.canWorkThisTick(
                tickingTime(surgeBlockEntity),
                networkSoulAmount(surgeBlockEntity)
        );
    }

    /**
     * Leftover paid duration. Mixins with {@code defaultRequire 0} can inject
     * a dummy {@code tickingTime = 0} on a subclass; the IF Souls field stays
     * on {@code SoulSurgeBlockEntity}. Use the highest value in the hierarchy.
     */
    public static int tickingTime(Object surgeBlockEntity) {
        return maxIntField(surgeBlockEntity, TICKING_TIME);
    }

    public static int networkSoulAmount(Object surgeBlockEntity) {
        if (surgeBlockEntity == null) {
            return 0;
        }
        Object network = networkOf(surgeBlockEntity);
        int stored = soulAmountOf(network);
        if (stored > 0) {
            return stored;
        }
        return soulAmountFromLaserDrills(network);
    }

    private static Object networkOf(Object surgeBlockEntity) {
        Object network = invokeNoArg(surgeBlockEntity, GET_NETWORK);
        if (network != null) {
            return network;
        }
        return networkViaManager(surgeBlockEntity);
    }

    private static Object networkViaManager(Object surgeBlockEntity) {
        Object level = invokeNoArg(surgeBlockEntity, GET_LEVEL);
        Object pos = invokeNoArg(surgeBlockEntity, GET_BLOCK_POS);
        if (pos == null) {
            pos = invokeNoArg(surgeBlockEntity, GET_POS);
        }
        if (level == null || pos == null) {
            return null;
        }
        try {
            Class<?> managerType = Class.forName(NETWORK_MANAGER);
            Method get = findMethod(managerType, "get", level.getClass());
            if (get == null) {
                for (Method candidate : managerType.getMethods()) {
                    if ("get".equals(candidate.getName()) && candidate.getParameterCount() == 1) {
                        get = candidate;
                        break;
                    }
                }
            }
            if (get == null) {
                return null;
            }
            get.setAccessible(true);
            Object manager = get.invoke(null, level);
            if (manager == null) {
                return null;
            }
            Object element = invoke(manager, GET_ELEMENT, pos);
            if (element == null) {
                return null;
            }
            return invokeNoArg(element, GET_NETWORK);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static int soulAmountOf(Object holder) {
        if (holder == null) {
            return 0;
        }
        int fromGetter = toInt(invokeNoArg(holder, GET_SOUL_AMOUNT));
        if (fromGetter > 0) {
            return fromGetter;
        }
        fromGetter = toInt(invokeNoArg(holder, GET_SOULS));
        if (fromGetter > 0) {
            return fromGetter;
        }
        return maxIntField(holder, SOUL_AMOUNT_FIELD);
    }

    private static int soulAmountFromLaserDrills(Object network) {
        Object drills = invokeNoArg(network, GET_SOUL_LASER_DRILLS);
        if (!(drills instanceof Iterable<?> elements)) {
            return 0;
        }
        int total = 0;
        for (Object element : elements) {
            if (element == null) {
                continue;
            }
            Object level = invokeNoArg(element, GET_LEVEL);
            Object pos = invokeNoArg(element, GET_POS);
            if (level == null || pos == null) {
                continue;
            }
            total += soulAmountOf(invoke(level, GET_BLOCK_ENTITY, pos));
        }
        return total;
    }

    private static int maxIntField(Object instance, String name) {
        if (instance == null) {
            return 0;
        }
        int best = 0;
        Class<?> current = instance.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                best = Math.max(best, toInt(field.get(instance)));
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // try the superclass
            }
            current = current.getSuperclass();
        }
        return best;
    }

    private static Object invokeNoArg(Object target, String name) {
        if (target == null) {
            return null;
        }
        Method method = findMethod(target.getClass(), name);
        if (method == null) {
            return null;
        }
        try {
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Object invoke(Object target, String name, Object argument) {
        if (target == null || argument == null) {
            return null;
        }
        Method method = findMethod(target.getClass(), name, argument.getClass());
        if (method == null) {
            method = findMethodCompatible(target.getClass(), name, argument.getClass());
        }
        if (method == null) {
            return null;
        }
        try {
            method.setAccessible(true);
            return method.invoke(target, argument);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameters) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, parameters);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        try {
            return type.getMethod(name, parameters);
        } catch (NoSuchMethodException | RuntimeException ignored) {
            return null;
        }
    }

    private static Method findMethodCompatible(Class<?> type, String name, Class<?> argumentType) {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (!name.equals(method.getName()) || method.getParameterCount() != 1) {
                    continue;
                }
                if (method.getParameterTypes()[0].isAssignableFrom(argumentType)) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}
