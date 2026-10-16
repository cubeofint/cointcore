package com.mawlee.cointcore.privilege;

import java.util.Locale;

public enum PrivilegeCommandId {
    REPAIR("repair"),
    CHARGE("charge"),
    REPAIR_ALL("repairall"),
    CHARGE_ALL("chargeall"),
    EXP_ADD("expadd");

    private final String key;

    PrivilegeCommandId(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static PrivilegeCommandId fromKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (PrivilegeCommandId id : values()) {
            if (id.key.equals(normalized)) {
                return id;
            }
        }
        return null;
    }
}
