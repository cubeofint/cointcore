package com.mawlee.cointcore.watchdog.probes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mawlee.cointcore.watchdog.ClassToModMapper;
import com.mawlee.cointcore.watchdog.StackSampleAggregator;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankedCounterTest {
    @Test
    void ranksAndCapsKeys() {
        RankedCounter counter = new RankedCounter(8);
        counter.increment("a", 5);
        counter.increment("b", 2);
        counter.increment("a", 3);
        List<RankedCounter.Rank> top = counter.top(1);
        assertEquals(1, top.size());
        assertEquals("a", top.getFirst().key());
        assertEquals(8L, top.getFirst().value());
    }

    @Test
    void incrementOverheadIsNegligible() {
        RankedCounter counter = new RankedCounter(512);
        long started = System.nanoTime();
        for (int i = 0; i < 200_000; i++) {
            counter.increment("chunk:" + (i & 255));
        }
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        assertTrue(elapsedMs < 80L, "200k increments took " + elapsedMs + " ms");
        assertEquals(256, counter.size());
        assertEquals(200_000L, counter.total());
    }
}

class WatchdogProbeSettingsTest {
    @Test
    void defaultsCheapProbesOn() {
        WatchdogProbeSettings.Snapshot defaults = WatchdogProbeSettings.Snapshot.defaults();
        assertTrue(defaults.gcMemory());
        assertTrue(defaults.chunks());
        assertTrue(defaults.diskSave());
        assertTrue(defaults.serverTasks());
        assertTrue(defaults.blockUpdates());
        assertTrue(defaults.groundEntities());
        assertTrue(defaults.pathfinding());
        assertTrue(defaults.network());
        assertTrue(defaults.modNetworks());
        assertTrue(defaults.trends());
        assertEquals(15, defaults.analysisIntervalSeconds());
    }

    @Test
    void parsesNestedProbesAndClamps() {
        JsonObject root = JsonParser.parseString("""
                {
                  "slowTickThresholdMs": 50,
                  "probes": {
                    "gcMemory": false,
                    "analysisIntervalSeconds": 99,
                    "topEntries": 4
                  }
                }
                """).getAsJsonObject();
        WatchdogProbeSettings.Snapshot parsed = WatchdogProbeSettings.parse(root);
        assertFalse(parsed.gcMemory());
        assertEquals(15, parsed.analysisIntervalSeconds());
        assertEquals(4, parsed.topEntries());
        assertTrue(parsed.chunks());
    }

    @Test
    void missingProbesUseDefaults() {
        WatchdogProbeSettings.Snapshot parsed = WatchdogProbeSettings.parse(new JsonObject());
        assertTrue(parsed.gcMemory());
        assertEquals(16, parsed.stackSampleCount());
    }
}

class GcMemoryProbeTest {
    @Test
    void snapshotReadsRealMxBeans() {
        GcMemoryProbe probe = new GcMemoryProbe();
        probe.onTickEnd(true, 1.0D);
        probe.onTickEnd(true, 1.0D);
        GcMemoryProbe.Snapshot snapshot = probe.snapshot();
        assertTrue(snapshot.heapUsedPercent() >= 0.0D);
        assertTrue(snapshot.heapUsedPercent() <= 100.0D);
        assertTrue(snapshot.heapUsedBytes() > 0L);
        assertFalse(GcMemoryProbe.humanBytes(3_500_000L).isBlank());
    }
}

class ChunkLoadProbeTest {
    @Test
    void attributesGeneratedChunksToPlayers() {
        ChunkLoadProbe probe = new ChunkLoadProbe();
        probe.record("minecraft:overworld", 10, -4, true, "player_fly", "Steve");
        probe.record("minecraft:overworld", 11, -4, true, "player_fly", "Steve");
        probe.record("minecraft:overworld", 0, 0, false, "forced", null);
        List<String> lines = probe.formatLines(5);
        assertTrue(lines.stream().anyMatch(line -> line.contains("generated=2")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("Steve")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("pos=168,~, -56") || line.contains("chunk=10,-4")));
    }
}

class TickMsptWindowTest {
    @Test
    void computesOneAndFiveMinuteAverages() {
        TickMsptWindow window = new TickMsptWindow(8);
        window.record(10_000_000L);
        window.record(20_000_000L);
        window.record(30_000_000L);
        assertEquals(20.0D, window.averageLastMillis(3), 0.01D);
        assertEquals(25.0D, window.averageLastMillis(2), 0.01D);
    }
}

class TrendProbeTest {
    @Test
    void tracksMsptAndBaseGrowth() {
        TrendProbe probe = new TrendProbe();
        for (int i = 0; i < 5; i++) {
            probe.recordTick(40_000_000L);
        }
        assertEquals(40.0D, probe.oneMinuteMspt(), 0.01D);
        probe.recordBaseCounts(List.of(new TrendProbe.BaseCount("Team A", 10, 40)));
        probe.recordBaseCounts(List.of(new TrendProbe.BaseCount("Team A", 14, 55)));
        TrendProbe.BaseGrowth growth = probe.growth().getFirst();
        assertEquals(4, growth.beDelta());
        assertEquals(15, growth.entityDelta());
        assertTrue(probe.formatLines(3).getFirst().contains("1m="));
    }
}

class ServerTaskProbeTest {
    @Test
    void groupsTopFramesByModPackage() {
        ServerTaskProbe probe = new ServerTaskProbe();
        probe.recordQueueLength(7);
        probe.recordQueueLength(3);
        ClassToModMapper mapper = ClassToModMapper.fromPackageMap(Map.of(
                "appeng.me", "ae2",
                "com.refinedmods.refinedstorage", "refinedstorage"
        ));
        probe.addSample(new StackTraceElement[] {
                new StackTraceElement("appeng.me.Grid", "tick", "Grid.java", 10)
        }, mapper);
        probe.addSample(new StackTraceElement[] {
                new StackTraceElement("appeng.me.Grid", "tick", "Grid.java", 10)
        }, mapper);
        List<String> lines = probe.formatLines(5);
        assertTrue(lines.getFirst().contains("queue=3"));
        assertTrue(lines.getFirst().contains("max=7"));
        assertTrue(lines.stream().anyMatch(line -> line.contains("ae2")));
    }
}

class DiskSaveProbeTest {
    @Test
    void flagsQuestSaves() {
        assertTrue(DiskSaveProbe.isQuestSave("dev.ftb.mods.ftbquests.quest.ServerQuestFile"));
        assertEquals("ServerQuestFile", DiskSaveProbe.shortName("dev.ftb.mods.ftbquests.quest.ServerQuestFile"));
        DiskSaveProbe probe = new DiskSaveProbe();
        probe.onSavedDataStart();
        probe.onSavedDataEnd("dev.ftb.mods.ftbquests.quest.ServerQuestFile");
        assertTrue(probe.formatLines(3).stream().anyMatch(line -> line.contains("ftbquests") || line.contains("ServerQuestFile")));
    }
}

class GroundEntityProbeTest {
    @Test
    void ranksChunksByTotal() {
        GroundEntityProbe probe = new GroundEntityProbe();
        Map<String, int[]> scan = new HashMap<>();
        scan.put("overworld chunk=0,0 pos=8,~,8", new int[] {2, 1, 0});
        scan.put("overworld chunk=1,0 pos=24,~,8", new int[] {40, 2, 5});
        probe.replaceScan(scan);
        assertEquals("overworld chunk=1,0 pos=24,~,8", probe.top(1).getFirst().key());
        assertEquals(47, probe.top(1).getFirst().total());
    }
}

class ModNetworkProbeTest {
    @Test
    void usesStackSamplesWhenApiMissing() {
        ModNetworkProbe probe = new ModNetworkProbe();
        probe.markSoftDependency(false, false);
        probe.consumeStackSamples(List.of(new StackSampleAggregator.MethodStat(
                "appeng.me.Grid",
                "tick",
                "ae2",
                8,
                8,
                80.0D,
                80.0D,
                "",
                "",
                0,
                0,
                0
        )));
        List<String> lines = probe.formatLines(3);
        assertTrue(lines.stream().anyMatch(line -> line.contains("ae2")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("absent")));
    }
}

class ProbeReportFormatterTest {
    @Test
    void concatenatesSections() {
        String text = ProbeReportFormatter.format(List.of(
                new ProbeReportFormatter.Section("GC и память", List.of("heap=10%")),
                new ProbeReportFormatter.Section("Чанки", List.of("generated=2"))
        ));
        assertTrue(text.contains("Расширенные пробы"));
        assertTrue(text.contains("GC и память"));
        assertTrue(text.contains("generated=2"));
    }
}

class WatchdogMixinRequireTest {
    @Test
    void watchdogMixinsAreOptional() throws Exception {
        String json = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/cointcore.watchdog.mixins.json"));
        assertTrue(json.contains("\"required\": false") || json.contains("\"required\":false"));
        assertTrue(json.contains("\"defaultRequire\": 0") || json.contains("\"defaultRequire\":0"));
        assertTrue(json.contains("PathNavigationWatchdogMixin"));
        assertTrue(json.contains("SavedDataWatchdogMixin"));
        assertTrue(json.contains("ServerLevelTickChunkWatchdogMixin"));
        assertTrue(json.contains("MinecraftServerSaveWatchdogMixin"));
        for (String name : List.of(
                "PathNavigationWatchdogMixin",
                "SavedDataWatchdogMixin",
                "ServerLevelTickChunkWatchdogMixin",
                "MinecraftServerSaveWatchdogMixin"
        )) {
            String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                    "src/main/java/com/mawlee/cointcore/mixin/watchdog/" + name + ".java"
            ));
            assertTrue(source.contains("require = 0") || source.contains("require=0"), name);
            assertTrue(source.contains("WatchdogHotFlags"), name);
        }
    }

    @Test
    void tickMixinsEarlyReturnOnHotFlags() throws Exception {
        String be = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/mawlee/cointcore/mixin/minecraft/BoundTickingBlockEntityMixin.java"
        ));
        String entity = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/mawlee/cointcore/mixin/ServerLevelMixin.java"
        ));
        assertTrue(be.contains("WatchdogHotFlags.TIME_BLOCK_ENTITIES"));
        assertTrue(entity.contains("WatchdogHotFlags.TIME_ENTITIES"));
    }

    @Test
    void savedDataMixinTargetsConcreteFileSave() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/mawlee/cointcore/mixin/watchdog/SavedDataWatchdogMixin.java"
        ));
        assertTrue(source.contains("save(Ljava/io/File;Lnet/minecraft/core/HolderLookup$Provider;)V"));
        assertFalse(source.contains("save(Lnet/minecraft/nbt/CompoundTag;"));
        assertTrue(source.contains("CallbackInfo"));
        assertFalse(source.contains("CallbackInfoReturnable"));
    }
}
