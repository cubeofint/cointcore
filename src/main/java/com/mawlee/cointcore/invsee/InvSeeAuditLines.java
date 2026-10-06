package com.mawlee.cointcore.invsee;

import java.util.UUID;

public final class InvSeeAuditLines {
    private InvSeeAuditLines() {
    }

    public static String opened(
            String viewer,
            String target,
            InvSeeSection section,
            boolean online,
            boolean editCapable
    ) {
        return viewer + " opened " + target
                + " section=" + section.id()
                + " status=" + (online ? "online" : "offline")
                + " mode=" + (editCapable ? "can-edit" : "view");
    }

    public static String closed(String viewer, String target, InvSeeSection section) {
        return viewer + " closed " + target + " section=" + section.id();
    }

    public static String editMode(String viewer, String target, boolean enabled) {
        return viewer + (enabled ? " edit-on " : " edit-off ") + target;
    }

    public static String slotChange(
            String viewer,
            String target,
            InvSeeSection section,
            int slot,
            String from,
            String to
    ) {
        return viewer + " moved " + target
                + " section=" + section.id()
                + " slot=" + slot
                + " " + from + " -> " + to;
    }

    public static String targetOnline(UUID targetId, String targetName) {
        return "system " + targetName + " joined; InvSee switched to live player (" + targetId + ")";
    }

    public static String targetOffline(UUID targetId, String targetName) {
        return "system " + targetName + " left; InvSee switched to saved data (" + targetId + ")";
    }
}
