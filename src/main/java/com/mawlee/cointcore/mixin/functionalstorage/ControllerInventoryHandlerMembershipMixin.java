package com.mawlee.cointcore.mixin.functionalstorage;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.functionalstorage.ControllerMembership;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Functional Storage 1.5.8 {@code ControllerInventoryHandler} scans the drawer list ({@code List#contains}) on every
 * slot access, so a slot-by-slot insert into a big storage controller is quadratic. Answer from an identity set
 * instead; a miss still asks the list, so the result never differs.
 */
@Pseudo
@Mixin(targets = "com.buuz135.functionalstorage.inventory.ControllerInventoryHandler", remap = false)
public abstract class ControllerInventoryHandlerMembershipMixin {
    @Unique
    private ControllerMembership.Snapshot cointcore$members;

    @WrapOperation(
            method = {"getStackInSlot", "insertItem", "extractItem"},
            at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z"),
            remap = false
    )
    private boolean cointcore$containsFast(List<?> handlers, Object handler, Operation<Boolean> original) {
        ControllerMembership.Snapshot snapshot = ControllerMembership.refresh(cointcore$members, handlers);
        cointcore$members = snapshot;
        return snapshot.members().contains(handler) || original.call(handlers, handler);
    }
}
