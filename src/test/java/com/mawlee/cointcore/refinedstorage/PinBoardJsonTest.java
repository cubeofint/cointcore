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
        var board = new PinBoard<String>(); board.addManual("removed mod"); board.addManual("known");
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
}
