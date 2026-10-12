package com.mawlee.cointcore.luckperms;

import com.mawlee.cointcore.config.PrefixConfig;
import com.mawlee.cointcore.prefix.OwnPrefixNodeMatch;
import com.mawlee.cointcore.prefix.PrefixCatalog;
import com.mawlee.cointcore.prefix.PrefixSelectionMeta;
import com.mawlee.cointcore.prefix.PrefixCatalogEntry;
import com.mawlee.cointcore.prefix.PrefixOption;
import com.mawlee.cointcore.prefix.PrefixSelectionRules;
import com.mawlee.cointcore.prefix.PrefixSnapshot;
import com.mawlee.cointcore.prefix.QuestPrefixState;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * LuckPerms user/group mutations used by prefix selection and catalog export.
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
        return LuckPermsUserMutations.enqueue(playerId, () -> loadUser(playerId)
                .thenCompose(user -> persistLostSelectionUnlocked(user)
                        .handle((ignored, error) -> snapshotOf(user))));
    }

    /**
     * If the stored quest selection is gone (group deleted / expired), drop user meta and the
     * render PrefixNode. Remaining quest ranks fall back to auto-render. Works for offline
     * users on the next {@code loadUser}.
     */
    public static CompletableFuture<Void> persistLostSelection(Object user) {
        UUID playerId = uniqueIdOf(user);
        if (playerId == null) {
            return persistLostSelectionUnlocked(user);
        }
        return LuckPermsUserMutations.enqueue(playerId, () -> persistLostSelectionUnlocked(user));
    }

    private static CompletableFuture<Void> persistLostSelectionUnlocked(Object user) {
        if (user == null) {
            return CompletableFuture.completedFuture(null);
        }
        PrefixSnapshot snapshot = snapshotOf(user);
        PrefixSelectionRules.Decision decision = PrefixSelectionRules.reconcile(
                snapshot.selected(),
                snapshot.availableGroups()
        );
        if (!decision.clearOverride()) {
            return CompletableFuture.completedFuture(null);
        }
        PrefixOption highest = snapshot.highestQuest();
        try {
            if (!applyQuestNodesIfChanged(user, null, highest == null ? null : highest.prefixRaw())) {
                return CompletableFuture.completedFuture(null);
            }
        } catch (ReflectiveOperationException exception) {
            return CompletableFuture.failedFuture(exception);
        }
        markOwnWrite(user);
        return saveUserUnlocked(user);
    }

    /**
     * Writes user meta {@code cointcore-prefix-select} (when selected) and a user-level PrefixNode
     * (default priority 5) that LuckPerms stacking can render via {@code highest_own} in slot 2,
     * below staff/donor {@code highest_not_on_track_ranks}.
     * No-ops (no {@code saveUser}) when own prefix/meta already match.
     */
    public static CompletableFuture<Void> syncQuestPrefix(UUID playerId, String selectedGroup, String prefixRaw) {
        return LuckPermsUserMutations.enqueue(playerId, () -> loadUser(playerId).thenCompose(user -> {
            try {
                if (!applyQuestNodesIfChanged(user, selectedGroup, prefixRaw)) {
                    return CompletableFuture.completedFuture(null);
                }
            } catch (ReflectiveOperationException exception) {
                return CompletableFuture.failedFuture(exception);
            }
            RecalcSuppressor.markOwnWrite(playerId, System.currentTimeMillis());
            return saveUserUnlocked(user);
        }));
    }

    static boolean applyQuestNodesIfChanged(Object user, String selectedGroup, String prefixRaw)
            throws ReflectiveOperationException {
        PrefixConfig.Settings settings = PrefixConfig.get();
        QuestPrefixState desired = QuestPrefixState.desired(selectedGroup, prefixRaw);
        QuestPrefixState current = readOwnQuestState(user, settings);
        if (!current.needsWrite(desired)) {
            return false;
        }
        applyQuestNodes(user, selectedGroup, prefixRaw, settings);
        return true;
    }

    private static void applyQuestNodes(
            Object user,
            String selectedGroup,
            String prefixRaw,
            PrefixConfig.Settings settings
    ) throws ReflectiveOperationException {
        clearOwnOverrideNodes(user, settings);
        String group = PrefixSnapshot.normalize(selectedGroup);
        if (group != null) {
            addNode(user, buildMetaNode(PrefixSelectionMeta.KEY, group, settings));
        }
        if (prefixRaw != null && !prefixRaw.isBlank()) {
            addNode(user, buildPrefixNode(prefixRaw, settings));
        }
    }

    private static void markOwnWrite(Object user) {
        try {
            UUID playerId = (UUID) LuckPermsIntegration.invoke(user, "getUniqueId");
            RecalcSuppressor.markOwnWrite(playerId, System.currentTimeMillis());
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static CompletableFuture<Void> saveUser(Object user) {
        UUID playerId = uniqueIdOf(user);
        if (playerId == null) {
            return saveUserUnlocked(user);
        }
        return LuckPermsUserMutations.enqueue(playerId, () -> saveUserUnlocked(user));
    }

    private static CompletableFuture<Void> saveUserUnlocked(Object user) {
        if (!LuckPermsIntegration.isAvailable() || user == null) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            Object userManager = LuckPermsIntegration.invoke(LuckPermsIntegration.provider(), "getUserManager");
            Class<?> userClass = Class.forName("net.luckperms.api.model.user.User");
            Object future = LuckPermsIntegration.invoke(
                    userManager,
                    "saveUser",
                    new Class<?>[]{userClass},
                    user
            );
            if (future instanceof CompletableFuture<?> completable) {
                return completable.thenApply(ignored -> null);
            }
            return CompletableFuture.completedFuture(null);
        } catch (ReflectiveOperationException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    public static CompletableFuture<Void> applyPrefixOverride(UUID playerId, String group, String prefixRaw) {
        return syncQuestPrefix(playerId, group, prefixRaw);
    }

    public static CompletableFuture<Void> clearPrefixOverride(UUID playerId) {
        return syncQuestPrefix(playerId, null, null);
    }

    public static CompletableFuture<Void> addGroup(UUID playerId, String group) {
        return addGroups(playerId, group == null ? List.of() : List.of(group));
    }

    /**
     * Adds inheritance nodes ({@code lp parent add} equivalent) in one {@code modifyUser}.
     * Does not mark {@link RecalcSuppressor}: LuckPerms recalc still reaches prefix site sync.
     */
    public static CompletableFuture<Void> addGroups(UUID playerId, Collection<String> groups) {
        List<String> names = new ArrayList<>();
        if (groups != null) {
            for (String group : groups) {
                String name = PrefixSnapshot.normalize(group);
                if (name != null && !names.contains(name)) {
                    names.add(name);
                }
            }
        }
        if (names.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return LuckPermsUserMutations.enqueue(playerId, () -> loadUser(playerId).thenCompose(user -> {
            List<String> missing = InheritanceNodes.groupsNotInherited(names, inheritedGroupNamesOf(user));
            if (missing.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            return modifyUserUnlocked(playerId, loaded -> {
                try {
                    for (String name : missing) {
                        Object node = buildInheritanceNode(name);
                        String key = nodeKey(node);
                        if (!InheritanceNodes.isInheritanceKey(key)) {
                            throw new IllegalStateException(
                                    "Inheritance node key is not group.*: " + key + " for " + name
                            );
                        }
                        addNode(loaded, node);
                    }
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Failed to add groups " + missing, exception);
                }
            });
        }));
    }

    /**
     * Inherited group names from the in-memory user. Empty if the user is not loaded.
     * Offline players are not in {@code getUser}; call {@link #loadUser} first or use
     * {@link #inheritedGroupNamesOf}.
     */
    public static Set<String> inheritedGroupNames(UUID playerId) {
        if (!LuckPermsIntegration.isAvailable() || playerId == null) {
            return Set.of();
        }
        try {
            return inheritedGroupNamesOf(LuckPermsIntegration.getUser(playerId));
        } catch (ReflectiveOperationException ignored) {
            return Set.of();
        }
    }

    /**
     * Inherited group names, including parents of parents, from an already-loaded user.
     */
    public static Set<String> inheritedGroupNamesOf(Object user) {
        if (user == null) {
            return Set.of();
        }
        Set<String> names = new LinkedHashSet<>();
        for (HeldGroup group : inheritedGroups(user)) {
            names.add(group.name());
        }
        return Set.copyOf(names);
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
        return LuckPermsUserMutations.enqueue(playerId, () -> modifyUserUnlocked(playerId, mutator));
    }

    private static CompletableFuture<Void> modifyUserUnlocked(UUID playerId, Consumer<Object> mutator) {
        if (!LuckPermsIntegration.isAvailable() || playerId == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("LuckPerms unavailable"));
        }
        try {
            Object userManager = LuckPermsIntegration.invoke(LuckPermsIntegration.provider(), "getUserManager");
            @SuppressWarnings("unchecked")
            CompletableFuture<Void> future = (CompletableFuture<Void>) LuckPermsIntegration.invoke(
                    userManager,
                    "modifyUser",
                    new Class<?>[]{UUID.class, Consumer.class},
                    playerId,
                    (Consumer<Object>) mutator::accept
            );
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

    @SuppressWarnings("unchecked")
    public static CompletableFuture<Set<UUID>> uniqueUsers() {
        if (!LuckPermsIntegration.isAvailable()) {
            return CompletableFuture.completedFuture(Set.of());
        }
        try {
            Object userManager = LuckPermsIntegration.invoke(LuckPermsIntegration.provider(), "getUserManager");
            Object future = LuckPermsIntegration.invoke(userManager, "getUniqueUsers");
            if (future instanceof CompletableFuture<?> completable) {
                return completable.thenApply(value -> value instanceof Set<?> set
                        ? Set.copyOf((Set<UUID>) set)
                        : Set.of());
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return CompletableFuture.completedFuture(Set.of());
    }

    public static void cleanupUser(Object user) {
        if (!LuckPermsIntegration.isAvailable() || user == null) {
            return;
        }
        try {
            Object userManager = LuckPermsIntegration.invoke(LuckPermsIntegration.provider(), "getUserManager");
            Class<?> userClass = Class.forName("net.luckperms.api.model.user.User");
            LuckPermsIntegration.invoke(userManager, "cleanupUser", new Class<?>[]{userClass}, user);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static List<PrefixCatalogEntry> catalogEntries() {
        List<String> ranks = ranksTrackGroups();
        List<PrefixCatalogEntry> entries = new ArrayList<>();
        for (int index = 0; index < ranks.size(); index++) {
            String group = ranks.get(index);
            Object holder = findGroup(group);
            OwnPrefix own = readOwnPrefix(holder);
            String metaName = readOwnMeta(holder, PrefixCatalog.META_DISPLAY_NAME);
            String displayNode = readOwnDisplayName(holder);
            String display = PrefixCatalog.displayName(metaName, displayNode, own.raw(), group);
            entries.add(new PrefixCatalogEntry(group, own.raw(), own.priority(), index, display, PrefixCatalog.CATEGORY));
        }
        return PrefixCatalog.normalize(entries);
    }

    public static PrefixSnapshot snapshotOf(Object user) {
        PrefixConfig.Settings settings = PrefixConfig.get();
        List<String> quest = ranksTrackGroups();
        List<HeldGroup> held = inheritedGroups(user);
        List<PrefixOption> heldOptions = new ArrayList<>();
        for (HeldGroup group : held) {
            int trackIndex = quest.indexOf(group.name());
            if (trackIndex < 0) {
                continue;
            }
            OwnPrefix own = group.own();
            if (own.raw().isBlank()) {
                own = readOwnPrefix(findGroup(group.name()));
            }
            heldOptions.add(new PrefixOption(
                    group.name(),
                    own.raw(),
                    "quest",
                    own.priority(),
                    trackIndex
            ));
        }
        List<PrefixOption> available = PrefixSelectionRules.available(heldOptions, quest, settings.excludeGroups());
        String selected = selectedOverride(user, settings);
        return PrefixSnapshot.of(available, selected);
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
                    result.add(new HeldGroup(name, readOwnPrefix(group)));
                }
            }
        } catch (ReflectiveOperationException exception) {
            LOGGER.debug("Failed to resolve inherited groups", exception);
        }
        return result;
    }

    static QuestPrefixState readOwnQuestState(Object user, PrefixConfig.Settings settings) {
        if (user == null || settings == null) {
            return QuestPrefixState.current(null, null, 0, 0);
        }
        try {
            Object prefixType = LuckPermsIntegration.getNodeType("PREFIX");
            Object metaType = LuckPermsIntegration.getNodeType("META");
            String server = serverName();
            Set<String> rankTexts = rankPrefixTexts();
            Iterable<?> nodes = nodesOf(user);
            String selected = null;
            String prefixRaw = null;
            int prefixCount = 0;
            int metaCount = 0;
            boolean staleMeta = false;
            boolean stalePriority = false;
            if (nodes != null) {
                for (Object node : nodes) {
                    if (Boolean.TRUE.equals(LuckPermsIntegration.invoke(node, "hasExpired"))) {
                        continue;
                    }
                    Object type = LuckPermsIntegration.invoke(node, "getType");
                    if (prefixType.equals(type)) {
                        if (isOurPrefixNode(node, settings, rankTexts) && matchesThisServer(node, server)) {
                            prefixCount++;
                            int priority = ((Number) LuckPermsIntegration.invoke(node, "getPriority")).intValue();
                            if (priority != settings.prefixPriority()) {
                                stalePriority = true;
                            }
                            if (prefixRaw == null) {
                                String value = (String) LuckPermsIntegration.invoke(node, "getMetaValue");
                                prefixRaw = value == null || value.isBlank() ? null : value;
                            }
                        }
                    } else if (metaType.equals(type)) {
                        String key = (String) LuckPermsIntegration.invoke(node, "getMetaKey");
                        if (isSelectionMetaKey(key, settings) && matchesThisServer(node, server)) {
                            metaCount++;
                            if (!PrefixSelectionMeta.isCanonicalKey(key)) {
                                staleMeta = true;
                            }
                            if (selected == null) {
                                selected = PrefixSnapshot.normalize(
                                        (String) LuckPermsIntegration.invoke(node, "getMetaValue")
                                );
                            }
                        }
                    }
                }
            }
            return QuestPrefixState.current(selected, prefixRaw, prefixCount, metaCount, staleMeta, stalePriority);
        } catch (ReflectiveOperationException ignored) {
            return QuestPrefixState.current(null, null, 0, 0);
        }
    }

    private static String selectedOverride(Object user, PrefixConfig.Settings settings) {
        try {
            Object metaType = LuckPermsIntegration.getNodeType("META");
            Iterable<?> nodes = (Iterable<?>) LuckPermsIntegration.invoke(user, "getNodes");
            if (nodes == null) {
                return null;
            }
            String server = serverName();
            for (Object node : nodes) {
                if (!metaType.equals(LuckPermsIntegration.invoke(node, "getType"))) {
                    continue;
                }
                if (Boolean.TRUE.equals(LuckPermsIntegration.invoke(node, "hasExpired"))) {
                    continue;
                }
                String key = (String) LuckPermsIntegration.invoke(node, "getMetaKey");
                if (!isSelectionMetaKey(key, settings) || !matchesThisServer(node, server)) {
                    continue;
                }
                String value = (String) LuckPermsIntegration.invoke(node, "getMetaValue");
                return PrefixSnapshot.normalize(value);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static void clearOwnOverrideNodes(Object user, PrefixConfig.Settings settings)
            throws ReflectiveOperationException {
        Object prefixType = LuckPermsIntegration.getNodeType("PREFIX");
        Object metaType = LuckPermsIntegration.getNodeType("META");
        String server = serverName();
        Set<String> rankTexts = rankPrefixTexts();
        Object data = LuckPermsIntegration.invoke(user, "data");
        Class<?> predicateClass = Class.forName("java.util.function.Predicate");
        LuckPermsIntegration.invoke(data, "clear", new Class<?>[]{predicateClass}, (java.util.function.Predicate<Object>) node -> {
            try {
                if (Boolean.TRUE.equals(LuckPermsIntegration.invoke(node, "hasExpired"))) {
                    return false;
                }
                Object type = LuckPermsIntegration.invoke(node, "getType");
                if (prefixType.equals(type)) {
                    return isOurPrefixNode(node, settings, rankTexts) && matchesThisServer(node, server);
                }
                if (metaType.equals(type)) {
                    String key = (String) LuckPermsIntegration.invoke(node, "getMetaKey");
                    return isSelectionMetaKey(key, settings) && matchesThisServer(node, server);
                }
                return false;
            } catch (ReflectiveOperationException ignored) {
                return false;
            }
        });
    }

    private static boolean isOurPrefixNode(
            Object node,
            PrefixConfig.Settings settings,
            Set<String> rankPrefixTexts
    ) throws ReflectiveOperationException {
        int priority = ((Number) LuckPermsIntegration.invoke(node, "getPriority")).intValue();
        String value = (String) LuckPermsIntegration.invoke(node, "getMetaValue");
        return OwnPrefixNodeMatch.matches(priority, value, settings.prefixPriority(), rankPrefixTexts);
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

    private static void addNode(Object user, Object node) throws ReflectiveOperationException {
        Object data = LuckPermsIntegration.invoke(user, "data");
        Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
        LuckPermsIntegration.invoke(data, "add", new Class<?>[]{nodeClass}, node);
    }

    private static Object buildPrefixNode(String prefixRaw, PrefixConfig.Settings settings)
            throws ReflectiveOperationException {
        Class<?> prefixNode = Class.forName("net.luckperms.api.node.types.PrefixNode");
        Object builder = LuckPermsIntegration.invokeExact(
                prefixNode,
                null,
                "builder",
                new Class<?>[]{String.class, int.class},
                prefixRaw,
                settings.prefixPriority()
        );
        return applyServerContext(builder, settings);
    }

    private static Object buildMetaNode(String key, String value, PrefixConfig.Settings settings)
            throws ReflectiveOperationException {
        Class<?> metaNode = Class.forName("net.luckperms.api.node.types.MetaNode");
        Object builder = LuckPermsIntegration.invokeExact(
                metaNode,
                null,
                "builder",
                new Class<?>[]{String.class, String.class},
                key,
                value
        );
        return applyServerContext(builder, settings);
    }

    private static Object applyServerContext(Object builder, PrefixConfig.Settings settings)
            throws ReflectiveOperationException {
        String server = serverName();
        if (server != null && !GLOBAL_SERVER.equals(server)) {
            builder = LuckPermsIntegration.invoke(
                    builder,
                    "withContext",
                    new Class<?>[]{String.class, String.class},
                    "server",
                    server
            );
        }
        return LuckPermsIntegration.invoke(builder, "build");
    }

    /**
     * Prefer {@code InheritanceNode.builder(String)} declared on that type. The public-API
     * resolver used to pick generic {@code Node.builder(String)} and write permission
     * {@code atm} instead of {@code group.atm}. Fall back to {@code Node.builder("group."+name)}.
     */
    static Object buildInheritanceNode(String group) throws ReflectiveOperationException {
        PrefixConfig.Settings settings = PrefixConfig.get();
        Object node = tryTypedInheritanceBuilder(group, settings);
        if (node != null && InheritanceNodes.isInheritanceKey(nodeKey(node))) {
            return node;
        }
        return buildGroupPermissionNode(group, settings);
    }

    private static Object tryTypedInheritanceBuilder(String group, PrefixConfig.Settings settings) {
        try {
            Class<?> inheritance = Class.forName("net.luckperms.api.node.types.InheritanceNode");
            Method builderMethod = inheritance.getMethod("builder", String.class);
            if (!builderMethod.getDeclaringClass().getName().equals("net.luckperms.api.node.types.InheritanceNode")) {
                return null;
            }
            Object builder = builderMethod.invoke(null, group);
            return applyServerContext(builder, settings);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object buildGroupPermissionNode(String group, PrefixConfig.Settings settings)
            throws ReflectiveOperationException {
        Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
        Method builderMethod = nodeClass.getMethod("builder", String.class);
        Object builder = builderMethod.invoke(null, InheritanceNodes.permissionKey(group));
        return applyServerContext(builder, settings);
    }

    private static String nodeKey(Object node) {
        if (node == null) {
            return "";
        }
        try {
            Object key = LuckPermsIntegration.invoke(node, "getKey");
            return key == null ? "" : key.toString();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private static UUID uniqueIdOf(Object user) {
        if (user == null) {
            return null;
        }
        try {
            Object id = LuckPermsIntegration.invoke(user, "getUniqueId");
            return id instanceof UUID uuid ? uuid : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    static boolean isSelectionMetaKey(String key, PrefixConfig.Settings settings) {
        return PrefixSelectionMeta.isSelectionKey(key, settings == null ? null : settings.prefixMetaKey());
    }

    private static Set<String> rankPrefixTexts() {
        Set<String> texts = new LinkedHashSet<>();
        for (String group : ranksTrackGroups()) {
            String raw = readOwnPrefix(findGroup(group)).raw();
            if (raw != null && !raw.isBlank()) {
                texts.add(raw);
            }
        }
        return texts;
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

    private static Object findGroup(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            return null;
        }
        try {
            Object groupManager = LuckPermsIntegration.invoke(LuckPermsIntegration.provider(), "getGroupManager");
            return LuckPermsIntegration.invoke(groupManager, "getGroup", new Class<?>[]{String.class}, groupName);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    static OwnPrefix readOwnPrefix(Object group) {
        if (group == null) {
            return OwnPrefix.EMPTY;
        }
        try {
            Object prefixType = LuckPermsIntegration.getNodeType("PREFIX");
            Iterable<?> nodes = nodesOf(group);
            int best = Integer.MIN_VALUE;
            String raw = "";
            boolean found = false;
            if (nodes != null) {
                for (Object node : nodes) {
                    if (!prefixType.equals(LuckPermsIntegration.invoke(node, "getType"))) {
                        continue;
                    }
                    if (Boolean.TRUE.equals(LuckPermsIntegration.invoke(node, "hasExpired"))) {
                        continue;
                    }
                    int priority = ((Number) LuckPermsIntegration.invoke(node, "getPriority")).intValue();
                    if (!found || priority > best) {
                        found = true;
                        best = priority;
                        String value = (String) LuckPermsIntegration.invoke(node, "getMetaValue");
                        raw = value == null ? "" : value;
                    }
                }
            }
            return found ? new OwnPrefix(raw, best) : OwnPrefix.EMPTY;
        } catch (ReflectiveOperationException ignored) {
            return OwnPrefix.EMPTY;
        }
    }

    private static String readOwnMeta(Object group, String key) {
        if (group == null || key == null) {
            return "";
        }
        try {
            Object metaType = LuckPermsIntegration.getNodeType("META");
            Iterable<?> nodes = nodesOf(group);
            if (nodes == null) {
                return "";
            }
            for (Object node : nodes) {
                if (!metaType.equals(LuckPermsIntegration.invoke(node, "getType"))) {
                    continue;
                }
                if (Boolean.TRUE.equals(LuckPermsIntegration.invoke(node, "hasExpired"))) {
                    continue;
                }
                String metaKey = (String) LuckPermsIntegration.invoke(node, "getMetaKey");
                if (key.equalsIgnoreCase(metaKey)) {
                    String value = (String) LuckPermsIntegration.invoke(node, "getMetaValue");
                    return value == null ? "" : value;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return "";
    }

    private static String readOwnDisplayName(Object group) {
        if (group == null) {
            return "";
        }
        try {
            Object displayType = LuckPermsIntegration.getNodeType("DISPLAY_NAME");
            Iterable<?> nodes = nodesOf(group);
            if (nodes == null) {
                return "";
            }
            for (Object node : nodes) {
                if (!displayType.equals(LuckPermsIntegration.invoke(node, "getType"))) {
                    continue;
                }
                if (Boolean.TRUE.equals(LuckPermsIntegration.invoke(node, "hasExpired"))) {
                    continue;
                }
                String value = (String) LuckPermsIntegration.invoke(node, "getDisplayName");
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return "";
    }

    private static Iterable<?> nodesOf(Object holder) throws ReflectiveOperationException {
        Object nodes = LuckPermsIntegration.invoke(holder, "getNodes");
        return nodes instanceof Iterable<?> iterable ? iterable : null;
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

    record OwnPrefix(String raw, int priority) {
        static final OwnPrefix EMPTY = new OwnPrefix("", 0);
    }

    private record HeldGroup(String name, OwnPrefix own) {
    }
}
