package com.mawlee.cointcore.privilege;

import com.mawlee.cointcore.config.PrivilegeCommandsParse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PrivilegeCommandRulesTest {
    @Test
    void remainingSecondsRoundsUp() {
        assertEquals(0, PrivilegeCommandRules.remainingSeconds(1_000L, 1_000L));
        assertEquals(1, PrivilegeCommandRules.remainingSeconds(1_001L, 1_000L));
        assertEquals(2, PrivilegeCommandRules.remainingSeconds(3_000L, 1_001L));
    }

    @Test
    void readyAtUsesWholeSeconds() {
        assertEquals(5_000L, PrivilegeCommandRules.readyAtEpochMs(5_000L, 0));
        assertEquals(8_000L, PrivilegeCommandRules.readyAtEpochMs(5_000L, 3));
    }

    @Test
    void expAddAmountMustBePositiveAndAtMostMax() {
        assertSame(PrivilegeCommandRules.ExpAddAmountVerdict.NON_POSITIVE,
                PrivilegeCommandRules.validateExpAddAmount(0, 1000));
        assertSame(PrivilegeCommandRules.ExpAddAmountVerdict.NON_POSITIVE,
                PrivilegeCommandRules.validateExpAddAmount(-3, 1000));
        assertSame(PrivilegeCommandRules.ExpAddAmountVerdict.OVER_MAX,
                PrivilegeCommandRules.validateExpAddAmount(1001, 1000));
        assertSame(PrivilegeCommandRules.ExpAddAmountVerdict.OK,
                PrivilegeCommandRules.validateExpAddAmount(1000, 1000));
    }

    @Test
    void parseConfigDefaultsAndUnitAliases() {
        PrivilegeCommandsParse.Parsed defaults = PrivilegeCommandsParse.parseJson("{}");
        assertEquals(300, defaults.repairCooldownSeconds());
        assertEquals(300, defaults.chargeCooldownSeconds());
        assertEquals(1800, defaults.repairAllCooldownSeconds());
        assertEquals(1800, defaults.chargeAllCooldownSeconds());
        assertEquals(3600, defaults.expAddCooldownSeconds());
        assertEquals(1000, defaults.expAddMaxAmount());
        assertSame(ExpAddUnit.POINTS, defaults.expAddUnit());
        assertEquals(300, defaults.cooldownSeconds(PrivilegeCommandId.REPAIR));

        PrivilegeCommandsParse.Parsed custom = PrivilegeCommandsParse.parseJson("""
                {
                  "repairCooldownSeconds": 10,
                  "expAddMaxAmount": 250,
                  "expAddUnit": "levels"
                }
                """);
        assertEquals(10, custom.repairCooldownSeconds());
        assertEquals(250, custom.expAddMaxAmount());
        assertSame(ExpAddUnit.LEVELS, custom.expAddUnit());
        assertSame(ExpAddUnit.POINTS, ExpAddUnit.parse("xp", ExpAddUnit.LEVELS));
        assertSame(ExpAddUnit.LEVELS, ExpAddUnit.parse("lvl", ExpAddUnit.POINTS));
    }

    @Test
    void parseClampsInvalidNumbers() {
        PrivilegeCommandsParse.Parsed parsed = PrivilegeCommandsParse.parseJson("""
                {
                  "repairCooldownSeconds": -5,
                  "expAddMaxAmount": 0
                }
                """);
        assertEquals(0, parsed.repairCooldownSeconds());
        assertEquals(1, parsed.expAddMaxAmount());
    }
}
