package com.mawlee.cointcore.adastra;

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
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.service.MixinService;

import java.util.List;

/**
 * Ad Astra's gravity handler is an {@code @Inject} method on minecarts, dropped
 * items, and primed TNT. Mixin renames it before any other mixin can target
 * {@code adastra$tick}, so the hook is inserted after that method has already
 * been merged onto those classes.
 */
public final class AdAstraGravityHandlerPatch {
    private static final String MERGED = "Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;";
    private static final String HANDLER_DESC = "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V";
    private static final String FAST_PATH_OWNER = "com/mawlee/cointcore/adastra/AdAstraGravityFastPath";
    private static final String FAST_PATH_NAME = "shouldSkipNeutralGravity";
    private static final String FAST_PATH_DESC = "(Lnet/minecraft/world/entity/Entity;)Z";

    private AdAstraGravityHandlerPatch() {
    }

    public static void install(ClassNode targetClass) {
        int patched = 0;
        for (MethodNode method : targetClass.methods) {
            if (isGravityHandler(method) && !alreadyPatched(method)) {
                insertSkip(method);
                patched++;
            }
        }
        ILogger logger = MixinService.getService().getLogger("cointcore");
        if (patched == 0) {
            logger.warn("Ad Astra gravity handler was not merged; neutral-gravity skip was not installed");
        } else {
            logger.info("Installed neutral-gravity skip on {} Ad Astra handler(s)", patched);
        }
    }

    private static boolean isGravityHandler(MethodNode method) {
        if ((method.access & Opcodes.ACC_STATIC) != 0 || !HANDLER_DESC.equals(method.desc)) {
            return false;
        }
        return mergedFromGravityMixin(method.visibleAnnotations) || mergedFromGravityMixin(method.invisibleAnnotations);
    }

    private static boolean mergedFromGravityMixin(List<AnnotationNode> annotations) {
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
                        && mixin.contains("GravityEntityMixin")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean alreadyPatched(MethodNode method) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call
                    && call.getOpcode() == Opcodes.INVOKESTATIC
                    && FAST_PATH_OWNER.equals(call.owner)
                    && FAST_PATH_NAME.equals(call.name)) {
                return true;
            }
            if (insn.getOpcode() >= 0) {
                return false;
            }
        }
        return false;
    }

    private static void insertSkip(MethodNode method) {
        LabelNode keepOriginal = new LabelNode();
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                FAST_PATH_OWNER,
                FAST_PATH_NAME,
                FAST_PATH_DESC,
                false
        ));
        hook.add(new JumpInsnNode(Opcodes.IFEQ, keepOriginal));
        hook.add(new InsnNode(Opcodes.RETURN));
        hook.add(keepOriginal);
        method.instructions.insert(hook);
    }
}
