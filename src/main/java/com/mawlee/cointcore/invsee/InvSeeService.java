package com.mawlee.cointcore.invsee;

import com.mawlee.cointcore.invsee.menu.InvSeeAttachmentMenu;
import com.mawlee.cointcore.invsee.menu.InvSeeCuriosMenu;
import com.mawlee.cointcore.invsee.menu.InvSeeEnderMenu;
import com.mawlee.cointcore.invsee.menu.InvSeePlayerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.fml.ModList;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class InvSeeService {
    private InvSeeService() {
    }

    public static boolean openPlayer(ServerPlayer viewer, InvSeeTarget target) {
        InvSeeSession session = InvSeeSessions.begin(viewer, target, InvSeeSection.INVENTORY);
        return open(viewer, title(target, "gui.cointcore.invsee.tab.player"),
                (id, inv, player) -> new InvSeePlayerMenu(id, inv, session),
                buf -> {
                    buf.writeBoolean(true);
                    buf.writeUUID(target.playerId());
                });
    }

    public static boolean openEnder(ServerPlayer viewer, InvSeeTarget target) {
        InvSeeSession session = InvSeeSessions.begin(viewer, target, InvSeeSection.ENDER);
        return open(viewer, title(target, "gui.cointcore.invsee.tab.ender"),
                (id, inv, player) -> new InvSeeEnderMenu(id, inv, session),
                buf -> buf.writeBoolean(true));
    }

    public static boolean openCurios(ServerPlayer viewer, InvSeeTarget target) {
        if (!ModList.get().isLoaded("curios")) {
            return false;
        }
        InvSeeSession session = InvSeeSessions.begin(viewer, target, InvSeeSection.CURIOS);
        return open(viewer, title(target, "gui.cointcore.invsee.tab.curios_all"),
                (id, inv, player) -> new InvSeeCuriosMenu(id, inv, session),
                buf -> buf.writeBoolean(true));
    }

    public static boolean openCosmetic(ServerPlayer viewer, InvSeeTarget target) {
        if (!ModList.get().isLoaded("cosmeticarmorreworked")) {
            return false;
        }
        InvSeeSession session = InvSeeSessions.begin(viewer, target, InvSeeSection.COSMETIC);
        return openReflective(
                viewer,
                Component.translatable("gui.cointcore.invsee.tab.cosmetic"),
                "com.mawlee.cointcore.invsee.menu.InvSeeCosmeticMenu",
                new Class<?>[] {int.class, Inventory.class, InvSeeSession.class},
                new Object[] {session},
                buf -> {
                    buf.writeBoolean(true);
                    buf.writeUUID(target.playerId());
                }
        );
    }

    public static boolean openPocket(ServerPlayer viewer, InvSeeTarget target, UUID storageId) {
        if (!ModList.get().isLoaded("pocketstorage")) {
            return false;
        }
        InvSeeSession session = InvSeeSessions.begin(viewer, target, InvSeeSection.POCKET);
        return openReflective(
                viewer,
                title(target, "gui.cointcore.invsee.tab.pocket_storage", shortId(storageId.toString())),
                "com.mawlee.cointcore.invsee.menu.InvSeePocketMenu",
                new Class<?>[] {int.class, Inventory.class, InvSeeSession.class, UUID.class},
                new Object[] {session, storageId},
                buf -> {
                    buf.writeBoolean(true);
                    buf.writeUUID(storageId);
                }
        );
    }

    public static boolean openBackpack(ServerPlayer viewer, InvSeeTarget target, String locationKey) {
        if (!ModList.get().isLoaded("sophisticatedbackpacks")) {
            return false;
        }
        InvSeeSession session = InvSeeSessions.begin(viewer, target, InvSeeSection.BACKPACK);
        return openReflective(
                viewer,
                title(target, "gui.cointcore.invsee.tab.backpack", shortKey(locationKey)),
                "com.mawlee.cointcore.invsee.menu.InvSeeBackpackMenu",
                new Class<?>[] {int.class, Inventory.class, InvSeeSession.class, String.class},
                new Object[] {session, locationKey},
                buf -> {
                    buf.writeBoolean(true);
                    buf.writeUtf(locationKey);
                }
        );
    }

    public static boolean openAttachment(ServerPlayer viewer, InvSeeTarget target, String attachmentKey) {
        InvSeeSession session = InvSeeSessions.begin(viewer, target, InvSeeSection.MODDATA);
        return open(viewer, title(target, "gui.cointcore.invsee.tab.attachment", shortAttachment(attachmentKey)),
                (id, inv, player) -> new InvSeeAttachmentMenu(id, inv, session, attachmentKey),
                buf -> {
                    buf.writeBoolean(true);
                    buf.writeUtf(attachmentKey);
                });
    }

    public static UUID resolveDefaultPocket(InvSeeTarget target) {
        List<String> ids = InvSeeDiscover.pocketIds(target.getPlayer());
        if (ids.isEmpty()) {
            return null;
        }
        return UUID.fromString(ids.getFirst());
    }

    public static String resolveDefaultBackpack(InvSeeTarget target) {
        List<String> keys = InvSeeDiscover.backpackKeys(target.getPlayer());
        return keys.isEmpty() ? null : keys.getFirst();
    }

    public static String resolveDefaultAttachment(InvSeeTarget target, ServerPlayer viewer) {
        List<String> keys = InvSeeDiscover.attachmentKeys(target.getPlayer(), viewer.registryAccess());
        return keys.isEmpty() ? null : keys.getFirst();
    }

    private static boolean open(
            ServerPlayer viewer,
            Component title,
            MenuFactory factory,
            Consumer<RegistryFriendlyByteBuf> extra
    ) {
        viewer.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return factory.create(containerId, inventory, player);
            }
        }, extra);
        return true;
    }

    private static boolean openReflective(
            ServerPlayer viewer,
            Component title,
            String menuClass,
            Class<?>[] ctorTypes,
            Object[] trailingArgs,
            Consumer<RegistryFriendlyByteBuf> extra
    ) {
        return open(viewer, title, (id, inv, player) -> {
            try {
                Class<?> cls = Class.forName(menuClass);
                Object[] args = new Object[trailingArgs.length + 2];
                args[0] = id;
                args[1] = inv;
                System.arraycopy(trailingArgs, 0, args, 2, trailingArgs.length);
                return (AbstractContainerMenu) cls.getConstructor(ctorTypes).newInstance(args);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to create " + menuClass, exception);
            }
        }, extra);
    }

    private static Component title(InvSeeTarget target, String sectionKey, Object... args) {
        Component section = args.length == 0
                ? Component.translatable(sectionKey)
                : Component.translatable(sectionKey, args);
        return Component.translatable(
                "gui.cointcore.invsee.title_section",
                target.displayName(),
                section
        );
    }

    private static String shortId(String id) {
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    private static String shortKey(String key) {
        int last = key.lastIndexOf('|');
        if (last >= 0 && last < key.length() - 1) {
            return key.substring(last + 1);
        }
        return key;
    }

    private static String shortAttachment(String key) {
        int colon = key.indexOf(':');
        if (colon >= 0 && colon < key.length() - 1) {
            return key.substring(colon + 1);
        }
        return key;
    }

    @FunctionalInterface
    private interface MenuFactory {
        AbstractContainerMenu create(int id, Inventory inventory, Player player);
    }
}
