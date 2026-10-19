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
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pins {@code LivingEntity.hurt(DamageSource, float)} against the mapped NeoForge/MC jar.
 */
class LivingEntityHurtSignatureTest {
    private static final String OWNER = "net/minecraft/world/entity/LivingEntity.class";
    private static final String HURT_DESC = "(Lnet/minecraft/world/damagesource/DamageSource;F)Z";

    @Test
    void mixinSourceUsesMappedHurtDescriptor() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/mawlee/cointcore/mixin/LivingEntityMixin.java"));
        assertTrue(source.contains("hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"));
        assertTrue(source.contains("require = 0"));
    }

    @Test
    void mappedJarHasConcreteHurt() throws IOException {
        Path jar = mappedJar();
        if (jar == null && System.getProperty("org.gradle.test.worker") != null) {
            fail("mapped NeoForge/MC jar missing under build/moddev after compileJava");
        }
        assumeTrue(jar != null, "mapped NeoForge/MC jar not in build/moddev yet; run via Gradle");
        ClassNode node = readClass(jar, OWNER);
        assertNotNull(node, "LivingEntity missing from " + jar);
        boolean found = false;
        boolean concrete = false;
        for (MethodNode method : node.methods) {
            if ("hurt".equals(method.name) && HURT_DESC.equals(method.desc)) {
                found = true;
                concrete = (method.access & 0x0400) == 0;
            }
        }
        assertTrue(found, "LivingEntity.hurt(DamageSource,float) missing in " + jar.getFileName());
        assertTrue(concrete, "LivingEntity.hurt must be concrete for @Inject");
    }

    private static Path mappedJar() throws IOException {
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
            try (Stream<Path> stream = Files.walk(root, 8)) {
                stream.filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                        .filter(LivingEntityHurtSignatureTest::containsLivingEntity)
                        .forEach(matches::add);
            }
        }
        return matches.stream()
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).contains("neoforge"))
                .findFirst()
                .orElse(matches.isEmpty() ? null : matches.getFirst());
    }

    private static boolean containsLivingEntity(Path jar) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            return zip.getEntry(OWNER) != null;
        } catch (IOException exception) {
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
