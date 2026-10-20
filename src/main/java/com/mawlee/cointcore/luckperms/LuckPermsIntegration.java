package com.mawlee.cointcore.luckperms;

import net.neoforged.fml.ModList;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class LuckPermsIntegration {
    private static final String PROVIDER_CLASS = "net.luckperms.api.LuckPermsProvider";
    private static final ConcurrentHashMap<UUID, TemporaryGrantSnapshot> GRANT_CACHE = new ConcurrentHashMap<>();

    private LuckPermsIntegration() {
    }

    public static void invalidateTemporaryGrantCache(UUID playerId) {
        if (playerId != null) {
            GRANT_CACHE.remove(playerId);
        }
    }

    static void clearTemporaryGrantCache() {
        GRANT_CACHE.clear();
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
        if (!isAvailable() || playerId == null) {
            return false;
        }

        try {
            Object user = getUser(playerId);
            if (user == null) {
                return false;
            }
            return grantSnapshot(playerId, user).hadExpired(permission);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static Optional<Instant> earliestActiveTemporaryGrantExpiry(UUID playerId, String permission) {
        if (!isAvailable() || playerId == null || permission == null || permission.isBlank()) {
            return Optional.empty();
        }

        try {
            Object user = getUser(playerId);
            if (user == null || !hasPermission(user, permission)) {
                return Optional.empty();
            }
            return grantSnapshot(playerId, user).earliestActive(permission);
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

    public static Optional<String> getMetaValue(UUID playerId, String metaKey) {
        if (!isAvailable() || playerId == null || metaKey == null || metaKey.isBlank()) {
            return Optional.empty();
        }

        try {
            Object user = getUser(playerId);
            if (user == null) {
                return Optional.empty();
            }

            Object cachedData = invoke(user, "getCachedData");
            Object metaData = invoke(cachedData, "getMetaData");
            String value = (String) invoke(metaData, "getMetaValue", new Class<?>[]{String.class}, metaKey);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(value.trim());
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    /** LuckPerms chat prefix (raw, with {@code &} codes), empty if unset or the player is not loaded. */
    public static Optional<String> getPrefix(UUID playerId) {
        if (!isAvailable() || playerId == null) {
            return Optional.empty();
        }
        try {
            Object user = getUser(playerId);
            if (user == null) {
                return Optional.empty();
            }
            Object metaData = invoke(invoke(user, "getCachedData"), "getMetaData");
            String prefix = (String) invoke(metaData, "getPrefix");
            return prefix == null || prefix.isEmpty() ? Optional.empty() : Optional.of(prefix);
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    public static int getMetaInt(UUID playerId, String metaKey, int defaultValue) {
        Optional<String> value = getMetaValue(playerId, metaKey);
        if (value.isEmpty()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.get());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public static void registerUserDataRecalculateListener(Consumer<UUID> listener) {
        subscribe("net.luckperms.api.event.user.UserDataRecalculateEvent", event -> {
            try {
                Object user = invoke(event, "getUser");
                UUID playerId = (UUID) invoke(user, "getUniqueId");
                invalidateTemporaryGrantCache(playerId);
                listener.accept(playerId);
            } catch (ReflectiveOperationException ignored) {
            }
        });
    }

    private static TemporaryGrantSnapshot grantSnapshot(UUID playerId, Object user) {
        return GRANT_CACHE.computeIfAbsent(playerId, ignored -> LuckPermsGrantScanner.scanTemporaryGrants(user));
    }

    /**
     * Group-side mutations that can change the quest catalog: node edits on a group,
     * group data recalc and create. Delete is registered separately so the exporter
     * can always push an authoritative full catalog.
     */
    public static void registerGroupCatalogListener(Runnable listener) {
        subscribe("net.luckperms.api.event.node.NodeMutateEvent", event -> {
            try {
                if (Boolean.TRUE.equals(invoke(event, "isGroup"))) {
                    listener.run();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        });
        subscribe("net.luckperms.api.event.group.GroupDataRecalculateEvent", event -> listener.run());
        subscribe("net.luckperms.api.event.group.GroupCreateEvent", event -> listener.run());
    }

    public static void registerGroupDeleteListener(Runnable listener) {
        subscribe("net.luckperms.api.event.group.GroupDeleteEvent", event -> listener.run());
    }

    private static void subscribe(String eventClassName, Consumer<Object> listener) {
        if (!isAvailable()) {
            return;
        }
        try {
            Object provider = invokeStatic(PROVIDER_CLASS, "get");
            Object eventBus = invoke(provider, "getEventBus");
            Class<?> eventClass = Class.forName(eventClassName);
            invoke(eventBus, "subscribe", new Class<?>[]{Class.class, Consumer.class}, eventClass, listener);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    static Object provider() throws ReflectiveOperationException {
        return invokeStatic(PROVIDER_CLASS, "get");
    }

    static Object getUser(UUID playerId) throws ReflectiveOperationException {
        Object userManager = invoke(provider(), "getUserManager");
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

    static Object getNodeType(String name) throws ReflectiveOperationException {
        Class<?> nodeTypeClass = Class.forName("net.luckperms.api.node.NodeType");
        return nodeTypeClass.getField(name).get(null);
    }

    static Class<?> queryOptionsClass() throws ClassNotFoundException {
        return Class.forName("net.luckperms.api.query.QueryOptions");
    }

    static Object invokeStatic(String className, String methodName, Object... args)
            throws ReflectiveOperationException {
        Class<?> owner = Class.forName(className);
        return invoke(owner, null, methodName, args);
    }

    static Object invoke(Object target, String methodName, Object... args)
            throws ReflectiveOperationException {
        return PublicApiMethodResolver.invoke(target, methodName, args);
    }

    static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args)
            throws ReflectiveOperationException {
        return PublicApiMethodResolver.invoke(target, methodName, parameterTypes, args);
    }

    static Object invoke(Class<?> owner, Object target, String methodName, Object... args)
            throws ReflectiveOperationException {
        return PublicApiMethodResolver.invoke(owner, target, methodName, args);
    }

    static Object invokeExact(Class<?> owner, Object target, String methodName, Class<?>[] parameterTypes, Object... args)
            throws ReflectiveOperationException {
        return PublicApiMethodResolver.resolve(owner, methodName, parameterTypes).invoke(target, args);
    }
}
