package com.mawlee.cointcore.vanish;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanishRevealPacketsTest {
    @Test
    void acceptsContainerFootstepAndBreakSounds() {
        assertTrue(VanishRevealPackets.isRevealSoundPath("block.chest.open"));
        assertTrue(VanishRevealPackets.isRevealSoundPath("block.barrel.close"));
        assertTrue(VanishRevealPackets.isRevealSoundPath("block.shulker_box.open"));
        assertTrue(VanishRevealPackets.isRevealSoundPath("block.stone.step"));
        assertTrue(VanishRevealPackets.isRevealSoundPath("block.stone.break"));
        assertTrue(VanishRevealPackets.isRevealSoundPath("block.stone.hit"));
    }

    @Test
    void rejectsUnrelatedSounds() {
        assertFalse(VanishRevealPackets.isRevealSoundPath("block.note_block.harp"));
        assertFalse(VanishRevealPackets.isRevealSoundPath("weather.rain"));
        assertFalse(VanishRevealPackets.isRevealSoundPath("entity.generic.explode"));
        assertFalse(VanishRevealPackets.isRevealSoundPath(""));
        assertFalse(VanishRevealPackets.isRevealSoundPath(null));
    }

    @Test
    void onlyDestroyBlockLevelEventIsReveal() {
        assertTrue(VanishRevealPackets.isRevealLevelEvent(2001));
        assertFalse(VanishRevealPackets.isRevealLevelEvent(1000));
        assertFalse(VanishRevealPackets.isRevealLevelEvent(1032));
    }
}
