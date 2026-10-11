package com.mawlee.cointcore.industrialforegoingsouls;

/**
 * When a Soul Surge may take the per-tick claim, matching Industrial
 * Foregoing Souls 1.10.7 {@code SoulSurgeBlockEntity.serverTick}.
 * <p>
 * IF Souls works this tick only after the soul refill attempt: if
 * {@code tickingTime <= 0} it consumes one soul from the pipe network
 * ({@code getSoulAmount() > 0} then {@code useSoul}) and sets
 * {@code tickingTime = SOUL_TIME}. Acceleration (and soul-time spend)
 * runs only when {@code tickingTime > 0} afterwards. An isolated surge
 * still has a network element, but {@code getSoulAmount()} is 0, so it
 * never enters that block.
 * <p>
 * Equivalent pre-refill test: leftover {@code tickingTime > 0} or the
 * network can supply a soul. Claim only when that is true; otherwise
 * release immediately so another surge can take over this tick.
 */
public final class SoulSurgeAcceleration {
    private SoulSurgeAcceleration() {
    }

    /**
     * {@code true} if IF Souls will have {@code tickingTime > 0} after
     * its soul refill attempt this tick.
     */
    public static boolean canWorkThisTick(int tickingTime, int networkSoulAmount) {
        return tickingTime > 0 || networkSoulAmount > 0;
    }

    /**
     * Claim {@code targetPacked} only when this surge can accelerate now.
     * Otherwise drop a sticky/current claim so a later surge in the same
     * tick may take over.
     */
    public static boolean tryAccelerate(
            SoulSurgeAccelerationClaim claims,
            long targetPacked,
            long surgePacked,
            long gameTime,
            boolean canWorkThisTick
    ) {
        if (!canWorkThisTick) {
            claims.release(targetPacked, surgePacked, gameTime);
            return false;
        }
        return claims.tryClaim(targetPacked, surgePacked, gameTime);
    }
}
