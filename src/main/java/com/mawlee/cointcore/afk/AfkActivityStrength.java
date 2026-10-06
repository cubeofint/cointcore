package com.mawlee.cointcore.afk;

/**
 * Strong activity resets AFK timers. Weak activity never resets them and is treated as
 * suspicious while the player is already marked AFK.
 */
public enum AfkActivityStrength {
    STRONG,
    WEAK
}
