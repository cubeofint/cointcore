package com.mawlee.cointcore.invsee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InvSeeChromeLayoutTest {
    @Test
    void editButtonSitsOutsidePanel() {
        int leftPos = 100;
        int imageWidth = 176;
        int buttonX = InvSeeChromeLayout.editButtonX(leftPos, imageWidth);
        assertEquals(leftPos + imageWidth + 2, buttonX);
        int titleWidth = InvSeeChromeLayout.titleMaxWidth(imageWidth);
        assertFalse(InvSeeChromeLayout.titleOverlapsEditButton(
                InvSeeChromeLayout.TITLE_X,
                6,
                titleWidth,
                9,
                buttonX - leftPos,
                InvSeeChromeLayout.editButtonY(0),
                InvSeeChromeLayout.EDIT_BUTTON_WIDTH,
                InvSeeChromeLayout.EDIT_BUTTON_HEIGHT
        ));
    }
}
