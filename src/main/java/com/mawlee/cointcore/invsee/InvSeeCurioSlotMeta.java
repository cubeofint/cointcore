package com.mawlee.cointcore.invsee;

/**
 * One real Curios slot: type id, handler index, cosmetic flag.
 */
public record InvSeeCurioSlotMeta(String identifier, int index, boolean cosmetic) {
    public boolean present() {
        return identifier != null && !identifier.isEmpty();
    }
}
