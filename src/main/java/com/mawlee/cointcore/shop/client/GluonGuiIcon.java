package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.CointCore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 16×16 {@code textures/gui/gluon.png}, drawn at {@link #SIZE} pixels.
 * Replace that file with the final art; this class does not crop a sprite atlas.
 */
public final class GluonGuiIcon {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "textures/gui/gluon.png");
    public static final int TEXTURE_SIZE = 16;
    public static final int SIZE = 8;

    private GluonGuiIcon() {
    }

    public static void blit(GuiGraphics graphics, int x, int y) {
        graphics.blit(TEXTURE, x, y, 0.0f, 0.0f, SIZE, SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
    }
}
