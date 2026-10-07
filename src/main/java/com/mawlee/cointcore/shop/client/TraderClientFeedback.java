package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.shop.TraderFeedbackPayload;
import com.mawlee.cointcore.shop.TraderMenu;
import net.minecraft.client.Minecraft;

/**
 * Client-only dispatch for trader GUI status lines. Must not be invoked on a dedicated server.
 */
public final class TraderClientFeedback {
    private TraderClientFeedback() {
    }

    public static void handle(TraderFeedbackPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.containerMenu instanceof TraderMenu menu)) {
            return;
        }
        if (menu.containerId != payload.containerId()) {
            return;
        }
        if (minecraft.screen instanceof TraderScreen screen) {
            screen.setFeedback(payload);
        }
    }
}
