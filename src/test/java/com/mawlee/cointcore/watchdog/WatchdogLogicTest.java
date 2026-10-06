package com.mawlee.cointcore.watchdog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassToModMapperTest {
    @Test
    void prefersLongestPackagePrefix() {
        ClassToModMapper mapper = ClassToModMapper.fromPackageMap(Map.of(
                "com.enderio", "enderio",
                "com.enderio.conduits", "enderio_conduits",
                "dev.ftb.mods.ftbchunks", "ftbchunks"
        ));
        assertEquals("enderio_conduits", mapper.resolve("com.enderio.conduits.item.ItemConduit"));
        assertEquals("enderio", mapper.resolve("com.enderio.machines.MachineBlock"));
        assertEquals("ftbchunks", mapper.resolve("dev.ftb.mods.ftbchunks.data.ClaimedChunkImpl"));
    }

    @Test
    void mapsVanillaAndNeoForgeByPrefix() {
        ClassToModMapper mapper = ClassToModMapper.empty();
        assertEquals("minecraft", mapper.resolve("net.minecraft.server.MinecraftServer"));
        assertEquals("neoforge", mapper.resolve("net.neoforged.neoforge.event.tick.ServerTickEvent"));
    }

    @Test
    void fallsBackToSecondPackageSegment() {
        ClassToModMapper mapper = ClassToModMapper.empty();
        assertEquals("relics", mapper.resolve("com.relics.items.RelicItem"));
    }

    @Test
    void mapsPackagesFromJarEntries(@TempDir Path dir) throws Exception {
        Path jar = dir.resolve("enderio.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry("com/enderio/conduits/ItemConduit.class"));
            out.write(new byte[] {0x00, 0x01});
            out.closeEntry();
            out.putNextEntry(new JarEntry("com/enderio/conduits/ItemConduit$Node.class"));
            out.write(new byte[] {0x00, 0x01});
            out.closeEntry();
        }
        Map<String, String> prefixes = WatchdogModIndex.scanJarForTest(jar, "enderio");
        assertEquals("enderio", prefixes.get("com.enderio.conduits"));
        ClassToModMapper mapper = ClassToModMapper.fromPackageMap(prefixes);
        assertEquals("enderio", mapper.resolve("com.enderio.conduits.ItemConduit"));
    }
}

class OffenderAggregatorTest {
    @Test
    void ranksBlockEntitiesByTotalTime() {
        OffenderAggregator aggregator = new OffenderAggregator();
        aggregator.addBlockEntity("enderio:item_conduit", "enderio", "minecraft:overworld", 100, 64, 200, 50_000_000L);
        aggregator.addBlockEntity("enderio:item_conduit", "enderio", "minecraft:overworld", 100, 64, 200, 30_000_000L);
        aggregator.addBlockEntity("ae2:storage_bus", "ae2", "minecraft:overworld", 8, 70, 8, 10_000_000L);

        List<OffenderAggregator.OffenderSnapshot> top = aggregator.top(OffenderAggregator.Kind.BLOCK_ENTITY, 10);
        assertEquals(2, top.size());
        assertEquals("enderio:item_conduit", top.getFirst().typeId());
        assertEquals(80_000_000L, top.getFirst().totalNanos());
        assertEquals(2L, top.getFirst().ticks());
        assertEquals(6, top.getFirst().chunkX());
        assertEquals(12, top.getFirst().chunkZ());
        assertEquals("enderio", top.getFirst().modId());
    }

    @Test
    void aggregatesChunksAndDimensions() {
        OffenderAggregator aggregator = new OffenderAggregator();
        aggregator.addEntity("minecraft:item", "minecraft", "minecraft:the_nether", 16, 70, 16, 5_000_000L);
        aggregator.addBlockEntity("ae2:interface", "ae2", "minecraft:the_nether", 17, 70, 18, 7_000_000L);

        List<OffenderAggregator.OffenderSnapshot> chunks = aggregator.top(OffenderAggregator.Kind.CHUNK, 5);
        assertEquals(1, chunks.size());
        assertEquals(12_000_000L, chunks.getFirst().totalNanos());

        List<OffenderAggregator.OffenderSnapshot> dims = aggregator.top(OffenderAggregator.Kind.DIMENSION, 5);
        assertEquals("minecraft:the_nether", dims.getFirst().dimension());
        assertEquals(12_000_000L, dims.getFirst().totalNanos());
    }
}

class TickStatsWindowTest {
    @Test
    void computesAverageP95MaxAndTps() {
        TickStatsWindow window = new TickStatsWindow(10, 50_000_000L);
        window.record(10_000_000L);
        window.record(12_000_000L);
        window.record(100_000_000L);
        assertEquals(3, window.size());
        assertEquals(1L, window.slowTicks());
        window.record(80_000_000L);
        assertTrue(window.averageMillis() > 40.0D);
        assertEquals(100.0D, window.maxMillis(), 0.01D);
        assertTrue(window.percentile95Millis() >= 12.0D);
        assertTrue(window.estimatedTps() < 20.0D);
        assertEquals(2, window.recentSlowCount(4));
    }
}

class StackSampleAggregatorTest {
    @Test
    void ranksSelfAndTotalAndMapsMods() {
        ClassToModMapper mapper = ClassToModMapper.fromPackageMap(Map.of(
                "com.enderio", "enderio",
                "it.hurts.sskirillss.relics", "relics"
        ));
        StackSampleAggregator aggregator = new StackSampleAggregator();
        StackTraceElement[] stack = new StackTraceElement[] {
                new StackTraceElement("it.hurts.sskirillss.relics.items.RelicItem", "copy", "RelicItem.java", 10),
                new StackTraceElement("com.enderio.conduits.ItemConduit", "tick", "ItemConduit.java", 20),
                new StackTraceElement("java.lang.Thread", "run", "Thread.java", 1)
        };
        TickProbe.enterBlockEntity("enderio:item_conduit", "enderio", "minecraft:overworld", 10, 64, 20);
        aggregator.addSample(stack, mapper, TickProbe.snapshot());
        aggregator.addSample(stack, mapper, TickProbe.snapshot());
        TickProbe.clear();

        List<StackSampleAggregator.MethodStat> self = aggregator.topBySelf(5);
        assertEquals("copy", self.getFirst().methodName());
        assertEquals("relics", self.getFirst().modId());
        assertTrue(self.getFirst().selfPercent() > 90.0D);
        assertEquals("enderio:item_conduit", self.getFirst().correlatedTypeId());

        List<StackSampleAggregator.MethodStat> total = aggregator.topByTotal(5);
        assertTrue(total.stream().anyMatch(stat -> "tick".equals(stat.methodName()) && "enderio".equals(stat.modId())));
        assertEquals(2L, aggregator.sampleCount());
    }
}

class WatchdogReportFormatterTest {
    @Test
    void includesCoordinatesAndPlainLanguageMethodLine() {
        TickStatsWindow stats = new TickStatsWindow(8, 100_000_000L);
        stats.record(12_000_000L);
        stats.record(110_000_000L);
        OffenderAggregator aggregator = new OffenderAggregator();
        aggregator.addBlockEntity("enderio:item_conduit", "enderio", "minecraft:overworld", 100, 64, -20, 80_000_000L);
        List<StackSampleAggregator.MethodStat> methods = List.of(new StackSampleAggregator.MethodStat(
                "it.hurts.sskirillss.relics.items.RelicItem",
                "copy",
                "relics",
                10,
                12,
                83.3D,
                100.0D,
                "enderio:item_conduit",
                "minecraft:overworld",
                100,
                64,
                -20
        ));
        String report = WatchdogReportFormatter.format(
                Instant.parse("2026-10-06T12:00:00Z"),
                stats,
                aggregator.top(OffenderAggregator.Kind.BLOCK_ENTITY, 5),
                List.of(),
                List.of(),
                List.of(),
                methods,
                12,
                ignored -> new WatchdogReportFormatter.ChunkContext("Team Coint", "ftbchunks")
        );
        assertTrue(report.contains("100,64,-20"));
        assertTrue(report.contains("enderio:item_conduit"));
        assertTrue(report.contains("метод copy мода relics"));
        assertTrue(report.contains("claim=Team Coint"));
        assertTrue(report.contains("forceload=ftbchunks"));
        String concise = WatchdogReportFormatter.conciseTopLine(
                aggregator.top(OffenderAggregator.Kind.BLOCK_ENTITY, 3),
                3
        );
        assertTrue(concise.contains("enderio:item_conduit"));
    }
}
