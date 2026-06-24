package com.mawlee.cointcore.privilege;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import org.slf4j.Logger;

public final class FlightSourceService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private FlightSourceService() {
    }

    public static boolean shouldPreserveFlight(ServerPlayer player) {
        return hasFlightMobEffect(player)
                || hasFlightItemEquipped(player)
                || hasFtbEssentialsFly(player);
    }

    public static boolean shouldPreserveGliding(ServerPlayer player) {
        return player.isFallFlying() || player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
    }

    private static boolean hasFlightMobEffect(ServerPlayer player) {
        for (MobEffectInstance instance : player.getActiveEffects()) {
            ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value());
            if (FlightItemMatcher.isFlightEffect(effectId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasFlightItemEquipped(ServerPlayer player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR
                    && slot != EquipmentSlot.MAINHAND
                    && slot != EquipmentSlot.OFFHAND) {
                continue;
            }

            if (FlightItemMatcher.isFlightItem(player.getItemBySlot(slot))) {
                return true;
            }
        }

        if (isModLoaded("curios")) {
            if (invokeCuriosScanner(player)) {
                return true;
            }
        }

        if (isModLoaded("accessories")) {
            return invokeAccessoriesScanner(player);
        }

        return false;
    }

    private static boolean invokeCuriosScanner(ServerPlayer player) {
        try {
            Class<?> scanner = Class.forName("com.mawlee.cointcore.privilege.integrations.CuriosFlightScanner");
            return (boolean) scanner.getMethod("hasFlightItemEquipped", ServerPlayer.class).invoke(null, player);
        } catch (ReflectiveOperationException exception) {
            LOGGER.debug("Unable to inspect Curios flight items", exception);
            return false;
        }
    }

    private static boolean invokeAccessoriesScanner(ServerPlayer player) {
        try {
            Class<?> scanner = Class.forName("com.mawlee.cointcore.privilege.integrations.AccessoriesFlightScanner");
            return (boolean) scanner.getMethod("hasFlightItemEquipped", ServerPlayer.class).invoke(null, player);
        } catch (ReflectiveOperationException exception) {
            LOGGER.debug("Unable to inspect Accessories flight items", exception);
            return false;
        }
    }

    private static boolean hasFtbEssentialsFly(ServerPlayer player) {
        if (!ModList.get().isLoaded("ftbessentials")) {
            return false;
        }

        try {
            Class<?> dataClass = Class.forName("dev.ftb.mods.ftbessentials.util.FTBEPlayerData");
            Object optional = dataClass.getMethod("getOrCreate", net.minecraft.world.entity.player.Player.class).invoke(null, player);
            if (!(optional instanceof java.util.Optional<?> dataOptional) || dataOptional.isEmpty()) {
                return false;
            }

            Object data = dataOptional.get();
            return (boolean) data.getClass().getMethod("canFly").invoke(data);
        } catch (ReflectiveOperationException exception) {
            LOGGER.debug("Unable to inspect FTB Essentials flight state", exception);
            return false;
        }
    }

    private static boolean isModLoaded(String modId) {
        return LoadingModList.get().getMods().stream()
                .map(ModInfo::getModId)
                .anyMatch(modId::equals);
    }
}
