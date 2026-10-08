package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.CointCore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code textures/gui/gluon.png} (32×32, the site's gluon icon), scaled down to
 * {@link #SIZE}×{@link #SIZE} GUI pixels so it stays sharp at GUI scale 2–4.
 */
public final class GluonGuiIcon {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "textures/gui/gluon.png");
    public static final int TEXTURE_SIZE = 32;
    public static final int SIZE = 8;

    private GluonGuiIcon() {
    }

    public static void blit(GuiGraphics graphics, int x, int y) {
        // Whole texture into SIZE×SIZE (the 9-arg overload would crop a SIZE×SIZE corner instead).
        graphics.blit(TEXTURE, x, y, SIZE, SIZE, 0.0f, 0.0f, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
    }
}
