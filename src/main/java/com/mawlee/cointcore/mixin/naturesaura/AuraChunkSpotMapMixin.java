package com.mawlee.cointcore.mixin.naturesaura;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import de.ellpeck.naturesaura.api.misc.ILevelData;
import de.ellpeck.naturesaura.chunk.AuraChunk;
import de.ellpeck.naturesaura.misc.LevelData;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * C2ME deserializes chunk attachments on worker threads. Nature's Aura then mutates the
 * shared unsynchronized {@link LevelData#auraChunksWithSpots} FastUtil map, which rehashes
 * into {@link ArrayIndexOutOfBoundsException} and NeoForge drops {@code naturesaura:aura_chunk}.
 */
@Mixin(value = AuraChunk.class, remap = false)
public abstract class AuraChunkSpotMapMixin {

    @WrapMethod(method = "addOrRemoveAsActive", remap = false)
    private void cointcore$syncAuraSpotMap(Operation<Void> original) {
        Long2ObjectMap<AuraChunk> map = cointcore$auraSpotMapOrNull();
        if (map == null) {
            original.call();
            return;
        }
        synchronized (map) {
            original.call();
        }
    }

    @Unique
    private Long2ObjectMap<AuraChunk> cointcore$auraSpotMapOrNull() {
        LevelChunk chunk = ((AuraChunkAccessor) this).cointcore$getChunk();
        if (chunk == null) {
            return null;
        }
        Level level = chunk.getLevel();
        if (level == null) {
            return null;
        }
        ILevelData raw = ILevelData.getLevelData(level);
        if (!(raw instanceof LevelData data)) {
            return null;
        }
        return data.auraChunksWithSpots;
    }
}
