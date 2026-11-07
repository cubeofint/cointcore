package com.mawlee.cointcore.modularbees;

import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;

/** Implemented only on ModularBees' ME hosts when both storage mods are present. */
public interface RsHatchHost {
    RsHatchConnection cointcore$rsHatch();
    boolean isActive();
    /** RS storage while the RS link is preferred, otherwise the AE2 grid's storage (may be null). */
    MEStorage getMEStorage();
    IActionSource getSource();
    void saveChanges();
}
