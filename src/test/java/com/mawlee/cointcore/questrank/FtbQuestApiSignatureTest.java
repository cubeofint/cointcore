package com.mawlee.cointcore.questrank;

import com.mawlee.cointcore.luckperms.PublicApiMethodResolver;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Locks the FTB Quests 2101.1.36 overload that crashed ATM10-Test:
 * {@code getTeamData} takes a player, not a team UUID. {@code resolveByCount}
 * then invokes the Player overload with a UUID and throws
 * {@code IllegalArgumentException: argument type mismatch}.
 */
class FtbQuestApiSignatureTest {
    private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    @Test
    void getTeamDataByCountWithUuidThrowsArgumentTypeMismatch() {
        FakeQuestFile file = new FakeQuestFile();
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> PublicApiMethodResolver.invoke(file, "getTeamData", TEAM_ID)
        );
        assertEquals("argument type mismatch", thrown.getMessage());
    }

    @Test
    void uuidOverloadsAreUsedWhenParameterTypesAreExplicit() throws Exception {
        FakeQuestFile file = new FakeQuestFile();
        assertEquals(
                "nullable",
                PublicApiMethodResolver.invoke(file, "getNullableTeamData", new Class<?>[]{UUID.class}, TEAM_ID)
        );
        assertEquals(
                "by-uuid",
                PublicApiMethodResolver.invoke(file, "getOrCreateTeamData", new Class<?>[]{UUID.class}, TEAM_ID)
        );
        assertEquals(
                "by-team",
                PublicApiMethodResolver.invoke(file, "getOrCreateTeamData", new Class<?>[]{FakeTeam.class}, new FakeTeam())
        );
        assertEquals(
                "by-player",
                PublicApiMethodResolver.invoke(file, "getTeamData", new Class<?>[]{FakePlayer.class}, new FakePlayer())
        );
    }

    public static final class FakeQuestFile {
        public Object getTeamData(FakePlayer player) {
            return "by-player";
        }

        public Object getOrCreateTeamData(FakeTeam team) {
            return "by-team";
        }

        public Object getOrCreateTeamData(UUID teamId) {
            return "by-uuid";
        }

        public Object getNullableTeamData(UUID teamId) {
            return "nullable";
        }
    }

    public static final class FakePlayer {
    }

    public static final class FakeTeam {
    }
}
