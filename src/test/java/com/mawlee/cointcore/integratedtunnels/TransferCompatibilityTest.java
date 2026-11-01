package com.mawlee.cointcore.integratedtunnels;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.*;

class TransferCompatibilityTest {
    @Test void bothNativeSlottedTransferOverloadsRemainStaticWithTheExpectedArguments() throws Exception {
        try (var stream = getClass().getResourceAsStream("/org/cyclops/cyclopscore/ingredient/storage/IngredientStorageHelpers.class")) {
            assertNotNull(stream);
            var node = new ClassNode(); new ClassReader(stream).accept(node, 0);
            var methods = node.methods.stream().filter(m -> m.name.equals("moveIngredientsSlotted")).toList();
            assertEquals(2, methods.size());
            for (var method : methods) {
                assertTrue((method.access & Opcodes.ACC_STATIC) != 0);
                var args = Type.getArgumentTypes(method.desc);
                assertEquals("org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage", args[0].getClassName());
                assertEquals(Type.INT_TYPE, args[1]); assertEquals(Type.INT_TYPE, args[3]);
                assertEquals(Type.BOOLEAN_TYPE, args[args.length - 1]);
                assertEquals(Type.getType(Object.class), Type.getReturnType(method.desc));
            }
            assertTrue(methods.stream().anyMatch(m -> Type.getArgumentTypes(m.desc).length == 7));
            assertTrue(methods.stream().anyMatch(m -> Type.getArgumentTypes(m.desc).length == 8));
        }
    }
}
