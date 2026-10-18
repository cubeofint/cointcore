package com.mawlee.cointcore.mixin.naturesaura;

import com.mawlee.cointcore.spawner.MachineSpawnLoot;
import com.mawlee.cointcore.spawner.VirtualSpawnContext;
import de.ellpeck.naturesaura.blocks.tiles.BlockEntityAnimalSpawner;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Animal Spawner {@code tick} is spawn-only; wrapping it to skip finalize is safe.
 * If this tile later finalizes unrelated mobs in the same tick, keep finalize
 * (set {@code skip_finalize_mob_spawn} false) — loot conversion still applies.
 */
@Mixin(value = BlockEntityAnimalSpawner.class, remap = false)
public abstract class AnimalSpawnerLootMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void cointcore$enterVirtual(CallbackInfo ci) {
        VirtualSpawnContext.enter();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void cointcore$exitVirtual(CallbackInfo ci) {
        VirtualSpawnContext.exit();
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
            ),
            remap = true
    )
    private boolean cointcore$lootInsteadOfMob(Level level, Entity entity) {
        return MachineSpawnLoot.captureFresh(level, entity, ((BlockEntity) (Object) this).getBlockPos());
    }
}
