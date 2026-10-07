package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SiteMovementPayloadTest {
    @Test
    void backoffDoublesUpToCap() {
        assertEquals(5_000L, SiteMovementPayload.nextBackoff(0));
        assertEquals(10_000L, SiteMovementPayload.nextBackoff(5_000L));
        assertEquals(300_000L, SiteMovementPayload.nextBackoff(200_000L));
        assertEquals(300_000L, SiteMovementPayload.nextBackoff(300_000L));
    }

    @Test
    void acceptedUpToNeverExceedsSentBatch() {
        assertEquals(7L, SiteMovementPayload.acceptedUpTo("{\"accepted_up_to\":7,\"stored\":7}", 9L));
        assertEquals(9L, SiteMovementPayload.acceptedUpTo("{\"accepted_up_to\":99}", 9L));
        assertEquals(9L, SiteMovementPayload.acceptedUpTo("garbage", 9L));
    }
}
