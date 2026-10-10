package com.mawlee.cointcore.claim;

/**
 * Pure chunk/geometry helpers for {@link BossClaimGuardService} (no Minecraft/FTB deps in tests).
 */
public final class BossClaimGeometry {
    private BossClaimGeometry() {
    }

    /**
     * True when the chunk's XZ square intersects the horizontal disk of a spike (crystal tower).
     */
    public static boolean chunkIntersectsDisk(int chunkX, int chunkZ, int centerX, int centerZ, int radius) {
        int minX = chunkX << 4;
        int maxX = minX + 15;
        int minZ = chunkZ << 4;
        int maxZ = minZ + 15;
        int closestX = Math.clamp(centerX, minX, maxX);
        int closestZ = Math.clamp(centerZ, minZ, maxZ);
        long dx = (long) closestX - centerX;
        long dz = (long) closestZ - centerZ;
        long radiusSq = (long) radius * radius;
        return dx * dx + dz * dz <= radiusSq;
    }

    public static boolean chunkWithinChebyshev(int chunkX, int chunkZ, int originChunkX, int originChunkZ, int radius) {
        return Math.max(Math.abs(chunkX - originChunkX), Math.abs(chunkZ - originChunkZ)) <= radius;
    }
}
