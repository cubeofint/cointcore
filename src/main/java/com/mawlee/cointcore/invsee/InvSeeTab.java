package com.mawlee.cointcore.invsee;

/**
 * Top-level admin GUI tabs. Command-only sections (curios/cosmetic/backpack/pocket)
 * stay available via {@code /invsee} subcommands.
 */
public enum InvSeeTab {
    INVENTORY(InvSeeSection.INVENTORY, "gui.cointcore.invsee.tab.player", 0),
    ENDER(InvSeeSection.ENDER, "gui.cointcore.invsee.tab.ender", 1),
    ACCESSORIES(InvSeeSection.ACCESSORIES, "gui.cointcore.invsee.tab.accessories", 2),
    FTB(InvSeeSection.FTB, "gui.cointcore.invsee.tab.ftb", 3),
    GRAVES(InvSeeSection.GRAVES, "gui.cointcore.invsee.tab.graves", 4),
    STATE(InvSeeSection.STATE, "gui.cointcore.invsee.tab.state", 5);

    private final InvSeeSection section;
    private final String langKey;
    private final int bit;

    InvSeeTab(InvSeeSection section, String langKey, int bit) {
        this.section = section;
        this.langKey = langKey;
        this.bit = bit;
    }

    public InvSeeSection section() {
        return section;
    }

    public String langKey() {
        return langKey;
    }

    public int bit() {
        return bit;
    }

    public int mask() {
        return 1 << bit;
    }

    public static InvSeeTab fromSection(InvSeeSection section) {
        for (InvSeeTab tab : values()) {
            if (tab.section == section) {
                return tab;
            }
        }
        return INVENTORY;
    }

    public static InvSeeTab fromOrdinalOrInventory(int ordinal) {
        InvSeeTab[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return INVENTORY;
        }
        return values[ordinal];
    }
}
