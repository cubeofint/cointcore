package com.mawlee.cointcore.ftbessentials;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.UUID;

public final class FtbTranslatableFix {
    private FtbTranslatableFix() {
    }

    public static MutableComponent translatable(String key, Object... args) {
        if (args.length == 0) {
            return Component.translatable(key);
        }

        Object[] sanitized = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            sanitized[i] = sanitizeArg(args[i]);
        }
        return Component.translatable(key, sanitized);
    }

    private static Object sanitizeArg(Object arg) {
        if (arg instanceof UUID uuid) {
            return Component.literal(uuid.toString());
        }
        return arg;
    }
}
