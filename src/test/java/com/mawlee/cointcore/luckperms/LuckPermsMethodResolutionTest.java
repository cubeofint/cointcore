package com.mawlee.cointcore.luckperms;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuckPermsMethodResolutionTest {
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
    void getNodesResolvesOnPublicHolder() throws Exception {
        HiddenHolder impl = new HiddenHolder(List.of("prefix.100.vip"));
        Object nodes = PublicApiMethodResolver.invoke(impl, "getNodes");

        assertEquals(List.of("prefix.100.vip"), nodes);
        Method method = PublicApiMethodResolver.resolve(impl.getClass(), "getNodes");
        assertEquals(PublicHolder.class, method.getDeclaringClass());
    }

    public interface PublicNodeMap {
        void clear(Predicate<Object> predicate);

        void add(Object node);
    }

    public interface PublicHolder {
        Iterable<String> getNodes();

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
}
