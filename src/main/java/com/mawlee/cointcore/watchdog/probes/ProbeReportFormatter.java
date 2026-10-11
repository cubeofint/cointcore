package com.mawlee.cointcore.watchdog.probes;

import java.util.List;

/**
 * Appends probe sections to the existing watchdog report text.
 */
public final class ProbeReportFormatter {
    private ProbeReportFormatter() {
    }

    public static String format(List<Section> sections) {
        if (sections == null || sections.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(2048);
        sb.append("--- Расширенные пробы ---").append('\n');
        for (Section section : sections) {
            if (section == null || section.lines().isEmpty()) {
                continue;
            }
            sb.append('\n').append("--- ").append(section.title()).append(" ---").append('\n');
            for (String line : section.lines()) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    public record Section(String title, List<String> lines) {
        public Section {
            lines = lines == null ? List.of() : List.copyOf(lines);
        }
    }
}
