package com.mawlee.cointcore.crowded;

/**
 * Per-mob crowded cache implemented by the Mob mixin.
 */
public interface CrowdedMobMarker {
    boolean cointcore$isCrowded();

    void cointcore$setCrowded(boolean crowded);

    int cointcore$nextCrowdedCheckTick();

    void cointcore$setNextCrowdedCheckTick(int tick);
}
