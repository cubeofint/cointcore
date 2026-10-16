package com.mawlee.cointcore.privilege;

import com.mawlee.cointcore.config.PrivilegeCommandsConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class PrivilegeCommandService {
    private PrivilegeCommandService() {
    }

    public static int repairMainHand(ServerPlayer player) {
        if (!tryConsumeCooldown(player, PrivilegeCommandId.REPAIR)) {
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PRIVILEGE_REPAIR_NOT_DAMAGEABLE));
            return 0;
        }
        if (!ItemChargeRepair.repair(stack)) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PRIVILEGE_REPAIR_ALREADY));
            return 0;
        }
        player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PRIVILEGE_REPAIR_DONE));
        PrivilegeCommandCooldownService.arm(player, PrivilegeCommandId.REPAIR);
        return 1;
    }

    public static int chargeMainHand(ServerPlayer player) {
        if (!tryConsumeCooldown(player, PrivilegeCommandId.CHARGE)) {
            return 0;
        }
        ItemChargeRepair.ChargeResult result = ItemChargeRepair.charge(player.getMainHandItem());
        switch (result) {
            case CHARGED -> {
                player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PRIVILEGE_CHARGE_DONE));
                PrivilegeCommandCooldownService.arm(player, PrivilegeCommandId.CHARGE);
                return 1;
            }
            case ALREADY_FULL -> {
                player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PRIVILEGE_CHARGE_ALREADY));
                return 0;
            }
            case NO_STORAGE -> {
                player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PRIVILEGE_CHARGE_NO_ENERGY));
                return 0;
            }
            default -> throw new IllegalStateException("Unhandled charge result: " + result);
        }
    }

    public static int repairAll(ServerPlayer player) {
        if (!tryConsumeCooldown(player, PrivilegeCommandId.REPAIR_ALL)) {
            return 0;
        }
        int[] repaired = {0};
        PrivilegeItemScope.forEachOwnedItem(player, stack -> {
            if (ItemChargeRepair.repair(stack)) {
                repaired[0]++;
            }
        });
        if (repaired[0] <= 0) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PRIVILEGE_REPAIR_ALL_NONE));
            return 0;
        }
        player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PRIVILEGE_REPAIR_ALL_DONE, repaired[0]));
        PrivilegeCommandCooldownService.arm(player, PrivilegeCommandId.REPAIR_ALL);
        return 1;
    }

    public static int chargeAll(ServerPlayer player) {
        if (!tryConsumeCooldown(player, PrivilegeCommandId.CHARGE_ALL)) {
            return 0;
        }
        int[] charged = {0};
        PrivilegeItemScope.forEachOwnedItem(player, stack -> {
            if (ItemChargeRepair.charge(stack) == ItemChargeRepair.ChargeResult.CHARGED) {
                charged[0]++;
            }
        });
        if (charged[0] <= 0) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PRIVILEGE_CHARGE_ALL_NONE));
            return 0;
        }
        player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PRIVILEGE_CHARGE_ALL_DONE, charged[0]));
        PrivilegeCommandCooldownService.arm(player, PrivilegeCommandId.CHARGE_ALL);
        return 1;
    }

    public static int addExperience(ServerPlayer player, int amount) {
        int maxAmount = PrivilegeCommandsConfig.expAddMaxAmount();
        PrivilegeCommandRules.ExpAddAmountVerdict verdict =
                PrivilegeCommandRules.validateExpAddAmount(amount, maxAmount);
        switch (verdict) {
            case NON_POSITIVE -> {
                player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PRIVILEGE_EXPADD_INVALID));
                return 0;
            }
            case OVER_MAX -> {
                player.sendSystemMessage(CointCoreMessages.forPlayer(
                        player,
                        CointCoreMessages.PRIVILEGE_EXPADD_OVER_MAX,
                        maxAmount
                ));
                return 0;
            }
            case OK -> {
                break;
            }
            default -> throw new IllegalStateException("Unhandled expadd verdict: " + verdict);
        }
        if (!tryConsumeCooldown(player, PrivilegeCommandId.EXP_ADD)) {
            return 0;
        }
        ExpAddUnit unit = PrivilegeCommandsConfig.expAddUnit();
        switch (unit) {
            case POINTS -> player.giveExperiencePoints(amount);
            case LEVELS -> player.giveExperienceLevels(amount);
            default -> throw new IllegalStateException("Unhandled expadd unit: " + unit);
        }
        String unitLabel = CointCoreMessages.translateKey(
                player.getLanguage(),
                unit == ExpAddUnit.LEVELS
                        ? CointCoreMessages.PRIVILEGE_EXPADD_UNIT_LEVELS
                        : CointCoreMessages.PRIVILEGE_EXPADD_UNIT_POINTS
        );
        player.sendSystemMessage(CointCoreMessages.forPlayer(
                player,
                CointCoreMessages.PRIVILEGE_EXPADD_DONE,
                amount,
                unitLabel
        ));
        PrivilegeCommandCooldownService.arm(player, PrivilegeCommandId.EXP_ADD);
        return 1;
    }

    private static boolean tryConsumeCooldown(ServerPlayer player, PrivilegeCommandId commandId) {
        int remaining = PrivilegeCommandCooldownService.remainingSeconds(player, commandId);
        if (remaining <= 0) {
            return true;
        }
        player.sendSystemMessage(CointCoreMessages.forPlayer(
                player,
                CointCoreMessages.PRIVILEGE_COMMAND_COOLDOWN,
                remaining
        ));
        return false;
    }
}
