package com.mawlee.cointcore.ae;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class MultiCraftingCompatibilityTest {
    @Test void nativeTrackerStillClearsRejectedJobsAndExposesTheExpectedHooks() throws Exception {
        try (var stream = getClass().getResourceAsStream("/appeng/helpers/MultiCraftingTracker.class")) {
            assertNotNull(stream);
            var type = new ClassNode();
            new ClassReader(stream).accept(type, 0);
            assertTrue(type.fields.stream().anyMatch(field -> field.name.equals("jobs") && field.desc.equals("[Ljava/util/concurrent/Future;")));
            assertTrue(type.methods.stream().anyMatch(method -> method.name.equals("cancel") && method.desc.equals("()V")));
            var handle = type.methods.stream().filter(method -> method.name.equals("handleCrafting")).findFirst().orElseThrow();
            int successChecks = 0, calculations = 0;
            boolean clearsJob = false;
            for (var instruction : handle.instructions) {
                if (!(instruction instanceof MethodInsnNode call)) continue;
                if (call.owner.equals("appeng/api/networking/crafting/ICraftingSubmitResult") && call.name.equals("successful")) successChecks++;
                if (call.name.equals("beginCraftingCalculation")) calculations++;
                if (call.name.equals("setJob") && call.getPrevious().getOpcode() == org.objectweb.asm.Opcodes.ACONST_NULL) clearsJob = true;
            }
            assertEquals(1, successChecks);
            assertEquals(1, calculations);
            assertTrue(clearsJob);
        }
    }
}
