package com.mawlee.cointcore.prefix;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrefixSelectionRulesTest {
    private static final List<String> STAFF = List.of("stazher", "helper", "moderator", "hmod", "admin", "hadmin");

    @Test
    void staffTrackGroupLocksSelection() {
        assertTrue(PrefixSelectionRules.isStaffLocked(List.of("default", "helper"), STAFF));
        assertFalse(PrefixSelectionRules.isStaffLocked(List.of("default", "vip"), STAFF));
        assertFalse(PrefixSelectionRules.isStaffLocked(List.of(), STAFF));
    }

    @Test
    void staffLockRefusesSetAndClearsOverride() {
        PrefixSelectionRules.Decision set = PrefixSelectionRules.decideSet(true, Set.of("vip"), "vip");
        assertEquals(PrefixSelectionRules.Kind.STAFF_LOCKED, set.kind());
        assertTrue(set.clearOverride());
        assertNull(set.selectedGroup());
        assertEquals(PrefixSelectionRules.REASON_STAFF_LOCKED, set.reason());

        PrefixSelectionRules.Decision auto = PrefixSelectionRules.decideAuto(true);
        assertEquals(PrefixSelectionRules.Kind.STAFF_LOCKED, auto.kind());
        assertTrue(auto.clearOverride());
    }

    @Test
    void staffReconcileAlwaysDropsOverride() {
        PrefixSelectionRules.Decision decision = PrefixSelectionRules.reconcile(true, "vip", Set.of("vip"));
        assertEquals(PrefixSelectionRules.Kind.STAFF_LOCKED, decision.kind());
        assertTrue(decision.clearOverride());
        assertNull(decision.selectedGroup());
    }

    @Test
    void availableExcludesStaffEmptyAndExcludedGroups() {
        List<PrefixOption> held = List.of(
                new PrefixOption("helper", "&cStaff ", "other"),
                new PrefixOption("vip", "&6[VIP] ", "donation"),
                new PrefixOption("default", "&7", "default"),
                new PrefixOption("hidden", "&8X", "other"),
                new PrefixOption("empty", "", "other")
        );
        List<PrefixOption> available = PrefixSelectionRules.available(held, STAFF, List.of("hidden"));
        assertEquals(List.of("vip", "default"), available.stream().map(PrefixOption::group).toList());
    }

    @Test
    void setAppliesOnlyHeldAvailableGroup() {
        PrefixSelectionRules.Decision ok = PrefixSelectionRules.decideSet(false, Set.of("vip", "default"), "VIP");
        assertEquals(PrefixSelectionRules.Kind.APPLIED, ok.kind());
        assertEquals("vip", ok.selectedGroup());
        assertFalse(ok.clearOverride());

        PrefixSelectionRules.Decision missing = PrefixSelectionRules.decideSet(false, Set.of("vip"), "mvp");
        assertEquals(PrefixSelectionRules.Kind.UNAVAILABLE, missing.kind());
        assertEquals("mvp", missing.selectedGroup());
        assertFalse(missing.clearOverride());
        assertEquals(PrefixSelectionRules.REASON_UNAVAILABLE, missing.reason());
    }

    @Test
    void autoClearsOverrideWhenNotStaff() {
        PrefixSelectionRules.Decision decision = PrefixSelectionRules.decideAuto(false);
        assertEquals(PrefixSelectionRules.Kind.AUTO, decision.kind());
        assertTrue(decision.clearOverride());
        assertNull(decision.selectedGroup());
        assertEquals(PrefixSelectionRules.REASON_AUTO, decision.reason());
    }

    @Test
    void blankSetIsAuto() {
        PrefixSelectionRules.Decision decision = PrefixSelectionRules.decideSet(false, Set.of("vip"), "  ");
        assertEquals(PrefixSelectionRules.Kind.AUTO, decision.kind());
        assertTrue(decision.clearOverride());
    }

    @Test
    void lostGroupFallsBackToAuto() {
        PrefixSelectionRules.Decision lost = PrefixSelectionRules.reconcile(false, "vip", Set.of("default"));
        assertEquals(PrefixSelectionRules.Kind.AUTO, lost.kind());
        assertTrue(lost.clearOverride());
        assertEquals(PrefixSelectionRules.REASON_GROUP_LOST, lost.reason());

        PrefixSelectionRules.Decision keep = PrefixSelectionRules.reconcile(false, "vip", Set.of("vip", "default"));
        assertEquals(PrefixSelectionRules.Kind.UNCHANGED, keep.kind());
        assertEquals("vip", keep.selectedGroup());
        assertFalse(keep.clearOverride());

        PrefixSelectionRules.Decision none = PrefixSelectionRules.reconcile(false, null, Set.of("vip"));
        assertEquals(PrefixSelectionRules.Kind.UNCHANGED, none.kind());
        assertNull(none.selectedGroup());
    }

    @Test
    void categoriesMatchTracksAndLists() {
        assertEquals("quest", PrefixSelectionRules.category(
                "miner", List.of("miner", "farmer"), List.of(), List.of(), List.of(), "default", "donation"));
        assertEquals("donation", PrefixSelectionRules.category(
                "vip", List.of("miner"), List.of("vip"), List.of(), List.of(), "default", "other"));
        assertEquals("media", PrefixSelectionRules.category(
                "youtube", List.of(), List.of(), List.of("youtube"), List.of(), "default", "donation"));
        assertEquals("builder", PrefixSelectionRules.category(
                "builder", List.of(), List.of(), List.of(), List.of("builder"), "default", "donation"));
        assertEquals("default", PrefixSelectionRules.category(
                "default", List.of(), List.of(), List.of(), List.of(), "default", "donation"));
        assertEquals("donation", PrefixSelectionRules.category(
                "mvp", List.of(), List.of(), List.of(), List.of(), "default", "donation"));
    }
}
