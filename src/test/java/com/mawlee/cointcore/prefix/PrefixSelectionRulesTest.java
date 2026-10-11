package com.mawlee.cointcore.prefix;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrefixSelectionRulesTest {
    private static final List<String> QUEST = List.of(
            "darwinawards", "explorer", "engineer", "atm", "arcanist",
            "bosskiller", "titan", "jesus", "godofatm"
    );

    private static PrefixOption quest(String group, String prefix, int priority, int trackIndex) {
        return new PrefixOption(group, prefix, "quest", priority, trackIndex);
    }

    @Test
    void availableIsOnlyHeldQuestGroups() {
        List<PrefixOption> held = List.of(
                quest("helper", "&cStaff ", 200, -1),
                quest("titan", "&6[Titan] ", 70, 6),
                quest("explorer", "&a[Explorer] ", 20, 1),
                new PrefixOption("emerald", "&a[E] ", "donation", 50, 0),
                quest("hidden", "&8X", 5, 0),
                quest("empty", "", 1, 0)
        );
        List<PrefixOption> available = PrefixSelectionRules.available(held, QUEST, List.of("hidden"));
        assertEquals(List.of("titan", "explorer"), available.stream().map(PrefixOption::group).toList());
        assertTrue(available.stream().allMatch(option -> "quest".equals(option.category())));
    }

    @Test
    void staffAndDonateAreNeverSelectableEvenIfListedAsHeld() {
        List<PrefixOption> held = List.of(
                quest("admin", "&c[A] ", 200, 0),
                quest("unobtanium", "&5[U] ", 80, 0),
                quest("media", "&d[M] ", 40, 0),
                quest("builder", "&e[B] ", 30, 0)
        );
        assertTrue(PrefixSelectionRules.available(held, QUEST, List.of()).isEmpty());
    }

    @Test
    void setAppliesOnlyHeldQuestGroup() {
        PrefixSelectionRules.Decision ok = PrefixSelectionRules.decideSet(Set.of("titan", "explorer"), "Titan");
        assertEquals(PrefixSelectionRules.Kind.APPLIED, ok.kind());
        assertEquals("titan", ok.selectedGroup());
        assertFalse(ok.clearOverride());

        PrefixSelectionRules.Decision staff = PrefixSelectionRules.decideSet(Set.of("titan"), "admin");
        assertEquals(PrefixSelectionRules.Kind.UNAVAILABLE, staff.kind());
        assertEquals("admin", staff.selectedGroup());
        assertFalse(staff.clearOverride());

        PrefixSelectionRules.Decision missing = PrefixSelectionRules.decideSet(Set.of("titan"), "explorer");
        assertEquals(PrefixSelectionRules.Kind.UNAVAILABLE, missing.kind());
        assertEquals(PrefixSelectionRules.REASON_UNAVAILABLE, missing.reason());
    }

    @Test
    void staffDoesNotBlockSelection() {
        PrefixSelectionRules.Decision set = PrefixSelectionRules.decideSet(Set.of("titan"), "titan");
        assertEquals(PrefixSelectionRules.Kind.APPLIED, set.kind());
        PrefixSelectionRules.Decision auto = PrefixSelectionRules.decideAuto();
        assertEquals(PrefixSelectionRules.Kind.AUTO, auto.kind());
        assertTrue(auto.clearOverride());
    }

    @Test
    void autoClearsOverride() {
        PrefixSelectionRules.Decision decision = PrefixSelectionRules.decideAuto();
        assertEquals(PrefixSelectionRules.Kind.AUTO, decision.kind());
        assertTrue(decision.clearOverride());
        assertNull(decision.selectedGroup());
        assertEquals(PrefixSelectionRules.REASON_AUTO, decision.reason());
    }

    @Test
    void blankSetIsAuto() {
        PrefixSelectionRules.Decision decision = PrefixSelectionRules.decideSet(Set.of("titan"), "  ");
        assertEquals(PrefixSelectionRules.Kind.AUTO, decision.kind());
        assertTrue(decision.clearOverride());
    }

    @Test
    void lostQuestGroupFallsBackToAuto() {
        PrefixSelectionRules.Decision lost = PrefixSelectionRules.reconcile("explorer", Set.of("titan"));
        assertEquals(PrefixSelectionRules.Kind.AUTO, lost.kind());
        assertTrue(lost.clearOverride());
        assertEquals(PrefixSelectionRules.REASON_GROUP_LOST, lost.reason());

        PrefixSelectionRules.Decision keep = PrefixSelectionRules.reconcile("titan", Set.of("titan", "explorer"));
        assertEquals(PrefixSelectionRules.Kind.UNCHANGED, keep.kind());
        assertEquals("titan", keep.selectedGroup());
        assertFalse(keep.clearOverride());

        PrefixSelectionRules.Decision none = PrefixSelectionRules.reconcile(null, Set.of("titan"));
        assertEquals(PrefixSelectionRules.Kind.UNCHANGED, none.kind());
        assertNull(none.selectedGroup());

        PrefixSelectionRules.Decision wiped = PrefixSelectionRules.reconcile("titan", Set.of());
        assertEquals(PrefixSelectionRules.Kind.AUTO, wiped.kind());
        assertTrue(wiped.clearOverride());
        assertEquals(PrefixSelectionRules.REASON_GROUP_LOST, wiped.reason());
    }

    @Test
    void highestQuestUsesPriorityThenTrackIndex() {
        List<PrefixOption> available = List.of(
                quest("explorer", "&a[E] ", 20, 1),
                quest("titan", "&6[T] ", 70, 6),
                quest("jesus", "&b[J] ", 70, 7)
        );
        PrefixOption highest = PrefixSelectionRules.highestQuest(available);
        assertEquals("jesus", highest.group());
        assertEquals("jesus", PrefixSnapshot.of(available, null).highestQuest().group());
    }

    @Test
    void snapshotForcesLockedStaffFalse() {
        PrefixSnapshot snapshot = new PrefixSnapshot(true, List.of(quest("titan", "&6[T] ", 70, 6)), "titan");
        assertFalse(snapshot.lockedStaff());
        assertEquals("titan", snapshot.selected());
    }

    @Test
    void categoriesStillRecognizeQuestTrack() {
        assertEquals("quest", PrefixSelectionRules.category(
                "titan", QUEST, List.of(), List.of(), List.of(), "default", "donation"));
        assertEquals("donation", PrefixSelectionRules.category(
                "emerald", QUEST, List.of("emerald"), List.of(), List.of(), "default", "other"));
        assertEquals("media", PrefixSelectionRules.category(
                "youtube", QUEST, List.of(), List.of("youtube"), List.of(), "default", "donation"));
        assertEquals("builder", PrefixSelectionRules.category(
                "builder", QUEST, List.of(), List.of(), List.of("builder"), "default", "donation"));
        assertEquals("default", PrefixSelectionRules.category(
                "default", QUEST, List.of(), List.of(), List.of(), "default", "donation"));
    }
}
