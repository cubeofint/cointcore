package com.mawlee.cointcore.refinedstorage;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/** Fails when the pinned RS bytecode no longer matches the hooks required by infinity disks. */
class InfinityDiskMixinTargetTest {
    @Test
    void stateTrackingConstructorHookExistsExactlyOnce() throws IOException {
        var node = read("com/refinedmods/refinedstorage/api/network/impl/node/AbstractStorageContainerNetworkNode");
        var target = node.methods.stream().filter(method -> method.name.equals("lambda$initializeStorage$0"))
                .findFirst().orElseThrow();
        int matches = 0;
        for (var instruction : target.instructions) {
            if (instruction instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW
                    && type.desc.equals("com/refinedmods/refinedstorage/api/storage/StateTrackedStorage")) {
                matches++;
            }
        }
        assertEquals(1, matches);
    }

    @Test
    void configuredProxyHookAndDelegateAccessorExist() throws IOException {
        var node = read("com/refinedmods/refinedstorage/api/network/impl/storage/AbstractConfiguredProxyStorage");
        assertTrue(node.methods.stream().anyMatch(method -> method.name.equals("compositeExtract")
                && method.desc.equals("(Lcom/refinedmods/refinedstorage/api/resource/ResourceKey;J"
                + "Lcom/refinedmods/refinedstorage/api/core/Action;Lcom/refinedmods/refinedstorage/api/storage/Actor;)"
                + "Lcom/refinedmods/refinedstorage/api/storage/composite/CompositeAwareChild$Amount;")));
        assertTrue(node.methods.stream().anyMatch(method -> method.name.equals("getUnsafeDelegate")
                && method.desc.equals("()Lcom/refinedmods/refinedstorage/api/storage/Storage;")
                && (method.access & Opcodes.ACC_PROTECTED) != 0));
    }

    private static ClassNode read(String name) throws IOException {
        try (var stream = InfinityDiskMixinTargetTest.class.getResourceAsStream("/" + name + ".class")) {
            assertNotNull(stream);
            var node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }
}
