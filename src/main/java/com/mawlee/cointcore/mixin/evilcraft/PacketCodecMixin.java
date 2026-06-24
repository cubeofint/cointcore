package com.mawlee.cointcore.mixin.evilcraft;

import org.cyclops.cyclopscore.datastructure.SingleCache;
import org.cyclops.cyclopscore.network.PacketCodec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Исправляет NPE в PacketCodec.loopCodecFields().
 * <p>
 * Проблема: SingleCache.get(null) возвращает null, когда getNewValue()
 * колбэка возвращает null. Последующий вызов .iterator() на null → NPE.
 * <p>
 * Фикс: проверяем результат на null перед итерацией.
 */
@Mixin(value = PacketCodec.class, remap = false)
public abstract class PacketCodecMixin {

    @Shadow(remap = false)
    @Final
    private SingleCache<Void, List<Field>> fieldCache;

    /**
     * Перехватываем loopCodecFields и добавляем null-check.
     */
    @Inject(
        method = "loopCodecFields",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void cointcore$fixLoopCodecFields(PacketCodec.ICodecRunnable runnable, CallbackInfo ci) {
        List<Field> fields = fieldCache.get(null);
        
        // === ГЛАВНЫЙ ФИКС ===
        // Если fields == null, просто пропускаем итерацию вместо NPE
        if (fields == null) {
            ci.cancel();
            return;
        }
        
        // Если fields не null, выполняем оригинальную логику вручную
        for (Field field : fields) {
            Class<?> fieldType = field.getType();
            PacketCodec.ICodecAction action = PacketCodec.getAction(fieldType);
            boolean wasAccessible = field.isAccessible();
            if (!wasAccessible) {
                field.setAccessible(true);
            }
            runnable.run(field, action);
        }
        
        ci.cancel();
    }
}