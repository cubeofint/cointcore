package com.mawlee.cointcore.refinedstorage;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WirelessPatternJeiRegistrationTest {
    private static final String NATIVE_MENU = "com/refinedmods/refinedstorage/common/autocrafting/patterngrid/PatternGridContainerMenu";
    private static final String WIRELESS = "com/mawlee/cointcore/refinedstorage/wireless/";

    @Test
    void clientFactoryAndJeiHandlersUseAnIndependentClassSoNativeRegistrationsSurvive() throws IOException {
        var specific = handlerClass(WIRELESS + "WirelessPatternJei$Handler");
        var processing = handlerClass(WIRELESS + "WirelessPatternJei$ProcessingHandler");
        assertNotEquals(NATIVE_MENU, specific, "JEI registration would overwrite native crafting handlers");
        assertNotEquals(NATIVE_MENU, processing, "JEI registration would overwrite native processing handler");
        assertEquals(specific, processing);
        assertEquals(NATIVE_MENU, read(specific).superName, "Keep native RS packets and menu behavior");

        var factory = read(WIRELESS + "WirelessPatternIntegration");
        assertTrue(factory.methods.stream().flatMap(method -> java.util.Arrays.stream(method.instructions.toArray()))
                .anyMatch(instruction -> instruction instanceof TypeInsnNode type
                        && type.getOpcode() == Opcodes.NEW && type.desc.equals(specific)),
                "JEI does exact class lookup: factory must construct the same class registered by handlers");

        // JEI 19.x's registry is a table keyed by (containerClass, recipeType). MenuType is only
        // checked after lookup, so different MenuTypes alone cannot keep registrations separate.
        record Key(String container, String recipe) { }
        var registrations = new HashMap<Key, String>();
        for (String recipe : List.of("crafting", "stonecutting", "smithing", "universal")) {
            registrations.put(new Key(NATIVE_MENU, recipe), "native");
            registrations.put(new Key(recipe.equals("universal") ? processing : specific, recipe), "wireless");
            assertEquals("native", registrations.get(new Key(NATIVE_MENU, recipe)));
            assertEquals("wireless", registrations.get(new Key(specific, recipe)));
        }
        assertEquals(8, registrations.size());
    }

    private static String handlerClass(String name) throws IOException {
        var method = read(name).methods.stream().filter(candidate -> candidate.name.equals("getContainerClass"))
                .findFirst().orElseThrow();
        for (var instruction : method.instructions) {
            if (instruction instanceof LdcInsnNode constant && constant.cst instanceof Type type) {
                return type.getInternalName();
            }
        }
        throw new AssertionError("Handler does not declare its container class");
    }

    private static ClassNode read(String name) throws IOException {
        try (var stream = WirelessPatternJeiRegistrationTest.class.getResourceAsStream("/" + name + ".class")) {
            assertNotNull(stream, name);
            var node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }
}
