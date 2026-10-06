package com.mawlee.cointcore.spawner;

import com.mawlee.cointcore.config.SpawnerByproductConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Deposits farm-spawner XP and EvilCraft blood into adjacent handlers.
 * A matching tank takes the fluid. Otherwise XP becomes experience gems and blood becomes condensed blood.
 */
public final class SpawnerByproductService {
    private SpawnerByproductService() {
    }

    public static void tryDepositAll(ServerLevel level, BlockPos spawnerPos, Mob mob) {
        if (!SpawnerByproductConfig.isEnabled()) {
            return;
        }
        tryDepositXp(level, spawnerPos, mob);
        tryDepositBlood(level, spawnerPos, mob);
    }

    public static void tryDepositXp(ServerLevel level, BlockPos spawnerPos, Mob mob) {
        Player killer = createFakeKiller(level, spawnerPos);
        int xp = Math.max(0, mob.getExperienceReward(level, killer));
        if (xp <= 0) {
            return;
        }

        Fluid xpFluid = resolveFluid(SpawnerByproductConfig.getXpFluidId());
        int mbPerPoint = SpawnerByproductConfig.getXpMbPerPoint();
        if (xpFluid != Fluids.EMPTY && mbPerPoint > 0 && hasAcceptingFluidHandler(level, spawnerPos, xpFluid)) {
            long totalMb = (long) xp * (long) mbPerPoint;
            int toFill = totalMb > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalMb;
            fillAdjacent(level, spawnerPos, xpFluid, toFill);
            return;
        }

        Item xpItem = resolveItem(SpawnerByproductConfig.getXpItemId());
        int itemValue = SpawnerByproductConfig.getXpItemValue();
        if (xpItem == Items.AIR || itemValue <= 0) {
            return;
        }

        int count = xp / itemValue;
        depositItemCount(level, spawnerPos, xpItem, count);
    }

    public static void tryDepositBlood(ServerLevel level, BlockPos spawnerPos, Mob mob) {
        int amount = rollBloodMb(level, mob);
        if (amount <= 0) {
            return;
        }

        Fluid blood = resolveFluid(SpawnerByproductConfig.getBloodFluidId());
        if (blood != Fluids.EMPTY && hasAcceptingFluidHandler(level, spawnerPos, blood)) {
            fillAdjacent(level, spawnerPos, blood, amount);
            return;
        }

        Item bloodItem = resolveItem(SpawnerByproductConfig.getBloodItemId());
        int perItem = SpawnerByproductConfig.getBloodItemMb();
        if (bloodItem == Items.AIR || perItem <= 0) {
            return;
        }

        depositItemCount(level, spawnerPos, bloodItem, amount / perItem);
    }

    private static int rollBloodMb(ServerLevel level, Mob mob) {
        float maxHealth = mob.getMaxHealth();
        int minMb = Mth.floor(maxHealth * SpawnerByproductConfig.getBloodMinMultiplier());
        int maxMb = Mth.floor(maxHealth * SpawnerByproductConfig.getBloodMaxMultiplier());
        if (maxMb < minMb) {
            maxMb = minMb;
        }
        if (maxMb <= 0) {
            return 0;
        }
        if (minMb >= maxMb) {
            return minMb;
        }
        return minMb + level.random.nextInt(maxMb - minMb + 1);
    }

    public static boolean hasAcceptingAdjacentFluid(ServerLevel level, BlockPos spawnerPos, Fluid fluid) {
        return hasAcceptingFluidHandler(level, spawnerPos, fluid);
    }

    /**
     * True when blood and/or XP fluid can accept at least 1 mB in an adjacent handler.
     */
    public static boolean hasFluidByproductSink(ServerLevel level, BlockPos spawnerPos) {
        if (!SpawnerByproductConfig.isEnabled()) {
            return false;
        }
        Fluid blood = resolveFluid(SpawnerByproductConfig.getBloodFluidId());
        if (blood != Fluids.EMPTY && hasAcceptingFluidHandler(level, spawnerPos, blood)) {
            return true;
        }
        Fluid xpFluid = resolveFluid(SpawnerByproductConfig.getXpFluidId());
        return xpFluid != Fluids.EMPTY
                && SpawnerByproductConfig.getXpMbPerPoint() > 0
                && hasAcceptingFluidHandler(level, spawnerPos, xpFluid);
    }

    private static boolean hasAcceptingFluidHandler(ServerLevel level, BlockPos spawnerPos, Fluid fluid) {
        FluidStack probe = new FluidStack(fluid, 1);
        for (Direction direction : Direction.values()) {
            IFluidHandler handler = getAdjacentFluidHandler(level, spawnerPos, direction);
            if (handler == null) {
                continue;
            }
            if (handler.fill(probe, IFluidHandler.FluidAction.SIMULATE) > 0) {
                return true;
            }
        }
        return false;
    }

    private static void fillAdjacent(ServerLevel level, BlockPos spawnerPos, Fluid fluid, int amount) {
        int remaining = amount;
        for (Direction direction : Direction.values()) {
            if (remaining <= 0) {
                return;
            }
            IFluidHandler handler = getAdjacentFluidHandler(level, spawnerPos, direction);
            if (handler == null) {
                continue;
            }
            int filled = handler.fill(new FluidStack(fluid, remaining), IFluidHandler.FluidAction.EXECUTE);
            remaining -= filled;
        }
    }

    private static void depositItemCount(ServerLevel level, BlockPos spawnerPos, Item item, int count) {
        if (item == Items.AIR || count <= 0) {
            return;
        }
        int maxStack = Math.max(1, item.getDefaultMaxStackSize());
        int remaining = count;
        while (remaining > 0) {
            int piece = Math.min(remaining, maxStack);
            depositItems(level, spawnerPos, new ItemStack(item, piece));
            remaining -= piece;
        }
    }

    private static void depositItems(ServerLevel level, BlockPos spawnerPos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack remaining = stack.copy();
        for (Direction direction : Direction.values()) {
            if (remaining.isEmpty()) {
                return;
            }
            IItemHandler handler = getAdjacentItemHandler(level, spawnerPos, direction);
            if (handler == null) {
                continue;
            }
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                remaining = handler.insertItem(slot, remaining, false);
                if (remaining.isEmpty()) {
                    return;
                }
            }
        }
        // Overflow XP items are voided — never dropped as ItemEntity.
    }

    private static IFluidHandler getAdjacentFluidHandler(ServerLevel level, BlockPos spawnerPos, Direction direction) {
        return SpawnerAdjacentCaps.fluidHandler(level, spawnerPos, direction);
    }

    private static IItemHandler getAdjacentItemHandler(ServerLevel level, BlockPos spawnerPos, Direction direction) {
        return SpawnerAdjacentCaps.itemHandler(level, spawnerPos, direction);
    }

    private static Fluid resolveFluid(net.minecraft.resources.ResourceLocation id) {
        Fluid fluid = BuiltInRegistries.FLUID.get(id);
        return fluid != null ? fluid : Fluids.EMPTY;
    }

    private static Item resolveItem(net.minecraft.resources.ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return item != null ? item : Items.AIR;
    }

    private static FakePlayer createFakeKiller(ServerLevel level, BlockPos spawnerPos) {
        FakePlayer fakePlayer = FakePlayerFactory.getMinecraft(level);
        fakePlayer.setPos(
                spawnerPos.getX() + 0.5D,
                spawnerPos.getY(),
                spawnerPos.getZ() + 0.5D
        );
        return fakePlayer;
    }
}
