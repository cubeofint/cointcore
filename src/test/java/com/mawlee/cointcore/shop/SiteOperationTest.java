package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SiteOperationTest {
    @Test
    void parsesDashlessAndDashedGameIds() {
        UUID id = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        assertEquals(id, SiteOperation.parseGameId("069a79f444e94726a5befca90e38aaf5"));
        assertEquals(id, SiteOperation.parseGameId(id.toString()));
        assertNull(SiteOperation.parseGameId("nope"));
    }

    @Test
    void validatesOperationIds() {
        assertTrue(SiteOperation.validId("0f8e2c1a-aaaa-bbbb"));
        assertFalse(SiteOperation.validId("../../x"));
        assertFalse(SiteOperation.validId(null));
    }

    @Test
    void parsesQueuePayload() {
        String json = "{\"operations\":[{\"id\":\"op_12345678\",\"game_id\":\"069a79f444e94726a5befca90e38aaf5\","
                + "\"name\":\"Notch\",\"amount\":\"25.00\",\"direction\":\"to_server\"},"
                + "{\"id\":\"op_87654321\",\"game_id\":\"069a79f4-44e9-4726-a5be-fca90e38aaf5\","
                + "\"amount\":7,\"direction\":\"from_server\"}]}";
        List<String> bad = new java.util.ArrayList<>();
        List<SiteOperation> ops = SiteOperation.parseAll(json, bad::add);
        assertEquals(2, ops.size());
        assertTrue(ops.get(0).toServer());
        assertEquals(25L, ops.get(0).amount());
        assertFalse(ops.get(1).toServer());
        assertTrue(bad.isEmpty());
    }

    @Test
    void reportsMalformedAndFractional() {
        List<String> bad = new java.util.ArrayList<>();
        List<SiteOperation> ops = SiteOperation.parseAll("{\"operations\":[{\"id\":\"op_frac0001\",\"game_id\":\"069a79f444e94726a5befca90e38aaf5\",\"amount\":\"1.50\",\"direction\":\"to_server\"}]}", bad::add);
        assertTrue(ops.isEmpty());
        assertEquals(List.of("op_frac0001"), bad);
    }

    @Test
    void parsesAdjustCreditDebitAndReconcileProbe() {
        String json = "{\"operations\":["
                + "{\"id\":\"9b2f6c1e-3d4a-4e0b\",\"user_id\":2275,\"game_id\":\"f3fc162d-d344-32fa-8c9a-0b987b0791cf\","
                + "\"name\":\"Nick\",\"direction\":\"adjust\",\"amount\":-50,\"reason\":\"Компенсация за баг\",\"source\":\"admin_user_edit\"},"
                + "{\"id\":\"op_credit01\",\"game_id\":\"f3fc162dd34432fa8c9a0b987b0791cf\",\"direction\":\"adjust\",\"amount\":12,\"source\":\"shop\"},"
                + "{\"id\":\"op_probe001\",\"game_id\":\"f3fc162d-d344-32fa-8c9a-0b987b0791cf\",\"direction\":\"adjust\",\"amount\":0,\"source\":\"reconcile\"}"
                + "]}";
        List<String> bad = new java.util.ArrayList<>();
        List<SiteOperation> ops = SiteOperation.parseAll(json, bad::add);
        assertTrue(bad.isEmpty());
        assertEquals(3, ops.size());
        assertEquals(SiteOperation.Kind.ADJUST, ops.get(0).kind());
        assertEquals(-50L, ops.get(0).amount());
        assertEquals("Компенсация за баг", ops.get(0).reason());
        assertEquals("admin_user_edit", ops.get(0).source());
        assertEquals(12L, ops.get(1).amount());
        assertEquals(0L, ops.get(2).amount());
        assertEquals("reconcile", ops.get(2).source());
    }

    @Test
    void rejectsZeroAdjustWithoutReconcileAndUnknownDirection() {
        List<String> bad = new java.util.ArrayList<>();
        List<SiteOperation> ops = SiteOperation.parseAll("{\"operations\":["
                + "{\"id\":\"op_zero0001\",\"game_id\":\"069a79f444e94726a5befca90e38aaf5\",\"direction\":\"adjust\",\"amount\":0,\"source\":\"admin_user_edit\"},"
                + "{\"id\":\"op_baddir01\",\"game_id\":\"069a79f444e94726a5befca90e38aaf5\",\"direction\":\"sideways\",\"amount\":5}"
                + "]}", bad::add);
        assertTrue(ops.isEmpty());
        assertEquals(List.of("op_zero0001", "op_baddir01"), bad);
    }
}
