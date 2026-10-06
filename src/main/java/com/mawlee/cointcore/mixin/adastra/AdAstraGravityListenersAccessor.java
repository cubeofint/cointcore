package com.mawlee.cointcore.mixin.adastra;

import earth.terrarium.adastra.api.events.AdAstraEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = AdAstraEvents.class, remap = false)
public interface AdAstraGravityListenersAccessor {
    @Accessor("ENTITY_GRAVITY_LISTENERS")
    static List<AdAstraEvents.EntityGravityEvent> cointcore$gravityListeners() {
        throw new AssertionError();
    }
}
