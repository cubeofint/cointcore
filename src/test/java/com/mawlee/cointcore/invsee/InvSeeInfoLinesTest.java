package com.mawlee.cointcore.invsee;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvSeeInfoLinesTest {
    @Test
    void playerStateFormatsOnlineOfflineAndEffects() {
        List<String> online = InvSeeInfoLines.playerState(
                "Steve",
                true,
                18.5f,
                20.0f,
                16,
                4.5f,
                12,
                0.25f,
                400,
                "minecraft:overworld",
                1.2,
                64,
                -8.8,
                "survival",
                List.of("minecraft:speed amp=1 t=40")
        );
        assertTrue(online.getFirst().contains("online"));
        assertTrue(online.stream().anyMatch(line -> line.contains("health 18.5 / 20.0")));
        assertTrue(online.stream().anyMatch(line -> line.contains("xp L12")));
        assertTrue(online.stream().anyMatch(line -> line.contains("minecraft:speed")));

        List<String> offline = InvSeeInfoLines.playerState(
                "Alex", false, 0, 20, 0, 0, 0, 0, 0, "minecraft:the_nether",
                0, 0, 0, "survival", List.of()
        );
        assertTrue(offline.getFirst().contains("offline"));
        assertTrue(offline.stream().anyMatch(line -> line.equals("effects none")));
    }

    @Test
    void ftbUnavailableAndHomes() {
        List<String> missing = InvSeeInfoLines.ftb(false, "x", List.of("home"), "death");
        assertEquals(1, missing.size());
        assertTrue(missing.getFirst().contains("not loaded"));

        List<String> present = InvSeeInfoLines.ftb(true, "Nick", List.of("home 0 64 0"), "minecraft:overworld 1 2 3");
        assertTrue(present.getFirst().contains("Nick"));
        assertTrue(present.stream().anyMatch(line -> line.contains("homes (1)")));
        assertTrue(present.stream().anyMatch(line -> line.contains("last death")));
    }

    @Test
    void gravesReadOnlyListing() {
        List<String> noMod = InvSeeInfoLines.graves(false, "overworld 0 70 0", List.of("ignored"));
        assertTrue(noMod.getFirst().contains("last death"));
        assertTrue(noMod.getLast().contains("not loaded"));

        List<String> empty = InvSeeInfoLines.graves(true, "", List.of());
        assertTrue(empty.stream().anyMatch(line -> line.equals("graves none")));

        List<String> listed = InvSeeInfoLines.graves(true, "nether 1 2 3", List.of("grave A", "grave B"));
        assertEquals(4, listed.size());
        assertTrue(listed.get(1).contains("graves (2)"));
    }
}
