package com.mawlee.cointcore.vanish;

/**
 * Packet kinds that can reveal a vanished player. Other world packets are ignored
 * so the filter does not walk every sound/particle on the server.
 */
public final class VanishRevealPackets {
    public static final int PARTICLES_DESTROY_BLOCK = 2001;

    private VanishRevealPackets() {
    }

    public static boolean isRevealSoundPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return containsToken(path, "step")
                || containsToken(path, "chest")
                || containsToken(path, "barrel")
                || containsToken(path, "shulker")
                || containsToken(path, "break")
                || containsToken(path, "hit")
                || containsToken(path, "open")
                || containsToken(path, "close");
    }

    public static boolean isRevealLevelEvent(int type) {
        return type == PARTICLES_DESTROY_BLOCK;
    }

    private static boolean containsToken(String path, String token) {
        int index = path.indexOf(token);
        if (index < 0) {
            return false;
        }
        boolean startOk = index == 0 || !isIdentChar(path.charAt(index - 1));
        int end = index + token.length();
        boolean endOk = end == path.length() || !isIdentChar(path.charAt(end));
        return startOk && endOk;
    }

    private static boolean isIdentChar(char character) {
        return character == '_' || character == '-' || Character.isLetterOrDigit(character);
    }
}
