package com.mawlee.cointcore.radio;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RadioPortTest {
    @Test void stationFormatsRetainNamesAndUrls() {
        assertEquals(new CointRadioConfig.RadioStation("main", "Моя станция", "https://example.org/live?a=1"),
                CointRadioConfig.parseStation(" MAIN | Моя станция | https://example.org/live?a=1 "));
        assertEquals(new CointRadioConfig.RadioStation("old", "old", "http://example.org/stream"),
                CointRadioConfig.parseStation("Old=http://example.org/stream"));
        assertNull(CointRadioConfig.parseStation("broken"));
        assertNull(CointRadioConfig.parseStation("main||https://example.org/stream"));
    }

    @Test void bookmarkNamesDoNotNeedNetworkAccessOrGameInitialization() {
        assertEquals("example.org", CointRadioBookmarks.createName("https://www.example.org/radio?token=test"));
        assertEquals("Радио", CointRadioBookmarks.createName(""));
    }

    @Test void allPacketsHaveNeoForgePayloadCodecsAndDirections() throws Exception {
        var network = read("CointRadioNetwork");
        int server = 0, client = 0;
        var register = network.methods.stream().filter(m -> m.name.equals("register")).findFirst().orElseThrow();
        for (var instruction : register.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.name.equals("playToServer")) server++;
                if (call.name.equals("playToClient")) client++;
            }
        }
        assertEquals(7, server);
        assertEquals(13, client);
        for (var inner : network.innerClasses) {
            if (!inner.name.startsWith(network.name + "$") || !inner.name.endsWith("Packet")) continue;
            var packet = read(inner.name.substring(inner.name.lastIndexOf('/') + 1));
            assertTrue(packet.interfaces.contains("net/minecraft/network/protocol/common/custom/CustomPacketPayload"), inner.name);
            assertTrue(packet.fields.stream().anyMatch(f -> f.name.equals("CODEC")));
            assertTrue(packet.fields.stream().anyMatch(f -> f.name.equals("TYPE")));
        }
    }

    @Test void radioSpeakerAndTunerHaveRecipesAndModelsInThe121Directories() throws Exception {
        var root = Path.of("src/main/resources");
        for (String name : List.of("coint_radio", "coint_speaker", "coint_tuner")) {
            var recipe = JsonParser.parseString(Files.readString(root.resolve("data/cointcore/recipe/" + name + ".json"))).getAsJsonObject();
            assertEquals("cointcore:" + name, recipe.getAsJsonObject("result").get("id").getAsString());
            assertTrue(Files.isRegularFile(root.resolve("assets/cointcore/models/item/" + name + ".json")));
        }
        for (String name : List.of("coint_radio", "coint_speaker")) {
            assertTrue(Files.isRegularFile(root.resolve("assets/cointcore/blockstates/" + name + ".json")));
            assertTrue(Files.isRegularFile(root.resolve("data/cointcore/loot_table/blocks/" + name + ".json")));
        }
    }

    private static ClassNode read(String name) throws Exception {
        try (var input = RadioPortTest.class.getResourceAsStream("/com/mawlee/cointcore/radio/" + name + ".class")) {
            assertNotNull(input, name);
            var node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }
}
