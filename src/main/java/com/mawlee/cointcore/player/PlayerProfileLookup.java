package com.mawlee.cointcore.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Last-resort Mojang name → UUID lookup for never-seen players.
 * Uses the server profile repository when it is cheap to reach.
 */
final class PlayerProfileLookup {
    private PlayerProfileLookup() {
    }

    static Optional<UUID> lookupByName(MinecraftServer server, String name) {
        if (server == null || name == null || name.isBlank()) {
            return Optional.empty();
        }

        Optional<UUID> fromCache = server.getProfileCache()
                .get(name)
                .map(GameProfile::getId);
        if (fromCache.isPresent()) {
            return fromCache;
        }

        GameProfile profile = lookupViaRepository(server, name.trim());
        if (profile == null || profile.getId() == null) {
            return Optional.empty();
        }
        try {
            server.getProfileCache().add(profile);
        } catch (RuntimeException ignored) {
        }
        return Optional.of(profile.getId());
    }

    private static GameProfile lookupViaRepository(MinecraftServer server, String name) {
        try {
            Object services = servicesOf(server);
            if (services == null) {
                return null;
            }
            Method repositoryGetter = services.getClass().getMethod("profileRepository");
            Object repository = repositoryGetter.invoke(services);
            if (repository == null) {
                return null;
            }
            Class<?> callbackType = Class.forName("com.mojang.authlib.ProfileLookupCallback");
            AtomicReference<GameProfile> found = new AtomicReference<>();
            Object callback = Proxy.newProxyInstance(
                    callbackType.getClassLoader(),
                    new Class<?>[]{callbackType},
                    (proxy, method, args) -> {
                        if ("onProfileLookupSucceeded".equals(method.getName())
                                && args != null
                                && args.length >= 1
                                && args[0] instanceof GameProfile profile) {
                            found.set(profile);
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
            invokeFindProfiles(repository, callbackType, callback, name);
            return found.get();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static void invokeFindProfiles(
            Object repository,
            Class<?> callbackType,
            Object callback,
            String name
    ) throws ReflectiveOperationException {
        try {
            Method method = repository.getClass().getMethod("findProfilesByNames", String[].class, callbackType);
            method.invoke(repository, new String[]{name}, callback);
            return;
        } catch (NoSuchMethodException ignored) {
        }

        Class<?> agentType = Class.forName("com.mojang.authlib.Agent");
        Object minecraft = null;
        for (Object constant : agentType.getEnumConstants()) {
            if ("MINECRAFT".equals(String.valueOf(constant))) {
                minecraft = constant;
                break;
            }
        }
        Method method = repository.getClass().getMethod("findProfilesByNames", String[].class, agentType, callbackType);
        method.invoke(repository, new String[]{name}, minecraft, callback);
    }

    private static Object servicesOf(MinecraftServer server) {
        try {
            Method getter = server.getClass().getMethod("services");
            return getter.invoke(server);
        } catch (ReflectiveOperationException ignored) {
        }
        Class<?> type = server.getClass();
        while (type != null && type != Object.class) {
            try {
                Field field = type.getDeclaredField("services");
                field.setAccessible(true);
                return field.get(server);
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }
}
