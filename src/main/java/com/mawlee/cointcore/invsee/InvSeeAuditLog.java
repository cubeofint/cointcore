package com.mawlee.cointcore.invsee;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Human-readable InvSee audit log under {@code logs/cointcore-invsee/}.
 */
public final class InvSeeAuditLog {
    private static final Logger LOGGER = LogUtils.getLogger();

    private InvSeeAuditLog() {
    }

    public static void bind(MinecraftServer server) {
        InvSeeAuditSink.bind(server.getServerDirectory().resolve("logs").resolve("cointcore-invsee"),
                (file, exception) -> LOGGER.error("Failed to write InvSee audit log {}", file, exception));
    }

    static void bindForTest(Path logDirectory) {
        InvSeeAuditSink.bind(logDirectory, null);
    }

    public static void opened(
            String viewer,
            String target,
            InvSeeSection section,
            boolean online,
            boolean editCapable
    ) {
        InvSeeAuditSink.write(InvSeeAuditLines.opened(viewer, target, section, online, editCapable));
    }

    public static void closed(String viewer, String target, InvSeeSection section) {
        InvSeeAuditSink.write(InvSeeAuditLines.closed(viewer, target, section));
    }

    public static void editMode(String viewer, String target, boolean enabled) {
        InvSeeAuditSink.write(InvSeeAuditLines.editMode(viewer, target, enabled));
    }

    public static void slotChange(
            String viewer,
            String target,
            InvSeeSection section,
            int slot,
            String from,
            String to
    ) {
        InvSeeAuditSink.write(InvSeeAuditLines.slotChange(viewer, target, section, slot, from, to));
    }

    public static void targetOnline(UUID targetId, String targetName) {
        InvSeeAuditSink.write(InvSeeAuditLines.targetOnline(targetId, targetName));
    }

    public static void targetOffline(UUID targetId, String targetName) {
        InvSeeAuditSink.write(InvSeeAuditLines.targetOffline(targetId, targetName));
    }
}
