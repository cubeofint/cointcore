package com.mawlee.cointcore.refinedstorage;

import com.mawlee.cointcore.refinedstorage.wireless.MagnetMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MagnetModeTest {
    @Test void cardCyclesThroughAllThreeModesAndReturnsToOff() {
        assertEquals(MagnetMode.INVENTORY, MagnetMode.OFF.next());
        assertEquals(MagnetMode.NETWORK, MagnetMode.INVENTORY.next());
        assertEquals(MagnetMode.OFF, MagnetMode.NETWORK.next());
    }

    @Test void invalidSavedModesNeverEnableTheMagnet() {
        assertEquals(MagnetMode.OFF, MagnetMode.fromId(-1));
        assertEquals(MagnetMode.OFF, MagnetMode.fromId(3));
        assertEquals(MagnetMode.OFF, MagnetMode.fromId(Integer.MAX_VALUE));
    }
}
