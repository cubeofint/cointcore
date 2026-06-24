package com.mawlee.cointcore.permission;

import com.mawlee.cointcore.CointCore;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

public final class CointPermissionNodes {
    private static final PermissionNode.PermissionResolver<Boolean> OP_ONLY =
            (player, uuid, context) -> player != null && player.hasPermissions(2);

    private static final PermissionNode.PermissionResolver<Boolean> EVERYONE =
            (player, uuid, context) -> true;

    private static final PermissionNode.PermissionResolver<Boolean> DENY_BY_DEFAULT =
            (player, uuid, context) -> false;

    public static final PermissionNode<Boolean> VANISH = new PermissionNode<>(
            CointCore.MOD_ID, "vanish", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.vanish"),
            Component.translatable("permission.desc.cointcore.vanish")
    );

    public static final PermissionNode<Boolean> VANISH_MOBS = new PermissionNode<>(
            CointCore.MOD_ID, "vanish.mobs", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.vanish.mobs"),
            Component.translatable("permission.desc.cointcore.vanish.mobs")
    );

    public static final PermissionNode<Boolean> SEE_VANISHED = new PermissionNode<>(
            CointCore.MOD_ID, "vanish.see", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.vanish.see"),
            Component.translatable("permission.desc.cointcore.vanish.see")
    );

    public static final PermissionNode<Boolean> RELOAD = new PermissionNode<>(
            CointCore.MOD_ID, "reload", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.reload"),
            Component.translatable("permission.desc.cointcore.reload")
    );

    public static final PermissionNode<Boolean> NIGHT_VISION = new PermissionNode<>(
            CointCore.MOD_ID, "nv", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.nv"),
            Component.translatable("permission.desc.cointcore.nv")
    );

    public static final PermissionNode<Boolean> FLY = new PermissionNode<>(
            CointCore.MOD_ID, "fly", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.fly"),
            Component.translatable("permission.desc.cointcore.fly")
    );

    public static final PermissionNode<Boolean> GOD = new PermissionNode<>(
            CointCore.MOD_ID, "god", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.god"),
            Component.translatable("permission.desc.cointcore.god")
    );

    public static final PermissionNode<Boolean> MESSAGE = new PermissionNode<>(
            CointCore.MOD_ID, "msg", PermissionTypes.BOOLEAN, EVERYONE
    ).setInformation(
            Component.translatable("permission.name.cointcore.msg"),
            Component.translatable("permission.desc.cointcore.msg")
    );

    public static final PermissionNode<Boolean> IGNORE = new PermissionNode<>(
            CointCore.MOD_ID, "ignore", PermissionTypes.BOOLEAN, EVERYONE
    ).setInformation(
            Component.translatable("permission.name.cointcore.ignore"),
            Component.translatable("permission.desc.cointcore.ignore")
    );

    public static final PermissionNode<Boolean> MUTE = new PermissionNode<>(
            CointCore.MOD_ID, "mute", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.mute"),
            Component.translatable("permission.desc.cointcore.mute")
    );

    public static final PermissionNode<Boolean> UNMUTE = new PermissionNode<>(
            CointCore.MOD_ID, "unmute", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.unmute"),
            Component.translatable("permission.desc.cointcore.unmute")
    );

    public static final PermissionNode<Boolean> BAN = new PermissionNode<>(
            CointCore.MOD_ID, "ban", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.ban"),
            Component.translatable("permission.desc.cointcore.ban")
    );

    public static final PermissionNode<Boolean> TPL = new PermissionNode<>(
            CointCore.MOD_ID, "tpl", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.tpl"),
            Component.translatable("permission.desc.cointcore.tpl")
    );

    public static final PermissionNode<Boolean> WARN = new PermissionNode<>(
            CointCore.MOD_ID, "warn", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.warn"),
            Component.translatable("permission.desc.cointcore.warn")
    );

    public static final PermissionNode<Boolean> PUNISHMENTS = new PermissionNode<>(
            CointCore.MOD_ID, "punishments", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.punishments"),
            Component.translatable("permission.desc.cointcore.punishments")
    );

    public static final PermissionNode<Boolean> TURN_PVP = new PermissionNode<>(
            CointCore.MOD_ID, "turn_pvp", PermissionTypes.BOOLEAN, EVERYONE
    ).setInformation(
            Component.translatable("permission.name.cointcore.turn_pvp"),
            Component.translatable("permission.desc.cointcore.turn_pvp")
    );

    public static final PermissionNode<Boolean> KEEP_INVENTORY = new PermissionNode<>(
            CointCore.MOD_ID, "keep_inventory", PermissionTypes.BOOLEAN, DENY_BY_DEFAULT
    ).setInformation(
            Component.translatable("permission.name.cointcore.keep_inventory"),
            Component.translatable("permission.desc.cointcore.keep_inventory")
    );

    public static final PermissionNode<Boolean> CLAIM_FLAGS = new PermissionNode<>(
            CointCore.MOD_ID, "claim_flags", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.claim_flags"),
            Component.translatable("permission.desc.cointcore.claim_flags")
    );

    public static final PermissionNode<Boolean> KIT_CREDITS = new PermissionNode<>(
            CointCore.MOD_ID, "kit_credits", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.kit_credits"),
            Component.translatable("permission.desc.cointcore.kit_credits")
    );

    public static final PermissionNode<Boolean> VOTE_DAY = new PermissionNode<>(
            CointCore.MOD_ID, "vote.day", PermissionTypes.BOOLEAN, EVERYONE
    ).setInformation(
            Component.translatable("permission.name.cointcore.vote.day"),
            Component.translatable("permission.desc.cointcore.vote.day")
    );

    public static final PermissionNode<Boolean> VOTE_CLEAR_WEATHER = new PermissionNode<>(
            CointCore.MOD_ID, "vote.clear_weather", PermissionTypes.BOOLEAN, EVERYONE
    ).setInformation(
            Component.translatable("permission.name.cointcore.vote.clear_weather"),
            Component.translatable("permission.desc.cointcore.vote.clear_weather")
    );

    public static final PermissionNode<Boolean> RESTART = new PermissionNode<>(
            CointCore.MOD_ID, "restart", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.restart"),
            Component.translatable("permission.desc.cointcore.restart")
    );

    public static final PermissionNode<Boolean> CHAT_SPY = new PermissionNode<>(
            CointCore.MOD_ID, "spy", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.spy"),
            Component.translatable("permission.desc.cointcore.spy")
    );

    public static final PermissionNode<Boolean> CHUNK_LIMIT = new PermissionNode<>(
            CointCore.MOD_ID, "chunklimit", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.chunklimit"),
            Component.translatable("permission.desc.cointcore.chunklimit")
    );

    public static final PermissionNode<Boolean> CHUNK_LIMIT_BYPASS = new PermissionNode<>(
            CointCore.MOD_ID, "chunklimit.bypass", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.chunklimit.bypass"),
            Component.translatable("permission.desc.cointcore.chunklimit.bypass")
    );

    public static final PermissionNode<Boolean> ADMIN_CHAT = new PermissionNode<>(
            CointCore.MOD_ID, "adminchat", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.adminchat"),
            Component.translatable("permission.desc.cointcore.adminchat")
    );

    private CointPermissionNodes() {
    }
}
