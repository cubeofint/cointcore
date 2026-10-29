package com.mawlee.cointcore.oiia;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Static decoration (no physics, no AI). Spinning and the looping song are client-side, driven by {@link #PLAYING}.
 * Only the player who placed it (or an operator) can take it back.
 */
public class OiiaCatEntity extends Entity {
    private static final EntityDataAccessor<Boolean> PLAYING =
            SynchedEntityData.defineId(OiiaCatEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID owner;

    public OiiaCatEntity(EntityType<? extends OiiaCatEntity> type, Level level) {
        super(type, level);
    }

    public boolean isPlaying() {
        return entityData.get(PLAYING);
    }

    public void setPlaying(boolean playing) {
        entityData.set(PLAYING, playing);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PLAYING, true);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            OiiaCatClientHooks.tick(this);
        }
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return false;
        }
        if (!(source.getEntity() instanceof Player player) || !mayManage(player)) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            spawnAtLocation(new ItemStack(OiiaCat.ITEM.get()));
        }
        playSound(SoundEvents.CAT_HURT, 0.6F, 1.0F);
        discard();
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            setPlaying(!isPlaying());
            if (!isPlaying()) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.CAT_AMBIENT, SoundSource.NEUTRAL, 0.6F, 1.0F);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private boolean mayManage(Player player) {
        return owner == null || owner.equals(player.getUUID()) || player.hasPermissions(2);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(OiiaCat.ITEM.get());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Playing")) {
            setPlaying(tag.getBoolean("Playing"));
        }
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Playing", isPlaying());
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }
}
