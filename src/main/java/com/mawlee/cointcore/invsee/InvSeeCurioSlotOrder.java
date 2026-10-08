package com.mawlee.cointcore.invsee;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Curios-like layout: slot types by {@code order}, then each index with its cosmetic beside it.
 */
public final class InvSeeCurioSlotOrder {
    private InvSeeCurioSlotOrder() {
    }

    public record TypeGroup(String id, int order, int size, boolean hasCosmetic) {
    }

    public static List<InvSeeCurioSlotMeta> flatten(List<TypeGroup> groups) {
        List<TypeGroup> sorted = new ArrayList<>(groups);
        sorted.sort(Comparator.comparingInt(TypeGroup::order).thenComparing(TypeGroup::id));
        List<InvSeeCurioSlotMeta> out = new ArrayList<>();
        for (TypeGroup group : sorted) {
            for (int index = 0; index < group.size(); index++) {
                out.add(new InvSeeCurioSlotMeta(group.id(), index, false));
                if (group.hasCosmetic()) {
                    out.add(new InvSeeCurioSlotMeta(group.id(), index, true));
                }
            }
        }
        return List.copyOf(out);
    }
}
