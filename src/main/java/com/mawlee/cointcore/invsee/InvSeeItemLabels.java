package com.mawlee.cointcore.invsee;

public final class InvSeeItemLabels {
    private InvSeeItemLabels() {
    }

    public static String describe(String itemId, int count) {
        if (itemId == null || itemId.isBlank() || "minecraft:air".equals(itemId) || count <= 0) {
            return "empty";
        }
        return itemId + " x" + count;
    }
}
