package com.mawlee.cointcore.watchdog;

import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IModFile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Builds a package-prefix → mod-id map from loaded mod jars.
 */
public final class WatchdogModIndex {
    private static volatile ClassToModMapper mapper = ClassToModMapper.empty();

    private WatchdogModIndex() {
    }

    public static ClassToModMapper mapper() {
        return mapper;
    }

    public static void rebuildIfPossible() {
        try {
            if (!ModList.get().isLoaded("cointcore")) {
                return;
            }
        } catch (Throwable ignored) {
            return;
        }
        rebuild();
    }

    public static void rebuild() {
        Map<String, String> prefixes = new HashMap<>();
        try {
            for (IModFileInfo fileInfo : ModList.get().getModFiles()) {
                IModFile file = fileInfo.getFile();
                String modId = primaryModId(fileInfo);
                if (modId == null || "minecraft".equals(modId) || "neoforge".equals(modId)) {
                    continue;
                }
                Path path = file.getFilePath();
                if (path == null) {
                    continue;
                }
                scanJar(path, modId, prefixes);
            }
        } catch (Throwable ignored) {
            // optional: mapping stays empty / previous
        }
        mapper = ClassToModMapper.fromPackageMap(prefixes);
    }

    static Map<String, String> scanJarForTest(Path jar, String modId) {
        Map<String, String> prefixes = new HashMap<>();
        scanJar(jar, modId, prefixes);
        return prefixes;
    }

    private static void scanJar(Path path, String modId, Map<String, String> prefixes) {
        if (path == null || !path.toString().endsWith(".jar")) {
            return;
        }
        try (JarFile jar = new JarFile(path.toFile())) {
            jar.stream()
                    .map(JarEntry::getName)
                    .filter(name -> name.endsWith(".class") && !name.contains("$") && !name.startsWith("META-INF/"))
                    .forEach(name -> {
                        String className = name.substring(0, name.length() - 6).replace('/', '.');
                        int lastDot = className.lastIndexOf('.');
                        if (lastDot <= 0) {
                            return;
                        }
                        String pkg = className.substring(0, lastDot);
                        prefixes.merge(pkg, modId, (existing, incoming) ->
                                existing.equals(incoming) ? existing : existing);
                    });
        } catch (Exception ignored) {
            // skip unreadable jars
        }
    }

    private static String primaryModId(IModFileInfo fileInfo) {
        try {
            for (IModInfo info : fileInfo.getMods()) {
                if (info.getModId() != null && !info.getModId().isBlank()) {
                    return info.getModId().toLowerCase(Locale.ROOT);
                }
            }
        } catch (Throwable ignored) {
            return null;
        }
        return null;
    }
}
