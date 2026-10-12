package com.mawlee.cointcore.mixin;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Verifies PR #42 watchdog inject targets exist and are concrete in mapped NeoForge/MC jars.
 * Abstract methods have no insn list; Mixin NPEs in {@code InsnList.indexOf} while preparing
 * {@code @Inject}, and {@code require=0} does not prevent that crash.
 */
class WatchdogMixinTargetAuditTest {
    private static final Path WATCHDOG_DIR = Path.of("src/main/java/com/mawlee/cointcore/mixin/watchdog");
    private static final Pattern MIXIN_CLASS = Pattern.compile("@Mixin\\s*\\(\\s*([A-Za-z0-9_.]+)\\.class");
    private static final Pattern IMPORT = Pattern.compile("import\\s+([A-Za-z0-9_.]+);");
    private static final Pattern INJECT_METHOD = Pattern.compile("method\\s*=\\s*\"([^\"]+)\"");

    private static final Map<String, String> EXPECTED_TARGETS = Map.of(
            "SavedDataWatchdogMixin",
            "net.minecraft.world.level.saveddata.SavedData#save(Ljava/io/File;Lnet/minecraft/core/HolderLookup$Provider;)V",
            "PathNavigationWatchdogMixin",
            "net.minecraft.world.entity.ai.navigation.PathNavigation#recomputePath()V",
            "ServerLevelTickChunkWatchdogMixin",
            "net.minecraft.server.level.ServerLevel#tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V",
            "MinecraftServerSaveWatchdogMixin",
            "net.minecraft.server.MinecraftServer#saveEverything(ZZZ)Z"
    );

    @Test
    void watchdogSourcesDoNotInjectIntoAbstractCompoundTagSave() throws IOException {
        String savedData = Files.readString(WATCHDOG_DIR.resolve("SavedDataWatchdogMixin.java"));
        assertFalse(
                savedData.contains("save(Lnet/minecraft/nbt/CompoundTag;"),
                "SavedData.save(CompoundTag, Provider) is abstract in 1.21.1"
        );
        assertTrue(savedData.contains("save(Ljava/io/File;Lnet/minecraft/core/HolderLookup$Provider;)V"));
        assertTrue(savedData.contains("CallbackInfo"));
        assertFalse(savedData.contains("CallbackInfoReturnable"));
    }

    @Test
    void mappedMinecraftMethodsAreConcrete() throws Exception {
        Optional<Path> jar = findMappedMinecraftJar();
        if (jar.isEmpty() && System.getProperty("org.gradle.test.worker") != null) {
            fail("mapped NeoForge/MC jar missing under build/moddev after compileJava");
        }
        assumeTrue(jar.isPresent(), "mapped NeoForge/MC jar not in build/moddev yet; run via Gradle");
        Path mapped = jar.get();

        List<String> failures = new ArrayList<>();
        try (Stream<Path> stream = Files.list(WATCHDOG_DIR)) {
            List<Path> sources = stream.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path source : sources) {
                auditSource(mapped, source, failures);
            }
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    @Test
    void expectedWatchdogDescriptorsStayPinned() throws IOException {
        for (Map.Entry<String, String> expected : EXPECTED_TARGETS.entrySet()) {
            String source = Files.readString(WATCHDOG_DIR.resolve(expected.getKey() + ".java"));
            String descriptor = expected.getValue().substring(expected.getValue().indexOf('#') + 1);
            String ownerSimple = expected.getValue().substring(
                    expected.getValue().lastIndexOf('.') + 1,
                    expected.getValue().indexOf('#')
            );
            assertTrue(source.contains(ownerSimple + ".class"), expected.getKey() + " target class");
            assertTrue(source.contains(descriptor), expected.getKey() + " " + descriptor);
        }
        String serverSave = Files.readString(WATCHDOG_DIR.resolve("MinecraftServerSaveWatchdogMixin.java"));
        assertTrue(serverSave.contains("saveAllChunks(ZZZ)V"));
    }

    private static void auditSource(Path jar, Path source, List<String> failures) throws Exception {
        String text = Files.readString(source);
        String owner = resolveMixinOwner(text);
        if (owner == null) {
            failures.add(source.getFileName() + ": missing @Mixin target");
            return;
        }
        Map<String, MethodFlags> methods = javapMethods(jar, owner);
        if (methods.isEmpty()) {
            failures.add(owner + " not found in " + jar.getFileName());
            return;
        }
        Matcher inject = INJECT_METHOD.matcher(text);
        boolean any = false;
        while (inject.find()) {
            any = true;
            String raw = inject.group(1);
            String name = raw.contains("(") ? raw.substring(0, raw.indexOf('(')) : raw;
            String descriptor = raw.contains("(") ? raw.substring(raw.indexOf('(')) : "";
            MethodFlags flags = findMethod(methods, name, descriptor);
            if (flags == null) {
                failures.add(source.getFileName() + " -> " + owner + "#" + raw + " is missing in mapped MC");
            } else if (flags.abstractMethod) {
                failures.add(source.getFileName() + " -> " + owner + "#" + raw + " is abstract (Mixin prepare NPE)");
            }
        }
        if (!any) {
            failures.add(source.getFileName() + ": no @Inject method descriptors");
        }
    }

    private static String resolveMixinOwner(String source) {
        Map<String, String> imports = new LinkedHashMap<>();
        Matcher importMatcher = IMPORT.matcher(source);
        while (importMatcher.find()) {
            String fq = importMatcher.group(1);
            imports.put(fq.substring(fq.lastIndexOf('.') + 1), fq);
        }
        Matcher mixin = MIXIN_CLASS.matcher(source);
        if (!mixin.find()) {
            return null;
        }
        String simpleOrFq = mixin.group(1);
        if (simpleOrFq.contains(".")) {
            return simpleOrFq;
        }
        return imports.getOrDefault(simpleOrFq, simpleOrFq);
    }

    private static MethodFlags findMethod(Map<String, MethodFlags> methods, String name, String descriptor) {
        if (!descriptor.isEmpty()) {
            return methods.get(name + descriptor);
        }
        return methods.values().stream()
                .filter(flags -> flags.name.equals(name))
                .findFirst()
                .orElse(null);
    }

    private static Map<String, MethodFlags> javapMethods(Path jar, String owner) throws Exception {
        Process process = new ProcessBuilder("javap", "-classpath", jar.toString(), "-s", "-public", owner)
                .redirectErrorStream(true)
                .start();
        List<String> lines;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            lines = reader.lines().toList();
        }
        int exit = process.waitFor();
        Map<String, MethodFlags> methods = new LinkedHashMap<>();
        if (exit != 0) {
            return methods;
        }
        String pending = null;
        boolean pendingAbstract = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("descriptor: ") && pending != null) {
                String descriptor = trimmed.substring("descriptor: ".length());
                methods.put(pending + descriptor, new MethodFlags(pending, descriptor, pendingAbstract));
                pending = null;
                continue;
            }
            if (!trimmed.contains("(") || trimmed.startsWith("Compiled") || trimmed.startsWith("descriptor")) {
                continue;
            }
            pendingAbstract = trimmed.contains(" abstract ");
            int nameStart = trimmed.lastIndexOf(' ', trimmed.indexOf('('));
            if (nameStart < 0) {
                continue;
            }
            pending = trimmed.substring(nameStart + 1, trimmed.indexOf('('));
            if (pending.contains(".")) {
                pending = pending.substring(pending.lastIndexOf('.') + 1);
            }
        }
        return methods;
    }

    private static Optional<Path> findMappedMinecraftJar() throws IOException {
        String override = System.getProperty("cointcore.minecraftJar");
        if (override != null && !override.isBlank()) {
            Path path = Path.of(override);
            return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
        }
        List<Path> roots = List.of(
                Path.of("build/moddev"),
                Path.of("build/tmp"),
                Path.of("build/neoform")
        );
        List<Path> matches = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root)) {
                stream.filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                        .filter(WatchdogMixinTargetAuditTest::containsSavedData)
                        .forEach(matches::add);
            }
        }
        return matches.stream()
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).contains("neoforge"))
                .findFirst()
                .or(() -> matches.stream().findFirst());
    }

    private static boolean containsSavedData(Path jar) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            return zip.getEntry("net/minecraft/world/level/saveddata/SavedData.class") != null;
        } catch (IOException exception) {
            return false;
        }
    }

    private record MethodFlags(String name, String descriptor, boolean abstractMethod) {
    }
}
