package com.mawlee.cointcore.invsee;

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

    private static void invoke(String method, Class<?> argType, Object arg) {
        try {
            Class<?> client = Class.forName("com.mawlee.cointcore.invsee.client.InvSeeClientChrome");
            client.getMethod(method, argType).invoke(null, arg);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
