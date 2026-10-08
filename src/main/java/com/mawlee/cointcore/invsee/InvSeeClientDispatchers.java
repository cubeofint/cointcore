package com.mawlee.cointcore.invsee;

import net.minecraft.resources.ResourceLocation;

/**
 * Keeps client GUI classes out of dedicated-server classloading.
 */
public final class InvSeeClientDispatchers {
    private InvSeeClientDispatchers() {
    }

    public static void applyChrome(InvSeeChromePayload payload) {
        invoke("applyChrome", InvSeeChromePayload.class, payload);
    }

    public static void applyInfo(InvSeeInfoPayload payload) {
        invoke("applyInfo", InvSeeInfoPayload.class, payload);
    }

    public static void applyCuriosLayout(InvSeeCuriosLayoutPayload payload) {
        invoke("applyCuriosLayout", InvSeeCuriosLayoutPayload.class, payload);
    }

    public static ResourceLocation curiosIcon(String identifier) {
        try {
            Class<?> client = Class.forName("com.mawlee.cointcore.invsee.client.InvSeeCuriosIcons");
            Object icon = client.getMethod("icon", String.class).invoke(null, identifier);
            return icon instanceof ResourceLocation location ? location : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void invoke(String method, Class<?> argType, Object arg) {
        try {
            Class<?> client = Class.forName("com.mawlee.cointcore.invsee.client.InvSeeClientChrome");
            client.getMethod(method, argType).invoke(null, arg);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
