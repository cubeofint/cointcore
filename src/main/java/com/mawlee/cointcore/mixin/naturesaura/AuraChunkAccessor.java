package com.mawlee.cointcore.mixin.naturesaura;

import de.ellpeck.naturesaura.chunk.AuraChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AuraChunk.class, remap = false)
public interface AuraChunkAccessor {
    @Accessor("chunk")
    LevelChunk cointcore$getChunk();
}
