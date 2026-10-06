package com.mawlee.cointcore.luckperms;

import net.neoforged.fml.ModList;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

public final class LuckPermsIntegration {
    private static final String PROVIDER_CLASS = "net.luckperms.api.LuckPermsProvider";

    private LuckPermsIntegration() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded("luckperms");
    }

    public static boolean hasActiveTemporaryGrant(UUID playerId, String permission) {
        try {
            if (!hasPermission(playerId, permission)) {
                return false;
            }
        } catch (ReflectiveOperationException ignored) {
            return false;
        }

        return earliestActiveTemporaryGrantExpiry(playerId, permission).isPresent();
    }

    public static boolean hadTemporaryGrantExpire(UUID playerId, String permission) {
        if (!isAvailable()) {
            return false;
        }

        try {
            Object user = getUser(playerId);
            if (user == null) {
                return false;
            }

            Object queryOptions = invoke(user, "getQueryOptions");
            if (queryOptions == null) {
                return false;
            }

            Object permissionNodeType = getNodeType("PERMISSION");
            Object inheritanceNodeType = getNodeType("INHERITANCE");
            Iterable<?> nodes = (Iterable<?>) invoke(
                    user,
                    "resolveDistinctInheritedNodes",
                    new Class<?>[]{queryOptionsClass()},
                    queryOptions
            );

            if (nodes == null) {
                return false;
            }

            for (Object node : nodes) {
                Object type = invoke(node, "getType");
                boolean expiredGrant = Boolean.TRUE.equals(invoke(node, "hasExpiry"))
                        && Boolean.TRUE.equals(invoke(node, "hasExpired"))
                        && Boolean.TRUE.equals(invoke(node, "getValue"));

                if (!expiredGrant) {
                    continue;
                }

                if (permissionNodeType.equals(type) && permission.equals(invoke(node, "getKey"))) {
                    return true;
                }

                if (inheritanceNodeType.equals(type)) {
                    return true;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return false;
    }

    public static Optional<Instant> earliestActiveTemporaryGrantExpiry(UUID playerId, String permission) {
        if (!isAvailable()) {
            return Optional.empty();
        }

        try {
            Object user = getUser(playerId);
            if (user == null || !hasPermission(user, permission)) {
                return Optional.empty();
            }

            Object queryOptions = invoke(user, "getQueryOptions");
            if (queryOptions == null) {
                return Optional.empty();
            }

            Object permissionNodeType = getNodeType("PERMISSION");
            Object inheritanceNodeType = getNodeType("INHERITANCE");
            Iterable<?> nodes = (Iterable<?>) invoke(
                    user,
                    "resolveDistinctInheritedNodes",
                    new Class<?>[]{queryOptionsClass()},
                    queryOptions
            );

            if (nodes == null) {
                return Optional.empty();
            }

            Instant earliest = null;
            for (Object node : nodes) {
                if (!Boolean.TRUE.equals(invoke(node, "hasExpiry"))
                        || Boolean.TRUE.equals(invoke(node, "hasExpired"))
                        || !Boolean.TRUE.equals(invoke(node, "getValue"))) {
                    continue;
                }

                Object type = invoke(node, "getType");
                boolean matches = permissionNodeType.equals(type) && permission.equals(invoke(node, "getKey"))
                        || inheritanceNodeType.equals(type);

                if (!matches) {
                    continue;
                }

                Instant expiry = (Instant) invoke(node, "getExpiry");
                if (expiry == null) {
                    continue;
                }

                if (earliest == null || expiry.isBefore(earliest)) {
                    earliest = expiry;
                }
            }

            return Optional.ofNullable(earliest);
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    /**
     * LuckPerms tristate for a node. Empty means unset, player not loaded, or LP missing.
     */
    public static Optional<Boolean> permissionTristate(UUID playerId, String permission) {
        if (!isAvailable() || playerId == null || permission == null || permission.isBlank()) {
            return Optional.empty();
        }

        try {
            Object user = getUser(playerId);
            if (user == null) {
                return Optional.empty();
            }

            Object cachedData = invoke(user, "getCachedData");
            Object permissionData = invoke(cachedData, "getPermissionData");
            Object result = invoke(permissionData, "checkPermission", new Class<?>[]{String.class}, permission);
            String name = result instanceof Enum<?> value ? value.name() : String.valueOf(result);
            if ("TRUE".equals(name)) {
                return Optional.of(true);
            }
            if ("FALSE".equals(name)) {
                return Optional.of(false);
            }
            return Optional.empty();
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    public static OptionalInt primaryGroupWeight(UUID playerId) {
        if (!isAvailable() || playerId == null) {
            return OptionalInt.empty();
        }

        try {
            Object user = getUser(playerId);
            if (user == null) {
                return OptionalInt.empty();
            }

            String primaryGroup = (String) invoke(user, "getPrimaryGroup");
            if (primaryGroup == null || primaryGroup.isBlank()) {
                return OptionalInt.empty();
            }

            Object provider = invokeStatic(PROVIDER_CLASS, "get");
            Object groupManager = invoke(provider, "getGroupManager");
            Object group = invoke(groupManager, "getGroup", new Class<?>[]{String.class}, primaryGroup);
            if (group == null) {
                return OptionalInt.empty();
            }

            Object weight = invoke(group, "getWeight");
            if (weight instanceof OptionalInt optionalInt) {
                return optionalInt;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return OptionalInt.empty();
    }

    public static int getMetaInt(UUID playerId, String metaKey, int defaultValue) {
        if (!isAvailable() || metaKey == null || metaKey.isBlank()) {
            return defaultValue;
        }

        try {
            Object user = getUser(playerId);
            if (user == null) {
                return defaultValue;
            }

            Object cachedData = invoke(user, "getCachedData");
            Object metaData = invoke(cachedData, "getMetaData");
            String value = (String) invoke(metaData, "getMetaValue", new Class<?>[]{String.class}, metaKey);
            if (value == null || value.isBlank()) {
                return defaultValue;
            }

            return Integer.parseInt(value.trim());
        } catch (ReflectiveOperationException | NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public static void registerUserDataRecalculateListener(Consumer<UUID> listener) {
        if (!isAvailable()) {
            return;
        }

        try {
            Object provider = invokeStatic(PROVIDER_CLASS, "get");
            Object eventBus = invoke(provider, "getEventBus");
            Class<?> eventClass = Class.forName("net.luckperms.api.event.user.UserDataRecalculateEvent");
            eventBus.getClass()
                    .getMethod("subscribe", Class.class, Consumer.class)
                    .invoke(eventBus, eventClass, (Consumer<Object>) event -> {
                        try {
                            Object user = invoke(event, "getUser");
                            UUID playerId = (UUID) invoke(user, "getUniqueId");
                            listener.accept(playerId);
                        } catch (ReflectiveOperationException ignored) {
                        }
                    });
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Object getUser(UUID playerId) throws ReflectiveOperationException {
        Object provider = invokeStatic(PROVIDER_CLASS, "get");
        Object userManager = invoke(provider, "getUserManager");
        return invoke(userManager, "getUser", new Class<?>[]{UUID.class}, playerId);
    }

    private static boolean hasPermission(UUID playerId, String permission) throws ReflectiveOperationException {
        Object user = getUser(playerId);
        return user != null && hasPermission(user, permission);
    }

    private static boolean hasPermission(Object user, String permission) throws ReflectiveOperationException {
        Object cachedData = invoke(user, "getCachedData");
        Object permissionData = invoke(cachedData, "getPermissionData");
        Object result = invoke(permissionData, "checkPermission", new Class<?>[]{String.class}, permission);
        return Boolean.TRUE.equals(invoke(result, "asBoolean"));
    }

    private static Object getNodeType(String name) throws ReflectiveOperationException {
        Class<?> nodeTypeClass = Class.forName("net.luckperms.api.node.NodeType");
        return nodeTypeClass.getField(name).get(null);
    }

    private static Class<?> queryOptionsClass() throws ClassNotFoundException {
        return Class.forName("net.luckperms.api.query.QueryOptions");
    }

    private static Object invokeStatic(String className, String methodName, Object... args)
            throws ReflectiveOperationException {
        Class<?> owner = Class.forName(className);
        return invoke(owner, null, methodName, args);
    }

    private static Object invoke(Object target, String methodName, Object... args)
            throws ReflectiveOperationException {
        return invoke(target.getClass(), target, methodName, args);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args)
            throws ReflectiveOperationException {
        return target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
    }

    private static Object invoke(Class<?> owner, Object target, String methodName, Object... args)
            throws ReflectiveOperationException {
        if (args.length == 0) {
            return owner.getMethod(methodName).invoke(target);
        }

        for (java.lang.reflect.Method method : owner.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }

            return method.invoke(target, args);
        }

        throw new NoSuchMethodException(owner.getName() + "#" + methodName);
    }
}
