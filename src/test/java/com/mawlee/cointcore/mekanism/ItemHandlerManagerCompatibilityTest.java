package com.mawlee.cointcore.mekanism;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ItemHandlerManagerCompatibilityTest {
    @Test void nativeItemManagerStillUsesTheExactSlotHolderGetterWeSpecialize() throws IOException {
        var item = read("mekanism/common/capabilities/resolver/manager/ItemHandlerManager");
        assertEquals("mekanism/common/capabilities/resolver/manager/CapabilityHandlerManager", item.superName);
        assertFalse(item.methods.stream().anyMatch(m -> m.name.equals("getContainers")),
                "An upstream override must be reviewed before adding our inherited-method specialization");
        var constructor = item.methods.stream().filter(m -> m.name.equals("<init>")).findFirst().orElseThrow();
        assertTrue(Arrays.stream(constructor.instructions.toArray()).anyMatch(i -> i instanceof InvokeDynamicInsnNode call
                && Arrays.stream(call.bsmArgs).anyMatch(a -> a instanceof Handle h
                && h.getOwner().equals("mekanism/common/capabilities/holder/slot/IInventorySlotHolder")
                && h.getName().equals("getInventorySlots"))));
        var base = read("mekanism/common/capabilities/resolver/manager/CapabilityHandlerManager");
        var getter = base.methods.stream().filter(m -> m.name.equals("getContainers")).findFirst().orElseThrow();
        var calls = Arrays.stream(getter.instructions.toArray()).filter(i -> i instanceof MethodInsnNode)
                .map(i -> (MethodInsnNode)i).toList();
        assertTrue(calls.stream().anyMatch(c -> c.name.equals("canHandle")));
        assertTrue(calls.stream().anyMatch(c -> c.owner.equals("java/util/function/BiFunction") && c.name.equals("apply")));
    }

    private static ClassNode read(String path) throws IOException {
        try (var stream = ItemHandlerManagerCompatibilityTest.class.getResourceAsStream("/" + path + ".class")) {
            assertNotNull(stream, path);
            var node = new ClassNode(); new ClassReader(stream).accept(node, 0); return node;
        }
    }
}
