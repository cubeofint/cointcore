package com.mawlee.cointcore.afk.client;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.afk.AfkGuiActivityPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Detects real GUI activity on the client and rate-limits heartbeats to the server.
 * Server remains the source of truth and only accepts heartbeats while a container is open.
 */
@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class AfkClientGuiActivity {
    private static final long SEND_COOLDOWN_MS = 2_000L;
    private static final double MOUSE_MOVE_THRESHOLD_PX = 8.0D;

    private static long lastSendMs;
    private static double lastMouseX = Double.NaN;
    private static double lastMouseY = Double.NaN;
    private static double pendingMouseDelta;
    private static boolean activityPending;

    private AfkClientGuiActivity() {
    }

    @SubscribeEvent
    public static void onMouseButton(ScreenEvent.MouseButtonPressed.Post event) {
        if (isTrackedContainerScreen(event.getScreen())) {
            markActivity();
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(ScreenEvent.MouseScrolled.Post event) {
        if (isTrackedContainerScreen(event.getScreen())) {
            markActivity();
        }
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Post event) {
        if (isTrackedContainerScreen(event.getScreen())) {
            markActivity();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isTrackedContainerOpen(minecraft)) {
            resetMouseSample();
            activityPending = false;
            return;
        }

        double mouseX = minecraft.mouseHandler.xpos();
        double mouseY = minecraft.mouseHandler.ypos();
        if (Double.isNaN(lastMouseX) || Double.isNaN(lastMouseY)) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        } else {
            double dx = mouseX - lastMouseX;
            double dy = mouseY - lastMouseY;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            pendingMouseDelta += Math.hypot(dx, dy);
            if (pendingMouseDelta >= MOUSE_MOVE_THRESHOLD_PX) {
                pendingMouseDelta = 0.0D;
                markActivity();
            }
        }

        if (activityPending) {
            trySendHeartbeat();
        }
    }

    private static void markActivity() {
        activityPending = true;
        trySendHeartbeat();
    }

    private static void trySendHeartbeat() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isTrackedContainerOpen(minecraft)) {
            activityPending = false;
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastSendMs < SEND_COOLDOWN_MS) {
            return;
        }

        lastSendMs = now;
        activityPending = false;
        PacketDistributor.sendToServer(AfkGuiActivityPayload.INSTANCE);
    }

    private static boolean isTrackedContainerOpen(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.getConnection() == null) {
            return false;
        }
        if (!(minecraft.screen instanceof AbstractContainerScreen<?>)) {
            return false;
        }
        return player.containerMenu != player.inventoryMenu;
    }

    private static boolean isTrackedContainerScreen(net.minecraft.client.gui.screens.Screen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(screen instanceof AbstractContainerScreen<?>)) {
            return false;
        }
        return player.containerMenu != player.inventoryMenu;
    }

    private static void resetMouseSample() {
        lastMouseX = Double.NaN;
        lastMouseY = Double.NaN;
        pendingMouseDelta = 0.0D;
    }
}
