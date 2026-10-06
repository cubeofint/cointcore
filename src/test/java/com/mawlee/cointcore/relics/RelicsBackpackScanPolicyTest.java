package com.mawlee.cointcore.relics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelicsBackpackScanPolicyTest {
    @Test
    void disabledAlwaysAllowsScan() {
        assertTrue(RelicsBackpackScanPolicy.shouldScanNested(
                false, RelicsBackpackScanPolicy.Mode.SKIP_NESTED, 20, 0L
        ));
    }

    @Test
    void skipNestedNeverScans() {
        assertFalse(RelicsBackpackScanPolicy.shouldScanNested(
                true, RelicsBackpackScanPolicy.Mode.SKIP_NESTED, 20, 0L
        ));
        assertFalse(RelicsBackpackScanPolicy.shouldScanNested(
                true, RelicsBackpackScanPolicy.Mode.SKIP_NESTED, 20, 100L
        ));
    }

    @Test
    void throttleScansOnInterval() {
        assertTrue(RelicsBackpackScanPolicy.shouldScanNested(
                true, RelicsBackpackScanPolicy.Mode.THROTTLE, 20, 0L
        ));
        assertFalse(RelicsBackpackScanPolicy.shouldScanNested(
                true, RelicsBackpackScanPolicy.Mode.THROTTLE, 20, 1L
        ));
        assertTrue(RelicsBackpackScanPolicy.shouldScanNested(
                true, RelicsBackpackScanPolicy.Mode.THROTTLE, 20, 40L
        ));
    }
}
