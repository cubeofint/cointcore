package com.mawlee.cointcore.luckperms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuckPermsMethodResolutionTest {
    @BeforeEach
    void clearResolverCache() {
        PublicApiMethodResolver.clearCache();
    }

    @Test
    void resolvesClearFromPublicInterfaceNotHiddenImpl() throws Exception {
        HiddenNodeMap impl = new HiddenNodeMap();
        Method method = PublicApiMethodResolver.resolve(impl.getClass(), "clear", Predicate.class);

        assertEquals(PublicNodeMap.class, method.getDeclaringClass());
        assertTrue(Modifier.isPublic(method.getDeclaringClass().getModifiers()));
        assertFalse(Modifier.isPublic(impl.getClass().getModifiers()));

        method.invoke(impl, (Predicate<Object>) node -> true);
        assertEquals(1, impl.clears);
    }

    @Test
    void invokeClearAndAddOnHiddenImpl() throws Exception {
        HiddenNodeMap impl = new HiddenNodeMap();
        PublicApiMethodResolver.invoke(
                impl,
                "clear",
                new Class<?>[]{Predicate.class},
                (Predicate<Object>) node -> "drop".equals(node)
        );
        PublicApiMethodResolver.invoke(impl, "add", new Class<?>[]{Object.class}, "keep");
        PublicApiMethodResolver.invoke(impl, "add", new Class<?>[]{Object.class}, "drop");

        assertEquals(1, impl.clears);
        assertEquals(List.of("keep", "drop"), impl.added);
    }

    @Test
    void cachesResolvedMethodWithoutRepeatedLookup() throws Exception {
        HiddenHolder impl = new HiddenHolder(List.of("prefix.100.vip"));
        Method first = PublicApiMethodResolver.resolve(impl.getClass(), "getNodes");
        int sizeAfterFirst = PublicApiMethodResolver.cacheSize();
        Method second = PublicApiMethodResolver.resolve(impl.getClass(), "getNodes");
        Method byCount = PublicApiMethodResolver.resolveByCount(impl.getClass(), "getNodes", 0);

        assertTrue(first == second);
        assertEquals(PublicHolder.class, first.getDeclaringClass());
        assertEquals(PublicHolder.class, byCount.getDeclaringClass());
        assertTrue(PublicApiMethodResolver.cacheSize() >= sizeAfterFirst);
        assertTrue(sizeAfterFirst >= 1);
    }

    @Test
    void getNodesResolvesOnPublicHolder() throws Exception {
        HiddenHolder impl = new HiddenHolder(List.of("prefix.100.vip"));
        Object nodes = PublicApiMethodResolver.invoke(impl, "getNodes");

        assertEquals(List.of("prefix.100.vip"), nodes);
        Method method = PublicApiMethodResolver.resolve(impl.getClass(), "getNodes");
        assertEquals(PublicHolder.class, method.getDeclaringClass());
    }

    @Test
    void walksPrefixBuilderSuperinterfacesForWithMetadata() throws Exception {
        HiddenPrefixBuilder impl = new HiddenPrefixBuilder();
        List<Class<?>> types = PublicApiMethodResolver.publicTypes(impl.getClass());

        assertTrue(types.contains(PublicPrefixNode.Builder.class));
        assertTrue(types.contains(PublicChatMetaNode.Builder.class));
        assertTrue(types.contains(PublicNodeBuilder.class));
        assertFalse(types.contains(impl.getClass()));
        assertFalse(Modifier.isPublic(impl.getClass().getModifiers()));

        Method method = PublicApiMethodResolver.resolve(
                impl.getClass(),
                "withMetadata",
                PublicNodeMetadataKey.class,
                Object.class
        );
        assertEquals(PublicNodeBuilder.class, method.getDeclaringClass());
        assertTrue(Modifier.isPublic(method.getDeclaringClass().getModifiers()));
    }

    @Test
    void withMetadataMatchesErasedNodeMetadataKeyObjectViaAssignableTypes() throws Exception {
        HiddenPrefixBuilder impl = new HiddenPrefixBuilder();
        HiddenMetadataKey key = new HiddenMetadataKey("cointcore-quest-render");

        Method method = PublicApiMethodResolver.resolve(
                impl.getClass(),
                "withMetadata",
                key.getClass(),
                String.class
        );
        assertEquals(PublicNodeBuilder.class, method.getDeclaringClass());

        Object returned = PublicApiMethodResolver.invoke(
                impl,
                "withMetadata",
                new Class<?>[]{key.getClass(), String.class},
                key,
                "1"
        );
        assertSame(impl, returned);
        assertEquals("1", impl.metadata.get(key));
    }

    @Test
    void withMetadataStringStringDoesNotFallBackToImpl() {
        HiddenPrefixBuilder impl = new HiddenPrefixBuilder();

        NoSuchMethodException exception = assertThrows(
                NoSuchMethodException.class,
                () -> PublicApiMethodResolver.resolve(
                        impl.getClass(),
                        "withMetadata",
                        String.class,
                        String.class
                )
        );
        assertTrue(exception.getMessage().contains("withMetadata"));
        assertTrue(exception.getMessage().contains("not found on public API types"));
        assertEquals(0, impl.stringStringCalls);
    }

    @Test
    void neverResolvesImplOnlyMethod() {
        HiddenPrefixBuilder impl = new HiddenPrefixBuilder();

        assertThrows(
                NoSuchMethodException.class,
                () -> PublicApiMethodResolver.resolve(impl.getClass(), "implOnlyTag", String.class)
        );
        assertThrows(
                NoSuchMethodException.class,
                () -> PublicApiMethodResolver.resolveByCount(impl.getClass(), "implOnlyTag", 1)
        );
    }

    @Test
    void getMetadataRequiresKeyAndDoesNotUseNoArgImplMap() throws Exception {
        HiddenPrefixNode impl = new HiddenPrefixNode();
        HiddenMetadataKey key = new HiddenMetadataKey("cointcore-quest-render");
        impl.metadata.put(key, "1");

        assertThrows(
                NoSuchMethodException.class,
                () -> PublicApiMethodResolver.resolve(impl.getClass(), "getMetadata")
        );

        Method method = PublicApiMethodResolver.resolve(
                impl.getClass(),
                "getMetadata",
                PublicNodeMetadataKey.class
        );
        assertEquals(PublicNode.class, method.getDeclaringClass());

        @SuppressWarnings("unchecked")
        Optional<String> value = (Optional<String>) PublicApiMethodResolver.invoke(
                impl,
                "getMetadata",
                new Class<?>[]{PublicNodeMetadataKey.class},
                key
        );
        assertEquals(Optional.of("1"), value);
        assertEquals(0, impl.noArgCalls);
    }

    @Test
    void withContextAndBuildResolveOnNodeBuilder() throws Exception {
        HiddenPrefixBuilder impl = new HiddenPrefixBuilder();
        PublicApiMethodResolver.invoke(
                impl,
                "withContext",
                new Class<?>[]{String.class, String.class},
                "server",
                "global"
        );
        Object built = PublicApiMethodResolver.invoke(impl, "build");

        assertEquals("global", impl.contexts.get("server"));
        assertSame(impl.built, built);
        assertEquals(PublicNodeBuilder.class, PublicApiMethodResolver.resolve(
                impl.getClass(),
                "withContext",
                String.class,
                String.class
        ).getDeclaringClass());
    }

    @Test
    void inheritanceAndMetaBuildersResolvePublicFactoryMethods() throws Exception {
        Method inheritance = PublicApiMethodResolver.resolve(
                PublicInheritanceNode.class,
                "builder",
                String.class
        );
        Method meta = PublicApiMethodResolver.resolve(
                PublicMetaNode.class,
                "builder",
                String.class,
                String.class
        );
        Method prefix = PublicApiMethodResolver.resolve(
                PublicPrefixNode.class,
                "builder",
                String.class,
                int.class
        );

        assertEquals(PublicInheritanceNode.class, inheritance.getDeclaringClass());
        assertEquals(PublicMetaNode.class, meta.getDeclaringClass());
        assertEquals(PublicPrefixNode.class, prefix.getDeclaringClass());
        assertEquals("group.vip", inheritance.invoke(null, "vip"));
        assertEquals("k=v", meta.invoke(null, "k", "v"));
        assertEquals("p:100", prefix.invoke(null, "p", 100));
    }

    @Test
    void publicTypesPreferTheRequestedPublicType() {
        List<Class<?>> types = PublicApiMethodResolver.publicTypes(PublicInheritanceNode.class);
        assertEquals(PublicInheritanceNode.class, types.get(0));
        assertTrue(types.contains(PublicGenericNode.class));
    }

    @Test
    void inheritanceBuilderIsNotGenericNodeBuilder() throws Exception {
        Method generic = PublicApiMethodResolver.resolve(PublicGenericNode.class, "builder", String.class);
        Method inheritance = PublicApiMethodResolver.resolve(PublicInheritanceNode.class, "builder", String.class);

        assertEquals(PublicGenericNode.class, generic.getDeclaringClass());
        assertEquals(PublicInheritanceNode.class, inheritance.getDeclaringClass());
        assertEquals("atm", generic.invoke(null, "atm"));
        assertEquals("group.atm", inheritance.invoke(null, "atm"));
        assertTrue(InheritanceNodes.isInheritanceKey((String) inheritance.invoke(null, "atm")));
        assertFalse(InheritanceNodes.isInheritanceKey((String) generic.invoke(null, "atm")));
    }

    @Test
    void userDataMethodsResolveOnPublicInterfaces() throws Exception {
        HiddenUser user = new HiddenUser();
        HiddenNodeMap data = (HiddenNodeMap) PublicApiMethodResolver.invoke(user, "data");
        PublicApiMethodResolver.invoke(data, "add", new Class<?>[]{Object.class}, "node");

        assertEquals(PublicUser.class, PublicApiMethodResolver.resolve(user.getClass(), "data").getDeclaringClass());
        assertEquals(PublicUser.class, PublicApiMethodResolver.resolve(user.getClass(), "getUniqueId").getDeclaringClass());
        assertEquals(List.of("node"), data.added);
    }

    public interface PublicNodeMap {
        void clear(Predicate<Object> predicate);

        void add(Object node);
    }

    public interface PublicHolder {
        Iterable<String> getNodes();

        PublicNodeMap data();
    }

    public interface PublicNodeMetadataKey {
        String name();
    }

    /**
     * Mirrors LuckPerms API 5.4 {@code NodeBuilder.withMetadata(NodeMetadataKey, T)}
     * after generic erasure ({@code NodeMetadataKey, Object}).
     */
    public interface PublicNodeBuilder {
        PublicNodeBuilder withMetadata(PublicNodeMetadataKey key, Object metadata);

        PublicNodeBuilder withContext(String key, String value);

        Object build();
    }

    public interface PublicChatMetaNode {
        interface Builder extends PublicNodeBuilder {
        }
    }

    public interface PublicPrefixNode {
        static String builder(String prefix, int priority) {
            return prefix + ":" + priority;
        }

        interface Builder extends PublicChatMetaNode.Builder {
        }
    }

    public interface PublicNode {
        Optional<Object> getMetadata(PublicNodeMetadataKey key);
    }

    /**
     * Mirrors LuckPerms {@code Node.builder(String)} — a generic permission node.
     */
    public interface PublicGenericNode {
        static String builder(String permission) {
            return permission;
        }
    }

    /**
     * Mirrors LuckPerms {@code InheritanceNode extends Node} with its own
     * {@code builder(String group)} that must win over {@link PublicGenericNode#builder(String)}.
     */
    public interface PublicInheritanceNode extends PublicGenericNode {
        static String builder(String group) {
            return "group." + group;
        }
    }

    public interface PublicMetaNode {
        static String builder(String key, String value) {
            return key + "=" + value;
        }
    }

    public interface PublicUser {
        Object getUniqueId();

        PublicNodeMap data();
    }

    static final class HiddenNodeMap implements PublicNodeMap {
        private final List<Object> added = new ArrayList<>();
        private int clears;

        @Override
        public void clear(Predicate<Object> predicate) {
            clears++;
            added.removeIf(predicate);
        }

        @Override
        public void add(Object node) {
            added.add(node);
        }
    }

    static final class HiddenHolder implements PublicHolder {
        private final List<String> nodes;
        private final HiddenNodeMap data = new HiddenNodeMap();

        private HiddenHolder(List<String> nodes) {
            this.nodes = nodes;
        }

        @Override
        public Iterable<String> getNodes() {
            return nodes;
        }

        @Override
        public PublicNodeMap data() {
            return data;
        }
    }

    static final class HiddenMetadataKey implements PublicNodeMetadataKey {
        private final String name;

        HiddenMetadataKey(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }
    }

    /**
     * Package-private impl, like {@code me.lucko.luckperms.common.node.types.Prefix$Builder}.
     * Exposes a {@code withMetadata(String, String)} that must never be selected.
     */
    static final class HiddenPrefixBuilder implements PublicPrefixNode.Builder {
        private final Map<PublicNodeMetadataKey, Object> metadata = new LinkedHashMap<>();
        private final Map<String, String> contexts = new LinkedHashMap<>();
        private final Object built = new Object();
        private int stringStringCalls;

        @Override
        public PublicNodeBuilder withMetadata(PublicNodeMetadataKey key, Object value) {
            metadata.put(key, value);
            return this;
        }

        public PublicNodeBuilder withMetadata(String key, String value) {
            stringStringCalls++;
            metadata.put(new HiddenMetadataKey(key), value);
            return this;
        }

        public void implOnlyTag(String ignored) {
        }

        @Override
        public PublicNodeBuilder withContext(String key, String value) {
            contexts.put(key, value);
            return this;
        }

        @Override
        public Object build() {
            return built;
        }
    }

    static final class HiddenPrefixNode implements PublicNode {
        private final Map<PublicNodeMetadataKey, Object> metadata = new LinkedHashMap<>();
        private int noArgCalls;

        @Override
        public Optional<Object> getMetadata(PublicNodeMetadataKey key) {
            return Optional.ofNullable(metadata.get(key));
        }

        public Map<String, String> getMetadata() {
            noArgCalls++;
            Map<String, String> copy = new LinkedHashMap<>();
            for (Map.Entry<PublicNodeMetadataKey, Object> entry : metadata.entrySet()) {
                copy.put(entry.getKey().name(), String.valueOf(entry.getValue()));
            }
            return copy;
        }
    }

    static final class HiddenUser implements PublicUser {
        private final HiddenNodeMap data = new HiddenNodeMap();

        @Override
        public Object getUniqueId() {
            return "user";
        }

        @Override
        public PublicNodeMap data() {
            return data;
        }
    }
}
