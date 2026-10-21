package com.mawlee.cointcore.watchdog.stall;

import com.mawlee.cointcore.watchdog.TickProbe;
import com.mawlee.cointcore.watchdog.WatchdogWorldContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Cheap volatile slot describing what the server thread is doing.
 * Written on the server thread, read by the stall watchdog thread.
 */
public final class StallContext {
    public static final int KIND_NONE = 0;
    public static final int KIND_ENTITY = 1;
    public static final int KIND_BLOCK_ENTITY = 2;
    public static final int KIND_DAMAGE = 3;
    public static final int KIND_TEST = 4;

    private static volatile int kind;
    private static volatile int entityId;
    private static volatile int x;
    private static volatile int y;
    private static volatile int z;
    /** EntityType / BlockEntityType / String; resolved to an id only when read (hot path stays allocation-free). */
    private static volatile Object typeRef = "";
    /** ResourceKey of the level or a String. */
    private static volatile Object dimensionRef = "";
    private static volatile String actor = "";
    private static volatile String weaponId = "";

    private StallContext() {
    }

    public static void enterEntity(Entity entity) {
        if (entity == null) {
            return;
        }
        kind = KIND_ENTITY;
        entityId = entity.getId();
        x = entity.getBlockX();
        y = entity.getBlockY();
        z = entity.getBlockZ();
        typeRef = entity.getType();
        dimensionRef = entity.level().dimension();
        TickProbe.enter("entity", entity.getType(), entity.level().dimension(), x, y, z);
    }

    public static void exitEntity(Entity entity) {
        if (entity != null && kind == KIND_ENTITY && entityId == entity.getId()) {
            kind = KIND_NONE;
        }
        TickProbe.exit();
    }

    public static void enterBlockEntity(BlockEntity blockEntity) {
        Level level = blockEntity == null ? null : blockEntity.getLevel();
        if (level == null) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        kind = KIND_BLOCK_ENTITY;
        entityId = 0;
        x = pos.getX();
        y = pos.getY();
        z = pos.getZ();
        typeRef = blockEntity.getType();
        dimensionRef = level.dimension();
        TickProbe.enter("block_entity", typeRef, dimensionRef, x, y, z);
    }

    public static void exitBlockEntity() {
        if (kind == KIND_BLOCK_ENTITY) {
            kind = KIND_NONE;
        }
        TickProbe.exit();
    }

    public static void enterDamage(LivingEntity target, DamageSource source) {
        if (target == null) {
            return;
        }
        kind = KIND_DAMAGE;
        entityId = target.getId();
        x = target.getBlockX();
        y = target.getBlockY();
        z = target.getBlockZ();
        typeRef = target.getType();
        dimensionRef = target.level().dimension();
        Entity direct = source == null ? null : source.getEntity();
        if (direct instanceof Player player) {
            actor = player.getGameProfile().getName();
            ItemStack weapon = player.getMainHandItem();
            weaponId = weapon.isEmpty()
                    ? ""
                    : BuiltInRegistries.ITEM.getKey(weapon.getItem()).toString();
        } else if (direct != null) {
            actor = BuiltInRegistries.ENTITY_TYPE.getKey(direct.getType()).toString();
            weaponId = "";
        }
        TickProbe.enter("damage", target.getType(), target.level().dimension(), x, y, z);
    }

    public static void exitDamage() {
        if (kind == KIND_DAMAGE) {
            kind = KIND_NONE;
        }
        TickProbe.exit();
    }

    public static void enterTest() {
        kind = KIND_TEST;
        typeRef = "stall-test";
        actor = "op";
        weaponId = "";
        entityId = 0;
    }

    public static void clear() {
        kind = KIND_NONE;
        entityId = 0;
        x = 0;
        y = 0;
        z = 0;
        typeRef = "";
        dimensionRef = "";
        actor = "";
        weaponId = "";
        TickProbe.clear();
    }

    public static int kind() {
        return kind;
    }

    public static int entityId() {
        return entityId;
    }

    public static int x() {
        return x;
    }

    public static int y() {
        return y;
    }

    public static int z() {
        return z;
    }

    public static String typeId() {
        Object type = typeRef;
        return type instanceof String string ? string : WatchdogWorldContext.LABELS.typeId(type);
    }

    public static String dimension() {
        Object level = dimensionRef;
        return level instanceof String string ? string : WatchdogWorldContext.LABELS.dimensionId(level);
    }

    public static String actor() {
        return actor;
    }

    public static String weaponId() {
        return weaponId;
    }

    public static String describe() {
        return switch (kind) {
            case KIND_ENTITY -> "entity " + typeId() + " #" + entityId + " @ " + pos();
            case KIND_BLOCK_ENTITY -> "block_entity " + typeId() + " @ " + pos();
            case KIND_DAMAGE -> "damage " + typeId() + " #" + entityId + " by " + actor + " " + weaponId + " @ " + pos();
            case KIND_TEST -> "stall-test command";
            default -> TickProbe.snapshot().active()
                    ? TickProbe.snapshot().kind() + " " + TickProbe.snapshot().typeId()
                    : "unknown";
        };
    }

    private static String pos() {
        return dimension() + " " + x + "," + y + "," + z;
    }
}
