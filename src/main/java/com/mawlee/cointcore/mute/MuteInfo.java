package com.mawlee.cointcore.mute;

public record MuteInfo(String muter, String reason, long expiresAt) {
    public boolean isExpired() {
        return expiresAt > 0L && System.currentTimeMillis() > expiresAt;
    }

    public boolean isPermanent() {
        return expiresAt <= 0L;
    }

    public long remainingMs() {
        if (isPermanent()) {
            return Long.MAX_VALUE;
        }

        return Math.max(0L, expiresAt - System.currentTimeMillis());
    }
}
