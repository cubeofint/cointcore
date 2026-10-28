package com.mawlee.cointcore.logisticsnetworks;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

/** Pin the contracts used by the signal pass, including the native no-exporter result. */
class TransferSignalsCompatibilityTest {
    @Test void signalPassHasOneWorldQueryAndReturnsAnEmptyMapWithoutExporters() throws IOException {
        var type = read("me/almana/logisticsnetworks/logic/TransferEngine");
        var method = type.methods.stream().filter(value -> value.name.equals("buildSignalCache")).findFirst().orElseThrow();
        assertEquals("(Ljava/util/List;)Ljava/util/Map;", method.desc);
        int queries = 0;
        boolean exportCheck = false, emptyMap = false;
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.owner.equals("net/minecraft/server/level/ServerLevel") && call.name.equals("getBestNeighborSignal")) queries++;
                if (call.owner.equals("java/util/Collections") && call.name.equals("emptyMap")) emptyMap = true;
            }
            if (instruction instanceof FieldInsnNode field && field.owner.equals("me/almana/logisticsnetworks/data/ChannelMode")
                    && field.name.equals("EXPORT")) exportCheck = true;
        }
        assertEquals(1, queries);
        assertTrue(exportCheck);
        assertTrue(emptyMap);
        assertTrue(read("me/almana/logisticsnetworks/data/RedstoneMode").fields.stream()
                .anyMatch(field -> field.name.equals("IGNORED")), "Tests must use the profiled Logistics 1.17.2 build");
        var node = read("me/almana/logisticsnetworks/entity/LogisticsNodeEntity");
        assertEquals(9, node.fields.stream().filter(field -> field.name.equals("CHANNEL_COUNT")).findFirst().orElseThrow().value);
    }

    static ClassNode read(String name) throws IOException {
        try (var stream = TransferSignalsCompatibilityTest.class.getResourceAsStream("/" + name + ".class")) {
            assertNotNull(stream);
            var node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }
}
