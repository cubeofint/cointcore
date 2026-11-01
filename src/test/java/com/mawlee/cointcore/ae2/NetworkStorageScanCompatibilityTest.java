package com.mawlee.cointcore.ae2;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class NetworkStorageScanCompatibilityTest {
    @Test void nativeStorageStillExposesTheGuardAndAllMutationHooks() throws IOException {
        var storage = read("appeng/me/storage/NetworkStorage");
        assertTrue(storage.fields.stream().anyMatch(f -> f.name.equals("mountsInUse") && f.desc.equals("Z")));
        for (var name : new String[]{"getAvailableStacks", "insert", "extract", "mount", "unmount"}) {
            assertEquals(1, storage.methods.stream().filter(m -> m.name.equals(name)).count(), name);
        }
        var service = read("appeng/me/service/StorageService");
        assertTrue(service.methods.stream().anyMatch(m -> m.name.equals("invalidateCache") && m.desc.equals("()V")));
    }

    private static ClassNode read(String path) throws IOException {
        try (var stream = NetworkStorageScanCompatibilityTest.class.getResourceAsStream("/" + path + ".class")) {
            assertNotNull(stream, path);
            var node = new ClassNode(); new ClassReader(stream).accept(node, 0); return node;
        }
    }
}
