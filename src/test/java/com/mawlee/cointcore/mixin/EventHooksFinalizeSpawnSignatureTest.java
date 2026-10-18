package com.mawlee.cointcore.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads the NeoForge jar this build compiles against ({@code neo_version} / merged userdev).
 */
class EventHooksFinalizeSpawnSignatureTest {
    private static final String EVENT_HOOKS = "net/neoforged/neoforge/event/EventHooks.class";
    private static final String FIVE_ARG =
            "(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;)Lnet/minecraft/world/entity/SpawnGroupData;";

    @Test
    void neoForgeJarHasFiveArgFinalizeMobSpawnOnly() throws IOException {
        Path jar = neoForgeMergedJar();
        assertNotNull(jar, "Expected build/moddev/artifacts/neoforge-*-merged.jar from createMinecraftArtifacts");
        ClassNode node = readClass(jar, EVENT_HOOKS);
        assertNotNull(node, "EventHooks missing from " + jar);

        List<MethodNode> finalizeOverloads = new ArrayList<>();
        boolean hasSpawner = false;
        for (MethodNode method : node.methods) {
            if ("finalizeMobSpawn".equals(method.name)) {
                finalizeOverloads.add(method);
            }
            if ("finalizeMobSpawnSpawner".equals(method.name)) {
                hasSpawner = true;
            }
        }
        assertFalse(finalizeOverloads.isEmpty(), "finalizeMobSpawn missing in " + jar.getFileName());
        for (MethodNode method : finalizeOverloads) {
            assertFalse(method.desc.contains("Z)"), "Unexpected boolean overload: " + method.desc);
            assertTrue(FIVE_ARG.equals(method.desc), "Unexpected descriptor " + method.desc + " in " + jar.getFileName());
        }
        assertTrue(hasSpawner, "finalizeMobSpawnSpawner should exist on NeoForge 21.1");
    }

    private static Path neoForgeMergedJar() throws IOException {
        Path artifacts = Path.of("build/moddev/artifacts");
        if (!Files.isDirectory(artifacts)) {
            return null;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(artifacts, "neoforge-*-merged.jar")) {
            Path found = null;
            for (Path path : stream) {
                found = path;
            }
            return found;
        }
    }

    private static ClassNode readClass(Path jar, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                return null;
            }
            try (InputStream in = zip.getInputStream(entry)) {
                ClassReader reader = new ClassReader(in.readAllBytes());
                ClassNode node = new ClassNode();
                reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                return node;
            }
        }
    }
}
