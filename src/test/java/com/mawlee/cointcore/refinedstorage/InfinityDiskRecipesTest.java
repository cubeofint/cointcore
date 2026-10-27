package com.mawlee.cointcore.refinedstorage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InfinityDiskRecipesTest {
    @Test
    void all39DisksHaveRecipesModelsTranslationsAndSafeOptionalIngredients() throws IOException {
        JsonArray catalog = read("cointcore/infinity_disks.json").getAsJsonArray();
        assertEquals(39, catalog.size());
        Set<String> names = new HashSet<>();
        Set<String> resources = new HashSet<>();
        int fluids = 0;
        var english = read("assets/cointcore/lang/en_us.json").getAsJsonObject();
        var russian = read("assets/cointcore/lang/ru_ru.json").getAsJsonObject();
        for (var element : catalog) {
            var disk = element.getAsJsonObject();
            String name = disk.get("name").getAsString();
            assertTrue(names.add(name), "Duplicate disk " + name);
            assertTrue(resources.add(disk.get("resource").getAsString()));
            if (disk.get("fluid").getAsBoolean()) fluids++;
            String item = "cointcore:infinity_" + name + "_disk";
            assertTrue(english.has("item." + item.replace(':', '.')));
            assertTrue(russian.has("item." + item.replace(':', '.')));
            assertNotNull(read("assets/cointcore/models/item/infinity_" + name + "_disk.json"));
            var recipe = recipe(name);
            assertEquals(item, recipe.getAsJsonObject("result").get("id").getAsString());
            assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
            var keys = recipe.getAsJsonObject("key");
            assertEquals("c:glass_blocks", keys.getAsJsonObject("G").get("tag").getAsString());
            Set<String> used = new HashSet<>();
            for (var row : recipe.getAsJsonArray("pattern")) {
                assertEquals(3, row.getAsString().length());
                row.getAsString().chars().forEach(character -> used.add(Character.toString((char) character)));
            }
            assertEquals(keys.keySet(), used);
            Set<String> guarded = new HashSet<>();
            for (var condition : recipe.getAsJsonArray("neoforge:conditions")) {
                var object = condition.getAsJsonObject();
                if (object.get("type").getAsString().equals("neoforge:item_exists")) {
                    guarded.add(object.get("item").getAsString());
                }
            }
            assertTrue(guarded.contains(item));
            for (var ingredient : keys.asMap().values()) {
                var object = ingredient.getAsJsonObject();
                if (object.has("item") && !object.get("item").getAsString().startsWith("minecraft:")) {
                    assertTrue(guarded.contains(object.get("item").getAsString()), "Unguarded mod ingredient in " + name);
                }
            }
        }
        assertEquals(2, fluids);
    }

    @Test
    void preservesAtmProgressionAlloyPositionsAndRsComponents() throws IOException {
        for (var entry : read("cointcore/infinity_disks.json").getAsJsonArray()) {
            String name = entry.getAsJsonObject().get("name").getAsString();
            var recipe = recipe(name);
            if (name.equals("water") || name.equals("cobblestone")) {
                assertEquals("refinedstorage:16k_storage_part", item(recipe, "C"));
                assertEquals("minecraft:water_bucket", item(recipe, "W"));
                assertEquals(name.equals("water") ? "minecraft:water_bucket" : "minecraft:lava_bucket", item(recipe, "I"));
                assertEquals("c:gems/diamond", recipe.getAsJsonObject("key").getAsJsonObject("D").get("tag").getAsString());
                continue;
            }
            assertEquals(JsonParser.parseString("[\"GIG\",\"XCY\",\"AVU\"]"), recipe.get("pattern"));
            assertEquals("allthemodium:unobtainium_allthemodium_alloy_ingot", item(recipe, "A"));
            assertEquals("allthemodium:unobtainium_vibranium_alloy_ingot", item(recipe, "V"));
            assertEquals("allthemodium:vibranium_allthemodium_alloy_ingot", item(recipe, "U"));
            String component = switch (name) {
                case "gravel" -> "cointcore:infinity_cobblestone_disk";
                case "sand" -> "cointcore:infinity_gravel_disk";
                default -> "extradisks:262144k_item_storage_part";
            };
            assertEquals(component, item(recipe, "C"), name);
            if (entry.getAsJsonObject().get("resource").getAsString().endsWith("_dye")) {
                assertEquals("allthecompressed:" + name + "_concrete_6x", item(recipe, "I"));
            }
        }
        assertEquals("mekanism:crusher", item(recipe("gravel"), "X"));
        assertEquals("mekanism:crusher", item(recipe("sand"), "Y"));
    }

    private static String item(JsonObject recipe, String key) {
        return recipe.getAsJsonObject("key").getAsJsonObject(key).get("item").getAsString();
    }

    private static JsonObject recipe(String name) throws IOException {
        return read("data/cointcore/recipe/infinity_disks/" + name + ".json").getAsJsonObject();
    }

    private static com.google.gson.JsonElement read(String path) throws IOException {
        try (var stream = InfinityDiskRecipesTest.class.getResourceAsStream("/" + path)) {
            assertNotNull(stream, path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader);
            }
        }
    }
}
