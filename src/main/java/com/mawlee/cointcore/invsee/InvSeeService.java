package com.mawlee.cointcore.invsee;

import com.mawlee.cointcore.invsee.menu.InvSeeAttachmentMenu;
import com.mawlee.cointcore.invsee.menu.InvSeeBaseMenu;
import com.mawlee.cointcore.invsee.menu.InvSeeCuriosMenu;
import com.mawlee.cointcore.invsee.menu.InvSeeEnderMenu;
import com.mawlee.cointcore.invsee.menu.InvSeeInfoMenu;
import com.mawlee.cointcore.invsee.menu.InvSeeNestedMenu;
import com.mawlee.cointcore.invsee.menu.InvSeePlayerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class InvSeeService {
    private InvSeeService() {
    }

    public static boolean openTab(ServerPlayer viewer, InvSeeTarget target, InvSeeTab tab) {
        if (!InvSeePermissions.canView(viewer, tab.section())) {
            return false;
        }
        return switch (tab) {
            case INVENTORY -> openPlayer(viewer, target);
            case ENDER -> openEnder(viewer, target);
            case ACCESSORIES -> openAccessories(viewer, target);
            case FTB, GRAVES, STATE -> openInfo(viewer, target, tab.section());
        };
    }

    public static int tabMask(ServerPlayer viewer) {
        return InvSeeTabPolicy.mask(
                InvSeePermissions.canView(viewer, InvSeeSection.INVENTORY),
                InvSeePermissions.canView(viewer, InvSeeSection.ENDER),
                InvSeeMods.accessories(),
                InvSeePermissions.canView(viewer, InvSeeSection.ACCESSORIES),
                InvSeeMods.ftbEssentials(),
                InvSeePermissions.canView(viewer, InvSeeSection.FTB),
                InvSeeMods.graves(),
                InvSeePermissions.canView(viewer, InvSeeSection.GRAVES),
                InvSeePermissions.canView(viewer, InvSeeSection.STATE)
        );
    }

    public static void sendChrome(ServerPlayer viewer, InvSeeTarget target, InvSeeSection section) {
        PacketDistributor.sendToPlayer(
                viewer,
                new InvSeeChromePayload(
                        target.playerId(),
                        !target.isOffline(),
                        tabMask(viewer),
                        InvSeeTab.fromSection(section).ordinal(),
                        target.displayName() == null ? "" : target.displayName()
                )
        );
    }

    public static boolean openPlayer(ServerPlayer viewer, InvSeeTarget target) {
        InvSeeSession session = InvSeeSessions.begin(viewer, target, InvSeeSection.INVENTORY);
        boolean opened = open(viewer, title(target, "gui.cointcore.invsee.tab.player"),
                (id, inv, player) -> new InvSeePlayerMenu(id, inv, session),
                buf -> {
                    buf.writeBoolean(true);
                    buf.writeUUID(target.playerId());
                });
        if (opened) {
            sendChrome(viewer, target, InvSeeSection.INVENTORY);
        }
        return opened;
    }

    public static boolean openEnder(ServerPlayer viewer, InvSeeTarget target) {
        InvSeeSession session = InvSeeSessions.begin(viewer, target, InvSeeSection.ENDER);
        boolean opened = open(viewer, title(target, "gui.cointcore.invsee.tab.ender"),
                (id, inv, player) -> new InvSeeEnderMenu(id, inv, session),
                buf -> buf.writeBoolean(true));
        if (opened) {
            sendChrome(viewer, target, InvSeeSection.ENDER);
        }
        return opened;
    }

    public static boolean openAccessories(ServerPlayer viewer, InvSeeTarget target) {
        if (!InvSeeMods.accessories()) {
            return false;
        }
        InvSeeSession session = InvSeeSessions.begin(viewer, target, InvSeeSection.ACCESSORIES);
        boolean opened = openReflective(
                viewer,
                title(target, "gui.cointcore.invsee.tab.accessories"),
                "com.mawlee.cointcore.invsee.menu.InvSeeAccessoriesMenu",
                new Class<?>[] {int.class, Inventory.class, InvSeeSession.class},
                new Object[] {session},
                buf -> buf.writeBoolean(true)
        );
        if (opened) {
            sendChrome(viewer, target, InvSeeSection.ACCESSORIES);
        }
        return opened;
    }

    public static boolean openInfo(ServerPlayer viewer, InvSeeTarget target, InvSeeSection section) {
        InvSeeSession session = InvSeeSessions.begin(viewer, target, section);
        boolean opened = open(viewer, title(target, "gui.cointcore.invsee.tab." + section.id()),
                (id, inv, player) -> new InvSeeInfoMenu(id, inv, session),
                buf -> buf.writeBoolean(true));
        if (opened) {
            sendChrome(viewer, target, section);
            List<String> lines = InvSeeInfoMenu.linesFor(section, session, viewer);
            PacketDistributor.sendToPlayer(viewer, new InvSeeInfoPayload(section.id(), lines));
        }
        return opened;
    }

    public static boolean openNestedFromSlot(ServerPlayer viewer, int containerId, int slotIndex) {
        if (!(viewer.containerMenu instanceof InvSeeBaseMenu menu) || menu.containerId != containerId) {
            return false;
        }
        InvSeeSession session = menu.session();
        if (session == null || !menu.stillValid(viewer)) {
            return false;
        }
        ItemStack stack = menu.contentStack(slotIndex);
        InvSeeNestedKind kind = InvSeeItemContents.kind(stack);
        if (!kind.opensMenu()) {
            return false;
        }
        if (kind == InvSeeNestedKind.BACKPACK) {
            String key = backpackKeyForStack(session.target().getPlayer(), stack);
            return key != null && openBackpack(viewer, session.target(), key);
        }
        InvSeeSession nestedSession = InvSeeSessions.begin(viewer, session.target(), session.section());
        boolean opened = open(
                viewer,
                title(session.target(), "gui.cointcore.invsee.tab.nested"),
                (id, inv, player) -> new InvSeeNestedMenu(id, inv, nestedSession, stack),
                buf -> buf.writeBoolean(true)
        );
        if (opened) {
            sendChrome(viewer, session.target(), session.section());
        }
        return opened;
    }

    private static String backpackKeyForStack(Player target, ItemStack stack) {
        if (target == null) {
            return null;
        }
        for (InvSeePlayerStacks.LocatedStack located : InvSeePlayerStacks.collect(target, candidate -> candidate == stack)) {
            return InvSeePlayerStacks.encodeLocation(located);
        }
        return null;
    }

    public static boolean openCurios(ServerPlayer viewer, InvSeeTarget target) {
        if (!ModList.get().isLoaded("curios")) {
            return false;
        }
        InvSeeSession session = InvSeeSessions.begin(viewer, target, InvSeeSection.CURIOS);
        boolean opened = open(viewer, title(target, "gui.cointcore.invsee.tab.curios_all"),
                (id, inv, player) -> new InvSeeCuriosMenu(id, inv, session),
                buf -> buf.writeBoolean(true));
        if (opened) {
            sendChrome(viewer, target, InvSeeSection.CURIOS);
        }
        return opened;
    }

    public static boolean openCosmetic(ServerPlayer viewer, InvSeeTarget target) {
        if (!ModList.get().isLoaded("cosmeticarmorreworked")) {
            return false;
        }
        InvSeeSession session = InvSeeSessions.begin(viewer, target, InvSeeSection.COSMETIC);
        boolean opened = openReflective(
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
        if (opened) {
            sendChrome(viewer, target, InvSeeSection.COSMETIC);
        }
        return opened;
    }

    public static boolean openPocket(ServerPlayer viewer, InvSeeTarget target, UUID storageId) {
        if (!ModList.get().isLoaded("pocketstorage")) {
            return false;
        }
        InvSeeSession session = InvSeeSessions.begin(viewer, target, InvSeeSection.POCKET);
        boolean opened = openReflective(
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
        if (opened) {
            sendChrome(viewer, target, InvSeeSection.POCKET);
        }
        return opened;
    }

    public static boolean openBackpack(ServerPlayer viewer, InvSeeTarget target, String locationKey) {
        if (!ModList.get().isLoaded("sophisticatedbackpacks")) {
            return false;
        }
        InvSeeSession session = InvSeeSessions.begin(viewer, target, InvSeeSection.BACKPACK);
        boolean opened = openReflective(
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
        if (opened) {
            sendChrome(viewer, target, InvSeeSection.BACKPACK);
        }
        return opened;
    }

    public static boolean openAttachment(ServerPlayer viewer, InvSeeTarget target, String attachmentKey) {
        InvSeeSession session = InvSeeSessions.begin(viewer, target, InvSeeSection.MODDATA);
        boolean opened = open(viewer, title(target, "gui.cointcore.invsee.tab.attachment", shortAttachment(attachmentKey)),
                (id, inv, player) -> new InvSeeAttachmentMenu(id, inv, session, attachmentKey),
                buf -> {
                    buf.writeBoolean(true);
                    buf.writeUtf(attachmentKey);
                });
        if (opened) {
            sendChrome(viewer, target, InvSeeSection.MODDATA);
        }
        return opened;
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
