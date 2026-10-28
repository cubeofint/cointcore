package com.mawlee.cointcore.modularbees;

/** Implemented only on ModularBees' ME hosts when both storage mods are present. */
public interface RsHatchHost {
    RsHatchConnection cointcore$rsHatch();
    boolean isActive();
}
