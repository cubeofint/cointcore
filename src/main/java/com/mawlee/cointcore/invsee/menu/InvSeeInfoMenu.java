package com.mawlee.cointcore.invsee.menu;

import com.mawlee.cointcore.invsee.InvSeeFtbData;
import com.mawlee.cointcore.invsee.InvSeeGravesData;
import com.mawlee.cointcore.invsee.InvSeeInfoPayload;
import com.mawlee.cointcore.invsee.InvSeeMenus;
import com.mawlee.cointcore.invsee.InvSeeSection;
import com.mawlee.cointcore.invsee.InvSeeSession;
import com.mawlee.cointcore.invsee.InvSeeStateCollector;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class InvSeeInfoMenu extends InvSeeBaseMenu {
    public static final int CONTENT_SLOTS = 0;
    public static final int VIEWER_INVENTORY_Y = 124;
    private int syncTicker;

    public static InvSeeInfoMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        buf.readBoolean();
        return new InvSeeInfoMenu(containerId, inventory, null, true);
    }

    public InvSeeInfoMenu(int containerId, Inventory inventory, InvSeeSession session) {
        this(containerId, inventory, session, false);
    }

    private InvSeeInfoMenu(int containerId, Inventory inventory, InvSeeSession session, boolean clientSide) {
        super(InvSeeMenus.INFO.get(), containerId, inventory, session, CONTENT_SLOTS, clientSide);
        finishServerInit();
        if (!clientSide) {
            pushInfo();
        }
    }

    @Override
    protected void buildContentSlots() {
    }

    @Override
    public int viewerInventoryY() {
        return VIEWER_INVENTORY_Y;
    }

    @Override
    protected void refreshContent() {
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (clientSide || viewer == null) {
            return;
        }
        syncTicker++;
        if (syncTicker % 20 == 0) {
            pushInfo();
        }
    }

    private void pushInfo() {
        if (session == null || viewer == null) {
            return;
        }
        List<String> lines = linesFor(session.section(), session, viewer);
        PacketDistributor.sendToPlayer(viewer, new InvSeeInfoPayload(session.section().id(), lines));
    }

    public static List<String> linesFor(InvSeeSection section, InvSeeSession session, ServerPlayer viewer) {
        return switch (section) {
            case STATE -> InvSeeStateCollector.collect(session.target());
            case FTB -> InvSeeFtbData.collect(session.target());
            case GRAVES -> InvSeeGravesData.collect(session.target().getPlayer(), session.target().playerId());
            default -> List.of();
        };
    }
}
