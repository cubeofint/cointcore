package com.mawlee.cointcore.chunklimit;

/**
 * Shared limit bucket for one or more blocks in a chunk (exact id, tag, or named group).
 */
public record ChunkLimitKey(String id, int limit) {
    public ChunkLimitKey {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("limit key id");
        }
        if (limit < 0) {
            throw new IllegalArgumentException("limit");
        }
    }
}
