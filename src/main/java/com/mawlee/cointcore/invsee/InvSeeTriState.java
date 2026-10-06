package com.mawlee.cointcore.invsee;

public enum InvSeeTriState {
    TRUE,
    FALSE,
    UNSET;

    public static InvSeeTriState ofBoolean(boolean value) {
        return value ? TRUE : FALSE;
    }
}
