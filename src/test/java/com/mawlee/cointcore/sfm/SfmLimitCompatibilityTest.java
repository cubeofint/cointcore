package com.mawlee.cointcore.sfm;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/** Guards the native SFM entry points and snapshot replacement used by the limit cache. */
class SfmLimitCompatibilityTest {
    private static final String MANAGER = "ca/teamdman/sfm/common/blockentity/ManagerBlockEntity";
    private static final String LABELS = "ca/teamdman/sfm/common/label/LabelPositionHolder";

    @Test
    void managerHasOneProgramTickCallAndTheExpectedMenuHook() throws IOException {
        var manager = read(MANAGER);
        assertEquals(1, calls(method(manager, "serverTick"), "ca/teamdman/sfml/ast/Program", "tick"));
        assertEquals("(ILnet/minecraft/world/entity/player/Inventory;)Lnet/minecraft/world/inventory/AbstractContainerMenu;",
                method(manager, "createMenu").desc);
    }

    @Test
    void labelGunActionStillUsesTheExpectedFieldsAndPushPullBranch() throws IOException {
        var action = read("ca/teamdman/sfm/common/label/LabelGunManagerPushOrPullAction");
        for (String field : new String[] {"player", "manager", "gunLabels", "msg"}) {
            assertTrue(action.fields.stream().anyMatch(value -> value.name.equals(field)), field);
        }
        assertEquals(1, calls(method(action, "run"),
                "ca/teamdman/sfm/common/net/ServerboundLabelGunUsePacket", "isPullModifierActive"));
        assertEquals(2, calls(method(action, "run"), LABELS, "save"));
    }

    @Test
    void savingLabelsCopiesTheSnapshotBeforePublishingItToTheDisk() throws IOException {
        var save = method(read(LABELS), "save");
        boolean copied = false;
        boolean published = false;
        for (var instruction : save.instructions) {
            if (!(instruction instanceof MethodInsnNode call)) continue;
            if (call.owner.equals(LABELS) && call.name.equals("<init>")
                    && call.desc.equals("(L" + LABELS + ";)V")) copied = true;
            if (call.owner.equals("net/minecraft/world/item/ItemStack") && call.name.equals("set")) {
                assertTrue(copied, "The cached component must be replaced, not mutated in place");
                published = true;
            }
        }
        assertTrue(published);
    }

    private static int calls(MethodNode method, String owner, String name) {
        int count = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)) count++;
        }
        return count;
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(value -> value.name.equals(name)).findFirst().orElseThrow();
    }

    private static ClassNode read(String name) throws IOException {
        try (var stream = SfmLimitCompatibilityTest.class.getResourceAsStream("/" + name + ".class")) {
            assertNotNull(stream);
            var node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }
}
