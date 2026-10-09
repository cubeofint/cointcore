package com.mawlee.cointcore.chunklimit;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockOwnerRegistryTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static BlockOwnerRegistry registryWith(Map<String, String> keys, int version) {
        BlockOwnerRegistry registry = new BlockOwnerRegistry();
        registry.ensureResolver(version, keys::get);
        return registry;
    }

    @Test
    void countsPerOwnerAcrossDimensions() {
        BlockOwnerRegistry registry = registryWith(Map.of("minecraft:hopper", "block:minecraft:hopper"), 1);
        registry.put(OVERWORLD, 1L, 10L, ALICE, "minecraft:hopper");
        registry.put(NETHER, 1L, 10L, ALICE, "minecraft:hopper");
        registry.put(OVERWORLD, 1L, 11L, BOB, "minecraft:hopper");

        assertEquals(2, registry.count(ALICE, "block:minecraft:hopper"));
        assertEquals(1, registry.count(BOB, "block:minecraft:hopper"));
        assertEquals(3, registry.size());
    }

    @Test
    void groupKeySharesCountBetweenBlocks() {
        BlockOwnerRegistry registry = registryWith(Map.of(
                "minecraft:hopper", "group:logistics",
                "minecraft:dropper", "group:logistics"
        ), 1);
        registry.put(OVERWORLD, 1L, 1L, ALICE, "minecraft:hopper");
        registry.put(OVERWORLD, 1L, 2L, ALICE, "minecraft:dropper");

        assertEquals(2, registry.count(ALICE, "group:logistics"));
    }

    @Test
    void removeAndReplaceKeepCountsConsistent() {
        BlockOwnerRegistry registry = registryWith(Map.of("minecraft:hopper", "block:minecraft:hopper"), 1);
        registry.put(OVERWORLD, 1L, 1L, ALICE, "minecraft:hopper");
        registry.put(OVERWORLD, 1L, 2L, ALICE, "minecraft:hopper");

        registry.put(OVERWORLD, 1L, 2L, BOB, "minecraft:hopper");
        assertEquals(1, registry.count(ALICE, "block:minecraft:hopper"));
        assertEquals(1, registry.count(BOB, "block:minecraft:hopper"));
        assertEquals(2, registry.size());

        registry.remove(OVERWORLD, 1L, 1L);
        assertEquals(0, registry.count(ALICE, "block:minecraft:hopper"));
        assertNull(registry.remove(OVERWORLD, 1L, 1L));
        assertEquals(1, registry.size());
    }

    @Test
    void unlimitedBlocksAreStoredButNotCounted() {
        BlockOwnerRegistry registry = registryWith(Map.of(), 1);
        registry.put(OVERWORLD, 1L, 1L, ALICE, "minecraft:stone");

        assertEquals(0, registry.count(ALICE, "block:minecraft:stone"));
        assertEquals(1, registry.size());
    }

    @Test
    void newResolverVersionRecountsStoredBlocks() {
        BlockOwnerRegistry registry = registryWith(Map.of(), 1);
        registry.put(OVERWORLD, 1L, 1L, ALICE, "minecraft:hopper");
        registry.put(OVERWORLD, 1L, 2L, ALICE, "minecraft:hopper");

        registry.ensureResolver(1, Map.of("minecraft:hopper", "block:minecraft:hopper")::get);
        assertEquals(0, registry.count(ALICE, "block:minecraft:hopper"), "same version must not rebuild");

        registry.ensureResolver(2, Map.of("minecraft:hopper", "block:minecraft:hopper")::get);
        assertEquals(2, registry.count(ALICE, "block:minecraft:hopper"));
    }

    @Test
    void chunkIndexTracksPositions() {
        BlockOwnerRegistry registry = registryWith(Map.of(), 1);
        registry.put(OVERWORLD, 5L, 100L, ALICE, "minecraft:hopper");
        registry.put(OVERWORLD, 5L, 101L, ALICE, "minecraft:hopper");
        registry.put(OVERWORLD, 6L, 200L, ALICE, "minecraft:hopper");

        List<Long> positions = registry.positionsInChunk(OVERWORLD, 5L);
        assertEquals(2, positions.size());
        assertTrue(positions.containsAll(List.of(100L, 101L)));

        registry.remove(OVERWORLD, 5L, 100L);
        registry.remove(OVERWORLD, 5L, 101L);
        assertTrue(registry.positionsInChunk(OVERWORLD, 5L).isEmpty());
        assertTrue(registry.positionsInChunk(NETHER, 5L).isEmpty());
    }
}
