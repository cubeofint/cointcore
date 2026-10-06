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

    public static final PermissionNode<Boolean> CLAIM_FLAG_MOB_SPAWN = new PermissionNode<>(
            CointCore.MOD_ID, "claim_flag.mob_spawn", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.claim_flag.mob_spawn"),
            Component.translatable("permission.desc.cointcore.claim_flag.mob_spawn")
    );

    public static final PermissionNode<Boolean> CLAIM_FLAG_MOB_DAMAGE = new PermissionNode<>(
            CointCore.MOD_ID, "claim_flag.mob_damage", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.claim_flag.mob_damage"),
            Component.translatable("permission.desc.cointcore.claim_flag.mob_damage")
    );

    public static final PermissionNode<Boolean> CLAIM_FLAG_FIRE_SPREAD = new PermissionNode<>(
            CointCore.MOD_ID, "claim_flag.fire_spread", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.claim_flag.fire_spread"),
            Component.translatable("permission.desc.cointcore.claim_flag.fire_spread")
    );

    public static final PermissionNode<Boolean> CLAIM_FLAG_PVP = new PermissionNode<>(
            CointCore.MOD_ID, "claim_flag.pvp", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.claim_flag.pvp"),
            Component.translatable("permission.desc.cointcore.claim_flag.pvp")
    );

    public static final PermissionNode<Boolean> CLAIM_FLAG_ENTRY = new PermissionNode<>(
            CointCore.MOD_ID, "claim_flag.entry", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.claim_flag.entry"),
            Component.translatable("permission.desc.cointcore.claim_flag.entry")
    );

    public static final PermissionNode<Boolean> CLAIM_FLAG_ENTRY_BYPASS = new PermissionNode<>(
            CointCore.MOD_ID, "claim_flag.entry.bypass", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.claim_flag.entry.bypass"),
            Component.translatable("permission.desc.cointcore.claim_flag.entry.bypass")
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

    public static final PermissionNode<Boolean> FLUX_ADMIN = new PermissionNode<>(
            CointCore.MOD_ID, "flux.admin", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.flux.admin"),
            Component.translatable("permission.desc.cointcore.flux.admin")
    );

    public static final PermissionNode<Boolean> AFK_BYPASS = new PermissionNode<>(
            CointCore.MOD_ID, "afk.bypass", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.afk.bypass"),
            Component.translatable("permission.desc.cointcore.afk.bypass")
    );

    public static final PermissionNode<Boolean> AFK_ALERTS = new PermissionNode<>(
            CointCore.MOD_ID, "afk.alerts", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.afk.alerts"),
            Component.translatable("permission.desc.cointcore.afk.alerts")
    );

    public static final PermissionNode<Boolean> INVSEE = new PermissionNode<>(
            CointCore.MOD_ID, "invsee", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.invsee"),
            Component.translatable("permission.desc.cointcore.invsee")
    );

    public static final PermissionNode<Boolean> INVSEE_EDIT = new PermissionNode<>(
            CointCore.MOD_ID, "invsee.edit", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.invsee.edit"),
            Component.translatable("permission.desc.cointcore.invsee.edit")
    );

    public static final PermissionNode<Boolean> STARTER_KIT = new PermissionNode<>(
            CointCore.MOD_ID, "starter", PermissionTypes.BOOLEAN, EVERYONE
    ).setInformation(
            Component.translatable("permission.name.cointcore.starter"),
            Component.translatable("permission.desc.cointcore.starter")
    );

    public static final PermissionNode<Boolean> STARTER_KIT_ADMIN = new PermissionNode<>(
            CointCore.MOD_ID, "starter.admin", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.starter.admin"),
            Component.translatable("permission.desc.cointcore.starter.admin")
    );

    public static final PermissionNode<Boolean> CLAIM_BUFFER_BYPASS = new PermissionNode<>(
            CointCore.MOD_ID, "claim.buffer.bypass", PermissionTypes.BOOLEAN, OP_ONLY
    ).setInformation(
            Component.translatable("permission.name.cointcore.claim.buffer.bypass"),
            Component.translatable("permission.desc.cointcore.claim.buffer.bypass")
    );

    private CointPermissionNodes() {
    }
}
