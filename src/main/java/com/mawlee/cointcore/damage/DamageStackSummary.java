package com.mawlee.cointcore.damage;

import java.util.LinkedHashSet;
import java.util.Set;

public final class DamageStackSummary {
    private DamageStackSummary() {
    }

    public static String of(StackTraceElement[] stack) {
        Set<String> packages = new LinkedHashSet<>();
        for (StackTraceElement frame : stack) {
            String className = frame.getClassName();
            if (className.startsWith("java.")
                    || className.startsWith("javax.")
                    || className.startsWith("jdk.")
                    || className.startsWith("sun.")
                    || className.startsWith("com.sun.")
                    || className.contains(".mixin.")
                    || className.startsWith("com.mawlee.cointcore.damage.")) {
                continue;
            }
            int last = className.lastIndexOf('.');
            if (last <= 0) {
                continue;
            }
            packages.add(className.substring(0, last));
            if (packages.size() >= 6) {
                break;
            }
        }
        return String.join(" | ", packages);
    }
}
