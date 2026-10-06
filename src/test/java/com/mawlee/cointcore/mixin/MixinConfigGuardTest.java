package com.mawlee.cointcore.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cheap CI guards: mixin JSON shape, plugin coverage for compat entries, and no mixin-on-mixin targets.
 */
class MixinConfigGuardTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path MIXIN_ROOT = Path.of("src/main/java/com/mawlee/cointcore/mixin");
    private static final Path PLUGIN = MIXIN_ROOT.resolve("CointCoreMixinPlugin.java");

    private static final Pattern MIXIN_ANNOTATION = Pattern.compile("@Mixin\\s*\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern TARGETS_STRING = Pattern.compile("targets\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern TARGETS_ARRAY = Pattern.compile("targets\\s*=\\s*\\{([^}]*)}");
    private static final Pattern CLASS_LITERAL = Pattern.compile("([A-Za-z0-9_.]+)\\.class");
    private static final Pattern IMPORT = Pattern.compile("import\\s+([A-Za-z0-9_.]+);");
    private static final Pattern MIXIN_STRING_LITERAL = Pattern.compile(
            "\"com\\.mawlee\\.cointcore\\.mixin\\.([A-Za-z0-9_.]+)\""
    );
    private static final Pattern SECTION_ARRAY = Pattern.compile(
            "\"(mixins|client|server)\"\\s*:\\s*\\[(.*?)]",
            Pattern.DOTALL
    );
    private static final Pattern QUOTED = Pattern.compile("\"([^\"]+)\"");

    @Test
    void coreConfigIsRequiredAndCompatIsSoft() throws IOException {
        String core = Files.readString(RESOURCES.resolve("cointcore.mixins.json"));
        String compat = Files.readString(RESOURCES.resolve("cointcore.compat.mixins.json"));

        assertTrue(core.contains("\"required\": true") || core.contains("\"required\":true"));
        assertTrue(core.contains("\"defaultRequire\": 1") || core.contains("\"defaultRequire\":1"));
        assertTrue(core.contains("\"com.mawlee.cointcore.mixin.CointCoreMixinPlugin\""));

        assertTrue(compat.contains("\"required\": false") || compat.contains("\"required\":false"));
        assertTrue(compat.contains("\"defaultRequire\": 0") || compat.contains("\"defaultRequire\":0"));
        assertTrue(compat.contains("\"com.mawlee.cointcore.mixin.CointCoreMixinPlugin\""));
    }

    @Test
    void modsTomlRegistersBothMixinConfigs() throws IOException {
        String toml = Files.readString(Path.of("src/main/templates/META-INF/neoforge.mods.toml"));
        assertTrue(toml.contains("config=\"${mod_id}.mixins.json\""));
        assertTrue(toml.contains("config=\"${mod_id}.compat.mixins.json\""));
    }

    @Test
    void noMixinTargetsAnotherModsMixinClass() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(MIXIN_ROOT)) {
            List<Path> sources = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("CointCoreMixinPlugin.java"))
                    .toList();
            for (Path source : sources) {
                String text = Files.readString(source);
                for (String target : extractTargets(text)) {
                    String normalized = target.replace('/', '.');
                    if (normalized.toLowerCase(Locale.ROOT).contains(".mixin.")) {
                        violations.add(source.getFileName() + " -> " + normalized);
                    }
                }
            }
        }
        assertTrue(
                violations.isEmpty(),
                "Mixins must not target another mod's mixin class (prepare-time crash):\n"
                        + String.join("\n", violations)
        );
    }

    @Test
    void everyCompatMixinIsGatedInPlugin() throws IOException {
        Set<String> compatEntries = listedMixins(Files.readString(RESOURCES.resolve("cointcore.compat.mixins.json")));
        String pluginSource = Files.readString(PLUGIN);
        Set<String> gated = new LinkedHashSet<>();
        Matcher matcher = MIXIN_STRING_LITERAL.matcher(pluginSource);
        while (matcher.find()) {
            gated.add(matcher.group(1));
        }

        List<String> missing = compatEntries.stream()
                .filter(name -> !gated.contains(name))
                .sorted()
                .toList();
        assertTrue(
                missing.isEmpty(),
                "Compat mixins missing from CointCoreMixinPlugin mod gates:\n" + String.join("\n", missing)
        );
    }

    @Test
    void listedMixinsHaveMatchingSourceFiles() throws IOException {
        Set<String> listed = new LinkedHashSet<>();
        listed.addAll(listedMixins(Files.readString(RESOURCES.resolve("cointcore.mixins.json"))));
        listed.addAll(listedMixins(Files.readString(RESOURCES.resolve("cointcore.compat.mixins.json"))));

        List<String> missingSources = listed.stream()
                .filter(name -> !Files.isRegularFile(MIXIN_ROOT.resolve(name.replace('.', '/') + ".java")))
                .sorted()
                .toList();
        assertTrue(missingSources.isEmpty(), "Mixin JSON entries without source files:\n" + String.join("\n", missingSources));
    }

    @Test
    void relicsBackpackScanTargetsBackpackItemNotRelicsMixin() throws IOException {
        String source = Files.readString(MIXIN_ROOT.resolve("relics/RelicsBackpackScanMixin.java"));
        assertTrue(source.contains("BackpackItem"));
        assertFalse(source.contains("it.hurts.sskirillss.relics.mixin"));
        assertTrue(source.contains("priority = 1100") || source.contains("priority=1100"));
        assertTrue(source.contains("relics$tickBackpackContents"));
        String plugin = Files.readString(PLUGIN);
        assertTrue(plugin.contains("RelicsBackpackScanMixin"));
        assertTrue(plugin.contains("sophisticatedbackpacks"));
    }

    private static Set<String> listedMixins(String json) {
        Set<String> names = new LinkedHashSet<>();
        Matcher section = SECTION_ARRAY.matcher(json);
        while (section.find()) {
            Matcher quoted = QUOTED.matcher(section.group(2));
            while (quoted.find()) {
                names.add(quoted.group(1));
            }
        }
        return names;
    }

    private static Set<String> extractTargets(String source) {
        Set<String> targets = new LinkedHashSet<>();
        java.util.Map<String, String> imports = IMPORT.matcher(source).results()
                .collect(Collectors.toMap(
                        match -> {
                            String fq = match.group(1);
                            return fq.substring(fq.lastIndexOf('.') + 1);
                        },
                        match -> match.group(1),
                        (left, right) -> left
                ));

        Matcher mixinMatcher = MIXIN_ANNOTATION.matcher(source);
        while (mixinMatcher.find()) {
            String body = mixinMatcher.group(1);
            Matcher stringTarget = TARGETS_STRING.matcher(body);
            while (stringTarget.find()) {
                targets.add(stringTarget.group(1));
            }
            Matcher arrayTarget = TARGETS_ARRAY.matcher(body);
            while (arrayTarget.find()) {
                Matcher quoted = Pattern.compile("\"([^\"]+)\"").matcher(arrayTarget.group(1));
                while (quoted.find()) {
                    targets.add(quoted.group(1));
                }
            }
            Matcher classLiteral = CLASS_LITERAL.matcher(body);
            while (classLiteral.find()) {
                String simpleOrFq = classLiteral.group(1);
                if (simpleOrFq.contains(".")) {
                    targets.add(simpleOrFq);
                } else if (imports.containsKey(simpleOrFq)) {
                    targets.add(imports.get(simpleOrFq));
                } else {
                    targets.add(simpleOrFq);
                }
            }
        }
        return targets;
    }
}
