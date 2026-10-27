package com.mawlee.cointcore.refinedstorage.wireless;

import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;

/** Added to RS PatternGridContainerMenu so all native RS/JEI packet handlers still recognize the editor. */
public interface WirelessPatternMenu {
    void cointcore$initializeWirelessPatterns(SlotReference reference, WirelessPatternSession session);
    SlotReference cointcore$patternGridReference();
}
