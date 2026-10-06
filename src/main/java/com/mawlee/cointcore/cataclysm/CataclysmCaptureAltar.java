package com.mawlee.cointcore.cataclysm;

import com.mawlee.cointcore.mixin.accessor.LivingEntityDeadAccessor;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Cataclysm places the resummon block from {@code AfterDefeatBoss}, which runs only inside {@code die()}.
 * Capture and other removals call {@code remove} while the boss is still alive, so the block never appears.
 * Chunk unload uses {@code setRemoved} and does not pass through this hook.
 */
public final class CataclysmCaptureAltar {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BOSS_TYPE = "com.github.L_Ender.cataclysm.entity.etc.Animation_Monsters";
    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("c", "bosses")
    );
    private static final ThreadLocal<Boolean> GUARD = ThreadLocal.withInitial(() -> false);

    private static volatile Method afterDefeatBoss;
    private static volatile boolean resolved;

    private CataclysmCaptureAltar() {
    }

    public static void onRemove(Entity entity, Entity.RemovalReason reason) {
        if (reason != Entity.RemovalReason.KILLED
                && reason != Entity.RemovalReason.DISCARDED
                && reason != Entity.RemovalReason.UNLOADED_TO_CHUNK) {
            return;
        }
        if (!(entity instanceof LivingEntity living) || living.level().isClientSide() || !living.isAddedToLevel()) {
            return;
        }
        if (living.deathTime > 0 || ((LivingEntityDeadAccessor) living).cointcore$isDead()) {
            return;
        }
        if (!ModList.get().isLoaded("cataclysm") || !living.getType().is(BOSSES)) {
            return;
        }
        Method defeat = afterDefeatBoss();
        if (defeat == null || !defeat.getDeclaringClass().isInstance(living) || GUARD.get()) {
            return;
        }
        GUARD.set(true);
        try {
            defeat.invoke(living, living.getKillCredit());
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("Failed to restore the Cataclysm altar for {}", living.getType(), exception);
        } finally {
            GUARD.set(false);
        }
    }

    private static Method afterDefeatBoss() {
        if (resolved) {
            return afterDefeatBoss;
        }
        synchronized (CataclysmCaptureAltar.class) {
            if (resolved) {
                return afterDefeatBoss;
            }
            resolved = true;
            try {
                Class<?> type = Class.forName(BOSS_TYPE);
                Method method = type.getDeclaredMethod("AfterDefeatBoss", LivingEntity.class);
                method.setAccessible(true);
                afterDefeatBoss = method;
            } catch (ReflectiveOperationException exception) {
                LOGGER.warn("Cataclysm AfterDefeatBoss is unavailable", exception);
            }
            return afterDefeatBoss;
        }
    }
}
