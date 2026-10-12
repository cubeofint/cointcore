package com.mawlee.cointcore.prefix;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnPrefixNodeMatchTest {
    private static final int PRIORITY = 5;
    private static final Set<String> RANK_TEXTS = Set.of("&8[DarwinAwards] ", "&b[ATM] ", "&6[Titan] ");

    @Test
    void matchesConfiguredPriorityEvenWithoutRankText() {
        assertTrue(OwnPrefixNodeMatch.matches(PRIORITY, "&8[DarwinAwards] ", PRIORITY, Set.of()));
        assertTrue(OwnPrefixNodeMatch.matches(PRIORITY, "anything", PRIORITY, RANK_TEXTS));
    }

    @Test
    void matchesRankTrackPrefixTextAtOtherPriority() {
        assertTrue(OwnPrefixNodeMatch.matches(50, "&8[DarwinAwards] ", PRIORITY, RANK_TEXTS));
        assertFalse(OwnPrefixNodeMatch.matches(50, "&c[Staff] ", PRIORITY, RANK_TEXTS));
    }

    @Test
    void clearsLegacyPriority1000ByRankTrackText() {
        assertTrue(OwnPrefixNodeMatch.matches(
                OwnPrefixNodeMatch.LEGACY_PREFIX_PRIORITY,
                "&8[DarwinAwards] ",
                PRIORITY,
                RANK_TEXTS
        ));
        assertTrue(OwnPrefixNodeMatch.matches(
                OwnPrefixNodeMatch.LEGACY_PREFIX_PRIORITY,
                "&b[ATM] ",
                PRIORITY,
                Set.of()
        ));
        for (String text : List.of("&8[DarwinAwards] ", "&b[ATM] ", "&6[Titan] ")) {
            assertTrue(OwnPrefixNodeMatch.matches(
                    OwnPrefixNodeMatch.LEGACY_PREFIX_PRIORITY,
                    text,
                    PRIORITY,
                    RANK_TEXTS
            ));
        }
    }

    @Test
    void staffDonorPrioritiesAreNotOwnUnlessTextIsARankPrefix() {
        assertFalse(OwnPrefixNodeMatch.matches(70, "&c[Helper] ", PRIORITY, RANK_TEXTS));
        assertFalse(OwnPrefixNodeMatch.matches(110, "&6[Donor] ", PRIORITY, RANK_TEXTS));
        assertTrue(OwnPrefixNodeMatch.matches(70, "&6[Titan] ", PRIORITY, RANK_TEXTS));
    }

    @Test
    void blankAndNullTextDoNotMatchByCatalog() {
        assertFalse(OwnPrefixNodeMatch.matches(10, null, PRIORITY, RANK_TEXTS));
        assertFalse(OwnPrefixNodeMatch.matches(10, "  ", PRIORITY, RANK_TEXTS));
        assertFalse(OwnPrefixNodeMatch.matches(10, "&b[ATM] ", PRIORITY, null));
    }
}
