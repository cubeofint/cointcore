package com.mawlee.cointcore.watchdog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WatchdogAlertSubscriptionsTest {
    @AfterEach void cleanup() { WatchdogAlertSubscriptions.clear(); }

    @Test
    void startsDisabledAndOnlyChangesTheSelectedPlayer() {
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        assertFalse(WatchdogAlertSubscriptions.isEnabled(first));
        WatchdogAlertSubscriptions.set(first, true);
        assertTrue(WatchdogAlertSubscriptions.isEnabled(first));
        assertFalse(WatchdogAlertSubscriptions.isEnabled(second));
        WatchdogAlertSubscriptions.set(first, false);
        assertFalse(WatchdogAlertSubscriptions.isEnabled(first));
    }

    @Test
    void restoredPreferenceIsVisibleToTheNotificationThreadAndServerStopClearsIt() throws Exception {
        var player = UUID.randomUUID();
        WatchdogAlertSubscriptions.set(player, true);
        var visible = new java.util.concurrent.atomic.AtomicBoolean();
        var notification = new Thread(() -> visible.set(WatchdogAlertSubscriptions.isEnabled(player)));
        notification.start();
        notification.join();
        assertTrue(visible.get());
        WatchdogAlertSubscriptions.clear();
        assertFalse(WatchdogAlertSubscriptions.isEnabled(player));
    }
}
