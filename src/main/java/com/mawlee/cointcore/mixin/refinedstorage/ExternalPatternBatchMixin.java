package com.mawlee.cointcore.mixin.refinedstorage;

import com.mawlee.cointcore.config.RsAutocraftingConfig;
import com.refinedmods.refinedstorage.api.autocrafting.task.ExternalPatternSinkProvider;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskListener;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList;
import com.refinedmods.refinedstorage.api.storage.root.RootStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * RS pushes one processing iteration into a machine per step, so a machine fed by an autocrafter receives one item
 * per step. Right after RS sends its iteration, keep sending further ones in the same step while the machine still
 * accepts them, up to {@code external_iterations_per_step}. Each extra iteration goes through the same RS checks
 * (inputs available, sink accepts, lock modes) and keeps the round-robin over machines.
 */
@Mixin(targets = "com.refinedmods.refinedstorage.api.autocrafting.task.ExternalTaskPattern", remap = false)
public abstract class ExternalPatternBatchMixin {
    @Shadow(remap = false)
    private long iterationsToSendToSink;

    @Shadow(remap = false)
    private boolean acceptsIterationInputs(MutableResourceList internalStorage, ExternalPatternSinkProvider sinkProvider) {
        throw new AssertionError();
    }

    @Inject(
            method = "step",
            at = @At(value = "FIELD",
                    target = "Lcom/refinedmods/refinedstorage/api/autocrafting/task/ExternalTaskPattern;iterationsToSendToSink:J",
                    opcode = org.objectweb.asm.Opcodes.PUTFIELD, shift = At.Shift.AFTER),
            remap = false
    )
    private void cointcore$sendBatch(MutableResourceList internalStorage, RootStorage rootStorage,
                                     ExternalPatternSinkProvider sinkProvider, TaskListener listener,
                                     CallbackInfoReturnable<?> cir) {
        int extra = RsAutocraftingConfig.getExternalIterationsPerStep() - 1;
        while (extra-- > 0 && iterationsToSendToSink > 0 && acceptsIterationInputs(internalStorage, sinkProvider)) {
            iterationsToSendToSink--;
        }
    }
}
