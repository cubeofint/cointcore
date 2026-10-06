package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GluonTransferTest {
    @Test
    void rejectsSelfAndNonPositive() {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID other = UUID.fromString("00000000-0000-0000-0000-000000000002");
        assertFalse(GluonTransfer.canTransfer(player, player, 10L));
        assertFalse(GluonTransfer.canTransfer(player, other, 0L));
        assertFalse(GluonTransfer.canTransfer(player, other, -1L));
        assertFalse(GluonTransfer.canTransfer(null, other, 5L));
        assertTrue(GluonTransfer.canTransfer(player, other, 1L));
    }
}
