package com.mawlee.cointcore.industrialforegoingsouls;

/**
 * When a Soul Surge may take the per-tick claim, matching Industrial
 * Foregoing Souls 1.10.7 {@code SoulSurgeBlockEntity.serverTick}.
 * <p>
 * The surge has no local soul tank. Each tick IF Souls does: if
 * {@code tickingTime <= 0} it reads {@code SoulNetwork} via
 * {@code getNetwork()} / {@code NetworkManager.get(level).getElement(pos)}
 * and, when {@code getSoulAmount() > 0}, {@code useSoul} then
 * {@code tickingTime = SOUL_TIME}. {@code getBlockState} / ticker run
 * only inside {@code if (tickingTime > 0)} afterwards. Pre-1.10.2 builds
 * pulled the same soul from connected laser bases
 * ({@code getSoulLaserDrills()}) instead of the network tank.
 * <p>
 * Equivalent pre-refill test: leftover {@code tickingTime > 0} or the
 * network (or laser bases) can supply a soul. Claim only when that is
 * true; otherwise release immediately so another surge can take over.
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
