package com.mawlee.cointcore.invsee.integrations;

import com.mawlee.cointcore.invsee.InvSeeInfoLines;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only FTB Essentials snapshot via reflection so missing methods do not crash.
 */
public final class FtbEssentialsInvSeeBridge {
    private FtbEssentialsInvSeeBridge() {
    }

    public static List<String> collect(Player player) {
        UUID id = player == null ? null : player.getUUID();
        String name = player == null ? "" : player.getGameProfile().getName();
        return collect(player, id, name);
    }

    public static List<String> collect(Player player, UUID playerId, String fallbackName) {
        Object data = loadData(player, playerId);
        if (data == null) {
            return InvSeeInfoLines.ftb(true, fallbackName == null ? "" : fallbackName, List.of(), "");
        }
        String nick = stringInvoke(data, "getNick", "nick", "getName", "name");
        if (nick == null || nick.isBlank()) {
            nick = fallbackName == null ? "" : fallbackName;
        }
        String lastDeath = stringify(invoke(data, "getLastDeath", "lastDeath", "getLastDeathPoint"));
        List<String> homes = homes(data);
        return InvSeeInfoLines.ftb(true, nick, homes, lastDeath);
    }

    private static Object loadData(Player player, UUID playerId) {
        try {
            Class<?> dataClass = Class.forName("dev.ftb.mods.ftbessentials.util.FTBEPlayerData");
            if (playerId != null) {
                Object byId = invokeStatic(dataClass, playerId, "get", UUID.class);
                if (byId instanceof Optional<?> optional && optional.isPresent()) {
                    return optional.get();
                }
                if (byId != null && !(byId instanceof Optional<?>)) {
                    return byId;
                }
            }
            if (player != null) {
                Object created = invokeStatic(dataClass, player, "getOrCreate", Player.class);
                if (created instanceof Optional<?> optional) {
                    return optional.orElse(null);
                }
                if (created != null) {
                    return created;
                }
                return invokeStatic(dataClass, player.getUUID(), "get", UUID.class);
            }
            return null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static List<String> homes(Object data) {
        Object homes = invoke(data, "homes", "getHomes");
        List<String> lines = new ArrayList<>();
        if (homes instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                lines.add(String.valueOf(entry.getKey()) + " " + stringify(entry.getValue()));
            }
            return lines;
        }
        if (homes instanceof Collection<?> collection) {
            for (Object home : collection) {
                lines.add(stringify(home));
            }
        }
        return lines;
    }

    private static Object invokeStatic(Class<?> type, Object arg, String name, Class<?> argType) throws ReflectiveOperationException {
        try {
            Method method = type.getMethod(name, argType);
            return method.invoke(null, arg);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static String stringInvoke(Object data, String... names) {
        Object value = invoke(data, names);
        return value == null ? "" : String.valueOf(value);
    }

    private static Object invoke(Object data, String... names) {
        if (data == null) {
            return null;
        }
        for (String name : names) {
            try {
                Method method = data.getClass().getMethod(name);
                return method.invoke(data);
            } catch (ReflectiveOperationException ignored) {
                try {
                    return data.getClass().getField(name).get(data);
                } catch (ReflectiveOperationException ignoredField) {
                    // try next name
                }
            }
        }
        return null;
    }

    private static String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Optional<?> optional) {
            return optional.map(FtbEssentialsInvSeeBridge::stringify).orElse("");
        }
        String pos = posString(value);
        return pos.isEmpty() ? String.valueOf(value) : pos;
    }

    private static String posString(Object value) {
        Object dim = invoke(value, "dimension", "getDimension", "dim");
        Object x = invoke(value, "x", "getX");
        Object y = invoke(value, "y", "getY");
        Object z = invoke(value, "z", "getZ");
        Object pos = invoke(value, "getPos", "pos");
        if (pos != null && x == null) {
            x = invoke(pos, "getX", "x");
            y = invoke(pos, "getY", "y");
            z = invoke(pos, "getZ", "z");
        }
        if (dim == null && x == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        if (dim != null) {
            builder.append(dim);
            builder.append(' ');
        }
        if (x != null) {
            builder.append(x).append(' ').append(y).append(' ').append(z);
        }
        return builder.toString().trim();
    }
}
