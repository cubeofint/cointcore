package com.mawlee.cointcore.mixin.apotheosis;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InvaderMissingSpawnRulesTest {
    @Test
    void missingDimensionRulesUseTheExistingConfigFallbackWithoutDereferencingNull() throws Exception {
        assertEquals(Optional.empty(), invoke(null, args -> {
            fail("The missing rules must not be dereferenced");
            return Optional.of(true);
        }));
    }

    @Test
    void configuredTrueFalseAndAbsentOverridesRemainUnchanged() throws Exception {
        Object rules = new Object();
        for (Optional<Boolean> configured : java.util.List.of(Optional.of(true), Optional.of(false), Optional.<Boolean>empty())) {
            assertSame(configured, invoke(rules, args -> {
                assertEquals(1, args.length);
                assertSame(rules, args[0]);
                return configured;
            }));
        }
    }

    private static Object invoke(Object rules, Operation<Optional<Boolean>> original) throws Exception {
        Method method = InvaderMissingSpawnRulesMixin.class.getDeclaredMethod(
                "cointcore$defaultCurseWithoutRules", Object.class, Operation.class);
        method.setAccessible(true);
        return method.invoke(new InvaderMissingSpawnRulesMixin() { }, rules, original);
    }
}
