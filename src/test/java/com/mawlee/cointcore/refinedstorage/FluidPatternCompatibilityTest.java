package com.mawlee.cointcore.refinedstorage;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.io.IOException;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class FluidPatternCompatibilityTest {
    @Test void addonStillChoosesTypeFromInputOrExistingOutputAndPreservesEncodedComponents() throws IOException {
        var addon = read("com/ultramega/refinedfluidsubstitution/common/mixin/PatternGridBlockEntityMixin");
        var choose = addon.methods.stream().filter(method -> method.name.equals("rfs$rememberPatternKind")).findFirst().orElseThrow();
        var fields = Arrays.stream(choose.instructions.toArray()).filter(instruction -> instruction instanceof FieldInsnNode)
                .map(instruction -> ((FieldInsnNode) instruction).name).toList();
        assertTrue(fields.contains("patternInput")); assertTrue(fields.contains("patternOutput"));
        var publish = addon.methods.stream().filter(method -> method.name.equals("rfs$restoreFluidSubstitutionPatternItem")).findFirst().orElseThrow();
        assertTrue(Arrays.stream(publish.instructions.toArray()).anyMatch(instruction -> instruction instanceof MethodInsnNode call
                && call.owner.equals("net/minecraft/world/item/ItemStack") && call.name.equals("transmuteCopy")),
                "The addon must preserve the recipe components when restoring its item type");
    }
    @Test void nativeMenuStillHasOutputAndAvailabilityHooksUsedByGhostSelection() throws IOException {
        var menu = read("com/refinedmods/refinedstorage/common/autocrafting/patterngrid/PatternGridContainerMenu");
        assertTrue(menu.fields.stream().anyMatch(field -> field.name.equals("patternOutput")
                && field.desc.equals("Lnet/minecraft/world/Container;") && (field.access & Opcodes.ACC_FINAL) != 0));
        assertTrue(menu.methods.stream().anyMatch(method -> method.name.equals("canCreatePattern") && method.desc.equals("()Z")));
        var block = read("com/refinedmods/refinedstorage/common/autocrafting/patterngrid/PatternGridBlockEntity");
        assertTrue(block.fields.stream().anyMatch(field -> field.name.equals("patternOutput")
                && field.desc.equals("Lcom/refinedmods/refinedstorage/common/support/FilteredContainer;")));
    }
    private static ClassNode read(String path) throws IOException {
        try (var stream = FluidPatternCompatibilityTest.class.getResourceAsStream("/" + path + ".class")) {
            assertNotNull(stream, path);
            var node = new ClassNode(); new ClassReader(stream).accept(node, 0); return node;
        }
    }
}
