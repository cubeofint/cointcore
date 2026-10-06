package com.mawlee.cointcore.afk;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class AfkListMarker {
    public static final String TAG = "[AFK]";

    private AfkListMarker() {
    }

    public static Component tag() {
        return Component.literal(TAG).withStyle(ChatFormatting.GRAY);
    }

    public static Component append(Component name) {
        Component base = name == null ? Component.empty() : name;
        if (alreadyDecorated(base)) {
            return base;
        }
        MutableComponent decorated = base.copy();
        decorated.append(Component.literal(" "));
        decorated.append(tag());
        return decorated;
    }

    public static boolean alreadyDecorated(Component name) {
        return name != null && name.getString().contains(TAG);
    }
}
