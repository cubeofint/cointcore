package com.mawlee.cointcore.refinedstorage;

import com.mawlee.cointcore.refinedstorage.wireless.PinBoard;
import org.junit.jupiter.api.Test;
import static com.mawlee.cointcore.refinedstorage.wireless.PinBoard.Section.*;
import static org.junit.jupiter.api.Assertions.*;

class PinBoardTest {
    @Test void eachClickAddsOrRemovesExactlyOneNineCellRowInItsOwnSection() {
        var board = new PinBoard<String>();
        board.addRow(MANUAL); board.addRow(MANUAL); board.addRow(CRAFTING);
        assertEquals(2, board.rows(MANUAL)); assertEquals(1, board.rows(CRAFTING));
        assertEquals(18, board.cells(MANUAL).size());
        assertTrue(board.cells(MANUAL).stream().allMatch(java.util.Objects::isNull));
        board.removeRow(MANUAL);
        assertEquals(1, board.rows(MANUAL)); assertEquals(1, board.rows(CRAFTING));
        board.removeRow(CRAFTING); board.removeRow(CRAFTING);
        assertEquals(0, board.rows(CRAFTING));
    }
    @Test void clearingAndReplacingManualGhostsNeverCompactsOtherCells() {
        var board = new PinBoard<String>(); board.addRow(MANUAL);
        board.set(MANUAL, 0, "cable"); board.set(MANUAL, 5, "processor");
        board.set(MANUAL, 0, null);
        assertNull(board.cells(MANUAL).get(0)); assertEquals("processor", board.cells(MANUAL).get(5));
        board.addManual("hatch"); assertEquals("hatch", board.cells(MANUAL).get(0));
        board.set(MANUAL, 5, "machine"); assertEquals("machine", board.cells(MANUAL).get(5));
    }
    @Test void craftOverflowIsKeptWithoutAutomaticallyCreatingVisibleRows() {
        var board = new PinBoard<Integer>();
        board.addRow(CRAFTING);
        for (int i = 0; i < 1000; i++) board.rememberCraft(i);
        assertEquals(1, board.rows(CRAFTING));
        assertEquals(9, board.cells(CRAFTING).size());
        for (int i = 0; i < 9; i++) assertEquals(i, board.cells(CRAFTING).get(i));
        for (int i = 1; i < 112; i++) board.addRow(CRAFTING);
        assertEquals(112, board.rows(CRAFTING));
        for (int i = 0; i < 1000; i++) assertEquals(i, board.cells(CRAFTING).get(i));
        assertEquals(0, board.rows(MANUAL));
    }
    @Test void repeatingAnOrderKeepsItsPositionAndUnpinningLeavesAHoleForTheNextOrder() {
        var board = new PinBoard<String>();
        board.addRow(CRAFTING);
        board.rememberCraft("one"); board.rememberCraft("two");
        long version = board.version(); board.rememberCraft("one");
        assertEquals(version, board.version());
        board.set(CRAFTING, 0, null); board.rememberCraft("three");
        assertEquals("three", board.cells(CRAFTING).get(0)); assertEquals("two", board.cells(CRAFTING).get(1));
    }
    @Test void noArtificialRowLimitAndRemovingLastRowDoesNotTouchEarlierPins() {
        var board = new PinBoard<String>();
        for (int row = 0; row < 300; row++) board.addRow(MANUAL);
        board.set(MANUAL, 0, "first"); board.set(MANUAL, 2699, "last");
        board.removeRow(MANUAL);
        assertEquals(299, board.rows(MANUAL)); assertEquals("first", board.cells(MANUAL).get(0));
        assertFalse(board.cells(MANUAL).contains("last"));
        board.addRow(MANUAL);
        assertEquals("last", board.cells(MANUAL).get(2699));
    }
    @Test void ordersWithoutRowsAndPinsInHiddenRowsSurviveUntilManuallyRevealed() {
        var board = new PinBoard<String>();
        board.rememberCraft("order");
        assertEquals(0, board.rows(CRAFTING));
        board.addRow(CRAFTING);
        assertEquals("order", board.cells(CRAFTING).get(0));
        board.addRow(MANUAL); board.set(MANUAL, 6, "machine"); board.removeRow(MANUAL);
        board.addManual("cable");
        assertEquals(0, board.rows(MANUAL));
        board.addRow(MANUAL);
        assertEquals("cable", board.cells(MANUAL).get(0));
        assertEquals("machine", board.cells(MANUAL).get(6));
    }
    @Test void staleVisibleCellsAfterRowRemovalCannotCrashOrCreateHiddenPins() {
        var board = new PinBoard<String>(); board.addRow(MANUAL); board.removeRow(MANUAL);
        long version = board.version(); board.set(MANUAL, 8, "stale"); board.set(MANUAL, -1, "invalid");
        assertEquals(version, board.version()); assertTrue(board.cells(MANUAL).isEmpty());
    }
}
