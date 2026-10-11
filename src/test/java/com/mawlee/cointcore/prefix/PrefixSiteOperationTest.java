package com.mawlee.cointcore.prefix;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrefixSiteOperationTest {
    private static final UUID PLAYER = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void parsesWrappedOperationsAndAutoGroup() {
        String json = """
                {"operations":[
                  {"op_id":"op_abc-12","uuid":"%s","group":"vip","type":"prefix_select"},
                  {"op_id":"op_auto-1","game_id":"%s","group":null}
                ]}
                """.formatted(PLAYER, PLAYER.toString().replace("-", ""));
        List<String> malformed = new ArrayList<>();
        List<PrefixSiteOperation> ops = PrefixSiteOperation.parseAll(json, malformed::add);
        assertTrue(malformed.isEmpty());
        assertEquals(2, ops.size());
        assertEquals("op_abc-12", ops.get(0).opId());
        assertEquals(PLAYER, ops.get(0).playerId());
        assertEquals("vip", ops.get(0).group());
        assertEquals("op_auto-1", ops.get(1).opId());
        assertNull(ops.get(1).group());
    }

    @Test
    void skipsOtherTypesAndMarksBadUuid() {
        String json = """
                [
                  {"op_id":"op_skip01","uuid":"%s","group":"vip","type":"coins"},
                  {"id":"op_bad-01","uuid":"not-a-uuid","group":"vip"}
                ]
                """.formatted(PLAYER);
        List<String> malformed = new ArrayList<>();
        List<PrefixSiteOperation> ops = PrefixSiteOperation.parseAll(json, malformed::add);
        assertTrue(ops.isEmpty());
        assertEquals(List.of("op_bad-01"), malformed);
    }
}
