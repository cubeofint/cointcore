package com.mawlee.cointcore.luckperms;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.metadata.NodeMetadataKey;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.util.Tristate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resolves LuckPerms API 5.4 methods through package-private fakes that implement
 * the public interfaces — the same layout as {@code Prefix$Builder}.
 */
class LuckPermsApi54MethodAuditTest {
    private static final NodeMetadataKey<String> RENDER =
            NodeMetadataKey.of("cointcore-quest-render", String.class);

    @BeforeEach
    void clearResolverCache() {
        PublicApiMethodResolver.clearCache();
    }

    @Test
    void prefixBuilderWithMetadataResolvesOnNodeBuilderNotImpl() throws Exception {
        HiddenPrefixBuilder impl = new HiddenPrefixBuilder();
        Method method = PublicApiMethodResolver.resolve(
                impl.getClass(),
                "withMetadata",
                NodeMetadataKey.class,
                Object.class
        );

        assertEquals(NodeBuilder.class, method.getDeclaringClass());
        assertTrue(Modifier.isPublic(method.getDeclaringClass().getModifiers()));
        assertFalse(Modifier.isPublic(impl.getClass().getModifiers()));

        Object returned = PublicApiMethodResolver.invoke(
                impl,
                "withMetadata",
                new Class<?>[]{NodeMetadataKey.class, Object.class},
                RENDER,
                "1"
        );
        assertSame(impl, returned);
        assertEquals("1", impl.metadata.get(RENDER));
    }

    @Test
    void prefixBuilderStringStringWithMetadataIsNotAnApiMethod() {
        HiddenPrefixBuilder impl = new HiddenPrefixBuilder();
        assertThrows(
                NoSuchMethodException.class,
                () -> PublicApiMethodResolver.resolve(
                        impl.getClass(),
                        "withMetadata",
                        String.class,
                        String.class
                )
        );
        assertEquals(0, impl.legacyStringCalls);
    }

    @Test
    void nodeGetMetadataRequiresNodeMetadataKey() throws Exception {
        HiddenPrefixNode impl = new HiddenPrefixNode();
        impl.metadata.put(RENDER, "1");

        assertThrows(
                NoSuchMethodException.class,
                () -> PublicApiMethodResolver.resolve(impl.getClass(), "getMetadata")
        );

        Method method = PublicApiMethodResolver.resolve(
                impl.getClass(),
                "getMetadata",
                NodeMetadataKey.class
        );
        assertEquals(Node.class, method.getDeclaringClass());

        @SuppressWarnings("unchecked")
        Optional<String> value = (Optional<String>) PublicApiMethodResolver.invoke(
                impl,
                "getMetadata",
                new Class<?>[]{NodeMetadataKey.class},
                RENDER
        );
        assertEquals(Optional.of("1"), value);
    }

    @Test
    void nodeMapClearAndAddResolveOnPublicNodeMap() throws Exception {
        HiddenNodeMap impl = new HiddenNodeMap();
        Method clear = PublicApiMethodResolver.resolve(impl.getClass(), "clear", Predicate.class);
        Method add = PublicApiMethodResolver.resolve(impl.getClass(), "add", Node.class);

        assertEquals(NodeMap.class, clear.getDeclaringClass());
        assertEquals(NodeMap.class, add.getDeclaringClass());
    }

    @Test
    void assignableStringMatchesErasedObjectMetadataValue() throws Exception {
        HiddenPrefixBuilder impl = new HiddenPrefixBuilder();
        Method method = PublicApiMethodResolver.resolve(
                impl.getClass(),
                "withMetadata",
                RENDER.getClass(),
                String.class
        );
        assertEquals(NodeBuilder.class, method.getDeclaringClass());
        method.invoke(impl, RENDER, "titan");
        assertEquals("titan", impl.metadata.get(RENDER));
    }

    static final class HiddenPrefixBuilder implements PrefixNode.Builder {
        private final Map<NodeMetadataKey<?>, Object> metadata = new LinkedHashMap<>();
        private int legacyStringCalls;

        @Override
        public PrefixNode.Builder prefix(String prefix) {
            return this;
        }

        @Override
        public PrefixNode.Builder priority(int priority) {
            return this;
        }

        @Override
        public PrefixNode.Builder value(boolean value) {
            return this;
        }

        @Override
        public PrefixNode.Builder negated(boolean negated) {
            return this;
        }

        @Override
        public PrefixNode.Builder expiry(long expiryEpochSeconds) {
            return this;
        }

        @Override
        public PrefixNode.Builder expiry(TemporalAccessor expiry) {
            return this;
        }

        @Override
        public PrefixNode.Builder expiry(TemporalAmount duration) {
            return this;
        }

        @Override
        public PrefixNode.Builder clearExpiry() {
            return this;
        }

        @Override
        public PrefixNode.Builder context(ContextSet contextSet) {
            return this;
        }

        @Override
        public PrefixNode.Builder withContext(String key, String value) {
            return this;
        }

        @Override
        public PrefixNode.Builder withContext(ContextSet contextSet) {
            return this;
        }

        @Override
        public <T> PrefixNode.Builder withMetadata(NodeMetadataKey<T> key, T metadata) {
            this.metadata.put(key, metadata);
            return this;
        }

        public PrefixNode.Builder withMetadata(String key, String value) {
            legacyStringCalls++;
            return this;
        }

        @Override
        public PrefixNode build() {
            return new HiddenPrefixNode(Map.copyOf(metadata));
        }
    }

    static final class HiddenPrefixNode implements PrefixNode {
        private final Map<NodeMetadataKey<?>, Object> metadata;

        HiddenPrefixNode() {
            this.metadata = new LinkedHashMap<>();
        }

        HiddenPrefixNode(Map<NodeMetadataKey<?>, Object> metadata) {
            this.metadata = new LinkedHashMap<>(metadata);
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public String getMetaValue() {
            return "";
        }

        @Override
        public ChatMetaType getMetaType() {
            return ChatMetaType.PREFIX;
        }

        @Override
        public String getKey() {
            return "prefix.0.";
        }

        @Override
        public Collection<String> resolveShorthand() {
            return java.util.List.of();
        }

        @Override
        public boolean getValue() {
            return true;
        }

        @Override
        public boolean hasExpiry() {
            return false;
        }

        @Override
        public java.time.Instant getExpiry() {
            return null;
        }

        @Override
        public boolean hasExpired() {
            return false;
        }

        @Override
        public java.time.Duration getExpiryDuration() {
            return null;
        }

        @Override
        public net.luckperms.api.context.ImmutableContextSet getContexts() {
            return net.luckperms.api.context.ImmutableContextSet.empty();
        }

        @Override
        public <T> Optional<T> getMetadata(NodeMetadataKey<T> key) {
            @SuppressWarnings("unchecked")
            T value = (T) metadata.get(key);
            return Optional.ofNullable(value);
        }

        public Map<String, String> getMetadata() {
            return Map.of();
        }

        @Override
        public boolean equals(Node other, NodeEqualityPredicate equalityPredicate) {
            return false;
        }

        @Override
        public PrefixNode.Builder toBuilder() {
            return new HiddenPrefixBuilder();
        }
    }

    static final class HiddenNodeMap implements NodeMap {
        @Override
        public Map<net.luckperms.api.context.ImmutableContextSet, Collection<Node>> toMap() {
            return Map.of();
        }

        @Override
        public Collection<Node> toCollection() {
            return java.util.List.of();
        }

        @Override
        public Tristate contains(Node node, NodeEqualityPredicate equalityPredicate) {
            return Tristate.UNDEFINED;
        }

        @Override
        public DataMutateResult add(Node node) {
            return DataMutateResult.SUCCESS;
        }

        @Override
        public DataMutateResult.WithMergedNode add(Node node, TemporaryNodeMergeStrategy strategy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DataMutateResult remove(Node node) {
            return DataMutateResult.SUCCESS;
        }

        @Override
        public void clear() {
        }

        @Override
        public void clear(Predicate<? super Node> test) {
        }

        @Override
        public void clear(ContextSet contextSet) {
        }

        @Override
        public void clear(ContextSet contextSet, Predicate<? super Node> test) {
        }
    }
}
