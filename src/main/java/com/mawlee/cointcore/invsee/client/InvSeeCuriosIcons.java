package com.mawlee.cointcore.invsee.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;

@OnlyIn(Dist.CLIENT)
public final class InvSeeCuriosIcons {
    private InvSeeCuriosIcons() {
    }

    public static ResourceLocation icon(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        return CuriosApi.getSlot(identifier, minecraft.level)
                .map(ISlotType::getIcon)
                .orElse(null);
    }
}
