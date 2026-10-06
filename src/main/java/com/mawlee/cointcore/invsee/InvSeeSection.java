package com.mawlee.cointcore.invsee;

public enum InvSeeSection {
    INVENTORY("inventory"),
    ENDER("ender"),
    CURIOS("curios"),
    COSMETIC("cosmetic"),
    BACKPACK("backpack"),
    POCKET("pocket"),
    MODDATA("moddata");

    private final String id;

    InvSeeSection(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String viewNode() {
        return "invsee.view." + id;
    }

    public String editNode() {
        return "invsee.edit." + id;
    }
}
