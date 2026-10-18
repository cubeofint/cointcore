package com.mawlee.cointcore.relics;

import com.mawlee.cointcore.config.RelicsServerTooltipConfig;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.service.MixinService;

import java.util.List;

/**
 * Relics injects {@code appendHoverText} from a common mixin and then calls
 * {@code relics$processTooltip}, which is {@code @OnlyIn(Dist.CLIENT)} and absent
 * on a dedicated server. Targeting Relics' {@code ItemMixin} class is impossible
 * (mixin-on-mixin). Cancelling vanilla {@code Item#appendHoverText} would drop
 * other mods' tooltip injects used by server-side search.
 *
 * <p>After our higher-priority {@code Item} mixin applies, this finds the merged
 * Relics handler (by {@code MixinMerged} and/or the {@code relics$processTooltip}
 * invoke) and returns before that invoke.
 */
public final class RelicsDedicatedTooltipPatch {
    private static final String MERGED = "Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;";
    private static final String RELICS_ITEM_MIXIN = "it.hurts.sskirillss.relics.mixin.ItemMixin";
    private static final String PROCESS_TOOLTIP = "relics$processTooltip";
    static final String SKIP_OWNER = "com/mawlee/cointcore/relics/RelicsDedicatedTooltipPatch";
    static final String SKIP_NAME = "shouldSkip";
    static final String SKIP_DESC = "()Z";

    private RelicsDedicatedTooltipPatch() {
    }

    public static boolean shouldSkip() {
        return RelicsServerTooltipConfig.isEnabled();
    }

    public static void install(ClassNode targetClass) {
        int patched = 0;
        for (MethodNode method : targetClass.methods) {
            if (isRelicsAppendHoverTextHandler(method) && !alreadyPatched(method)) {
                insertSkip(method);
                patched++;
            }
        }
        ILogger logger = MixinService.getService().getLogger("cointcore");
        if (patched == 0) {
            logger.warn("Relics appendHoverText handler was not merged; dedicated-server tooltip skip was not installed");
        } else {
            logger.info("Installed dedicated-server Relics tooltip skip on {} handler(s)", patched);
        }
    }

    static boolean isRelicsAppendHoverTextHandler(MethodNode method) {
        if (callsProcessTooltip(method)) {
            return true;
        }
        String name = method.name;
        if (name == null || !name.contains("appendHoverText")) {
            return false;
        }
        return mergedFromRelicsItemMixin(method.visibleAnnotations)
                || mergedFromRelicsItemMixin(method.invisibleAnnotations);
    }

    static boolean callsProcessTooltip(MethodNode method) {
        if (method.instructions == null) {
            return false;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && PROCESS_TOOLTIP.equals(call.name)) {
                return true;
            }
        }
        return false;
    }

    static boolean alreadyPatched(MethodNode method) {
        if (method.instructions == null) {
            return false;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call
                    && call.getOpcode() == Opcodes.INVOKESTATIC
                    && SKIP_OWNER.equals(call.owner)
                    && SKIP_NAME.equals(call.name)) {
                return true;
            }
            if (insn.getOpcode() >= 0) {
                return false;
            }
        }
        return false;
    }

    static void insertSkip(MethodNode method) {
        LabelNode keepOriginal = new LabelNode();
        InsnList hook = new InsnList();
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SKIP_OWNER, SKIP_NAME, SKIP_DESC, false));
        hook.add(new JumpInsnNode(Opcodes.IFEQ, keepOriginal));
        hook.add(new InsnNode(Opcodes.RETURN));
        hook.add(keepOriginal);
        method.instructions.insert(hook);
    }

    private static boolean mergedFromRelicsItemMixin(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return false;
        }
        for (AnnotationNode annotation : annotations) {
            if (!MERGED.equals(annotation.desc) || annotation.values == null) {
                continue;
            }
            List<Object> values = annotation.values;
            for (int index = 0; index < values.size() - 1; index += 2) {
                if ("mixin".equals(values.get(index)) && values.get(index + 1) instanceof String mixin
                        && RELICS_ITEM_MIXIN.equals(mixin)) {
                    return true;
                }
            }
        }
        return false;
    }
}
