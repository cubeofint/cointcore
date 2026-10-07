package com.mawlee.cointcore.invsee;

public enum InvSeeSection {
    INVENTORY("inventory", true),
    ENDER("ender", true),
    CURIOS("curios", true),
    COSMETIC("cosmetic", true),
    BACKPACK("backpack", true),
    POCKET("pocket", true),
    MODDATA("moddata", true),
    ACCESSORIES("accessories", true),
    STATE("state", false),
    FTB("ftb", false),
    GRAVES("graves", false);

    private final String id;
    private final boolean allowsEdit;

    InvSeeSection(String id, boolean allowsEdit) {
        this.id = id;
        this.allowsEdit = allowsEdit;
    }

    public String id() {
        return id;
    }

    public boolean allowsEdit() {
        return allowsEdit;
    }

    public String viewNode() {
        return "invsee.view." + id;
    }

    public String editNode() {
        return "invsee.edit." + id;
    }
}
