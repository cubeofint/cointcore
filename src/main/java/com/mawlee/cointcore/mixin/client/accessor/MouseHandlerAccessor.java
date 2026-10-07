package com.mawlee.cointcore.mixin.client.accessor;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("xpos")
    void cointcore$setXpos(double xpos);

    @Accessor("ypos")
    void cointcore$setYpos(double ypos);

    @Accessor("ignoreFirstMove")
    void cointcore$setIgnoreFirstMove(boolean ignoreFirstMove);
}
