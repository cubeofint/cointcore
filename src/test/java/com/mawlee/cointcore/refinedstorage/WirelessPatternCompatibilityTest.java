package com.mawlee.cointcore.refinedstorage;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

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
    void pickUsesNativeSecuredWirelessOperationsAndRequestConfirmationHooksExist() throws IOException {
        var wireless = read("com/refinedmods/refinedstorage/common/grid/WirelessGrid");
        var constructed = wireless.methods.stream().flatMap(method -> java.util.Arrays.stream(method.instructions.toArray()))
                .filter(instruction -> instruction instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW)
                .map(instruction -> ((TypeInsnNode) instruction).desc).toList();
        assertTrue(constructed.contains("com/refinedmods/refinedstorage/common/grid/SecuredGridOperations"));
        assertTrue(constructed.contains("com/refinedmods/refinedstorage/common/grid/WirelessGridOperations"));
        var api = read("com/refinedmods/refinedstorage/common/RefinedStorageApiImpl");
        assertTrue(api.fields.stream().anyMatch(field -> field.name.equals("slotReferenceProvider")
                && field.desc.equals("Lcom/refinedmods/refinedstorage/common/support/slotreference/CompositeSlotReferenceProvider;")));
        var request = read("com/refinedmods/refinedstorage/common/autocrafting/preview/AutocraftingRequest");
        assertTrue(request.fields.stream().anyMatch(field -> field.name.equals("id") && field.desc.equals("Ljava/util/UUID;")));
        assertTrue(request.fields.stream().anyMatch(field -> field.name.equals("resource")
                && field.desc.equals("Lcom/refinedmods/refinedstorage/api/resource/ResourceKey;")));
        var preview = read("com/refinedmods/refinedstorage/common/autocrafting/preview/AutocraftingPreviewContainerMenu");
        assertTrue(preview.methods.stream().anyMatch(method -> method.name.equals("responseReceived")
                && method.desc.equals("(Ljava/util/UUID;Z)V")));
    }

    @Test
    void networkPatternPreviewHooksCoverNativeInputAndSuccessfulOutputCommit() throws IOException {
        var backend = read("com/refinedmods/refinedstorage/common/autocrafting/patterngrid/PatternGridBlockEntity");
        var encoding = backend.methods.stream().filter(method -> method.name.equals("createPattern")).findFirst().orElseThrow();
        int commits = 0;
        boolean removesBlank = false;
        for (var instruction : encoding.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals("com/refinedmods/refinedstorage/common/support/FilteredContainer")) {
                if (call.name.equals("removeItem")) removesBlank = true;
                if (call.name.equals("setItem")) {
                    assertTrue(removesBlank, "Native blank removal must precede output publication");
                    assertEquals("(ILnet/minecraft/world/item/ItemStack;)V", call.desc);
                    commits++;
                }
            }
        }
        assertEquals(1, commits, "Every new output must pass the network consumption hook");

        var menu = read("com/refinedmods/refinedstorage/common/autocrafting/patterngrid/PatternGridContainerMenu");
        var slots = menu.methods.stream().filter(method -> method.name.equals("addPatternSlots")).findFirst().orElseThrow();
        int inputSlots = 0;
        int inputTransfers = 0;
        for (var instruction : slots.instructions) {
            if (instruction instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW
                    && type.desc.equals("com/refinedmods/refinedstorage/common/support/containermenu/ValidatedSlot")) inputSlots++;
            if (instruction instanceof MethodInsnNode call && call.name.equals("addBiTransfer")
                    && call.owner.equals("com/refinedmods/refinedstorage/common/support/containermenu/TransferManager")) inputTransfers++;
        }
        assertEquals(1, inputSlots, "Only the blank input slot should become read-only");
        assertEquals(1, inputTransfers, "Disable all native shift-click paths into the preview");
    }

    @Test
    void wiredPreviewScopeCoversBothConstructorsAndTheOnlyNativeEncodingEntry() throws IOException {
        var menu = read("com/refinedmods/refinedstorage/common/autocrafting/patterngrid/PatternGridContainerMenu");
        assertEquals(2, menu.methods.stream().filter(method -> method.name.equals("<init>")).count(),
                "Client and server must both install the same four count properties");
        var mixin = read("com/mawlee/cointcore/mixin/refinedstorage/WirelessPatternMenuMixin");
        var initializer = mixin.methods.stream().filter(method -> method.name.equals("cointcore$initializeNetworkPatterns"))
                .findFirst().orElseThrow();
        var inject = initializer.visibleAnnotations.stream()
                .filter(annotation -> annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;"))
                .findFirst().orElseThrow();
        @SuppressWarnings("unchecked")
        var targets = (java.util.List<String>) inject.values.get(inject.values.indexOf("method") + 1);
        assertEquals(menu.methods.stream().filter(method -> method.name.equals("<init>"))
                .map(method -> "<init>" + method.desc).collect(java.util.stream.Collectors.toSet()),
                new java.util.HashSet<>(targets), "Explicitly target both constructors; a bare <init> only selects one");
        var create = menu.methods.stream().filter(method -> method.name.equals("createPattern")).findFirst().orElseThrow();
        assertEquals(1, java.util.Arrays.stream(create.instructions.toArray())
                .filter(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.equals("com/refinedmods/refinedstorage/common/autocrafting/patterngrid/PatternGridBlockEntity")
                        && call.name.equals("createPattern") && call.desc.equals("()V")).count(),
                "All menu encoding requests must enter the per-player network scope");

        var block = read("com/refinedmods/refinedstorage/common/autocrafting/patterngrid/PatternGridBlockEntity");
        var encode = block.methods.stream().filter(method -> method.name.equals("createPattern")).findFirst().orElseThrow();
        assertEquals(1, java.util.Arrays.stream(encode.instructions.toArray())
                .filter(instruction -> instruction instanceof MethodInsnNode call && call.name.equals("isPatternAvailable")
                        && call.desc.equals("()Z")).count(),
                "The wired preview must replace native blank availability without modifying the block inventory");
        assertTrue(block.fields.stream().anyMatch(field -> field.name.equals("patternOutput")
                && field.desc.equals("Lcom/refinedmods/refinedstorage/common/support/FilteredContainer;")
                && (field.access & Opcodes.ACC_FINAL) != 0));
    }

    @Test
    void wiredConsumptionUsesSecuredNativeGridOperations() throws IOException {
        var grid = read("com/refinedmods/refinedstorage/common/grid/AbstractGridBlockEntity");
        var operations = grid.methods.stream().filter(method -> method.name.equals("createOperations")).findFirst().orElseThrow();
        assertTrue(java.util.Arrays.stream(operations.instructions.toArray())
                .anyMatch(instruction -> instruction instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW
                        && type.desc.equals("com/refinedmods/refinedstorage/common/grid/SecuredGridOperations")),
                "Network blank extraction must retain the wired grid's security wrapper");
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
