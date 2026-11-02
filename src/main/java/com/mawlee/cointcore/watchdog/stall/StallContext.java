package com.mawlee.cointcore.watchdog.stall;

import com.mawlee.cointcore.watchdog.PublishedWatchdogContext;
import com.mawlee.cointcore.watchdog.StackSampler;
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

/** Single server-thread publisher, with consistent copies for the stall watchdog thread. */
public final class StallContext {
    public static final int KIND_NONE = 0;
    public static final int KIND_ENTITY = 1;
    public static final int KIND_BLOCK_ENTITY = 2;
    public static final int KIND_DAMAGE = 3;
    public static final int KIND_TEST = 4;

    private static final PublishedWatchdogContext CONTEXT = new PublishedWatchdogContext();

    private StallContext() { }

    public static void enterEntity(Entity entity) {
        if (entity == null) return;
        var pos = entity.blockPosition();
        CONTEXT.enter(KIND_ENTITY, entity.getId(), entity.getType(), entity.level().dimension(),
                pos.getX(), pos.getY(), pos.getZ(), "", "");
        if (StackSampler.isCapturing()) {
            TickProbe.enter("entity", entity.getType(), entity.level().dimension(), pos.getX(), pos.getY(), pos.getZ());
        }
    }

    public static void exitEntity(Entity entity) {
        if (entity != null && CONTEXT.isCurrent(KIND_ENTITY, entity.getId())) CONTEXT.exit();
        TickProbe.exit();
    }

    public static void enterBlockEntity(BlockEntity blockEntity) {
        Level level = blockEntity == null ? null : blockEntity.getLevel();
        if (level == null) return;
        BlockPos pos = blockEntity.getBlockPos();
        CONTEXT.enter(KIND_BLOCK_ENTITY, 0, blockEntity.getType(), level.dimension(),
                pos.getX(), pos.getY(), pos.getZ(), "", "");
        if (StackSampler.isCapturing()) {
            TickProbe.enter("block_entity", blockEntity.getType(), level.dimension(), pos.getX(), pos.getY(), pos.getZ());
        }
    }

    public static void exitBlockEntity() {
        if (CONTEXT.isCurrent(KIND_BLOCK_ENTITY, 0)) CONTEXT.exit();
        TickProbe.exit();
    }

    public static void enterDamage(LivingEntity target, DamageSource source) {
        if (target == null) return;
        String actor = "", weaponId = "";
        Entity direct = source == null ? null : source.getEntity();
        if (direct instanceof Player player) {
            actor = player.getGameProfile().getName();
            ItemStack weapon = player.getMainHandItem();
            weaponId = weapon.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(weapon.getItem()).toString();
        } else if (direct != null) {
            actor = BuiltInRegistries.ENTITY_TYPE.getKey(direct.getType()).toString();
        }
        var pos = target.blockPosition();
        CONTEXT.enter(KIND_DAMAGE, target.getId(), target.getType(), target.level().dimension(),
                pos.getX(), pos.getY(), pos.getZ(), actor, weaponId);
        if (StackSampler.isCapturing()) {
            TickProbe.enter("damage", target.getType(), target.level().dimension(), pos.getX(), pos.getY(), pos.getZ());
        }
    }

    public static void exitDamage() {
        if (CONTEXT.isCurrentKind(KIND_DAMAGE)) CONTEXT.exit();
        TickProbe.exit();
    }

    public static void enterTest() {
        CONTEXT.enter(KIND_TEST, 0, "stall-test", "", 0, 0, 0, "op", "");
    }

    public static void clear() {
        CONTEXT.clear();
        TickProbe.clear();
    }

    public static PublishedWatchdogContext.Snapshot snapshot() { return CONTEXT.snapshot(); }
    public static int kind() { return snapshot().kind(); }
    public static int entityId() { return snapshot().entityId(); }
    public static int x() { return snapshot().x(); }
    public static int y() { return snapshot().y(); }
    public static int z() { return snapshot().z(); }
    public static String typeId() { return WatchdogWorldContext.LABELS.typeId(snapshot().type()); }
    public static String dimension() { return WatchdogWorldContext.LABELS.dimensionId(snapshot().dimension()); }
    public static String actor() { return snapshot().actor(); }
    public static String weaponId() { return snapshot().weapon(); }

    public static String describe() {
        var context = snapshot();
        if (context.kind() == KIND_NONE) {
            var probe = TickProbe.snapshot();
            return probe.active() ? probe.kind() + " " + probe.typeId() : "unknown";
        }
        if (context.kind() == KIND_TEST) return "stall-test command";
        String type = WatchdogWorldContext.LABELS.typeId(context.type());
        String pos = WatchdogWorldContext.LABELS.dimensionId(context.dimension()) + " "
                + context.x() + "," + context.y() + "," + context.z();
        return switch (context.kind()) {
            case KIND_ENTITY -> "entity " + type + " #" + context.entityId() + " @ " + pos;
            case KIND_BLOCK_ENTITY -> "block_entity " + type + " @ " + pos;
            case KIND_DAMAGE -> "damage " + type + " #" + context.entityId() + " by "
                    + context.actor() + " " + context.weapon() + " @ " + pos;
            default -> "unknown";
        };
    }
}
