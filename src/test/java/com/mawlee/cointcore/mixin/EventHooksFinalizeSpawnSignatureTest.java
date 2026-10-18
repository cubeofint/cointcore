package com.mawlee.cointcore.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads the NeoForge jar this build compiles against ({@code neo_version}).
 */
class EventHooksFinalizeSpawnSignatureTest {
    private static final String EVENT_HOOKS = "net/neoforged/neoforge/event/EventHooks.class";
    private static final String FIVE_ARG =
            "(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;)Lnet/minecraft/world/entity/SpawnGroupData;";

    @Test
    void neoForgeJarHasFiveArgFinalizeMobSpawnOnly() throws IOException {
        Path jar = neoForgeJarWithEventHooks();
        assertNotNull(
                jar,
                "Could not find a NeoForge jar containing EventHooks (build/moddev or Gradle caches)"
        );
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

    private static Path neoForgeJarWithEventHooks() throws IOException {
        List<Path> roots = new ArrayList<>();
        roots.add(Path.of("build/moddev/artifacts"));
        roots.add(Path.of("build"));
        Path gradleCaches = Path.of(System.getProperty("user.home", ""), ".gradle", "caches");
        if (Files.isDirectory(gradleCaches)) {
            roots.add(gradleCaches);
        }
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            int depth = root.endsWith("caches") ? 16 : 8;
            try (Stream<Path> walk = Files.walk(root, depth)) {
                List<Path> jars = walk
                        .filter(path -> {
                            String name = path.getFileName().toString();
                            return name.startsWith("neoforge-") && name.endsWith(".jar") && !name.contains("sources");
                        })
                        .toList();
                Path merged = jars.stream()
                        .filter(path -> path.getFileName().toString().endsWith("-merged.jar"))
                        .filter(EventHooksFinalizeSpawnSignatureTest::containsEventHooks)
                        .findFirst()
                        .orElse(null);
                if (merged != null) {
                    return merged;
                }
                for (Path jar : jars) {
                    if (containsEventHooks(jar)) {
                        return jar;
                    }
                }
            }
        }
        return null;
    }

    private static boolean containsEventHooks(Path jar) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            return zip.getEntry(EVENT_HOOKS) != null;
        } catch (IOException ignored) {
            return false;
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
