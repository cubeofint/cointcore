package com.mawlee.cointcore.invsee.integrations;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Best-effort read-only graves listing for ATM10 grave mods (YIGD / Tombstone / Gravestone).
 */
public final class GravesInvSeeBridge {
    private GravesInvSeeBridge() {
    }

    public static List<String> listGraves(Player player, UUID playerId) {
        List<String> lines = new ArrayList<>();
        if (ModList.get().isLoaded("yigd")) {
            lines.addAll(tryYigd(playerId));
        }
        if (ModList.get().isLoaded("tombstone")) {
            lines.addAll(tryGeneric("ovh.corail.tombstone.helper.Helper", playerId, player));
        }
        if (ModList.get().isLoaded("gravestone")) {
            lines.addAll(tryGeneric("de.maxhenkel.gravestone.Main", playerId, player));
        }
        return lines;
    }

    private static List<String> tryYigd(UUID playerId) {
        String[] classes = {
                "com.b1n_ry.yigd.data.DeathInfoManager",
                "com.b1n_ry.yigd.Yigd",
                "com.b1n_ry.yigd.config.YigdConfig"
        };
        for (String className : classes) {
            List<String> found = invokeCollection(className, playerId);
            if (!found.isEmpty()) {
                return found;
            }
        }
        return List.of();
    }

    private static List<String> tryGeneric(String className, UUID playerId, Player player) {
        List<String> found = invokeCollection(className, playerId);
        if (!found.isEmpty()) {
            return found;
        }
        return invokeCollection(className, player);
    }

    private static List<String> invokeCollection(String className, Object arg) {
        try {
            Class<?> type = Class.forName(className);
            Object instance = instanceOrNull(type);
            for (Method method : type.getMethods()) {
                if (method.getParameterCount() != 1) {
                    continue;
                }
                if (!method.getParameterTypes()[0].isInstance(arg)) {
                    continue;
                }
                Object target = java.lang.reflect.Modifier.isStatic(method.getModifiers()) ? null : instance;
                if (target == null && !java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                Object result = method.invoke(target, arg);
                List<String> lines = stringifyCollection(result);
                if (!lines.isEmpty() && method.getName().toLowerCase().contains("grave")) {
                    return lines;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return List.of();
    }

    private static Object instanceOrNull(Class<?> type) {
        try {
            return type.getField("INSTANCE").get(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static List<String> stringifyCollection(Object result) {
        if (result instanceof Collection<?> collection) {
            List<String> lines = new ArrayList<>();
            for (Object item : collection) {
                if (item != null) {
                    lines.add(String.valueOf(item));
                }
            }
            return lines;
        }
        return List.of();
    }
}
