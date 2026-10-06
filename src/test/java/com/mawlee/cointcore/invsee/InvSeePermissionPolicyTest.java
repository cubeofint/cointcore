package com.mawlee.cointcore.invsee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvSeePermissionPolicyTest {
    @Test
    void explicitDenyWinsOverLegacyGrant() {
        assertFalse(InvSeePermissionPolicy.allows(InvSeeTriState.FALSE, InvSeeTriState.TRUE));
    }

    @Test
    void explicitGrantWins() {
        assertTrue(InvSeePermissionPolicy.allows(InvSeeTriState.TRUE, InvSeeTriState.FALSE));
    }

    @Test
    void unsetFallsBackToLegacy() {
        assertTrue(InvSeePermissionPolicy.allows(InvSeeTriState.UNSET, InvSeeTriState.TRUE));
        assertFalse(InvSeePermissionPolicy.allows(InvSeeTriState.UNSET, InvSeeTriState.FALSE));
        assertFalse(InvSeePermissionPolicy.allows(InvSeeTriState.UNSET, InvSeeTriState.UNSET));
    }

    @Test
    void commandAllowedWithLegacyOrAnySection() {
        assertTrue(InvSeePermissionPolicy.canUseCommand(InvSeeTriState.TRUE, new InvSeeTriState[0]));
        assertTrue(InvSeePermissionPolicy.canUseCommand(
                InvSeeTriState.UNSET,
                new InvSeeTriState[] {InvSeeTriState.UNSET, InvSeeTriState.TRUE}
        ));
        assertFalse(InvSeePermissionPolicy.canUseCommand(
                InvSeeTriState.FALSE,
                new InvSeeTriState[] {InvSeeTriState.UNSET, InvSeeTriState.FALSE}
        ));
    }

    @Test
    void bypassAlwaysInspectsExemptTarget() {
        assertTrue(InvSeePermissionPolicy.canInspect(
                InvSeeTriState.TRUE,
                InvSeeTriState.TRUE,
                0,
                100
        ));
    }

    @Test
    void nonExemptTargetIsOpen() {
        assertTrue(InvSeePermissionPolicy.canInspect(
                InvSeeTriState.FALSE,
                InvSeeTriState.UNSET,
                0,
                50
        ));
    }

    @Test
    void exemptRequiresStrictlyHigherWeight() {
        assertTrue(InvSeePermissionPolicy.canInspect(
                InvSeeTriState.UNSET,
                InvSeeTriState.TRUE,
                50,
                20
        ));
        assertFalse(InvSeePermissionPolicy.canInspect(
                InvSeeTriState.UNSET,
                InvSeeTriState.TRUE,
                20,
                20
        ));
        assertFalse(InvSeePermissionPolicy.canInspect(
                InvSeeTriState.UNSET,
                InvSeeTriState.TRUE,
                10,
                20
        ));
    }
}
