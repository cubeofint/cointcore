package com.mawlee.cointcore.modularbees;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

/** The adapter must keep the native filtered transfer paths of ModularBees 3.4. */
class ModularBeesHatchCompatibilityTest {
    private static final String BASE = "com/glodblock/github/modularbees/xmod/ae/tileentities/";

    @Test void hostExposesTheLifecycleAndStorageHooks() throws IOException {
        var type = read(BASE + "TileAENetworkHost");
        for (var name : new String[] {"onReady", "onChunkUnloaded", "setRemoved", "clearRemoved", "onGridConnectableSidesChanged"})
            assertTrue(type.methods.stream().anyMatch(method -> method.name.equals(name) && method.desc.equals("()V")), name);
        assertTrue(type.methods.stream().anyMatch(method -> method.name.equals("getMEStorage")
                && method.desc.equals("()Lappeng/api/storage/MEStorage;")));
        // isActive belongs to the component superclass, not to TileMEExport/Import.
        assertTrue(read("com/glodblock/github/modularbees/common/tileentities/base/TileMBModularComponent").methods.stream()
                .anyMatch(method -> method.name.equals("isActive") && method.desc.equals("()Z")));
    }

    @Test void itemAndFluidExportsStillUseStorageInsertionBeforeRemovingLocalResources() throws IOException {
        for (var name : new String[] {"TileMECentrifugeExport", "TileMEHiveExport"}) {
            var type = read(BASE + name);
            var method = type.methods.stream().filter(value -> value.name.equals("extractOutputs")).findFirst().orElseThrow();
            int inserts = 0;
            boolean usesFilter = false, extractsItems = false;
            for (var instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call)) continue;
                if (call.owner.equals("appeng/api/storage/MEStorage") && call.name.equals("insert")) inserts++;
                if (call.name.equals("checkFilter")) usesFilter = true;
                if (call.name.equals("forceExtractItem")) extractsItems = true;
            }
            assertEquals(2, inserts, name + " must export items and fluid via the same storage adapter");
            assertTrue(usesFilter);
            assertTrue(extractsItems);
        }
    }

    @Test void importCanBothPullConfiguredItemsAndReturnDisplacedItems() throws IOException {
        var type = read(BASE + "TileMECentrifugeImport");
        var method = type.methods.stream().filter(value -> value.name.equals("updateStorage")).findFirst().orElseThrow();
        int inserts = 0, extracts = 0;
        for (var instruction : method.instructions) {
            if (!(instruction instanceof MethodInsnNode call) || !call.owner.equals("appeng/api/storage/MEStorage")) continue;
            if (call.name.equals("insert")) inserts++;
            if (call.name.equals("extract")) extracts++;
        }
        assertEquals(1, inserts);
        assertEquals(1, extracts);
        var fluid = read("appeng/api/stacks/AEFluidKey");
        assertEquals(1000, fluid.fields.stream().filter(field -> field.name.equals("AMOUNT_BUCKET")).findFirst().orElseThrow().value);
    }

    private static ClassNode read(String name) throws IOException {
        try (var stream = ModularBeesHatchCompatibilityTest.class.getResourceAsStream("/" + name + ".class")) {
            assertNotNull(stream);
            var type = new ClassNode();
            new ClassReader(stream).accept(type, 0);
            return type;
        }
    }
}
