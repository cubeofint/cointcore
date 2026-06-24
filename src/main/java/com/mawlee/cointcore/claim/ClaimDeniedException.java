package com.mawlee.cointcore.claim;

import net.minecraft.core.BlockPos;

public class ClaimDeniedException extends RuntimeException {
    private final BlockPos pos;

    public ClaimDeniedException(BlockPos pos) {
        super("Claim denied at " + pos.toShortString());
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }
}
