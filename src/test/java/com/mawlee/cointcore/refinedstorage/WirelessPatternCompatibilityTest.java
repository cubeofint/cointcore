package com.mawlee.cointcore.refinedstorage;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class WirelessPatternCompatibilityTest {
    @Test
    void nativePatternMenuConstructorAndWirelessGridFactoryRemainAvailable() throws IOException {
        var menu = read("com/refinedmods/refinedstorage/common/autocrafting/patterngrid/PatternGridContainerMenu");
        assertTrue(menu.methods.stream().anyMatch(method -> method.name.equals("<init>") && method.desc.equals(
                "(ILnet/minecraft/world/entity/player/Inventory;"
                        + "Lcom/refinedmods/refinedstorage/common/autocrafting/patterngrid/PatternGridBlockEntity;)V")));
        var wireless = read("com/refinedmods/refinedstorage/common/grid/WirelessGrid");
        assertTrue(wireless.interfaces.contains("com/refinedmods/refinedstorage/common/api/grid/Grid"));
        assertTrue(wireless.methods.stream().anyMatch(method -> method.name.equals("<init>") && method.desc.equals(
                "(Lcom/refinedmods/refinedstorage/common/api/support/network/item/NetworkItemContext;)V")));
        // The mixin overrides inherited lifecycle methods; fail if RS starts declaring its own implementations.
        for (String inherited : new String[]{"getType", "stillValid", "removed", "broadcastChanges"}) {
            assertTrue(menu.methods.stream().noneMatch(method -> method.name.equals(inherited)), inherited);
        }
    }

    @Test
    void universalGridTabHookAndDisabledSlotContractRemainCompatible() throws IOException {
        var client = read("com/ultramega/universalgrid/common/ClientUtils");
        assertTrue(client.methods.stream().anyMatch(method -> method.name.equals("isUniversalGrid")
                && (method.access & Opcodes.ACC_STATIC) != 0 && method.desc.equals(
                "(Lcom/refinedmods/refinedstorage/common/support/AbstractBaseContainerMenu;"
                        + "Lnet/minecraft/world/entity/player/Player;)Z")));
        var slot = read("com/ultramega/universalgrid/common/interfaces/MixinDisabledSlot");
        assertTrue(slot.methods.stream().anyMatch(method -> method.name.equals("universalgrid$getDisabledSlot")
                && method.desc.equals("()Lcom/refinedmods/refinedstorage/common/api/support/slotreference/SlotReference;")));
        var item = read("com/ultramega/universalgrid/common/wirelessuniversalgrid/WirelessUniversalGridItem");
        assertTrue(item.methods.stream().anyMatch(method -> method.name.equals("openGridType") && method.desc.equals(
                "(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;"
                        + "Lcom/refinedmods/refinedstorage/common/api/support/slotreference/SlotReference;"
                        + "Lcom/ultramega/universalgrid/common/gui/view/GridTypes;)V")));
    }

    private static ClassNode read(String path) throws IOException {
        try (var stream = WirelessPatternCompatibilityTest.class.getResourceAsStream("/" + path + ".class")) {
            assertNotNull(stream, path);
            var node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }
}
