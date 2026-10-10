package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketLotLineLayoutTest {
    @Test
    void keepsOneLineWhenItFits() {
        List<String> lines = MarketLotLineLayout.wrap(
                List.of("Доступно: 248 шт", "1 шт = 3", "За сделку: 10 шт"),
                80,
                String::length
        );
        assertEquals(1, lines.size());
        assertEquals("Доступно: 248 шт · 1 шт = 3 · За сделку: 10 шт", lines.getFirst());
    }

    @Test
    void wrapsWithoutDroppingNumbers() {
        List<String> lines = MarketLotLineLayout.wrap(
                List.of("Доступно: 248 шт", "1 шт = 3", "За сделку: 10 шт", "Mawlee"),
                22,
                String::length
        );
        assertEquals(2, lines.size());
        assertEquals(true, lines.getFirst().contains("248"));
        assertEquals(true, lines.get(1).contains("10") || lines.getFirst().contains("10"));
    }
}
