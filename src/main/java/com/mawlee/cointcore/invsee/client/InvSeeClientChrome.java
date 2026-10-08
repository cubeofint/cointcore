package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.invsee.InvSeeChromePayload;
import com.mawlee.cointcore.invsee.InvSeeCuriosLayoutPayload;
import com.mawlee.cointcore.invsee.InvSeeInfoPayload;
import com.mawlee.cointcore.invsee.InvSeeTab;
import com.mawlee.cointcore.invsee.InvSeeTabPolicy;
import com.mawlee.cointcore.invsee.menu.InvSeeCuriosMenu;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public final class InvSeeClientChrome {
    private static UUID targetId = new UUID(0, 0);
    private static boolean online;
    private static int tabMask;
    private static int activeTab;
    private static String displayName = "";
    private static List<String> infoLines = List.of();
    private static String infoKind = "";

    private InvSeeClientChrome() {
    }

    public static void applyChrome(InvSeeChromePayload payload) {
        targetId = payload.targetId();
        online = payload.online();
        tabMask = payload.tabMask();
        activeTab = payload.activeTab();
        displayName = payload.displayName() == null ? "" : payload.displayName();
        InvSeeTab tab = InvSeeTab.fromOrdinalOrInventory(activeTab);
        if (tab != InvSeeTab.FTB && tab != InvSeeTab.GRAVES && tab != InvSeeTab.STATE) {
            infoLines = List.of();
            infoKind = "";
        }
    }

    public static void applyInfo(InvSeeInfoPayload payload) {
        infoKind = payload.kind();
        infoLines = List.copyOf(payload.lines());
    }

    public static void applyCuriosLayout(InvSeeCuriosLayoutPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && minecraft.player.containerMenu instanceof InvSeeCuriosMenu menu
                && menu.containerId == payload.containerId()) {
            menu.applyClientLayout(payload.slots());
        }
    }

    public static UUID targetId() {
        return targetId;
    }

    public static boolean online() {
        return online;
    }

    public static int tabMask() {
        return tabMask;
    }

    public static int activeTab() {
        return activeTab;
    }

    public static String displayName() {
        return displayName;
    }

    public static List<InvSeeTab> tabs() {
        return InvSeeTabPolicy.visible(tabMask);
    }

    public static List<String> infoLines() {
        return infoLines;
    }

    public static String infoKind() {
        return infoKind;
    }
}
