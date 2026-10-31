package com.mawlee.cointcore.refinedstorage;

import com.google.gson.JsonPrimitive;
import com.mawlee.cointcore.refinedstorage.wireless.PinBoard;
import com.mawlee.cointcore.refinedstorage.wireless.PinBoardJson;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static com.mawlee.cointcore.refinedstorage.wireless.PinBoard.Section.*;
import static org.junit.jupiter.api.Assertions.*;

class PinBoardJsonTest {
    @Test void savedProfilesKeepEmptyRowsHolesBothSectionsAndPositions() {
        var board = new PinBoard<String>();
        board.addRow(MANUAL); board.addRow(MANUAL); board.addRow(CRAFTING);
        board.set(MANUAL, 12, "named component-bearing item"); board.set(CRAFTING, 8, "fluid");
        var json = PinBoardJson.write(board, JsonPrimitive::new);
        var loaded = PinBoardJson.read(json, value -> Optional.of(value.getAsString()));
        assertEquals(board.cells(MANUAL), loaded.cells(MANUAL));
        assertEquals(board.cells(CRAFTING), loaded.cells(CRAFTING));
    }
    @Test void missingResourcesLeaveTheirCellEmptyInsteadOfShiftingFollowingPins() {
        var board = new PinBoard<String>(); board.addRow(MANUAL); board.addManual("removed mod"); board.addManual("known");
        var loaded = PinBoardJson.read(PinBoardJson.write(board, JsonPrimitive::new),
                value -> value.getAsString().equals("known") ? Optional.of("known") : Optional.empty());
        assertEquals(1, loaded.rows(MANUAL)); assertNull(loaded.cells(MANUAL).get(0));
        assertEquals("known", loaded.cells(MANUAL).get(1));
    }
    @Test void malformedOrUnknownProfileIsRejectedRatherThanSilentlyOverwritten() {
        var json = PinBoardJson.write(new PinBoard<String>(), JsonPrimitive::new);
        json.addProperty("version", 999);
        assertThrows(IllegalArgumentException.class, () -> PinBoardJson.read(json, value -> Optional.of(value.getAsString())));
    }
    @Test void hiddenRowsAndCraftOverflowSurviveSavingAndReopening() {
        var board = new PinBoard<String>();
        board.addRow(MANUAL); board.set(MANUAL, 7, "manual"); board.removeRow(MANUAL);
        board.addRow(CRAFTING);
        for (int i = 0; i < 12; i++) board.rememberCraft("order" + i);
        var loaded = PinBoardJson.read(PinBoardJson.write(board, JsonPrimitive::new), value -> Optional.of(value.getAsString()));
        assertEquals(0, loaded.rows(MANUAL)); assertEquals(1, loaded.rows(CRAFTING));
        loaded.addRow(MANUAL); loaded.addRow(CRAFTING);
        assertEquals("manual", loaded.cells(MANUAL).get(7));
        assertEquals("order11", loaded.cells(CRAFTING).get(11));
    }
    @Test void versionOneProfilesKeepTheirExistingRowsAndPins() {
        var board = new PinBoard<String>(); board.addRow(MANUAL); board.set(MANUAL, 4, "old pin");
        var json = PinBoardJson.write(board, JsonPrimitive::new);
        json.addProperty("version", 1); json.remove("manual_visible_rows"); json.remove("crafting_visible_rows");
        var loaded = PinBoardJson.read(json, value -> Optional.of(value.getAsString()));
        assertEquals(1, loaded.rows(MANUAL)); assertEquals("old pin", loaded.cells(MANUAL).get(4));
    }
}
