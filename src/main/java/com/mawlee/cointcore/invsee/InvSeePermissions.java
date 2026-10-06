package com.mawlee.cointcore.invsee;

import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class InvSeePermissions {
    public static final String WEIGHT_META_KEY = "invsee-weight";

    private InvSeePermissions() {
    }

    public static boolean canUseCommand(ServerPlayer viewer) {
        InvSeeTriState[] sectionViews = new InvSeeTriState[InvSeeSection.values().length];
        InvSeeSection[] sections = InvSeeSection.values();
        for (int i = 0; i < sections.length; i++) {
            sectionViews[i] = triState(viewer, viewNode(sections[i]));
        }
        return InvSeePermissionPolicy.canUseCommand(triState(viewer, CointPermissionNodes.INVSEE), sectionViews);
    }

    public static boolean canView(ServerPlayer viewer, InvSeeSection section) {
        return InvSeePermissionPolicy.allows(
                triState(viewer, viewNode(section)),
                triState(viewer, CointPermissionNodes.INVSEE)
        );
    }

    public static boolean canEdit(ServerPlayer viewer, InvSeeSection section) {
        return canView(viewer, section) && InvSeePermissionPolicy.allows(
                triState(viewer, editNode(section)),
                triState(viewer, CointPermissionNodes.INVSEE_EDIT)
        );
    }

    public static boolean canOpenOffline(ServerPlayer viewer) {
        return InvSeePermissionPolicy.allows(
                triState(viewer, CointPermissionNodes.INVSEE_OFFLINE),
                triState(viewer, CointPermissionNodes.INVSEE)
        );
    }

    public static boolean canInspect(ServerPlayer viewer, MinecraftServer server, UUID targetId) {
        return InvSeePermissionPolicy.canInspect(
                triState(viewer, CointPermissionNodes.INVSEE_EXEMPT_BYPASS),
                triState(server, targetId, CointPermissionNodes.INVSEE_EXEMPT),
                weight(viewer),
                weight(server, targetId)
        );
    }

    public static int weight(ServerPlayer player) {
        return weight(player.server, player.getUUID(), player);
    }

    public static int weight(MinecraftServer server, UUID playerId) {
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        return weight(server, playerId, online);
    }

    private static int weight(MinecraftServer server, UUID playerId, ServerPlayer online) {
        int meta = LuckPermsIntegration.getMetaInt(playerId, WEIGHT_META_KEY, Integer.MIN_VALUE);
        if (meta != Integer.MIN_VALUE) {
            return meta;
        }

        OptionalInt groupWeight = LuckPermsIntegration.primaryGroupWeight(playerId);
        if (groupWeight.isPresent()) {
            return groupWeight.getAsInt();
        }

        if (online != null) {
            return PermissionService.integer(online, CointPermissionNodes.INVSEE_WEIGHT, opWeight(server, playerId));
        }
        return opWeight(server, playerId);
    }

    static PermissionNode<Boolean> viewNode(InvSeeSection section) {
        return switch (section) {
            case INVENTORY -> CointPermissionNodes.INVSEE_VIEW_INVENTORY;
            case ENDER -> CointPermissionNodes.INVSEE_VIEW_ENDER;
            case CURIOS -> CointPermissionNodes.INVSEE_VIEW_CURIOS;
            case COSMETIC -> CointPermissionNodes.INVSEE_VIEW_COSMETIC;
            case BACKPACK -> CointPermissionNodes.INVSEE_VIEW_BACKPACK;
            case POCKET -> CointPermissionNodes.INVSEE_VIEW_POCKET;
            case MODDATA -> CointPermissionNodes.INVSEE_VIEW_MODDATA;
        };
    }

    static PermissionNode<Boolean> editNode(InvSeeSection section) {
        return switch (section) {
            case INVENTORY -> CointPermissionNodes.INVSEE_EDIT_INVENTORY;
            case ENDER -> CointPermissionNodes.INVSEE_EDIT_ENDER;
            case CURIOS -> CointPermissionNodes.INVSEE_EDIT_CURIOS;
            case COSMETIC -> CointPermissionNodes.INVSEE_EDIT_COSMETIC;
            case BACKPACK -> CointPermissionNodes.INVSEE_EDIT_BACKPACK;
            case POCKET -> CointPermissionNodes.INVSEE_EDIT_POCKET;
            case MODDATA -> CointPermissionNodes.INVSEE_EDIT_MODDATA;
        };
    }

    private static InvSeeTriState triState(ServerPlayer player, PermissionNode<Boolean> node) {
        return triState(player.server, player.getUUID(), node, player);
    }

    private static InvSeeTriState triState(MinecraftServer server, UUID playerId, PermissionNode<Boolean> node) {
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        return triState(server, playerId, node, online);
    }

    private static InvSeeTriState triState(
            MinecraftServer server,
            UUID playerId,
            PermissionNode<Boolean> node,
            ServerPlayer online
    ) {
        if (LuckPermsIntegration.isAvailable()) {
            Optional<Boolean> luckPerms = LuckPermsIntegration.permissionTristate(
                    playerId,
                    CointPermissionNodes.luckPermsKey(node)
            );
            if (luckPerms.isPresent()) {
                return InvSeeTriState.ofBoolean(luckPerms.get());
            }
            if (online == null) {
                return InvSeeTriState.UNSET;
            }
        }

        if (online != null) {
            return InvSeeTriState.ofBoolean(PermissionService.has(online, node));
        }
        return InvSeeTriState.ofBoolean(isOp(server, playerId));
    }

    private static int opWeight(MinecraftServer server, UUID playerId) {
        GameProfile profile = profile(server, playerId);
        if (!server.getPlayerList().isOp(profile)) {
            return 0;
        }
        return Math.max(2, server.getProfilePermissions(profile)) * 10;
    }

    private static boolean isOp(MinecraftServer server, UUID playerId) {
        return server.getPlayerList().isOp(profile(server, playerId));
    }

    private static GameProfile profile(MinecraftServer server, UUID playerId) {
        return server.getProfileCache()
                .get(playerId)
                .orElseGet(() -> new GameProfile(playerId, ""));
    }
}
