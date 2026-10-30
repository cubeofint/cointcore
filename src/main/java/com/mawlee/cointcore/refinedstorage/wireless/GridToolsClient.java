package com.mawlee.cointcore.refinedstorage.wireless;

import com.mawlee.cointcore.CointCore;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.grid.screen.AbstractGridScreen;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CointCore.MOD_ID, value = Dist.CLIENT)
public final class GridToolsClient {
    private static GlobalPos marker;
    private static long markerUntil;
    private GridToolsClient() { }
    private static boolean available() { return ModList.get().isLoaded("refinedstorage") && ModList.get().isLoaded("universalgrid"); }

    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event) { if (available()) PinStorage.load(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        marker = null;
        if (available()) PinStorage.reset();
    }

    @SubscribeEvent public static void buttons(ScreenEvent.Init.Post event) {
        if (!available() || !(event.getScreen() instanceof AbstractGridScreen<?> screen)) return;
        PinStorage.load();
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        int top = screen.getMenu().slots.stream().filter(slot -> slot.container == player.getInventory())
                .mapToInt(slot -> slot.y).min().orElse(screen.getYSize() - 80);
        int x = screen.getGuiLeft() + screen.getXSize() - 96;
        int y = screen.getGuiTop() + top - 13;
        event.addListener(new PinRowsButton(x, y));
        event.addListener(Button.builder(Component.literal("↶"), ignored -> request(screen, new ItemResource(Items.AIR), GridToolsPackets.UNDO, 0))
                .bounds(x + 72, y, 18, 12).tooltip(Tooltip.create(Component.translatable("tooltip.cointcore.tools.undo"))).build());
    }

    @SubscribeEvent public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!available() || !(event.getScreen() instanceof AbstractGridScreen<?> screen)
                || (event.getButton() != 0 && event.getButton() != 1)) return;
        if (screen.children().stream().anyMatch(child -> child instanceof PinRowsButton button
                && button.isMouseOver(event.getMouseX(), event.getMouseY()))) return;
        var resource = screen.getCurrentGridResource();
        if (resource == null) return;
        var board = PinStorage.board();
        var carried = screen.getMenu().getCarried();
        if (resource instanceof RequestedGridResource pin) {
            if (event.getButton() == 1 && Screen.hasShiftDown()) {
                board.set(pin.section(), pin.slot(), null);
                PinStorage.save();
                event.setCanceled(true);
                return;
            }
            if (event.getButton() == 1 && pin.section() == PinBoard.Section.MANUAL && !carried.isEmpty()) {
                board.set(pin.section(), pin.slot(), ItemResource.ofItemStack(carried));
                PinStorage.save();
                event.setCanceled(true);
                return;
            }
            if (pin.key() == null) {
                // Empty ghost cells must never send a real insert/extract request to RS.
                event.setCanceled(true);
                return;
            }
        }
        var key = resource.getResourceForRecipeMods();
        if (key == null) return;
        if (event.getButton() == 1 && Screen.hasAltDown()) {
            request(screen, key, GridToolsPackets.FIND, 0);
            event.setCanceled(true);
        } else if (!(resource instanceof RequestedGridResource) && event.getButton() == 1 && carried.isEmpty()
                && !Screen.hasShiftDown() && !Screen.hasControlDown()) {
            board.addManual(key);
            PinStorage.save();
            event.setCanceled(true);
        }
    }

    public static void request(AbstractGridScreen<?> parent, PlatformResourceKey resource, int action, int index) {
        PacketDistributor.sendToServer(new GridToolsPackets.Request(parent.getMenu().containerId, resource, action, index));
    }
    public static void results(GridToolsPackets.Results results) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AbstractGridScreen<?> parent && parent.getMenu().containerId == results.menuId())
            minecraft.setScreen(new PatternFinderScreen(parent, results));
    }
    public static void highlight(GlobalPos position) {
        marker = position;
        markerUntil = System.currentTimeMillis() + 60_000;
        var minecraft = Minecraft.getInstance();
        if (minecraft.player != null) minecraft.player.displayClientMessage(Component.translatable("message.cointcore.finder.marker",
                position.pos().getX(), position.pos().getY(), position.pos().getZ(), position.dimension().location().toString()), false);
    }

    @SubscribeEvent public static void renderMarker(RenderLevelStageEvent event) {
        if (marker == null || System.currentTimeMillis() > markerUntil || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().equals(marker.dimension())
                || !minecraft.level.hasChunkAt(marker.pos()) || event.getCamera().getPosition().distanceToSqr(marker.pos().getCenter()) > 256 * 256) return;
        var camera = event.getCamera().getPosition();
        var buffers = minecraft.renderBuffers().bufferSource();
        LevelRenderer.renderLineBox(event.getPoseStack(), buffers.getBuffer(RenderType.lines()),
                new AABB(marker.pos()).inflate(0.03).move(-camera.x, -camera.y, -camera.z), 1f, 0.8f, 0.1f, 1f);
        buffers.endBatch(RenderType.lines());
    }
}
