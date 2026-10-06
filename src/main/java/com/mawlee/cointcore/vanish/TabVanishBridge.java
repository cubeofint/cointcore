package com.mawlee.cointcore.vanish;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Notifies NEZNAMY TAB when vanish toggles so vanished players are dropped from
 * (or restored to) other players' tab lists.
 */
public final class TabVanishBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAB_MOD_ID = "tab";

    private TabVanishBridge() {
    }

    public static void notifyVanishChanged(ServerPlayer player) {
        if (player == null || !ModList.get().isLoaded(TAB_MOD_ID)) {
            return;
        }

        try {
            Class<?> tabClass = Class.forName("me.neznamy.tab.shared.TAB");
            Object tab = tabClass.getMethod("getInstance").invoke(null);
            if (tab == null) {
                return;
            }

            Method getPlayer = tabClass.getMethod("getPlayer", UUID.class);
            Object tabPlayer = getPlayer.invoke(tab, player.getUUID());
            if (tabPlayer == null) {
                return;
            }

            Class<?> tabPlayerClass = Class.forName("me.neznamy.tab.shared.platform.TabPlayer");
            Object featureManager = tabClass.getMethod("getFeatureManager").invoke(tab);
            featureManager.getClass()
                    .getMethod("onVanishStatusChange", tabPlayerClass)
                    .invoke(featureManager, tabPlayer);

            // PlayerList skips reformatting while vanished; force a refresh so [V] appears for staff.
            try {
                Class<?> tabApiClass = Class.forName("me.neznamy.tab.api.TabAPI");
                Object tabApi = tabApiClass.getMethod("getInstance").invoke(null);
                Object formatManager = tabApiClass.getMethod("getTabListFormatManager").invoke(tabApi);
                if (formatManager != null) {
                    formatManager.getClass()
                            .getMethod("formatPlayerForEveryone", tabPlayerClass, boolean.class)
                            .invoke(formatManager, tabPlayer, true);
                }
            } catch (ReflectiveOperationException ignored) {
                // Tablist formatting feature may be disabled.
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.debug("Failed to notify TAB of vanish change for {}", player.getGameProfile().getName(), exception);
        }
    }

    public static void hideFromViewer(ServerPlayer vanished, ServerPlayer viewer) {
        if (vanished == null || viewer == null || !ModList.get().isLoaded(TAB_MOD_ID)) {
            return;
        }

        try {
            Class<?> tabClass = Class.forName("me.neznamy.tab.shared.TAB");
            Object tab = tabClass.getMethod("getInstance").invoke(null);
            if (tab == null) {
                return;
            }

            Object viewerTab = tabClass.getMethod("getPlayer", UUID.class).invoke(tab, viewer.getUUID());
            if (viewerTab == null) {
                return;
            }

            Object tabList = viewerTab.getClass().getMethod("getTabList").invoke(viewerTab);
            tabList.getClass().getMethod("removeEntry", UUID.class).invoke(tabList, vanished.getUUID());
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.debug(
                    "Failed to remove vanished player {} from TAB of {}",
                    vanished.getGameProfile().getName(),
                    viewer.getGameProfile().getName(),
                    exception
            );
        }
    }
}
