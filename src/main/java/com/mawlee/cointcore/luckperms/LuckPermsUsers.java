package com.mawlee.cointcore.luckperms;

import com.mawlee.cointcore.config.PrefixConfig;
import com.mawlee.cointcore.prefix.PrefixOption;
import com.mawlee.cointcore.prefix.PrefixSelectionRules;
import com.mawlee.cointcore.prefix.PrefixSnapshot;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * LuckPerms user/group mutations used by prefix selection and the speedrunner title.
 * All access is reflective so the API jar is compileOnly.
 */
public final class LuckPermsUsers {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String GLOBAL_SERVER = "global";

    private LuckPermsUsers() {
    }

    public static List<String> trackGroups(String trackName) {
        if (!LuckPermsIntegration.isAvailable() || trackName == null || trackName.isBlank()) {
            return List.of();
        }
        try {
            Object trackManager = LuckPermsIntegration.invoke(LuckPermsIntegration.provider(), "getTrackManager");
            Object track = LuckPermsIntegration.invoke(
                    trackManager,
                    "getTrack",
                    new Class<?>[]{String.class},
                    trackName
            );
            if (track == null) {
                return List.of();
            }
            @SuppressWarnings("unchecked")
            Collection<String> groups = (Collection<String>) LuckPermsIntegration.invoke(track, "getGroups");
            return normalizeNames(groups);
        } catch (ReflectiveOperationException ignored) {
            return List.of();
        }
    }

    public static List<String> staffGroups() {
        PrefixConfig.Settings settings = PrefixConfig.get();
        List<String> fromTrack = trackGroups(settings.staffTrack());
        return fromTrack.isEmpty() ? settings.staffGroups() : fromTrack;
    }

    public static List<String> ranksTrackGroups() {
        return trackGroups(PrefixConfig.get().ranksTrack());
    }

    public static Optional<String> loadedUsername(UUID playerId) {
        if (!LuckPermsIntegration.isAvailable() || playerId == null) {
            return Optional.empty();
        }
        try {
            Object user = LuckPermsIntegration.getUser(playerId);
            if (user == null) {
                return Optional.empty();
            }
            String name = (String) LuckPermsIntegration.invoke(user, "getUsername");
            return name == null || name.isBlank() ? Optional.empty() : Optional.of(name);
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    public static Optional<PrefixSnapshot> snapshotIfLoaded(UUID playerId) {
        if (!LuckPermsIntegration.isAvailable() || playerId == null) {
            return Optional.empty();
        }
        try {
            Object user = LuckPermsIntegration.getUser(playerId);
            return user == null ? Optional.empty() : Optional.of(snapshotOf(user));
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    public static CompletableFuture<PrefixSnapshot> loadSnapshot(UUID playerId) {
        return loadUser(playerId).thenApply(LuckPermsUsers::snapshotOf);
    }

    public static CompletableFuture<Void> applyPrefixOverride(UUID playerId, String group, String prefixRaw) {
        PrefixConfig.Settings settings = PrefixConfig.get();
        return modifyUser(playerId, user -> {
            try {
                clearOwnOverrideNodes(user, settings);
                if (group == null || group.isBlank() || prefixRaw == null || prefixRaw.isBlank()) {
                    return;
                }
                Object node = buildPrefixNode(prefixRaw, group, settings);
                Object data = LuckPermsIntegration.invoke(user, "data");
                Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
                LuckPermsIntegration.invoke(data, "add", new Class<?>[]{nodeClass}, node);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to apply prefix override", exception);
            }
        });
    }

    public static CompletableFuture<Void> clearPrefixOverride(UUID playerId) {
        PrefixConfig.Settings settings = PrefixConfig.get();
        return modifyUser(playerId, user -> {
            try {
                clearOwnOverrideNodes(user, settings);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to clear prefix override", exception);
            }
        });
    }

    public static CompletableFuture<Void> addGroup(UUID playerId, String group) {
        String name = PrefixSnapshot.normalize(group);
        if (name == null) {
            return CompletableFuture.completedFuture(null);
        }
        return modifyUser(playerId, user -> {
            try {
                Object node = buildInheritanceNode(name);
                Object data = LuckPermsIntegration.invoke(user, "data");
                Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
                LuckPermsIntegration.invoke(data, "add", new Class<?>[]{nodeClass}, node);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to add group " + name, exception);
            }
        });
    }

    public static CompletableFuture<Void> removeGroup(UUID playerId, String group) {
        String name = PrefixSnapshot.normalize(group);
        if (name == null) {
            return CompletableFuture.completedFuture(null);
        }
        return modifyUser(playerId, user -> {
            try {
                Object inheritanceType = LuckPermsIntegration.getNodeType("INHERITANCE");
                Object data = LuckPermsIntegration.invoke(user, "data");
                Class<?> predicateClass = Class.forName("java.util.function.Predicate");
                LuckPermsIntegration.invoke(data, "clear", new Class<?>[]{predicateClass}, (java.util.function.Predicate<Object>) node ->
                        isInheritanceGroup(node, inheritanceType, name));
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to remove group " + name, exception);
            }
        });
    }

    public static CompletableFuture<Void> modifyUser(UUID playerId, Consumer<Object> mutator) {
        if (!LuckPermsIntegration.isAvailable() || playerId == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("LuckPerms unavailable"));
        }
        try {
            Object userManager = LuckPermsIntegration.invoke(LuckPermsIntegration.provider(), "getUserManager");
            @SuppressWarnings("unchecked")
            CompletableFuture<Void> future = (CompletableFuture<Void>) userManager.getClass()
                    .getMethod("modifyUser", UUID.class, Consumer.class)
                    .invoke(userManager, playerId, (Consumer<Object>) mutator::accept);
            return future;
        } catch (ReflectiveOperationException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    public static CompletableFuture<Object> loadUser(UUID playerId) {
        if (!LuckPermsIntegration.isAvailable() || playerId == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("LuckPerms unavailable"));
        }
        try {
            Object loaded = LuckPermsIntegration.getUser(playerId);
            if (loaded != null) {
                return CompletableFuture.completedFuture(loaded);
            }
            Object userManager = LuckPermsIntegration.invoke(LuckPermsIntegration.provider(), "getUserManager");
            @SuppressWarnings("unchecked")
            CompletableFuture<Object> future = (CompletableFuture<Object>) LuckPermsIntegration.invoke(
                    userManager,
                    "loadUser",
                    new Class<?>[]{UUID.class},
                    playerId
            );
            return future;
        } catch (ReflectiveOperationException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    static PrefixSnapshot snapshotOf(Object user) {
        PrefixConfig.Settings settings = PrefixConfig.get();
        List<String> staff = staffGroups();
        List<HeldGroup> held = inheritedGroups(user);
        List<PrefixOption> heldOptions = new ArrayList<>();
        List<String> heldNames = new ArrayList<>();
        for (HeldGroup group : held) {
            heldNames.add(group.name());
            String prefix = group.prefixRaw();
            if (prefix == null || prefix.isBlank()) {
                prefix = groupPrefix(group.name());
            }
            heldOptions.add(new PrefixOption(group.name(), prefix == null ? "" : prefix, categorize(group.name(), settings)));
        }
        boolean locked = PrefixSelectionRules.isStaffLocked(heldNames, staff);
        List<PrefixOption> available = PrefixSelectionRules.available(heldOptions, staff, settings.excludeGroups());
        String selected = selectedOverride(user, settings);
        return new PrefixSnapshot(locked, available, selected);
    }

    static String categorize(String group, PrefixConfig.Settings settings) {
        String override = settings.categoryOverrides().get(PrefixSnapshot.normalize(group));
        if (override != null) {
            return override;
        }
        return PrefixSelectionRules.category(
                group,
                ranksTrackGroups(),
                settings.donationGroups(),
                settings.mediaGroups(),
                settings.builderGroups(),
                settings.defaultGroup(),
                settings.unmatchedCategory()
        );
    }

    private static List<HeldGroup> inheritedGroups(Object user) {
        List<HeldGroup> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        try {
            Object queryOptions = LuckPermsIntegration.invoke(user, "getQueryOptions");
            if (queryOptions == null) {
                return List.of();
            }
            @SuppressWarnings("unchecked")
            Collection<Object> groups = (Collection<Object>) LuckPermsIntegration.invoke(
                    user,
                    "getInheritedGroups",
                    new Class<?>[]{LuckPermsIntegration.queryOptionsClass()},
                    queryOptions
            );
            if (groups != null) {
                for (Object group : groups) {
                    String name = PrefixSnapshot.normalize((String) LuckPermsIntegration.invoke(group, "getName"));
                    if (name == null || !seen.add(name)) {
                        continue;
                    }
                    result.add(new HeldGroup(name, readPrefix(group)));
                }
            }
        } catch (ReflectiveOperationException exception) {
            LOGGER.debug("Failed to resolve inherited groups", exception);
        }
        return result;
    }

    private static String selectedOverride(Object user, PrefixConfig.Settings settings) {
        try {
            Object prefixType = LuckPermsIntegration.getNodeType("PREFIX");
            Iterable<?> nodes = (Iterable<?>) LuckPermsIntegration.invoke(user, "getNodes");
            if (nodes == null) {
                return null;
            }
            String server = serverName();
            for (Object node : nodes) {
                if (!prefixType.equals(LuckPermsIntegration.invoke(node, "getType"))) {
                    continue;
                }
                if (Boolean.TRUE.equals(LuckPermsIntegration.invoke(node, "hasExpired"))) {
                    continue;
                }
                String tagged = overrideGroup(node, settings);
                if (tagged == null || !matchesThisServer(node, server)) {
                    continue;
                }
                return tagged;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static void clearOwnOverrideNodes(Object user, PrefixConfig.Settings settings)
            throws ReflectiveOperationException {
        Object prefixType = LuckPermsIntegration.getNodeType("PREFIX");
        String server = serverName();
        Object data = LuckPermsIntegration.invoke(user, "data");
        Class<?> predicateClass = Class.forName("java.util.function.Predicate");
        LuckPermsIntegration.invoke(data, "clear", new Class<?>[]{predicateClass}, (java.util.function.Predicate<Object>) node -> {
            try {
                if (!prefixType.equals(LuckPermsIntegration.invoke(node, "getType"))) {
                    return false;
                }
                return overrideGroup(node, settings) != null && matchesThisServer(node, server);
            } catch (ReflectiveOperationException ignored) {
                return false;
            }
        });
    }

    private static String overrideGroup(Object node, PrefixConfig.Settings settings) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> metadata = (Map<String, String>) LuckPermsIntegration.invoke(node, "getMetadata");
            if (metadata == null) {
                return null;
            }
            String tagged = metadata.get(settings.prefixMetaKey());
            return PrefixSnapshot.normalize(tagged);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean matchesThisServer(Object node, String server) {
        if (server == null || GLOBAL_SERVER.equals(server)) {
            return true;
        }
        try {
            Object contexts = LuckPermsIntegration.invoke(node, "getContexts");
            @SuppressWarnings("unchecked")
            Set<String> values = (Set<String>) LuckPermsIntegration.invoke(
                    contexts,
                    "getValues",
                    new Class<?>[]{String.class},
                    "server"
            );
            if (values == null || values.isEmpty()) {
                return true;
            }
            for (String value : values) {
                if (server.equalsIgnoreCase(value)) {
                    return true;
                }
            }
            return false;
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    private static Object buildPrefixNode(String prefixRaw, String group, PrefixConfig.Settings settings)
            throws ReflectiveOperationException {
        Class<?> prefixNode = Class.forName("net.luckperms.api.node.types.PrefixNode");
        Object builder = prefixNode.getMethod("builder", String.class, int.class)
                .invoke(null, prefixRaw, settings.prefixPriority());
        builder = builder.getClass()
                .getMethod("withMetadata", String.class, String.class)
                .invoke(builder, settings.prefixMetaKey(), PrefixSnapshot.normalize(group));
        String server = serverName();
        if (server != null && !GLOBAL_SERVER.equals(server)) {
            builder = builder.getClass()
                    .getMethod("withContext", String.class, String.class)
                    .invoke(builder, "server", server);
        }
        return builder.getClass().getMethod("build").invoke(builder);
    }

    private static Object buildInheritanceNode(String group) throws ReflectiveOperationException {
        Class<?> inheritance = Class.forName("net.luckperms.api.node.types.InheritanceNode");
        Object builder = inheritance.getMethod("builder", String.class).invoke(null, group);
        String server = serverName();
        if (server != null && !GLOBAL_SERVER.equals(server)) {
            builder = builder.getClass()
                    .getMethod("withContext", String.class, String.class)
                    .invoke(builder, "server", server);
        }
        return builder.getClass().getMethod("build").invoke(builder);
    }

    private static boolean isInheritanceGroup(Object node, Object inheritanceType, String group) {
        try {
            if (!inheritanceType.equals(LuckPermsIntegration.invoke(node, "getType"))) {
                return false;
            }
            String name = PrefixSnapshot.normalize((String) LuckPermsIntegration.invoke(node, "getGroupName"));
            return group.equals(name) && matchesThisServer(node, serverName());
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static String groupPrefix(String groupName) {
        try {
            Object groupManager = LuckPermsIntegration.invoke(LuckPermsIntegration.provider(), "getGroupManager");
            Object group = LuckPermsIntegration.invoke(groupManager, "getGroup", new Class<?>[]{String.class}, groupName);
            return group == null ? "" : readPrefix(group);
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private static String readPrefix(Object group) {
        try {
            Object cached = LuckPermsIntegration.invoke(group, "getCachedData");
            Object meta = LuckPermsIntegration.invoke(cached, "getMetaData");
            String prefix = (String) LuckPermsIntegration.invoke(meta, "getPrefix");
            return prefix == null ? "" : prefix;
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    static String serverName() {
        try {
            String name = (String) LuckPermsIntegration.invoke(LuckPermsIntegration.provider(), "getServerName");
            if (name == null || name.isBlank()) {
                return GLOBAL_SERVER;
            }
            return name.trim().toLowerCase(Locale.ROOT);
        } catch (ReflectiveOperationException ignored) {
            return GLOBAL_SERVER;
        }
    }

    private static List<String> normalizeNames(Collection<String> values) {
        if (values == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            String normalized = PrefixSnapshot.normalize(value);
            if (normalized != null && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return List.copyOf(result);
    }

    private record HeldGroup(String name, String prefixRaw) {
    }
}
