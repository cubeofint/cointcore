package com.mawlee.cointcore.mixin.rftoolsutility;

import com.mawlee.cointcore.spawner.MachineSpawnLoot;
import com.mawlee.cointcore.spawner.VirtualSpawnContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mcjty.rftoolsutility.modules.spawner.blocks.SpawnerTileEntity", remap = false)
public abstract class SpawnerTileLootMixin {

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void cointcore$enterVirtual(CallbackInfo ci) {
        VirtualSpawnContext.enter();
    }

    @Inject(method = "tickServer", at = @At("RETURN"))
    private void cointcore$exitVirtual(CallbackInfo ci) {
        VirtualSpawnContext.exit();
    }

    @Redirect(
            method = "tickServer",
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
