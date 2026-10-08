package com.mawlee.cointcore.invsee;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvSeeCurioSlotOrderTest {
    @Test
    void sortsByOrderThenPlacesCosmeticBesideBase() {
        List<InvSeeCurioSlotMeta> placed = InvSeeCurioSlotOrder.flatten(List.of(
                new InvSeeCurioSlotOrder.TypeGroup("ring", 40, 2, true),
                new InvSeeCurioSlotOrder.TypeGroup("head", 10, 1, false),
                new InvSeeCurioSlotOrder.TypeGroup("charm", 30, 1, true)
        ));
        assertEquals(List.of(
                new InvSeeCurioSlotMeta("head", 0, false),
                new InvSeeCurioSlotMeta("charm", 0, false),
                new InvSeeCurioSlotMeta("charm", 0, true),
                new InvSeeCurioSlotMeta("ring", 0, false),
                new InvSeeCurioSlotMeta("ring", 0, true),
                new InvSeeCurioSlotMeta("ring", 1, false),
                new InvSeeCurioSlotMeta("ring", 1, true)
        ), placed);
    }
}
