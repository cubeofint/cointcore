package com.mawlee.cointcore.relics;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelicsDedicatedTooltipPatchTest {
    @Test
    void detectsProcessTooltipInvoke() {
        MethodNode method = tooltipHandler("handler$dck000$relics$appendHoverText", true, false);
        assertTrue(RelicsDedicatedTooltipPatch.callsProcessTooltip(method));
        assertTrue(RelicsDedicatedTooltipPatch.isRelicsAppendHoverTextHandler(method));
    }

    @Test
    void detectsMergedAppendHoverTextWithoutInvoke() {
        MethodNode method = tooltipHandler("handler$aaa$relics$appendHoverText", false, true);
        assertFalse(RelicsDedicatedTooltipPatch.callsProcessTooltip(method));
        assertTrue(RelicsDedicatedTooltipPatch.isRelicsAppendHoverTextHandler(method));
    }

    @Test
    void ignoresInventoryTickHandler() {
        MethodNode method = tooltipHandler("handler$bbb$relics$inventoryTick", false, true);
        assertFalse(RelicsDedicatedTooltipPatch.isRelicsAppendHoverTextHandler(method));
    }

    @Test
    void insertSkipPrefixesShouldSkip() {
        MethodNode method = tooltipHandler("handler$dck000$relics$appendHoverText", true, false);
        RelicsDedicatedTooltipPatch.insertSkip(method);
        assertTrue(RelicsDedicatedTooltipPatch.alreadyPatched(method));

        AbstractInsnNode first = firstOpcode(method);
        assertTrue(first instanceof MethodInsnNode call
                && RelicsDedicatedTooltipPatch.SKIP_NAME.equals(call.name)
                && RelicsDedicatedTooltipPatch.SKIP_OWNER.equals(call.owner));
    }

    private static AbstractInsnNode firstOpcode(MethodNode method) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() >= 0) {
                return insn;
            }
        }
        return null;
    }

    private static MethodNode tooltipHandler(String name, boolean invokeProcess, boolean merged) {
        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC,
                name,
                "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V",
                null,
                null
        );
        InsnList insns = new InsnList();
        if (invokeProcess) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insns.add(new VarInsnNode(Opcodes.ALOAD, 1));
            insns.add(new VarInsnNode(Opcodes.ALOAD, 2));
            insns.add(new VarInsnNode(Opcodes.ALOAD, 3));
            insns.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/world/item/Item",
                    "relics$processTooltip",
                    "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/List;)V",
                    false
            ));
        }
        insns.add(new InsnNode(Opcodes.RETURN));
        method.instructions = insns;
        if (merged) {
            AnnotationNode annotation = new AnnotationNode("Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;");
            List<Object> values = new ArrayList<>();
            values.add("mixin");
            values.add("it.hurts.sskirillss.relics.mixin.ItemMixin");
            annotation.values = values;
            method.visibleAnnotations = List.of(annotation);
        }
        return method;
    }
}
