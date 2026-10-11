package com.mawlee.cointcore.player;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerIdsTest {
    private static final UUID NOTCH = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    @Test
    void parsesDashedAndDashlessUuid() {
        assertEquals(NOTCH, PlayerIds.parse(NOTCH.toString()));
        assertEquals(NOTCH, PlayerIds.parse("069a79f444e94726a5befca90e38aaf5"));
        assertEquals(NOTCH, PlayerIds.parse(" 069a79f4-44e9-4726-a5be-fca90e38aaf5 "));
    }

    @Test
    void rejectsInvalidValues() {
        assertNull(PlayerIds.parse(null));
        assertNull(PlayerIds.parse(""));
        assertNull(PlayerIds.parse("Notch"));
        assertNull(PlayerIds.parse("069a79f444e94726a5befca90e38aaf"));
    }
}
