package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.chunklimit.ChunkLimitIndex;
import com.mawlee.cointcore.chunklimit.ChunkLimitKey;
import com.mawlee.cointcore.chunklimit.TeamLimitIndex;
import com.mawlee.cointcore.chunklimit.TeamMobLimitIndex;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkLimitConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final ChunkLimitKey UNLIMITED = new ChunkLimitKey("__cointcore_unlimited__", 0);

    private static boolean enabled = true;
    private static LimitRules chunkRules = LimitRules.empty();
    private static LimitRules teamRules = LimitRules.empty();
    private static LimitRules playerRules = LimitRules.empty();
    private static EntityRules chunkEntityRules = EntityRules.empty();
    private static EntityRules teamEntityRules = EntityRules.empty();
    private static volatile int rulesVersion;

    private static final ConcurrentHashMap<Block, ChunkLimitKey> chunkKeyCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Block, ChunkLimitKey> teamKeyCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Block, ChunkLimitKey> playerKeyCache = new ConcurrentHashMap<>();

    /** JSON key / command token for the general mob cap (all {@link net.minecraft.world.entity.Mob}). */
    public static final String ENTITY_CAP_KEY = "*";
    public static final String ENTITY_CAP_LIMIT_ID = "mob:cap";

    private ChunkLimitConfig() {
    }

    public enum LimitScope {
        CHUNK,
        TEAM,
        /** Blocks owned (placed) by one player, summed across all dimensions. */
        PLAYER
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Bumped on every reload / runtime edit so derived counts can rebuild lazily. */
    public static int rulesVersion() {
        return rulesVersion;
    }

    public static boolean hasAnyBlockLimits() {
        return chunkRules.hasAny() || teamRules.hasAny() || playerRules.hasAny();
    }

    public static boolean hasPlayerBlockLimits() {
        return playerRules.hasAny();
    }

    public static Map<ResourceLocation, Integer> getBlockLimits(LimitScope scope) {
        return rules(scope).exactLimits();
    }

    public static Map<String, GroupLimit> getGroups(LimitScope scope) {
        return rules(scope).groups();
    }

    public static List<TagLimitBinding> getTagBindings(LimitScope scope) {
        return rules(scope).tagBindings();
    }

    public static List<ModLimitBinding> getModBindings(LimitScope scope) {
        return rules(scope).modBindings();
    }

    public static boolean hasChunkBlockLimits() {
        return chunkRules.hasAny();
    }

    public static boolean hasTeamBlockLimits() {
        return teamRules.hasAny();
    }

    public static Map<ResourceLocation, Integer> getBlockLimits() {
        return chunkRules.exactLimits();
    }

    public static Map<ResourceLocation, Integer> getTeamBlockLimits() {
        return teamRules.exactLimits();
    }

    public static Map<ResourceLocation, Integer> getEntityLimits() {
        return chunkEntityRules.exactLimits();
    }

    public static Map<ResourceLocation, Integer> getTeamEntityLimits() {
        return teamEntityRules.exactLimits();
    }

    public static List<ModLimitBinding> getEntityModBindings() {
        return chunkEntityRules.modBindings();
    }

    public static List<ModLimitBinding> getTeamEntityModBindings() {
        return teamEntityRules.modBindings();
    }

    public static Integer getEntityCap(LimitScope scope) {
        int cap = entityRules(scope).generalCap();
        return cap >= 0 ? cap : null;
    }

    public static boolean hasAnyEntityLimits() {
        return chunkEntityRules.hasAny() || teamEntityRules.hasAny();
    }

    public static boolean hasChunkEntityLimits() {
        return chunkEntityRules.hasAny();
    }

    public static boolean hasTeamEntityLimits() {
        return teamEntityRules.hasAny();
    }

    public static Map<String, GroupLimit> getGroups() {
        return chunkRules.groups();
    }

    public static Map<String, GroupLimit> getTeamGroups() {
        return teamRules.groups();
    }

    public static List<TagLimitBinding> getTagBindings() {
        return chunkRules.tagBindings();
    }

    public static List<TagLimitBinding> getTeamTagBindings() {
        return teamRules.tagBindings();
    }

    public static List<ModLimitBinding> getModBindings() {
        return chunkRules.modBindings();
    }

    public static List<ModLimitBinding> getTeamModBindings() {
        return teamRules.modBindings();
    }

    /** Key ids configured for team aggregate limits. */
    public static boolean isTeamKeyId(String keyId) {
        if (keyId == null || !teamRules.hasAny()) {
            return false;
        }
        for (ChunkLimitKey key : teamRules.blockIdKeys().values()) {
            if (key.id().equals(keyId)) {
                return true;
            }
        }
        for (TagLimitBinding binding : teamRules.tagBindings()) {
            if (binding.key().id().equals(keyId)) {
                return true;
            }
        }
        for (ModLimitBinding binding : teamRules.modBindings()) {
            if (binding.key().id().equals(keyId)) {
                return true;
            }
        }
        for (GroupLimit group : teamRules.groups().values()) {
            if (("group:" + group.name()).equals(keyId)) {
                return true;
            }
        }
        return false;
    }

    public static Integer getBlockLimit(ResourceLocation id) {
        return chunkRules.exactLimits().get(id);
    }

    public static Integer getEntityLimit(ResourceLocation id) {
        return chunkEntityRules.exactLimits().get(id);
    }

    public static Integer getTeamEntityLimit(ResourceLocation id) {
        return teamEntityRules.exactLimits().get(id);
    }

    /**
     * Entity-limit keys for this type in the given scope (most specific wins).
     * <ol>
     *   <li>exact entity id</li>
     *   <li>else mod mask ({@code namespace:*})</li>
     *   <li>else general mob cap ({@code *})</li>
     * </ol>
     * Exact limits must win: setting {@code minecraft:chicken} to 100 must not still be
     * blocked by a global {@code *} / {@code minecraft:*} cap.
     */
    public static List<ChunkLimitKey> resolveEntityKeys(LimitScope scope, EntityType<?> type, boolean isMob) {
        EntityRules rules = entityRules(scope);
        if (!rules.hasAny()) {
            return List.of();
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id != null) {
            Integer exact = rules.exactLimits().get(id);
            if (exact != null) {
                return List.of(new ChunkLimitKey("entity:" + id, exact));
            }
            if (isMob) {
                for (ModLimitBinding binding : rules.modBindings()) {
                    if (binding.namespace().equals(id.getNamespace())) {
                        return List.of(binding.key());
                    }
                }
            }
        }
        if (isMob && rules.generalCap() >= 0) {
            return List.of(new ChunkLimitKey(ENTITY_CAP_LIMIT_ID, rules.generalCap()));
        }
        return List.of();
    }

    public static Path getConfigPath() {
        return configPath();
    }

    public static ChunkLimitKey resolveBlockKey(Block block) {
        return resolveBlockKey(LimitScope.CHUNK, block);
    }

    public static ChunkLimitKey resolveTeamBlockKey(Block block) {
        return resolveBlockKey(LimitScope.TEAM, block);
    }

    public static ChunkLimitKey resolveBlockKey(LimitScope scope, Block block) {
        if (block == null) {
            return null;
        }
        LimitRules rules = rules(scope);
        if (!rules.hasAny()) {
            return null;
        }
        ConcurrentHashMap<Block, ChunkLimitKey> cache = switch (scope) {
            case CHUNK -> chunkKeyCache;
            case TEAM -> teamKeyCache;
            case PLAYER -> playerKeyCache;
        };
        ChunkLimitKey cached = cache.get(block);
        if (cached != null) {
            return cached == UNLIMITED ? null : cached;
        }
        ChunkLimitKey resolved = resolveUncached(rules, block);
        cache.put(block, resolved != null ? resolved : UNLIMITED);
        return resolved;
    }

    private static ChunkLimitKey resolveUncached(LimitRules rules, Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id != null) {
            ChunkLimitKey exact = rules.blockIdKeys().get(id);
            if (exact != null) {
                return exact;
            }
        }
        for (TagLimitBinding binding : rules.tagBindings()) {
            if (block.builtInRegistryHolder().is(binding.tag())) {
                return binding.key();
            }
        }
        if (id != null) {
            for (ModLimitBinding binding : rules.modBindings()) {
                if (binding.namespace().equals(id.getNamespace())) {
                    return binding.key();
                }
            }
        }
        return null;
    }

    public static boolean isLimitedBlock(Block block) {
        return resolveBlockKey(block) != null
                || resolveTeamBlockKey(block) != null
                || resolveBlockKey(LimitScope.PLAYER, block) != null;
    }

    public static boolean isChunkLimitedBlock(Block block) {
        return resolveBlockKey(block) != null;
    }

    public static void clearResolveCache() {
        chunkKeyCache.clear();
        teamKeyCache.clear();
        playerKeyCache.clear();
    }

    public static void load() {
        apply(loadFromDisk(false));
    }

    public static boolean reload() {
        LoadedConfig loaded = loadFromDisk(true);
        if (loaded == null) {
            return false;
        }
        apply(loaded);
        return true;
    }

    public static boolean setEnabled(boolean value) {
        enabled = value;
        afterMutation();
        return saveCurrentState();
    }

    public static boolean setBlockLimit(LimitScope scope, ResourceLocation id, int limit) {
        if (limit < 0 || id == null) {
            return false;
        }
        LimitRules rules = rules(scope);
        Map<ResourceLocation, Integer> updated = new LinkedHashMap<>(rules.exactLimits());
        updated.put(id, limit);
        setRules(scope, rules.withExactLimits(updated));
        afterMutation();
        return saveCurrentState();
    }

    public static boolean setBlockLimit(ResourceLocation id, int limit) {
        return setBlockLimit(LimitScope.CHUNK, id, limit);
    }

    public static boolean removeBlockLimit(LimitScope scope, ResourceLocation id) {
        LimitRules rules = rules(scope);
        if (!rules.exactLimits().containsKey(id)) {
            return false;
        }
        Map<ResourceLocation, Integer> updated = new LinkedHashMap<>(rules.exactLimits());
        updated.remove(id);
        setRules(scope, rules.withExactLimits(updated));
        afterMutation();
        return saveCurrentState();
    }

    public static boolean removeBlockLimit(ResourceLocation id) {
        return removeBlockLimit(LimitScope.CHUNK, id);
    }

    public static boolean setModLimit(LimitScope scope, String namespace, int limit) {
        String ns = normalizeNamespace(namespace);
        if (ns == null || limit < 0) {
            return false;
        }
        LimitRules rules = rules(scope);
        Map<String, Integer> mods = new LinkedHashMap<>(toModMap(rules.modBindings()));
        mods.put(ns, limit);
        setRules(scope, rebuildRules(rules.exactLimits(), rules.groups(), toTagMap(rules.tagBindings()), mods));
        afterMutation();
        return saveCurrentState();
    }

    public static boolean removeModLimit(LimitScope scope, String namespace) {
        String ns = normalizeNamespace(namespace);
        if (ns == null) {
            return false;
        }
        LimitRules rules = rules(scope);
        Map<String, Integer> mods = new LinkedHashMap<>(toModMap(rules.modBindings()));
        if (mods.remove(ns) == null) {
            return false;
        }
        setRules(scope, rebuildRules(rules.exactLimits(), rules.groups(), toTagMap(rules.tagBindings()), mods));
        afterMutation();
        return saveCurrentState();
    }

    public static boolean setTagLimit(LimitScope scope, ResourceLocation tagId, int limit) {
        if (tagId == null || limit < 0) {
            return false;
        }
        LimitRules rules = rules(scope);
        Map<String, Integer> tags = new LinkedHashMap<>(toTagMap(rules.tagBindings()));
        tags.put(tagId.toString(), limit);
        setRules(scope, rebuildRules(rules.exactLimits(), rules.groups(), tags, toModMap(rules.modBindings())));
        afterMutation();
        return saveCurrentState();
    }

    public static boolean removeTagLimit(LimitScope scope, ResourceLocation tagId) {
        if (tagId == null) {
            return false;
        }
        LimitRules rules = rules(scope);
        Map<String, Integer> tags = new LinkedHashMap<>(toTagMap(rules.tagBindings()));
        if (tags.remove(tagId.toString()) == null) {
            return false;
        }
        setRules(scope, rebuildRules(rules.exactLimits(), rules.groups(), tags, toModMap(rules.modBindings())));
        afterMutation();
        return saveCurrentState();
    }

    public static boolean setGroupLimit(LimitScope scope, String name, int limit) {
        String groupName = normalizeGroupName(name);
        if (groupName == null || limit < 0) {
            return false;
        }
        LimitRules rules = rules(scope);
        Map<String, GroupLimit> groups = new LinkedHashMap<>(rules.groups());
        GroupLimit existing = groups.get(groupName);
        List<ResourceLocation> blocks = existing != null ? existing.blocks() : List.of();
        groups.put(groupName, new GroupLimit(groupName, limit, blocks));
        setRules(scope, rebuildRules(rules.exactLimits(), groups, toTagMap(rules.tagBindings()), toModMap(rules.modBindings())));
        afterMutation();
        return saveCurrentState();
    }

    public static boolean addBlockToGroup(LimitScope scope, String name, ResourceLocation blockId) {
        String groupName = normalizeGroupName(name);
        if (groupName == null || blockId == null) {
            return false;
        }
        LimitRules rules = rules(scope);
        Map<String, GroupLimit> groups = new LinkedHashMap<>(rules.groups());
        GroupLimit existing = groups.get(groupName);
        if (existing == null) {
            return false;
        }
        List<ResourceLocation> blocks = new ArrayList<>(existing.blocks());
        if (!blocks.contains(blockId)) {
            blocks.add(blockId);
        }
        groups.put(groupName, new GroupLimit(groupName, existing.limit(), List.copyOf(blocks)));
        setRules(scope, rebuildRules(rules.exactLimits(), groups, toTagMap(rules.tagBindings()), toModMap(rules.modBindings())));
        afterMutation();
        return saveCurrentState();
    }

    public static boolean removeBlockFromGroup(LimitScope scope, String name, ResourceLocation blockId) {
        String groupName = normalizeGroupName(name);
        if (groupName == null || blockId == null) {
            return false;
        }
        LimitRules rules = rules(scope);
        Map<String, GroupLimit> groups = new LinkedHashMap<>(rules.groups());
        GroupLimit existing = groups.get(groupName);
        if (existing == null) {
            return false;
        }
        List<ResourceLocation> blocks = new ArrayList<>(existing.blocks());
        if (!blocks.remove(blockId)) {
            return false;
        }
        groups.put(groupName, new GroupLimit(groupName, existing.limit(), List.copyOf(blocks)));
        setRules(scope, rebuildRules(rules.exactLimits(), groups, toTagMap(rules.tagBindings()), toModMap(rules.modBindings())));
        afterMutation();
        return saveCurrentState();
    }

    public static boolean deleteGroup(LimitScope scope, String name) {
        String groupName = normalizeGroupName(name);
        if (groupName == null) {
            return false;
        }
        LimitRules rules = rules(scope);
        Map<String, GroupLimit> groups = new LinkedHashMap<>(rules.groups());
        if (groups.remove(groupName) == null) {
            return false;
        }
        setRules(scope, rebuildRules(rules.exactLimits(), groups, toTagMap(rules.tagBindings()), toModMap(rules.modBindings())));
        afterMutation();
        return saveCurrentState();
    }

    public static boolean setEntityLimit(LimitScope scope, ResourceLocation id, int limit) {
        if (id == null || limit < 0) {
            return false;
        }
        EntityRules rules = entityRules(scope);
        Map<ResourceLocation, Integer> updated = new LinkedHashMap<>(rules.exactLimits());
        updated.put(id, limit);
        setEntityRules(scope, rebuildEntityRules(updated, toModMap(rules.modBindings()), rules.generalCap()));
        afterMutation();
        return saveCurrentState();
    }

    public static boolean setEntityLimit(ResourceLocation id, int limit) {
        return setEntityLimit(LimitScope.CHUNK, id, limit);
    }

    public static boolean removeEntityLimit(LimitScope scope, ResourceLocation id) {
        EntityRules rules = entityRules(scope);
        if (!rules.exactLimits().containsKey(id)) {
            return false;
        }
        Map<ResourceLocation, Integer> updated = new LinkedHashMap<>(rules.exactLimits());
        updated.remove(id);
        setEntityRules(scope, rebuildEntityRules(updated, toModMap(rules.modBindings()), rules.generalCap()));
        afterMutation();
        return saveCurrentState();
    }

    public static boolean removeEntityLimit(ResourceLocation id) {
        return removeEntityLimit(LimitScope.CHUNK, id);
    }

    public static boolean setEntityModLimit(LimitScope scope, String namespace, int limit) {
        String ns = normalizeNamespace(namespace);
        if (ns == null || limit < 0) {
            return false;
        }
        EntityRules rules = entityRules(scope);
        Map<String, Integer> mods = new LinkedHashMap<>(toModMap(rules.modBindings()));
        mods.put(ns, limit);
        setEntityRules(scope, rebuildEntityRules(rules.exactLimits(), mods, rules.generalCap()));
        afterMutation();
        return saveCurrentState();
    }

    public static boolean removeEntityModLimit(LimitScope scope, String namespace) {
        String ns = normalizeNamespace(namespace);
        if (ns == null) {
            return false;
        }
        EntityRules rules = entityRules(scope);
        Map<String, Integer> mods = new LinkedHashMap<>(toModMap(rules.modBindings()));
        if (mods.remove(ns) == null) {
            return false;
        }
        setEntityRules(scope, rebuildEntityRules(rules.exactLimits(), mods, rules.generalCap()));
        afterMutation();
        return saveCurrentState();
    }

    public static boolean setEntityCap(LimitScope scope, int limit) {
        if (limit < 0) {
            return false;
        }
        EntityRules rules = entityRules(scope);
        setEntityRules(scope, rebuildEntityRules(rules.exactLimits(), toModMap(rules.modBindings()), limit));
        afterMutation();
        return saveCurrentState();
    }

    public static boolean removeEntityCap(LimitScope scope) {
        EntityRules rules = entityRules(scope);
        if (rules.generalCap() < 0) {
            return false;
        }
        setEntityRules(scope, rebuildEntityRules(rules.exactLimits(), toModMap(rules.modBindings()), -1));
        afterMutation();
        return saveCurrentState();
    }

    private static void afterMutation() {
        clearResolveCache();
        rulesVersion++;
        ChunkLimitIndex.invalidateAll();
        TeamLimitIndex.invalidateAll();
        TeamMobLimitIndex.invalidateAll();
    }

    private static LimitRules rules(LimitScope scope) {
        return switch (scope) {
            case CHUNK -> chunkRules;
            case TEAM -> teamRules;
            case PLAYER -> playerRules;
        };
    }

    private static EntityRules entityRules(LimitScope scope) {
        return switch (scope) {
            case CHUNK -> chunkEntityRules;
            case TEAM -> teamEntityRules;
            case PLAYER -> EntityRules.empty();
        };
    }

    private static void setRules(LimitScope scope, LimitRules rules) {
        switch (scope) {
            case CHUNK -> chunkRules = rules;
            case TEAM -> teamRules = rules;
            case PLAYER -> playerRules = rules;
        }
    }

    private static void setEntityRules(LimitScope scope, EntityRules rules) {
        switch (scope) {
            case CHUNK -> chunkEntityRules = rules;
            case TEAM -> teamEntityRules = rules;
            case PLAYER -> throw new IllegalArgumentException("Per-player entity limits are not supported yet");
        }
    }

    private static boolean saveCurrentState() {
        try {
            save(toFileDataSnapshot(), configPath());
            return true;
        } catch (IOException exception) {
            LOGGER.error("Failed to save chunk limit config to {}", configPath(), exception);
            return false;
        }
    }

    private static Map<String, Integer> toBlockLimitsFileMap(LimitRules rules) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : rules.exactLimits().entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        for (TagLimitBinding binding : rules.tagBindings()) {
            result.put("#" + binding.tag().location(), binding.key().limit());
        }
        for (ModLimitBinding binding : rules.modBindings()) {
            result.put(binding.namespace() + ":*", binding.key().limit());
        }
        return result;
    }

    private static Map<String, Integer> toStringMap(Map<ResourceLocation, Integer> source) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : source.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }

    private static Map<String, GroupFileData> toGroupFileMap(Map<String, GroupLimit> source) {
        Map<String, GroupFileData> result = new LinkedHashMap<>();
        for (Map.Entry<String, GroupLimit> entry : source.entrySet()) {
            GroupFileData group = new GroupFileData();
            group.limit = entry.getValue().limit();
            group.blocks = new ArrayList<>();
            for (ResourceLocation blockId : entry.getValue().blocks()) {
                group.blocks.add(blockId.toString());
            }
            result.put(entry.getKey(), group);
        }
        return result;
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                save(defaults, path);
                LOGGER.info("Created default chunk limit config at {}", path);
                return parse(defaults);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                LoadedConfig loaded = parse(data != null ? data : defaultFileData());
                LOGGER.info(
                        "Loaded chunk limit config (enabled: {}, chunk exact={}, tags={}, mods={}, groups={}; "
                                + "team exact={}, tags={}, mods={}, groups={}; player exact={}, tags={}, mods={}, groups={}; "
                                + "entity chunk exact={}, mods={}, cap={}; team entity exact={}, mods={}, cap={})",
                        loaded.enabled,
                        loaded.chunkRules.exactLimits().size(),
                        loaded.chunkRules.tagBindings().size(),
                        loaded.chunkRules.modBindings().size(),
                        loaded.chunkRules.groups().size(),
                        loaded.teamRules.exactLimits().size(),
                        loaded.teamRules.tagBindings().size(),
                        loaded.teamRules.modBindings().size(),
                        loaded.teamRules.groups().size(),
                        loaded.playerRules.exactLimits().size(),
                        loaded.playerRules.tagBindings().size(),
                        loaded.playerRules.modBindings().size(),
                        loaded.playerRules.groups().size(),
                        loaded.chunkEntityRules.exactLimits().size(),
                        loaded.chunkEntityRules.modBindings().size(),
                        loaded.chunkEntityRules.generalCap(),
                        loaded.teamEntityRules.exactLimits().size(),
                        loaded.teamEntityRules.modBindings().size(),
                        loaded.teamEntityRules.generalCap()
                );
                if (reloading) {
                    LOGGER.info("Reloaded chunk limit config from {}", path);
                }
                return loaded;
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load chunk limit config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading chunk limit config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled;
        chunkRules = loaded.chunkRules;
        teamRules = loaded.teamRules;
        playerRules = loaded.playerRules;
        chunkEntityRules = loaded.chunkEntityRules;
        teamEntityRules = loaded.teamEntityRules;
        afterMutation();
    }

    private static FileData toFileDataSnapshot() {
        FileData data = new FileData();
        data.enabled = enabled;
        data.blockLimits = toBlockLimitsFileMap(chunkRules);
        data.entityLimits = toEntityLimitsFileMap(chunkEntityRules);
        data.groups = toGroupFileMap(chunkRules.groups());
        data.teamBlockLimits = toBlockLimitsFileMap(teamRules);
        data.teamGroups = toGroupFileMap(teamRules.groups());
        data.teamEntityLimits = toEntityLimitsFileMap(teamEntityRules);
        data.playerBlockLimits = toBlockLimitsFileMap(playerRules);
        data.playerGroups = toGroupFileMap(playerRules.groups());
        return data;
    }

    private static LoadedConfig parse(FileData data) {
        boolean active = data.enabled != null ? data.enabled : true;
        LimitRules chunk = parseRules(data.blockLimits, data.groups, "chunk");
        LimitRules team = parseRules(data.teamBlockLimits, data.teamGroups, "team");
        LimitRules player = parseRules(data.playerBlockLimits, data.playerGroups, "player");
        EntityRules chunkEntities = parseEntityRules(data.entityLimits, "chunk");
        EntityRules teamEntities = parseEntityRules(data.teamEntityLimits, "team");
        return new LoadedConfig(active, chunk, team, player, chunkEntities, teamEntities);
    }

    private static LimitRules parseRules(
            Map<String, Integer> blockLimits,
            Map<String, GroupFileData> groupsData,
            String label
    ) {
        Map<ResourceLocation, Integer> exactBlocks = new LinkedHashMap<>();
        List<TagLimitBinding> tags = new ArrayList<>();
        List<ModLimitBinding> mods = new ArrayList<>();
        Map<ResourceLocation, ChunkLimitKey> idKeys = new LinkedHashMap<>();

        if (blockLimits != null) {
            for (Map.Entry<String, Integer> entry : blockLimits.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue() < 0) {
                    continue;
                }
                String raw = entry.getKey().trim();
                int limit = entry.getValue();
                if (raw.startsWith("#")) {
                    ResourceLocation tagId = ResourceLocation.tryParse(raw.substring(1));
                    if (tagId == null) {
                        LOGGER.warn("Skipping invalid {} chunk limit tag: {}", label, raw);
                        continue;
                    }
                    TagKey<Block> tag = TagKey.create(Registries.BLOCK, tagId);
                    tags.add(new TagLimitBinding(tag, new ChunkLimitKey("tag:" + tagId, limit)));
                } else if (isModMask(raw)) {
                    String namespace = raw.substring(0, raw.length() - 2);
                    if (!isValidNamespace(namespace)) {
                        LOGGER.warn("Skipping invalid {} chunk limit mod mask: {}", label, raw);
                        continue;
                    }
                    mods.add(new ModLimitBinding(namespace, new ChunkLimitKey("mod:" + namespace, limit)));
                } else {
                    ResourceLocation id = ResourceLocation.tryParse(raw);
                    if (id == null) {
                        LOGGER.warn("Skipping invalid {} chunk limit id: {}", label, raw);
                        continue;
                    }
                    if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                        LOGGER.warn("Unknown {} chunk limit block: {}", label, id);
                    }
                    exactBlocks.put(id, limit);
                    idKeys.put(id, new ChunkLimitKey("block:" + id, limit));
                }
            }
        }

        Map<String, GroupLimit> parsedGroups = new LinkedHashMap<>();
        if (groupsData != null) {
            for (Map.Entry<String, GroupFileData> entry : groupsData.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                    continue;
                }
                GroupFileData groupData = entry.getValue();
                if (groupData.limit == null || groupData.limit < 0) {
                    continue;
                }
                List<ResourceLocation> blocks = new ArrayList<>();
                if (groupData.blocks != null) {
                    for (String blockId : groupData.blocks) {
                        if (blockId == null || blockId.isBlank()) {
                            continue;
                        }
                        ResourceLocation id = ResourceLocation.tryParse(blockId.trim());
                        if (id == null) {
                            LOGGER.warn("Skipping invalid {} group block id in {}: {}", label, entry.getKey(), blockId);
                            continue;
                        }
                        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                            LOGGER.warn("Unknown {} group block in {}: {}", label, entry.getKey(), id);
                        }
                        blocks.add(id);
                    }
                }
                ChunkLimitKey key = new ChunkLimitKey("group:" + entry.getKey(), groupData.limit);
                GroupLimit group = new GroupLimit(entry.getKey(), groupData.limit, List.copyOf(blocks));
                parsedGroups.put(entry.getKey(), group);
                for (ResourceLocation blockId : blocks) {
                    idKeys.putIfAbsent(blockId, key);
                }
            }
        }

        return new LimitRules(
                Collections.unmodifiableMap(exactBlocks),
                Collections.unmodifiableMap(parsedGroups),
                List.copyOf(tags),
                List.copyOf(mods),
                Collections.unmodifiableMap(idKeys)
        );
    }

    private static LimitRules rebuildRules(
            Map<ResourceLocation, Integer> exact,
            Map<String, GroupLimit> groups,
            Map<String, Integer> tags,
            Map<String, Integer> mods
    ) {
        Map<String, Integer> blockMap = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : exact.entrySet()) {
            blockMap.put(entry.getKey().toString(), entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : tags.entrySet()) {
            blockMap.put("#" + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : mods.entrySet()) {
            blockMap.put(entry.getKey() + ":*", entry.getValue());
        }
        Map<String, GroupFileData> groupFiles = new LinkedHashMap<>();
        for (Map.Entry<String, GroupLimit> entry : groups.entrySet()) {
            GroupFileData file = new GroupFileData();
            file.limit = entry.getValue().limit();
            file.blocks = new ArrayList<>();
            for (ResourceLocation id : entry.getValue().blocks()) {
                file.blocks.add(id.toString());
            }
            groupFiles.put(entry.getKey(), file);
        }
        return parseRules(blockMap, groupFiles, "runtime");
    }

    private static Map<String, Integer> toTagMap(List<TagLimitBinding> bindings) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (TagLimitBinding binding : bindings) {
            map.put(binding.tag().location().toString(), binding.key().limit());
        }
        return map;
    }

    private static Map<String, Integer> toModMap(List<ModLimitBinding> bindings) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (ModLimitBinding binding : bindings) {
            map.put(binding.namespace(), binding.key().limit());
        }
        return map;
    }

    private static boolean isModMask(String raw) {
        return raw.endsWith(":*") && raw.length() > 2;
    }

    private static boolean isValidNamespace(String namespace) {
        return namespace != null && !namespace.isBlank() && ResourceLocation.isValidNamespace(namespace);
    }

    private static String normalizeNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return null;
        }
        String trimmed = namespace.trim().toLowerCase(Locale.ROOT);
        if (trimmed.endsWith(":*")) {
            trimmed = trimmed.substring(0, trimmed.length() - 2);
        }
        return isValidNamespace(trimmed) ? trimmed : null;
    }

    private static String normalizeGroupName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim();
    }

    private static Map<String, Integer> toEntityLimitsFileMap(EntityRules rules) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (rules.generalCap() >= 0) {
            result.put(ENTITY_CAP_KEY, rules.generalCap());
        }
        for (Map.Entry<ResourceLocation, Integer> entry : rules.exactLimits().entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        for (ModLimitBinding binding : rules.modBindings()) {
            result.put(binding.namespace() + ":*", binding.key().limit());
        }
        return result;
    }

    private static EntityRules parseEntityRules(Map<String, Integer> source, String label) {
        Map<ResourceLocation, Integer> exact = new LinkedHashMap<>();
        List<ModLimitBinding> mods = new ArrayList<>();
        int generalCap = -1;
        if (source == null || source.isEmpty()) {
            return EntityRules.empty();
        }
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue() < 0) {
                continue;
            }
            String raw = entry.getKey().trim();
            int limit = entry.getValue();
            if (ENTITY_CAP_KEY.equals(raw)) {
                generalCap = limit;
                continue;
            }
            if (isModMask(raw)) {
                String namespace = raw.substring(0, raw.length() - 2);
                if (!isValidNamespace(namespace)) {
                    LOGGER.warn("Skipping invalid {} entity mod mask: {}", label, raw);
                    continue;
                }
                mods.add(new ModLimitBinding(namespace, new ChunkLimitKey("emod:" + namespace, limit)));
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id == null) {
                LOGGER.warn("Skipping invalid {} entity limit id: {}", label, raw);
                continue;
            }
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                LOGGER.warn("Unknown {} entity limit type: {}", label, id);
            }
            exact.put(id, limit);
        }
        return new EntityRules(Collections.unmodifiableMap(exact), List.copyOf(mods), generalCap);
    }

    private static EntityRules rebuildEntityRules(
            Map<ResourceLocation, Integer> exact,
            Map<String, Integer> mods,
            int generalCap
    ) {
        Map<String, Integer> file = new LinkedHashMap<>();
        if (generalCap >= 0) {
            file.put(ENTITY_CAP_KEY, generalCap);
        }
        for (Map.Entry<ResourceLocation, Integer> entry : exact.entrySet()) {
            file.put(entry.getKey().toString(), entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : mods.entrySet()) {
            file.put(entry.getKey() + ":*", entry.getValue());
        }
        return parseEntityRules(file, "runtime");
    }

    private static Map<ResourceLocation, Integer> parseEntityLimitMap(Map<String, Integer> source) {
        return parseEntityRules(source, "legacy").exactLimits();
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(CointCore.MOD_ID)
                .resolve("chunk-limits.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.blockLimits = new LinkedHashMap<>();
        data.entityLimits = new LinkedHashMap<>();
        data.groups = new LinkedHashMap<>();
        data.teamBlockLimits = new LinkedHashMap<>();
        data.teamGroups = new LinkedHashMap<>();
        data.teamEntityLimits = new LinkedHashMap<>();
        data.playerBlockLimits = new LinkedHashMap<>();
        data.playerGroups = new LinkedHashMap<>();
        return data;
    }

    public static boolean hasBlockLimit(Block block) {
        return isLimitedBlock(block);
    }

    public static boolean hasEntityLimit(EntityType<?> type) {
        return !resolveEntityKeys(LimitScope.CHUNK, type, true).isEmpty()
                || !resolveEntityKeys(LimitScope.TEAM, type, true).isEmpty()
                || !resolveEntityKeys(LimitScope.CHUNK, type, false).isEmpty()
                || !resolveEntityKeys(LimitScope.TEAM, type, false).isEmpty();
    }

    /** True if this type is subject to any configured entity/mob limit (exact/mod/cap). */
    public static boolean isEntityLimited(EntityType<?> type, boolean isMob) {
        return !resolveEntityKeys(LimitScope.CHUNK, type, isMob).isEmpty()
                || !resolveEntityKeys(LimitScope.TEAM, type, isMob).isEmpty();
    }

    public record GroupLimit(String name, int limit, List<ResourceLocation> blocks) {
    }

    public record TagLimitBinding(TagKey<Block> tag, ChunkLimitKey key) {
    }

    public record ModLimitBinding(String namespace, ChunkLimitKey key) {
    }

    private record EntityRules(
            Map<ResourceLocation, Integer> exactLimits,
            List<ModLimitBinding> modBindings,
            int generalCap
    ) {
        static EntityRules empty() {
            return new EntityRules(Map.of(), List.of(), -1);
        }

        boolean hasAny() {
            return !exactLimits.isEmpty() || !modBindings.isEmpty() || generalCap >= 0;
        }
    }

    private record LimitRules(
            Map<ResourceLocation, Integer> exactLimits,
            Map<String, GroupLimit> groups,
            List<TagLimitBinding> tagBindings,
            List<ModLimitBinding> modBindings,
            Map<ResourceLocation, ChunkLimitKey> blockIdKeys
    ) {
        static LimitRules empty() {
            return new LimitRules(Map.of(), Map.of(), List.of(), List.of(), Map.of());
        }

        boolean hasAny() {
            return !blockIdKeys.isEmpty() || !tagBindings.isEmpty() || !modBindings.isEmpty();
        }

        LimitRules withExactLimits(Map<ResourceLocation, Integer> exact) {
            return rebuildRules(exact, groups, toTagMap(tagBindings), toModMap(modBindings));
        }
    }

    private record LoadedConfig(
            boolean enabled,
            LimitRules chunkRules,
            LimitRules teamRules,
            LimitRules playerRules,
            EntityRules chunkEntityRules,
            EntityRules teamEntityRules
    ) {
    }

    private static final class FileData {
        @SerializedName("enabled")
        private Boolean enabled;

        @SerializedName("blockLimits")
        private Map<String, Integer> blockLimits = new HashMap<>();

        @SerializedName("entityLimits")
        private Map<String, Integer> entityLimits = new HashMap<>();

        @SerializedName("groups")
        private Map<String, GroupFileData> groups = new HashMap<>();

        @SerializedName("teamBlockLimits")
        private Map<String, Integer> teamBlockLimits = new HashMap<>();

        @SerializedName("teamGroups")
        private Map<String, GroupFileData> teamGroups = new HashMap<>();

        @SerializedName("teamEntityLimits")
        private Map<String, Integer> teamEntityLimits = new HashMap<>();

        @SerializedName("playerBlockLimits")
        private Map<String, Integer> playerBlockLimits = new HashMap<>();

        @SerializedName("playerGroups")
        private Map<String, GroupFileData> playerGroups = new HashMap<>();
    }

    private static final class GroupFileData {
        @SerializedName("limit")
        private Integer limit;

        @SerializedName("blocks")
        private List<String> blocks = new ArrayList<>();
    }
}
