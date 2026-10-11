package com.mawlee.cointcore.prefix;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrefixCatalogTest {
    @Test
    void displayNamePrefersMetaThenDisplayNodeThenStrippedPrefixThenGroup() {
        assertEquals("Titan", PrefixCatalog.displayName(" Titan ", "Ignored", "&6[X] ", "titan"));
        assertEquals("Hero", PrefixCatalog.displayName("  ", " Hero ", "&6[X] ", "titan"));
        assertEquals("Titan", PrefixCatalog.displayName(null, "", "&6[Titan] ", "titan"));
        assertEquals("titan", PrefixCatalog.displayName(null, null, "   ", "Titan"));
        assertEquals("", PrefixCatalog.displayName(null, null, "", "  "));
    }

    @Test
    void stripPrefixTextRemovesColorsTagsAndBrackets() {
        assertEquals("Titan", PrefixCatalog.stripPrefixText("&6[Titan] "));
        assertEquals("U", PrefixCatalog.stripPrefixText("&x&a&a&a&a&a&a[U]"));
        assertEquals("God", PrefixCatalog.stripPrefixText("&#ffaa00[God]"));
        assertEquals("ATM", PrefixCatalog.stripPrefixText("<#aabbcc><bold>[ATM]</bold>"));
        assertEquals("Jesus", PrefixCatalog.stripPrefixText("§b[Jesus]§r"));
        assertEquals("", PrefixCatalog.stripPrefixText(" &7[] "));
    }

    @Test
    void normalizeSortsByTrackIndexThenGroupAndForcesQuestCategory() {
        List<PrefixCatalogEntry> normalized = PrefixCatalog.normalize(java.util.Arrays.asList(
                new PrefixCatalogEntry("Titan", "&6[Titan] ", 70, 6, "Titan", "other"),
                new PrefixCatalogEntry("explorer", "&a[Explorer] ", 20, 1, "", "donation"),
                new PrefixCatalogEntry("", "&cX", 1, 0, "Nope", "quest"),
                null
        ));
        assertEquals(List.of("explorer", "titan"), normalized.stream().map(PrefixCatalogEntry::group).toList());
        assertEquals(1, normalized.get(0).trackIndex());
        assertEquals("Explorer", normalized.get(0).displayName());
        assertTrue(normalized.stream().allMatch(entry -> PrefixCatalog.CATEGORY.equals(entry.category())));
    }

    @Test
    void hashIsStableForEquivalentCatalogsAndChangesWhenPrefixChanges() {
        List<PrefixCatalogEntry> first = PrefixCatalog.normalize(List.of(
                new PrefixCatalogEntry("explorer", "&a[E] ", 20, 1, "Explorer", "quest"),
                new PrefixCatalogEntry("titan", "&6[T] ", 70, 6, "Titan", "quest")
        ));
        List<PrefixCatalogEntry> shuffled = PrefixCatalog.normalize(List.of(
                new PrefixCatalogEntry("titan", "&6[T] ", 70, 6, "Titan", "quest"),
                new PrefixCatalogEntry("explorer", "&a[E] ", 20, 1, "Explorer", "quest")
        ));
        List<PrefixCatalogEntry> changed = PrefixCatalog.normalize(List.of(
                new PrefixCatalogEntry("explorer", "&a[E] ", 20, 1, "Explorer", "quest"),
                new PrefixCatalogEntry("titan", "&6[Titan] ", 70, 6, "Titan", "quest")
        ));
        assertEquals(64, PrefixCatalog.hash(first).length());
        assertEquals(PrefixCatalog.hash(first), PrefixCatalog.hash(shuffled));
        assertNotEquals(PrefixCatalog.hash(first), PrefixCatalog.hash(changed));
    }

    @Test
    void payloadUsesSchemaOneFullTrueAndHashedGroups() {
        List<PrefixCatalogEntry> groups = PrefixCatalog.normalize(List.of(
                new PrefixCatalogEntry("titan", "&6[Titan] ", 70, 6, "Titan", "quest")
        ));
        String hash = PrefixCatalog.hash(groups);
        JsonObject payload = PrefixCatalog.toPayload(groups, hash, true);
        assertEquals(1, payload.get("schema").getAsInt());
        assertTrue(payload.get("full").getAsBoolean());
        assertEquals(hash, payload.get("hash").getAsString());
        assertEquals(1, payload.getAsJsonArray("groups").size());
        JsonObject row = payload.getAsJsonArray("groups").get(0).getAsJsonObject();
        assertEquals("titan", row.get("group").getAsString());
        assertEquals("&6[Titan] ", row.get("prefix_raw").getAsString());
        assertEquals(70, row.get("priority").getAsInt());
        assertEquals(6, row.get("track_index").getAsInt());
        assertEquals("Titan", row.get("display_name").getAsString());
        assertEquals("quest", row.get("category").getAsString());
    }

    @Test
    void bulkCursorSkipsUsersUpToSavedId() {
        java.util.UUID first = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");
        java.util.UUID second = java.util.UUID.fromString("22222222-2222-2222-2222-222222222222");
        java.util.UUID third = java.util.UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        List<java.util.UUID> remaining = PrefixCatalog.sortAfterCursor(
                java.util.Set.of(third, first, second),
                first.toString()
        );
        assertEquals(List.of(second, third), remaining);
    }
}
